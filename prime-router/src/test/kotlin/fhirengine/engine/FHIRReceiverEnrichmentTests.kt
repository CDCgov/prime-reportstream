package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
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
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val ORGANIZATION_NAME = "co-phd"
private const val RECEIVER_NAME = "full-elr-hl7"
private const val BLOB_URL = "https://blob.url"
private const val BLOB_SUB_FOLDER_NAME = "test-sender"
private val FILTER_FAIL: ReportStreamFilter = listOf("false")
private const val ORU_R01_SCHEMA = "classpath:/metadata/hl7_mapping/receivers/STLTs/CA/CA-receiver-transform.yml"

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
    fun `fail - invalid message queue type`() {
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

    @Test
    fun `test receiver enrichment`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        val blobInfo = BlobAccess.BlobInfo(MimeFormat.FHIR, BLOB_URL, "test".toByteArray(Charsets.UTF_8))
        val slot = slot<ByteArray>()
        every { BlobAccess.uploadBody(any(), capture(slot), any(), any(), any()) } returns blobInfo
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

        // set up
        val schemaName = ORU_R01_SCHEMA
        val receiver = Receiver(
            RECEIVER_NAME,
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = false, schemaName = schemaName),
            enrichmentSchemaNames = listOf(
                "classpath:/enrichments/testing.yml",
                "classpath:/enrichments/testing2.yml"
            )
        )

        val testOrg = DeepOrganization(
            ORGANIZATION_NAME, "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)

        val fhirData = File("src/test/resources/fhirengine/engine/valid_data_testing_sender.fhir").readText()
        every { BlobAccess.downloadBlob(BLOB_URL, "test") } returns fhirData

        val engine = makeFhirEngine(metadata, settings = settings)
        val messages = settings.receivers.map {
            spyk(
                FhirReceiverEnrichmentQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR,
                    "co-phd.full-elr-hl7"
                )
            )
        }
        messages.forEach { message ->
            // act + assert
            accessSpy.transact { txn ->
                engine.run(message, actionLogger, actionHistory, txn)
            }
        }
        val uploadedFhir = slot.captured.decodeToString()
        val enrichedBundle = FhirTranscoder.decode(uploadedFhir)
        assertThat(enrichedBundle).isNotNull()
        val software = FhirPathUtils.evaluate(
            null,
            enrichedBundle,
            enrichedBundle,
            "Bundle.entry.resource.ofType(MessageHeader).source.software"
        )
            .filterIsInstance<StringType>()
            .firstOrNull()
            ?.value
        val version = FhirPathUtils.evaluate(
            null,
            enrichedBundle,
            enrichedBundle,
            "Bundle.entry.resource.ofType(MessageHeader).source.version"
        )
            .filterIsInstance<StringType>()
            .firstOrNull()
            ?.value
        assertThat(software).isEqualTo("Purple PRIME ReportStream")
        assertThat(version).isEqualTo("0.2-YELLOW")
        val vendorOrganization = FhirPathUtils.evaluate(
            null,
            enrichedBundle,
            enrichedBundle,
            "Bundle.entry.resource.ofType(MessageHeader)" +
                ".source.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org').value"
        )
            .filterIsInstance<Reference>()
            .firstOrNull()
            ?.resource as org.hl7.fhir.r4.model.Organization
        assertThat(vendorOrganization.name.toString()).isEqualTo("Orange Software Vendor Name")
    }
}