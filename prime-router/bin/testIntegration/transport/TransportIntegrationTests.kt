package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Logger

/**
 * Base class for transport tests that provide common test functionality.
 */
abstract class TransportIntegrationTests {
    /**
     * The mock logger.
     */
    private val logger = mockk<Logger>().also {
        every { it.log(any(), any(), any<Throwable>()) }.returns(Unit)
        every { it.warning(any<String>()) }.returns(Unit)
        every { it.severe(any<String>()) }.returns(Unit)
        every { it.info(any<String>()) }.returns(Unit)
    }

    /**
     * The mock context.
     */
    protected val context = mockk<ExecutionContext>().also {
        every { it.logger }.returns(logger)
    }

    /**
     * A test report ID.
     */
    protected val reportId: UUID = UUID.randomUUID()

    /**
     * A test report file.
     */
    protected val reportFile = ReportFile(
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
}