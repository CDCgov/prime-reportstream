package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import io.mockk.every
import io.mockk.mockk
import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Logger

/**
 * Base class for transport tests that provide common test functionality.
 */
abstract class TransportIntegrationTests : Logging {
    /**
     * The mock logger. Will passthrough calls to Apache mixin logger
     */
    private val passthroughContextLogger = mockk<Logger>().also {
        every { it.log(any(), any(), any<Throwable>()) }.returns(Unit)
        every { it.warning(any<String>()) }.answers {
            logger.warn(firstArg<String>())
        }
        every { it.severe(any<String>()) }.answers {
            logger.error(firstArg<String>())
        }
        every { it.info(any<String>()) }.answers {
            logger.info(firstArg<String>())
        }
    }

    /**
     * The mock context.
     */
    protected val context = mockk<ExecutionContext>().also {
        every { it.logger }.returns(passthroughContextLogger)
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