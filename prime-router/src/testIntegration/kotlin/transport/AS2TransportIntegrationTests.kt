package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import com.helger.as2lib.exception.WrappedAS2Exception
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.AS2TransportType
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.credentials.UserJksCredential
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.ConnectException
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Logger

class AS2TransportIntegrationTests {
    val context = mockkClass(ExecutionContext::class)
    val metadata = Metadata.getInstance()
    val settings = FileSettings(FileSettings.defaultSettingsDirectory)
    val logger = mockkClass(Logger::class)
    val reportId = UUID.randomUUID()
    val as2Transport = spyk(AS2Transport(metadata))
    val actionHistory = ActionHistory(TaskAction.send)
    val transportType = AS2TransportType("", "id1", "id2", "a@cdc.gov")
    val task = Task(
        reportId,
        TaskAction.send,
        null,
        "standard.standard-covid-19",
        "az-phd.elr-test",
        4,
        "",
        "",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    )
    private val reportFile = ReportFile(
        reportId,
        null,
        TaskAction.send,
        null,
        null,
        null,
        "az-phd",
        "elr-test",
        null, null, "standard.standard-covid-19", null, null, null, null, null,
        4, // pretend we have 4 items to send.
        null, OffsetDateTime.now(), null, null
    )

    fun setupLogger() {
        every { context.logger }.returns(logger)
        every { logger.log(any(), any(), any<Throwable>()) }.returns(Unit)
        every { logger.warning(any<String>()) }.returns(Unit)
        every { logger.severe(any<String>()) }.returns(Unit)
        every { logger.info(any<String>()) }.returns(Unit)
    }

    fun makeHeader(): WorkflowEngine.Header {
        val content = "HL7|Stuff"
        return WorkflowEngine.Header(
            task, reportFile,
            null,
            settings.findOrganization("ignore"),
            settings.findReceiver("ignore.AS2"),
            metadata.findSchema("covid-19"),
            content = content.toByteArray(),
            true
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test send happy path`() {
        val header = makeHeader()
        setupLogger()
        every { as2Transport.sendViaAS2(any(), any(), any(), any(), any()) }
            .returns(Unit)
        every { as2Transport.lookupCredentials(any()) }
            .returns(UserJksCredential("x", "xzy", "pass", "a1", "a2"))

        // The Test
        val retryItems = as2Transport.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)

        assertThat(retryItems).isNull()
    }

    @Test
    fun `test connection error`() {
        // Setup to throw a connection error
        val header = makeHeader()
        setupLogger()
        every { as2Transport.sendViaAS2(any(), any(), any(), any(), any()) }
            .throws(WrappedAS2Exception.wrap(ConnectException()))
        every { as2Transport.lookupCredentials(any()) }
            .returns(UserJksCredential("x", "xzy", "pass", "a1", "a2"))

        // Test that retryItems was returned
        val retryItems = as2Transport.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)

        assertThat(retryItems).isSameAs(RetryToken.allItems)
    }
}