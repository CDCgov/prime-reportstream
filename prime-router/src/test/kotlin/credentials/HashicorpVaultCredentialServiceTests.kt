package gov.cdc.prime.router.credentials

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import io.mockk.*
import net.wussmann.kenneth.mockfuel.MockFuelClient
import net.wussmann.kenneth.mockfuel.MockFuelStore
import net.wussmann.kenneth.mockfuel.data.MockResponse
import kotlin.test.*

internal class HashicorpVaultCredentialServiceTests {

    private val mockFuelStore = MockFuelStore()
    private val credentialService = spyk(HashicorpVaultCredentialService, recordPrivateCalls = true)

    @BeforeTest
    fun setUp() {
        every { credentialService getProperty "VAULT_API_ADDR" } returns VAULT_API_ADDR
        every { credentialService getProperty "VAULT_TOKEN" } returns VAULT_TOKEN
        every { credentialService getProperty "manager" } returns credentialService.initVaultApi(
            FuelManager().apply {
                client = MockFuelClient(mockFuelStore)
            }
        )
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(credentialService)
        mockFuelStore.reset()
    }

    @Test
    fun `uses Vault api to fetch a credential`() {
        mockFuelStore.on(Method.GET, "/v1/secret/$CONNECTION_ID") {
            MockResponse(200, """{"@type":"UserPass","user":"user","pass":"pass"}""".toByteArray())
        }

        val credential = credentialService.fetchCredential(CONNECTION_ID, "HashicorpVaultCredentialServiceTests", CredentialRequestReason.AUTOMATED_TEST)

        mockFuelStore.verifyRequest {
            assertMethod(Method.GET)
            assertPath("/v1/secret/$CONNECTION_ID")
            assertHeader("X-Vault-Token", VAULT_TOKEN)
        }

        assertTrue(credential is UserPassCredential, "Deserialized class is not UserPassCredential")
        assertEquals("user", credential.user, "User did not match")
        assertEquals("pass", credential.pass, "Pass did not match")
    }

    @Test
    fun `uses Vault api to save a credential`() {
        mockFuelStore.on(Method.POST, "/v1/secret/$CONNECTION_ID") {
            MockResponse(200)
        }

        credentialService.saveCredential(CONNECTION_ID, VALID_CREDENTIAL, "HashicorpVaultCredentialServiceTests")

        mockFuelStore.verifyRequest {
            assertMethod(Method.POST)
            assertPath("/v1/secret/$CONNECTION_ID")
            assertHeader("X-Vault-Token", VAULT_TOKEN)
            assertBody("""{"@type":"UserPass","user":"user1","pass":"pass1"}""")
        }
    }

    companion object {
        private const val VAULT_API_ADDR = "http://mock.vault.api"
        private const val VAULT_TOKEN = "TESTING_VAULT_TOKEN"
        private const val CONNECTION_ID = "connection-id-1"
        private val VALID_CREDENTIAL = UserPassCredential("user1", "pass1")
    }
}