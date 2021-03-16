package gov.cdc.prime.router.secrets

import com.azure.core.credential.TokenCredential
import com.azure.security.keyvault.secrets.SecretClient
import com.azure.security.keyvault.secrets.SecretClientBuilder
import com.azure.security.keyvault.secrets.models.KeyVaultSecret
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AzureSecretServiceTests {
    private val secretService = spyk(AzureSecretService, recordPrivateCalls = true)
    private val secretClient = mockk<SecretClient>()

    @BeforeTest
    fun setUp() {
        every { secretService getProperty "KEY_VAULT_NAME" } returns KEY_VAULT_NAME
        every { secretService getProperty "secretClient" } returns secretClient
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(secretService)
    }

    @Test
    fun `initializes a Key Vault using the correct URL`() {
        val secretClientBuilder = mockkClass(SecretClientBuilder::class)
        every { secretClientBuilder.vaultUrl(any()) } returns secretClientBuilder
        every { secretClientBuilder.credential(any()) } returns secretClientBuilder
        every { secretClientBuilder.buildClient() } returns secretClient

        val mockAzureCredential = mockkClass(TokenCredential::class)

        val retVal = secretService.initSecretClient(
            secretClientBuilder = secretClientBuilder, credential = mockAzureCredential
        )
        verify { secretClientBuilder.vaultUrl("https://$KEY_VAULT_NAME.vault.azure.net") }
        verify { secretClientBuilder.credential(mockAzureCredential) }

        assertEquals(secretClient, retVal, "Expects mock secret client to be returned")
    }

    @Test
    fun `uses Key Vault to retrieve a secret`() {
        every { secretClient.getSecret(any()) } returns KeyVaultSecret("functionapp-secret-name", "From Azure")
        val secret = secretService.fetchSecret("SECRET_NAME")
        assertEquals("From Azure", secret, "Expected secret not returned")
    }

    companion object {
        private const val KEY_VAULT_NAME = "UNIT_TEST_KEY_VAULT"
    }
}