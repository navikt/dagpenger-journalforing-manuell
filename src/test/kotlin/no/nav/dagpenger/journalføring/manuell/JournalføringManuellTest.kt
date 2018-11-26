package no.nav.dagpenger.journalføring.manuell

import no.nav.dagpenger.events.avro.Annet
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.Ettersending
import no.nav.dagpenger.events.avro.HenvendelsesType
import no.nav.dagpenger.events.avro.Søknad
import org.junit.Test
import kotlin.test.assertFalse

class JournalføringManuellTest {

    @Test
    fun `Process messages with trengerManuellBehandling flag set`() {
        val behovAnnet = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setAnnet(Annet()).build())
                .setTrengerManuellBehandling(true)
                .build()

        val behovNy = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setSøknad(Søknad()).build())
                .setTrengerManuellBehandling(true)
                .build()

        assert(shouldBeProcessed(behovAnnet))
        assert(shouldBeProcessed(behovNy))
    }

    @Test
    fun `Process behov with hendenvendesType Annet and with behandlendeEnhet  `() {

        val behov = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setAnnet(Annet()).build())
                .setBehandleneEnhet("beh")
                .build()

        assert(shouldBeProcessed(behov))
    }

    @Test
    fun `Do not process behov missing behandlendeEnhet `() {
        val behovMissing = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setAnnet(Annet()).build())
                .build()

        assertFalse(shouldBeProcessed(behovMissing))
    }

    @Test
    fun `Do not process behov with henvendelsesType other than Annet `() {
        val behovSøknad = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setSøknad(Søknad()).build())
                .build()

        val behovEttersending = Behov
                .newBuilder()
                .setBehovId("000")
                .setHenvendelsesType(HenvendelsesType.newBuilder().setEttersending(Ettersending()).build())
                .build()

        assertFalse(shouldBeProcessed(behovSøknad))
        assertFalse(shouldBeProcessed(behovEttersending))
    }
}