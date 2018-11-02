package no.nav.dagpenger.journalføring.manuell

import mu.KotlinLogging
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.JournalpostType
import no.nav.dagpenger.oidc.StsOidcClient
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topics.INNGÅENDE_JOURNALPOST
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import java.time.LocalDate
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class JournalføringManuell(val env: Environment, private val gsakHttpClient: GsakHttpClient) : Service() {
    override val SERVICE_APP_ID = "journalføring-manuell" // NB: also used as group.id for the consumer group - do not change!

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val env = Environment()
            val service = JournalføringManuell(
                    env, GsakHttpClient(env.gsakUrl, StsOidcClient(env.oicdStsUrl, env.username, env.password)))
            service.start()
        }
    }

    override fun setupStreams(): KafkaStreams {
        println(SERVICE_APP_ID)
        val builder = StreamsBuilder()

        val inngåendeJournalposter = builder.consumeTopic(INNGÅENDE_JOURNALPOST)

        inngåendeJournalposter
                .peek { key, value -> LOGGER.info("Processing ${value.javaClass} with key $key") }
                .filter { _, behov -> behov.getJournalpost().getBehandleneEnhet() != null }
                .filter { _, behov -> behov.getJournalpost().getJournalpostType() == JournalpostType.MANUELL ||
                        behov.getJournalpost().getJournalpostType() == JournalpostType.UKJENT }
                .foreach { _, value -> createManuellJournalføringsoppgave(value) }

        return KafkaStreams(builder.build(), this.getConfig())
    }

    override fun getConfig(): Properties {
        return streamConfig(appId = SERVICE_APP_ID, bootStapServerUrl = env.bootstrapServersUrl, credential = KafkaCredential(env.username, env.password))
    }

    fun createManuellJournalføringsoppgave(behov: Behov) {
        val response = gsakHttpClient.createManuellJournalføringsoppgave(ManuellJournalføringsoppgaveRequest(
                aktivDato = LocalDate.now().toString(),
                fristFerdigstillelse = LocalDate.now().toString(),
                prioritet = Prioritet.NORM))

        LOGGER.info("Created manuell journalføringsoppgave with id ${response.id}")
    }
}