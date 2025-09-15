package gov.cdc.prime.router.azure.observability.context

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.event.ItemEventData
import gov.cdc.prime.router.azure.observability.event.ReportEventData
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamItemEvent
import gov.cdc.prime.router.azure.observability.event.ReportStreamReportEvent
import gov.cdc.prime.router.azure.observability.event.SubmissionEventData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import java.time.OffsetDateTime
import java.util.UUID
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

    @Test
    fun `withLoggingContext with a ReportStreamReportEvent`() {
        val event = ReportStreamReportEvent(
            ReportEventData(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Topic.FULL_ELR,
                "",
                TaskAction.send,
                OffsetDateTime.now(),
                "",
                ""
            ),
            SubmissionEventData(
                emptyList(),
                listOf("sender")
            ),
            mapOf(
                ReportStreamEventProperties.FILENAME to "filename"
            )
        )
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
        withLoggingContext(event) {
            val context = MDC.getCopyOfContextMap()
            assertThat(context["parentReportId"]).isNotNull()
            assertThat(context["topic"]).isEqualTo(Topic.FULL_ELR.jsonVal)
            assertThat(context["pipelineStepName"]).isEqualTo("send")
            assertThat(context["timestamp"]).isNotNull()
            assertThat(context["params"]).isEqualTo("{\"filename\":\"filename\"}")
        }
    }

    @Test
    fun `withLoggingContext with a ReportStreamItemEvent`() {
        val event = ReportStreamItemEvent(
            ReportEventData(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Topic.FULL_ELR,
                "",
                TaskAction.send,
                OffsetDateTime.now(),
                "",
                ""
            ),
            SubmissionEventData(
                emptyList(),
                listOf("sender")
            ),
            ItemEventData(
                1,
                1,
                1,
                "tracking"
            ),
            mapOf(
                ReportStreamEventProperties.FILENAME to "filename"
            )
        )
        assertThat(MDC.getCopyOfContextMap()).isEmpty()
        withLoggingContext(event) {
            val context = MDC.getCopyOfContextMap()
            assertThat(context["parentReportId"]).isNotNull()
            assertThat(context["topic"]).isEqualTo(Topic.FULL_ELR.jsonVal)
            assertThat(context["pipelineStepName"]).isEqualTo("send")
            assertThat(context["timestamp"]).isNotNull()
            assertThat(context["params"]).isEqualTo("{\"filename\":\"filename\"}")
            assertThat(context["childItemIndex"]).isEqualTo("1")
            assertThat(context["parentItemIndex"]).isEqualTo("1")
            assertThat(context["submittedItemIndex"]).isEqualTo("1")
            assertThat(context["trackingId"]).isEqualTo("tracking")
            assertThat(context["sender"]).isEqualTo("[\"sender\"]")
        }
    }
}

data class TestContext(val key1: String, val key2: Int) : AzureLoggingContext