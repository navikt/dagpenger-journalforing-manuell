package no.nav.dagpenger.journalføring.manuell

import mu.KotlinLogging
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.hasBehandlendeEnhet
import no.nav.dagpenger.events.isAnnet
import no.nav.dagpenger.oidc.StsOidcClient
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topics.INNGÅENDE_JOURNALPOST
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import java.time.LocalDate
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class JournalføringManuell(val env: Environment, private val gsakClient: GsakClient) : Service() {

    override val SERVICE_APP_ID = "journalføring-manuell" // NB: also used as group.id for the consumer group - do not change!

    override val HTTP_PORT: Int = env.httpPort ?: super.HTTP_PORT

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val env = Environment()
            val service = JournalføringManuell(
                    env, GsakHttpClient(env.gsakOppgaveUrl, StsOidcClient(env.oicdStsUrl, env.username, env.password)))
            service.start()
        }
    }

    override fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        val inngåendeJournalposter = builder.consumeTopic(INNGÅENDE_JOURNALPOST, env.schemaRegistryUrl)

        inngåendeJournalposter
            .peek { key, value -> LOGGER.info("Processing behov with id ${value.getBehovId()}") }
            .filter { _, behov -> shouldBeProcessed(behov) }
            .foreach { _, value -> createManuellJournalføringsoppgave(value) }
        return builder.build()
    }

    override fun getConfig(): Properties {
        val props = streamConfig(appId = SERVICE_APP_ID, bootStapServerUrl = env.bootstrapServersUrl, credential = KafkaCredential(env.username, env.password))
        return props
    }

    fun createManuellJournalføringsoppgave(behov: Behov) {
        val response = gsakClient.createManuellJournalføringsoppgave(
                ManuellJournalføringsoppgaveRequest(
                        aktivDato = LocalDate.now().toString(),
                        fristFerdigstillelse = LocalDate.now().toString(),
                        prioritet = Prioritet.NORM),
                behov.getBehovId())

        LOGGER.info("Created manuell journalføringsoppgave with id ${response.id}")
    }
}

fun shouldBeProcessed(behov: Behov): Boolean =
        behov.getTrengerManuellBehandling() || (behov.isAnnet() && behov.hasBehandlendeEnhet())
