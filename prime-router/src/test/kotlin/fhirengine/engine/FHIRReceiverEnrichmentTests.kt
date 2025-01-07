package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.contains
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CodeStringConditionFilter
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamConditionFilter
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.event.InMemoryAzureEventService
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayInputStream
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val ORGANIZATION_NAME = "co-phd"
private const val RECEIVER_NAME = "full-elr-hl7"
private const val VALID_FHIR_FILEPATH = "src/test/resources/fhirengine/engine/routing/valid.fhir"
private const val BLOB_URL = "https://blob.url"
private const val BLOB_SUB_FOLDER_NAME = "test-sender"
private const val BODY_URL = "https://anyblob.com"
private const val CONDITION_FILTER = "%resource.code.coding.code = '95418-0'"
private val FILTER_PASS: ReportStreamFilter = listOf("true")
private val FILTER_FAIL: ReportStreamFilter = listOf("false")
private val MAPPED_CONDITION_FILTER_FAIL = CodeStringConditionFilter("foo,bar")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FHIRReceiverEnrichmentTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    private val actionHistory = ActionHistory(TaskAction.receiver_enrichment)
    private val azureEventService = InMemoryAzureEventService()
    private val reportServiceMock = mockk<ReportService>()
    private val submittedId = UUID.randomUUID()

    val csv = """
            variable,fhirPath
            processingId,Bundle.entry.resource.ofType(MessageHeader).meta.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id').value.coding.code
            messageId,Bundle.entry.resource.ofType(MessageHeader).id
            patient,Bundle.entry.resource.ofType(Patient)
            performerState,Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state
            patientState,Bundle.entry.resource.ofType(Patient).address.state
            specimen,Bundle.entry.resource.ofType(Specimen)
            serviceRequest,Bundle.entry.resource.ofType(ServiceRequest)
            observation,Bundle.entry.resource.ofType(Observation)
            test-dash,Bundle.test.dash
            test_underscore,Bundle.test.underscore
            test'apostrophe,Bundle.test.apostrophe
    """.trimIndent()

    private val shorthandTable = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one).loadLookupTable("fhirpath_filter_shorthand", shorthandTable)
    val report = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)

    private var actionLogger = ActionLogger()

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider): FHIREngine {
        val rootReport = mockk<ReportFile>()
        every { rootReport.reportId } returns submittedId
        every { rootReport.sendingOrg } returns "sendingOrg"
        every { rootReport.sendingOrgClient } returns "sendingOrgClient"
        every { reportServiceMock.getRootReport(any()) } returns rootReport
        every { reportServiceMock.getRootReports(any()) } returns listOf(rootReport)
        every { reportServiceMock.getRootItemIndex(any(), any()) } returns 1

        return FHIREngine.Builder()
            .metadata(metadata)
            .settingsProvider(settings)
            .databaseAccess(accessSpy)
            .blobAccess(blobMock)
            .azureEventService(azureEventService)
            .reportService(reportServiceMock)
            .build(TaskAction.receiver_enrichment)
    }

    private fun createOrganizationWithFilteredReceivers(
        jurisdictionFilter: List<String> = emptyList(),
        qualityFilter: List<String> = emptyList(),
        routingFilter: List<String> = emptyList(),
        processingModeFilter: List<String> = emptyList(),
        conditionFilter: List<String> = emptyList(),
        mappedConditionFilter: ReportStreamConditionFilter = emptyList(),
    ) = DeepOrganization(
        ORGANIZATION_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                RECEIVER_NAME,
                ORGANIZATION_NAME,
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one",
                jurisdictionalFilter = jurisdictionFilter,
                qualityFilter = qualityFilter,
                routingFilter = routingFilter,
                processingModeFilter = processingModeFilter,
                conditionFilter = conditionFilter,
                mappedConditionFilter = mappedConditionFilter
            ),
            Receiver(
                "full-elr-hl7-2",
                ORGANIZATION_NAME,
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one",
                jurisdictionalFilter = jurisdictionFilter,
                qualityFilter = qualityFilter,
                routingFilter = routingFilter,
                processingModeFilter = processingModeFilter,
                conditionFilter = conditionFilter,
                mappedConditionFilter = mappedConditionFilter
            )
        )
    )

    @BeforeEach
    fun reset() {
        actionLogger = ActionLogger()
        actionHistory.reportsIn.clear()
        actionHistory.reportsOut.clear()
        actionHistory.reportsReceived.clear()
        actionHistory.actionLogs.clear()
        azureEventService.events.clear()
        mockkObject(BlobAccess)
        clearAllMocks()
    }

    @Test
    fun `fail - invalid message type`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                qualityFilter = FILTER_FAIL
            )
        )
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverEnrichment)
        val messages = settings.receivers.map {
            spyk(
                FhirReceiverFilterQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR,
                    it.fullName
                )
            )
        }

        // act on each message (with assert)
        val exception = assertFailsWith<RuntimeException> {
            messages.forEach { message ->
                // act + assert
                accessSpy.transact { txn ->
                    engine.run(message, actionLogger, actionHistory, txn)
                }
            }
        }
        assertThat(exception.message.toString())
            .contains(
                "Message was not a FhirReceiverEnrichmentQueueMessage and cannot be processed by FHIRReceiverEnrichment"
            )
    }

    @Test
    fun `fail - missing receiver full name`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                qualityFilter = FILTER_FAIL
            )
        )
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverEnrichment)
        val messages = settings.receivers.map {
            spyk(
                FhirReceiverEnrichmentQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR,
                    "'missing-full-name'"
                )
            )
        }

        // act on each message (with assert)
        val exception = assertFailsWith<RuntimeException> {
            messages.forEach { message ->
                // act + assert
                accessSpy.transact { txn ->
                    engine.run(message, actionLogger, actionHistory, txn)
                }
            }
        }
        assertThat(exception.message.toString())
            .contains(
                "Receiver with name 'missing-full-name' was not found"
            )
    }
}