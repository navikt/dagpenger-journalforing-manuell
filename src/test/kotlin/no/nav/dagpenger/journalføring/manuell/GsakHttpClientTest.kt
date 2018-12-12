package no.nav.dagpenger.journalføring.manuell

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.gson.Gson
import no.nav.dagpenger.oidc.OidcClient
import no.nav.dagpenger.oidc.OidcToken
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class GsakHttpClientTest {

    @Rule
    @JvmField
    var wireMockRule = WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort())

    class DummyOidcClient : OidcClient {
        override fun oidcToken(): OidcToken = OidcToken(UUID.randomUUID().toString(), "openid", 3000)
    }

    @Test
    fun `Create manuell journalføringsoppgave`() {

        val request = ManuellJournalføringsoppgaveRequest(
                aktivDato = LocalDate.now().toString(),
                fristFerdigstillelse = LocalDate.now().toString(),
                prioritet = Prioritet.NORM)

        stubFor(
            WireMock.post(WireMock.urlEqualTo("/oppgaver"))
                .withRequestBody(equalToJson(Gson().toJson(request).toString()))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "id": 12345,
                                "tildeltEnhetsnr": "0100",
                                "endretAvEnhetsnr": "0101",
                                "opprettetAvEnhetsnr": "0200",
                                "journalpostId": "84938201",
                                "journalpostkilde": "AS36",
                                "behandlesAvApplikasjon": "FS22",
                                "saksreferanse": "84942299",
                                "bnr": "11250199559",
                                "samhandlernr": "80000999999",
                                "aktoerId": "123456789",
                                "orgnr": "979312059",
                                "tilordnetRessurs": "Z998323",
                                "beskrivelse": "string",
                                "temagruppe": "ANSOS",
                                "tema": "AAP",
                                "behandlingstema": "ab0203",
                                "oppgavetype": "HAST_BANK_OPPLYS",
                                "behandlingstype": "ae0001",
                                "versjon": 1,
                                "mappeId": 848,
                                "fristFerdigstillelse": "2018-03-24",
                                "aktivDato": "2018-03-10",
                                "opprettetTidspunkt": "string",
                                "opprettetAv": "string",
                                "endretAv": "string",
                                "ferdigstiltTidspunkt": "string",
                                "endretTidspunkt": "string",
                                "prioritet": "HOY",
                                "status": "UNDER_BEHANDLING",
                                "metadata": {
                                    "prop": "propvalue"
                                }
                            }
                        """.trimIndent()
                        )
                )
        )

        val response =
                GsakHttpClient(wireMockRule.url("/oppgaver"), DummyOidcClient()).createManuellJournalføringsoppgave(
                        request, "123")

        assertEquals(12345, response.id)
    }

    @Test(expected = ManuellException::class)
    fun `Create oppgave request throws exception on error`() {

        val request = ManuellJournalføringsoppgaveRequest(
                aktivDato = LocalDate.now().toString(),
                fristFerdigstillelse = LocalDate.now().toString(),
                prioritet = Prioritet.NORM)

        stubFor(
            WireMock.post(WireMock.urlEqualTo("/oppgaver"))
                .withRequestBody(equalToJson(Gson().toJson(request).toString()))
                .willReturn(
                    WireMock.serverError()
                )
        )

        val response =
                GsakHttpClient(wireMockRule.url("/oppgaver"), DummyOidcClient()).createManuellJournalføringsoppgave(
                        request, "456")
    }
}