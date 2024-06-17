package gov.cdc.prime.router.azure.observability.context

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import kotlin.test.Test

class MDCUtilsTest {

    @BeforeEach
    fun setup() = MDC.clear() // start with a clean slate

    @AfterEach
    fun cleanUp() = MDC.clear() // don't potentially pollute other tests

    @Test
    fun `putAll utility function`() {
        val context = TestContext("some string", 5)

        MDCUtils.putAll(context)

        assertThat(MDC.getCopyOfContextMap()).isEqualTo(
            mapOf(
                "key1" to "some string",
                "key2" to "5"
            )
        )
    }

    @Test
    fun `put utility function`() {
        val context = TestContext("some string", 5)

        MDCUtils.put("key", context)

        assertThat(MDC.getCopyOfContextMap()).isEqualTo(
            mapOf(
                "key" to "{\"key1\":\"some string\",\"key2\":5}",
            )
        )
    }

    @Test
    fun `withLoggingContext context object function`() {
        val context = TestContext("some string", 5)

        assertThat(MDC.getCopyOfContextMap()).isEmpty()
        withLoggingContext(context) {
            assertThat(MDC.getCopyOfContextMap()).isEqualTo(
                mapOf(
                    "key1" to "some string",
                    "key2" to "5"
                )
            )
        }
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
    }

    @Test
    fun `withLoggingContext any serializable object function`() {
        val context = TestContext("some string", 5)

        assertThat(MDC.getCopyOfContextMap()).isEmpty()
        withLoggingContext(MDCUtils.MDCProperty.USERNAME to context) {
            assertThat(MDC.getCopyOfContextMap()).isEqualTo(
                mapOf(
                    "USERNAME" to "{\"key1\":\"some string\",\"key2\":5}",
                )
            )
        }
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
    }
}

data class TestContext(
    val key1: String,
    val key2: Int,
) : AzureLoggingContext