package no.nav.dagpenger.journalføring.manuell

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import no.nav.dagpenger.oidc.OidcClient

class GsakHttpClient(private val gsakUrl: String, private val oidcClient: OidcClient) : GsakClient {

    override fun createManuellJournalføringsoppgave(
        request: ManuellJournalføringsoppgaveRequest,
        correlationId: String
    ): ManuellJournalføringsoppgaveResponse {
        val url = "${gsakUrl}v1/oppgaver"
        val json = Gson().toJson(request).toString()
        val (_, response, result) = with(url.httpPost().body(json)) {
            header("Authorization" to oidcClient.oidcToken().access_token.toBearerToken())
            header("X-Correlation-ID" to correlationId)
            responseObject<ManuellJournalføringsoppgaveResponse>()
        }

        return when (result) {
            is Result.Failure -> throw ManuellException(
                    response.statusCode, response.responseMessage, result.getException())
            is Result.Success -> result.get()
        }
    }
}

fun String.toBearerToken() = "Bearer $this"

data class ManuellJournalføringsoppgaveRequest(
    val tildeltEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val aktoerId: String? = null,
    val journalpostId: String? = null,
    val journalpostkilde: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val saksreferanse: String? = null,
    val orgnr: String? = null,
    val bnr: String? = null,
    val samhandlernr: String? = null,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val temagruppe: String? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val mappeId: Int? = null,
    val aktivDato: String,
    val fristFerdigstillelse: String,
    val prioritet: Prioritet,
    val metadata: Map<String, String>? = null
)

data class ManuellJournalføringsoppgaveResponse(
    val id: Int? = null,
    val tildeltEnhetsnr: String? = null,
    val endretAvEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val journalpostkilde: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val saksreferanse: String? = null,
    val bnr: String? = null,
    val samhandlernr: String? = null,
    val aktoerId: String? = null,
    val orgnr: String? = null,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val temagruppe: String? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val versjon: Int,
    val mappeId: Int? = null,
    val fristFerdigstillelse: String? = null,
    val aktivDato: String,
    val opprettetTidspunkt: String? = null,
    val opprettetAv: String? = null,
    val endretAv: String? = null,
    val ferdigstiltTidspunkt: String? = null,
    val endretTidspunkt: String? = null,
    val prioritet: Prioritet,
    val status: String,
    val metadata: Map<String, String>? = null
)

enum class Prioritet {
    HOY, NORM, LAV
}

class ManuellException(val statusCode: Int, override val message: String, override val cause: Throwable) : RuntimeException(message, cause)
