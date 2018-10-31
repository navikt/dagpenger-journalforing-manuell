package no.nav.dagpenger.journalføring.manuell

import mu.KotlinLogging
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.JournalpostType
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topics.INNGÅENDE_JOURNALPOST
import no.nav.dagpenger.streams.consumeTopic
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder

private val LOGGER = KotlinLogging.logger {}

private val gsakUrl = getEnvVar("GSAK_OPPGAVER_URL")

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

class JournalføringManuell(private val gsakHttpClient: GsakHttpClient) : Service() {
    override val SERVICE_APP_ID = "journalføring-manuell"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val service = JournalføringManuell(GsakHttpClient(gsakUrl))
            service.start()
        }
    }

    override fun setupStreams(): KafkaStreams {
        println(SERVICE_APP_ID)
        val builder = StreamsBuilder()

        val inngåendeJournalposter = builder.consumeTopic(INNGÅENDE_JOURNALPOST)

        inngåendeJournalposter
                .peek { key, value -> LOGGER.info("Processing ${value.javaClass} with key $key") }
                .filter { _, behov -> behov.getJournalpost().getJournalpostType() == JournalpostType.MANUELL ||
                        behov.getJournalpost().getJournalpostType() == JournalpostType.UKJENT }
                .foreach { _, value -> createManuellJournalføringsoppgave(value) }

        return KafkaStreams(builder.build(), this.getConfig())
    }

    fun createManuellJournalføringsoppgave(behov: Behov) {
        val response = gsakHttpClient.createManuellJournalføringsoppgave(ManuellJournalføringsoppgaveRequest(
                aktivDato = "",
                fristFerdigstillelse = "",
                prioritet = Prioritet.NORM))

        LOGGER.info("Created manuell journalføringsoppgave with id ${response.id}")
    }
}