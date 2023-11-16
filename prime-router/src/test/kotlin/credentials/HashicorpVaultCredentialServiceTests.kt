package gov.cdc.prime.router.credentials

// import net.wussmann.kenneth.mockfuel.MockFuelClient
// import net.wussmann.kenneth.mockfuel.MockFuelStore
// import net.wussmann.kenneth.mockfuel.data.MockResponse
import gov.cdc.prime.router.cli.ApiMockEngine
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class HashicorpVaultCredentialServiceTests {
    /**
     * Helper
     */
    private fun getMockClient(
        url: String,
        status: HttpStatusCode,
        body: String,
    ): HttpClient {
        return ApiMockEngine(
                url,
                status,
                body
            ).client()
    }

//    private val mockFuelStore = MockFuelStore()
    private val credentialService = spyk(HashicorpVaultCredentialService, recordPrivateCalls = true)

    @BeforeTest
    fun setUp() {
        every { credentialService getProperty "VAULT_API_ADDR" } returns VAULT_API_ADDR
        every { credentialService getProperty "VAULT_TOKEN" } returns VAULT_TOKEN
//        every { credentialService getProperty "manager" } returns credentialService.initVaultApi(
//            FuelManager().apply {
//                client = MockFuelClient(mockFuelStore)
//            }
//        )
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(credentialService)
//        mockFuelStore.reset()
    }

    @Test
    fun `uses Vault api to fetch a credential`() {
        val creds: Credential? = HashicorpVaultCredentialService.fetchCredential(
            CONNECTION_ID,
            "HashicorpVaultCredentialServiceTests",
            CredentialRequestReason.AUTOMATED_TEST,
            httpClient = getMockClient(
                url = "/v1/secret/$CONNECTION_ID",
                HttpStatusCode.OK,
                body = """{"data":{"@type":"UserPass","user":"user","pass":"pass"}}"""
            )
        )
        assertTrue(creds is UserPassCredential)
        assertTrue(creds.user == "user")
        assertTrue(creds.pass == "pass")
//        mockFuelStore.on(Method.GET, "/v1/secret/$CONNECTION_ID") {
//            MockResponse(200, """{"data":{"@type":"UserPass","user":"user","pass":"pass"}}""".toByteArray())
//        }

//        val credential = credentialService.fetchCredential(
//            CONNECTION_ID, "HashicorpVaultCredentialServiceTests", CredentialRequestReason.AUTOMATED_TEST
//        )

//        mockFuelStore.verifyRequest {
//            assertMethod(Method.GET)
//            assertPath("/v1/secret/$CONNECTION_ID")
//            assertHeader("X-Vault-Token", VAULT_TOKEN)
//        }

//        @Suppress("USELESS_IS_CHECK")
//        assertThat(credential is UserPassCredential).isTrue()
//        if (credential is UserPassCredential) {
//            assertThat(credential.user).isEqualTo("user")
//            assertThat(credential.pass).isEqualTo("pass")
//        }
    }

    @Test
    fun `uses Vault api to fetch a missing credential`() {
//        mockFuelStore.on(Method.GET, "/v1/secret/$CONNECTION_ID") {
//            MockResponse(400)
//        }

//        val credential = credentialService.fetchCredential(
//            CONNECTION_ID, "HashicorpVaultCredentialServiceTests", CredentialRequestReason.AUTOMATED_TEST
//        )

//        mockFuelStore.verifyRequest {
//            assertMethod(Method.GET)
//            assertPath("/v1/secret/$CONNECTION_ID")
//            assertHeader("X-Vault-Token", VAULT_TOKEN)
//        }

//        assertThat(credential).isNull()
    }

    @Test
    fun `uses Vault api to save a credential`() {
//        mockFuelStore.on(Method.POST, "/v1/secret/$CONNECTION_ID") {
//            MockResponse(204)
//        }

//        credentialService.saveCredential(
//            CONNECTION_ID, VALID_CREDENTIAL, "HashicorpVaultCredentialServiceTests"
//        )

//        mockFuelStore.verifyRequest {
//            assertMethod(Method.POST)
//            assertPath("/v1/secret/$CONNECTION_ID")
//            assertHeader("X-Vault-Token", VAULT_TOKEN)
//            assertHeader("Content-Type", "application/json")
//            assertBody("""{"@type":"UserPass","user":"user1","pass":"pass1"}""")
//        }
    }

    companion object {
        private const val VAULT_API_ADDR = "http://mock.vault.api"
        private const val VAULT_TOKEN = "TESTING_VAULT_TOKEN"
        private const val CONNECTION_ID = "connection-id-1"
        private val VALID_CREDENTIAL = UserPassCredential("user1", "pass1")
    }
}