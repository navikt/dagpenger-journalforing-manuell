package no.nav.dagpenger.journalføring.manuell

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import no.nav.dagpenger.oidc.OidcClient

class GsakHttpClient(private val gsakUrl: String, private val oidcClient: OidcClient) {

    fun createManuellJournalføringsoppgave(request: ManuellJournalføringsoppgaveRequest): ManuellJournalføringsoppgaveResponse {
        val url = "${gsakUrl}v1/oppgaver"
        val json = Gson().toJson(request).toString()
        val (_, response, result) = with(url.httpPost().body(json)) {
            header("Authorization" to oidcClient.oidcToken().access_token.toBearerToken())
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
    val id: Int?,
    val tildeltEnhetsnr: String?,
    val endretAvEnhetsnr: String?,
    val opprettetAvEnhetsnr: String?,
    val journalpostId: String?,
    val journalpostkilde: String?,
    val behandlesAvApplikasjon: String?,
    val saksreferanse: String?,
    val bnr: String?,
    val samhandlernr: String?,
    val aktoerId: String?,
    val orgnr: String?,
    val tilordnetRessurs: String?,
    val beskrivelse: String?,
    val temagruppe: String?,
    val tema: String?,
    val behandlingstema: String?,
    val oppgavetype: String?,
    val behandlingstype: String?,
    val versjon: Int,
    val mappeId: Int?,
    val fristFerdigstillelse: String,
    val aktivDato: String,
    val opprettetTidspunkt: String?,
    val opprettetAv: String?,
    val endretAv: String?,
    val ferdigstiltTidspunkt: String?,
    val endretTidspunkt: String?,
    val prioritet: Prioritet,
    val status: String,
    val metadata: Map<String, String>? = null
)

enum class Prioritet {
    HOY, NORM, LAV
}

class ManuellException(val statusCode: Int, override val message: String, override val cause: Throwable) : RuntimeException(message, cause)
