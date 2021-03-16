package gov.cdc.prime.router.secrets

import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals
import kotlin.test.fail

internal class SecretServiceTests : SecretManagement {

    override val secretService: SecretService
        get() = EnvVarSecretService

    @Test
    fun `test fetch from envVar`() {
        mockkStatic(System::class)
        every { System.getenv("SECRET_SERVICE_TEST") } returns "value_expected"
        assertEquals("value_expected", secretService.fetchSecret("SECRET_SERVICE_TEST"))
    }

    @Test
    fun `test fetchSecret handles valid secretNames`() {
        VALID_SECRET_NAMES.forEach {
            secretService.fetchSecret(it)
        }
    }

    @Test
    fun `test fetchSecret throws IllegalArgumentException with non-url safe secretNames`() {
        INVALID_SECRET_NAMES.forEach {
            try {
                secretService.fetchSecret(it)
                fail("IllegalArgumentException not thrown for $it")
            } catch (e: IllegalArgumentException) {
                assertEquals("secretName must match: ^[a-zA-Z0-9-_]*\$", e.message)
            }
        }
    }

    companion object {
        private val VALID_SECRET_NAMES = listOf(
            "valid1", "35wtfsdfe4t4wr4w4343", "with-dashes-in-name-", "with_UNDER-score"
        )
        private val INVALID_SECRET_NAMES = listOf(
            "slashes/are/not/allowed", "no spaces", "?andotherthings"
        )
    }
}