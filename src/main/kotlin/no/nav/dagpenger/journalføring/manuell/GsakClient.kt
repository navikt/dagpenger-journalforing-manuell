package no.nav.dagpenger.journalføring.manuell

interface GsakClient {
    fun createManuellJournalføringsoppgave(request: ManuellJournalføringsoppgaveRequest, correlationId: String): ManuellJournalføringsoppgaveResponse
}