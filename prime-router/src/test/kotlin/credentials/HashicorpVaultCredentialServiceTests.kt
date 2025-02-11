package gov.cdc.prime.router.credentials

import com.sendgrid.Method
import gov.cdc.prime.router.cli.ApiMockEngine
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class HashicorpVaultCredentialServiceTests {
    /**
     * Helper
     */
    private fun getMockClient(
        url: String,
        status: HttpStatusCode,
        body: String,
        f: ((request: HttpRequestData) -> Unit)? = null,
    ): HttpClient = ApiMockEngine(
            url,
            status,
            body,
            f = f
        ).client()

    @Test
    fun `uses Vault api to fetch a credential`() {
        val creds: Credential? = HashicorpVaultCredentialService.fetchCredentialHelper(
            CONNECTION_ID,
            httpClient = getMockClient(
                url = "/v1/secret/$CONNECTION_ID",
                HttpStatusCode.OK,
                body = """{"data":{"@type":"UserPass","user":"user","pass":"pass"}}"""
            ),
        )
        assertTrue(creds is UserPassCredential)
        assertTrue(creds.user == "user")
        assertTrue(creds.pass == "pass")
    }

    @Test
    fun `uses Vault api to fetch a missing credential`() {
        val creds = HashicorpVaultCredentialService.fetchCredentialHelper(
            connectionId = CONNECTION_ID,
            httpClient = getMockClient(
                url = "/v1/secret/$CONNECTION_ID",
                HttpStatusCode.BadRequest,
                body = """{"data":{"@type":"UserPass","user":"user","pass":"pass"}}"""
            ) {
                assertTrue(it.headers.contains("X-Vault-Token"))
                assertEquals(it.headers.get("X-Vault-Token"), VAULT_TOKEN)
                assertEquals(it.method.value, Method.GET.toString())
                assertEquals(it.url.encodedPath, "/v1/secret/$CONNECTION_ID")
            },
            vaultAddr = VAULT_API_ADDR,
            vaultToken = VAULT_TOKEN
        )
        assertNull(creds, "Expect null return for the fetch credential...")
    }

    @Test
    fun `uses Vault api to save a credential`() {
        HashicorpVaultCredentialService.saveCredentialHelper(
            CONNECTION_ID,
            VALID_CREDENTIAL,
            vaultAddr = VAULT_API_ADDR,
            vaultToken = VAULT_TOKEN,
            httpClient = getMockClient(
                url = "/v1/secret/$CONNECTION_ID",
                HttpStatusCode.NoContent,
                body = ""
            ) {
                assertTrue(it.headers.contains("X-Vault-Token"))
                assertEquals(it.headers["X-Vault-Token"], VAULT_TOKEN)
                assertEquals(it.method.value, Method.POST.toString())
                assertEquals(it.url.encodedPath, "/v1/secret/$CONNECTION_ID")
            }
        )
    }

    companion object {
        private const val VAULT_API_ADDR = "http://mock.vault.api"
        private const val VAULT_TOKEN = "TESTING_VAULT_TOKEN"
        private const val CONNECTION_ID = "connection-id-1"
        private val VALID_CREDENTIAL = UserPassCredential("user1", "pass1")
    }
}