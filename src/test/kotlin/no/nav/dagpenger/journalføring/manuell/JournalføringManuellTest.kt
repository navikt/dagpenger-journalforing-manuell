package no.nav.dagpenger.journalføring.manuell

import no.nav.dagpenger.events.avro.Annet
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.Ettersending
import no.nav.dagpenger.events.avro.Søknad
import org.junit.Test
import kotlin.test.assertFalse

class JournalføringManuellTest {

    @Test
    fun `Process messages with trengerManuellBehandling flag set`() {
        val behovAnnet = Behov
            .newBuilder()
            .setBehovId("000")
            .setHenvendelsesType(Annet())
            .setTrengerManuellBehandling(true)
            .build()

        val behovNy = Behov
            .newBuilder()
            .setBehovId("000")
            .setHenvendelsesType(Søknad())
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
            .setHenvendelsesType(Annet())
            .setBehandleneEnhet("beh")
            .build()

        assert(shouldBeProcessed(behov))
    }

    @Test
    fun `Do not process behov missing behandlendeEnhet `() {
        val behovMissing = Behov
            .newBuilder()
            .setBehovId("000")
            .setHenvendelsesType(Annet())
            .build()

        assertFalse(shouldBeProcessed(behovMissing))
    }

    @Test
    fun `Do not process behov with henvendelsesType other than Annet `() {
        val behovSøknad = Behov
            .newBuilder()
            .setBehovId("000")
            .setHenvendelsesType(Søknad())
            .build()

        val behovEttersending = Behov
            .newBuilder()
            .setBehovId("000")
            .setHenvendelsesType(Ettersending())
            .build()

        assertFalse(shouldBeProcessed(behovSøknad))
        assertFalse(shouldBeProcessed(behovEttersending))
    }
}