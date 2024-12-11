package gov.cdc.prime.router.azure

import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamReportProcessingErrorEventBuilder
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.createFHIRFunctionsInstance
import gov.cdc.prime.router.fhirengine.azure.FHIRFunctions
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.jooq.exception.DataAccessException
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64
import java.util.UUID

class FHIRFunctionsTests {

    val queueMessage = """
        {
            "type": "convert",
            "reportId": "${UUID.randomUUID()}",
            "blobURL": "",
            "digest": "",
            "blobSubFolderName": "ignore.full-elr",
            "topic": "full-elr",
            "schemaName": ""
        }
    """

    @BeforeEach
    fun beforeEach() {
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns "poison-123"
        mockkObject(BlobAccess)
        mockkConstructor(DatabaseLookupTableAccess::class)
    }

    private fun createFHIRFunctionsInstance(): FHIRFunctions {
        val settings = FileSettings().loadOrganizations(UniversalPipelineTestUtils.universalPipelineOrganization)
        val metadata = UnitTestUtils.simpleMetadata
        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        val dataProvider = MockDataProvider { emptyArray<MockResult>() }
        val connection = MockConnection(dataProvider)
        val accessSpy = spyk(DatabaseAccess(connection))
        val workflowEngine = WorkflowEngine.Builder()
            .metadata(metadata)
            .settingsProvider(settings)
            .databaseAccess(accessSpy)
            .build()
        every { accessSpy.fetchReportFile(any()) } returns mockk<ReportFile>(relaxed = true)
        return FHIRFunctions(
            workflowEngine,
            databaseAccess = accessSpy,
            submissionTableService = mockk<SubmissionTableService>()
        )
    }

    @Test
    fun `test should add to the poison queue and catch an unexpected exception`() {
        val fhirFunctions = createFHIRFunctionsInstance()

        val mockReportEventService = mockk<IReportStreamEventService>(relaxed = true)
        val init = slot<ReportStreamReportProcessingErrorEventBuilder.() -> Unit>()
        every {
            mockReportEventService.sendReportProcessingError(
                any(),
                any<ReportFile>(),
                any(),
                any(),
                any(),
                capture(init)
            )
        } returns Unit
        val mockFHIRConverter = mockk<FHIRConverter>(relaxed = true)
        every { mockFHIRConverter.run(any(), any(), any(), any()) } throws RuntimeException("Error")
        every { mockFHIRConverter.reportEventService } returns mockReportEventService
        every { mockFHIRConverter.taskAction } returns TaskAction.convert
        fhirFunctions.process(queueMessage, 1, mockFHIRConverter, ActionHistory(TaskAction.convert))

        verify(exactly = 1) {
            QueueAccess.sendMessage(
                "${QueueMessage.elrConvertQueueName}-poison",
                Base64.getEncoder().encodeToString(queueMessage.toByteArray())
            )
            mockReportEventService.sendReportProcessingError(
                ReportStreamEventName.PIPELINE_EXCEPTION,
                any<ReportFile>(),
                TaskAction.convert,
                "Error",
                any(),
                init.captured
            )
        }
    }

    @Test
    fun `test should not add to the poison queue and throw a data access exception`() {
        val fhirFunctions = createFHIRFunctionsInstance()

        val mockFHIRConverter = mockk<FHIRConverter>(relaxed = true)
        every { mockFHIRConverter.run(any(), any(), any(), any()) } throws DataAccessException("Error")
        assertThrows<DataAccessException> {
            fhirFunctions.process(queueMessage, 1, mockFHIRConverter, ActionHistory(TaskAction.convert))
        }

        verify(exactly = 0) {
            QueueAccess.sendMessage(
                "${QueueMessage.elrConvertQueueName}-poison",
                Base64.getEncoder().encodeToString(queueMessage.toByteArray())
            )
        }
    }
}