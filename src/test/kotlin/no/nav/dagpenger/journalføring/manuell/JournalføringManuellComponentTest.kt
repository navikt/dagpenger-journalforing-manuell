package no.nav.dagpenger.journalføring.manuell

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import mu.KotlinLogging
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.common.embeddedutils.getAvailablePort
import no.nav.dagpenger.events.avro.Annet
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.Ettersending
import no.nav.dagpenger.events.avro.Journalpost
import no.nav.dagpenger.events.avro.Mottaker
import no.nav.dagpenger.events.avro.Søknad
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.Topics.INNGÅENDE_JOURNALPOST
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.lang.Thread.sleep
import java.util.Properties
import kotlin.test.assertEquals

class JournalføringRutingComponentTest {

    private val LOGGER = KotlinLogging.logger {}

    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = false,
            withSchemaRegistry = true,
            withSecurity = true,
            topics = listOf(Topics.INNGÅENDE_JOURNALPOST.name)
        )

        @BeforeClass
        @JvmStatic
        fun setup() {
            embeddedEnvironment.start()
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            embeddedEnvironment.tearDown()
        }
    }

    @Test
    fun ` embedded kafka cluster is up and running `() {
        kotlin.test.assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }

    @Test
    fun ` Component test of JournalføringManuell `() {

        //Test data: (hasBehandlendeEnhet, henvendelsesType)
        val testData = listOf(
            Pair(true, Søknad()),
            Pair(true, Annet()),
            Pair(true, Søknad()),
            Pair(true, Søknad()),
            Pair(true, Søknad()),
            Pair(true, Annet()),
            Pair(true, Annet()),
            Pair(true, Annet()),
            Pair(false, Annet()),
            Pair(true, Ettersending())
        )

        val behovsToProcess = testData.filter { it.first && it.second is Annet }.size

        // given an environment
        val env = Environment(
            gsakOppgaveUrl = "local",
            oicdStsUrl = "local",
            username = username,
            password = password,
            bootstrapServersUrl = embeddedEnvironment.brokersURL,
            schemaRegistryUrl = embeddedEnvironment.schemaRegistry!!.url,
            httpPort = getAvailablePort()
        )

        val dummyGsakClient = DummyGsakClient()
        val manuell = JournalføringManuell(env, dummyGsakClient)

        //produce behov...

        val behovProducer = behovProducer(env)

        manuell.start()

        testData.forEach { data ->
            val innkommendeBehov = Behov.newBuilder()
                .setBehovId("123")
                .setMottaker(Mottaker("12345678912"))
                .setBehandleneEnhet(if (data.first) "behandleneEnhet" else null)
                .setHenvendelsesType(data.second)
                .setJournalpost(
                    Journalpost
                        .newBuilder()
                        .setJournalpostId("12345")
                        .build()
                )
                .build()

            val record = behovProducer.send(ProducerRecord(INNGÅENDE_JOURNALPOST.name, innkommendeBehov)).get()
            LOGGER.info { "Produced -> ${record.topic()}  to offset ${record.offset()}" }
        }

        //Wait for manuell to consume behov
        sleep(5000)

        manuell.stop()

        assertEquals(behovsToProcess, dummyGsakClient.oppgaverCreated)
    }

    class DummyGsakClient : GsakClient {
        var oppgaverCreated = 0

        override fun createManuellJournalføringsoppgave(
            request: ManuellJournalføringsoppgaveRequest,
            correlationId: String
        ): ManuellJournalføringsoppgaveResponse {
            oppgaverCreated++
            return ManuellJournalføringsoppgaveResponse(
                versjon = 1,
                aktivDato = "2018-12-14",
                prioritet = Prioritet.NORM,
                status = "UNDER_BEHANDLING"
            )
        }
    }

    private fun behovProducer(env: Environment): KafkaProducer<String, Behov> {
        val producer: KafkaProducer<String, Behov> = KafkaProducer(Properties().apply {
            put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, env.schemaRegistryUrl)
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
            put(ProducerConfig.CLIENT_ID_CONFIG, "dummy-behov-producer")
            put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                Topics.INNGÅENDE_JOURNALPOST.keySerde.serializer().javaClass.name
            )
            put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                Topics.INNGÅENDE_JOURNALPOST.valueSerde.serializer().javaClass.name
            )
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${env.username}\" password=\"${env.password}\";"
            )
        })

        return producer
    }
}