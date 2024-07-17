package gov.cdc.prime.router.fhirengine.azure

import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.fhirengine.engine.FhirDestinationFilterQueueMessage
import gov.cdc.prime.router.fhirengine.engine.QueueMessage
import gov.cdc.prime.router.fhirengine.engine.elrDestinationFilterQueueName
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

// TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
class FhirFunctionTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val timing1 = mockkClass(Receiver.Timing::class)

    val oneOrganization = DeepOrganization(
        "phd", "test", Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "phd",
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            ),
            Receiver(
                "elr2",
                "phd",
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml",
                timing = timing1,
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                processingModeFilter = listOf("true"),
                format = MimeFormat.HL7,
            )
        ),
    )

    private fun makeWorkflowEngine(
        metadata: Metadata,
        settings: SettingsProvider,
        databaseAccess: DatabaseAccess = accessSpy,
    ): WorkflowEngine {
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(databaseAccess)
                .blobAccess(blobMock).queueAccess(queueMock).build()
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()

        // setup
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()
        every { timing1.nextTime(any()) } returns OffsetDateTime.now().plusHours(1)
    }

    fun commonSetup() {
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()
    }

    @Test
    fun `test convert-fhir`() {
        mockkObject(BlobAccess.Companion)
        mockkObject(QueueMessage.Companion)
        // setup
        commonSetup()
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = spyk(
            FHIRConverter(
                metadata,
                settings,
                accessSpy,
                blobMock,
            )
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine = makeWorkflowEngine(metadata, settings)

        val fhirFunc = spyk(FHIRFunctions(workflowEngine, queueAccess = queueMock))

        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"
        every { queueMock.sendMessage(any(), any()) } returns Unit

        val report = Report(
            MimeFormat.FHIR,
            emptyList(),
            1,
            itemLineage = listOf(
                ItemLineage()
            ),
            metadata = UnitTestUtils.simpleMetadata,
            topic = Topic.FULL_ELR,
        )
        val routeEvent = ProcessEvent(
            Event.EventAction.DESTINATION_FILTER,
            report.id,
            Options.None,
            emptyMap(),
            emptyList()
        )
        val message = FhirDestinationFilterQueueMessage(
            report.id,
            "",
            "BlobAccess.digestToString(blobInfo.digest)",
            "ignore.ignore-full-elr",
            Topic.FULL_ELR
        )
        every { fhirEngine.doWork(any(), any(), any()) } returns listOf(
            FHIREngine.FHIREngineRunResult(
                routeEvent,
                report,
                "",
                message
            )
        )

        val queueMessage = "{\"type\":\"convert\",\"reportId\":\"011bb9ab-15c7-4ecd-8fae-0dd21e04d353\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-011bb9ab-15c7-4ecd-8fae-0dd21e04d353-20220729171318.hl7\",\"digest\":\"58ffffffaaffffffc22ffffff" +
            "f044ffffff85ffffffd4ffffffc9ffffffceffffff9bffffffe3ffffff8fffffff86ffffff9a5966fffffff6ffffff87fffff" +
            "fff5bffffffae6015fffffffbffffffdd363037ffffffed51ffffffd3\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"someSchema\",\"topic\":\"full-elr\"}"

        // act
        fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

        // assert
        verify(exactly = 1) {
            fhirEngine.doWork(any(), any(), any())
            actionHistory.trackActionParams(queueMessage) // string
            actionHistory.trackLogs(emptyList()) // list actionLog
            queueMock.sendMessage(elrDestinationFilterQueueName, message.serialize())
            workflowEngine.recordAction(any(), any())
        }
    }

    // test translate-fhir
    @Test
    fun `test translate-fhir`() {
        mockkObject(BlobAccess.Companion)
        mockkObject(QueueMessage.Companion)
        // setup
        commonSetup()
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = spyk(
            FHIRTranslator(
                metadata,
                settings,
                accessSpy,
                blobMock,
            )
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine = makeWorkflowEngine(metadata, settings)

        val fhirFunc = spyk(FHIRFunctions(workflowEngine))

        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"

        val report = Report(
            MimeFormat.FHIR,
            emptyList(),
            1,
            itemLineage = listOf(
                ItemLineage()
            ),
            metadata = UnitTestUtils.simpleMetadata,
            topic = Topic.FULL_ELR,
        )
        val nextEvent = ProcessEvent(
            Event.EventAction.BATCH,
            report.id,
            Options.None,
            emptyMap(),
            emptyList()
        )

        every { fhirEngine.doWork(any(), any(), any()) } returns listOf(
            FHIREngine.FHIREngineRunResult(
                nextEvent,
                report,
                "",
                null
            )
        )

        val queueMessage = "{\"type\":\"translate\",\"reportId\":\"011bb9ab-15c7-4ecd-8fae-0dd21e04d353\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-011bb9ab-15c7-4ecd-8fae-0dd21e04d353-20220729171318.hl7\",\"digest\":\"58ffffffaaffffffc22ffffff" +
            "f044ffffff85ffffffd4ffffffc9ffffffceffffff9bffffffe3ffffff8fffffff86ffffff9a5966fffffff6ffffff87fffff" +
            "fff5bffffffae6015fffffffbffffffdd363037ffffffed51ffffffd3\",\"sender\":\"ignore.ignore-full-elr\"," +
            "\"blobSubFolderName\":\"ignore.ignore-full-elr\",\"topic\":\"full-elr\",\"receiverFullName\":\"elr.phd\"}"

        // act
        fhirFunc.doTranslate(queueMessage, 1, fhirEngine, actionHistory)

        // assert
        verify(exactly = 1) {
            fhirEngine.doWork(any(), any(), any())
            actionHistory.trackActionParams(queueMessage) // string
            actionHistory.trackLogs(emptyList()) // list actionLog
            workflowEngine.recordAction(any(), any())
        }
        verify(exactly = 0) {
            queueMock.sendMessage(any(), any())
        }
    }
}