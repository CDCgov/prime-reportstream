package gov.cdc.prime.router.fhirengine.engine

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isInstanceOf
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
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
import gov.cdc.prime.router.azure.observability.bundleDigest.BundleDigestLabResult
import gov.cdc.prime.router.azure.observability.event.CodeSummary
import gov.cdc.prime.router.azure.observability.event.InMemoryAzureEventService
import gov.cdc.prime.router.azure.observability.event.ItemEventData
import gov.cdc.prime.router.azure.observability.event.ObservationSummary
import gov.cdc.prime.router.azure.observability.event.OrderingFacilitySummary
import gov.cdc.prime.router.azure.observability.event.ReportEventData
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamItemEvent
import gov.cdc.prime.router.azure.observability.event.TestSummary
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import gov.cdc.prime.router.version.Version
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

private const val ORGANIZATION_NAME = "co-phd"
private const val RECEIVER_NAME = "full-elr-hl7"
private const val TOPIC_TEST_ORG_NAME = "topic-test"
private const val PROVENANCE_COUNT_GREATER_THAN_ZERO = "Bundle.entry.resource.ofType(Provenance).count() > 0"
private const val VALID_FHIR_URL = "src/test/resources/fhirengine/engine/routing/valid.fhir"
private const val BLOB_URL = "https://blob.url"
private const val BLOB_SUB_FOLDER_NAME = "test-sender"
private const val BODY_URL = "https://anyblob.com"
private const val loincSystem = "http://loinc.org"
private const val snomedSystem = "SNOMEDCT"

private val PASS_FILTER: ReportStreamFilter = listOf("true")
private val FAIL_FILTER: ReportStreamFilter = listOf("false")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FhirDestinationFilterTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    private val actionHistory = ActionHistory(TaskAction.destination_filter)
    private val azureEventService = InMemoryAzureEventService()
    private val reportServiceMock = mockk<ReportService>()
    private val submittedId = UUID.randomUUID()

    val oneOrganization = DeepOrganization(
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
            ),
            Receiver(
                "full-elr-hl7-2",
                ORGANIZATION_NAME,
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one"
            )
        )
    )

    val secondElrOrganization = DeepOrganization(
        "second org",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "second-receiver-2",
                "second org",
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one",
            ),
            Receiver(
                "full-elr-hl7-2",
                "second org",
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one"
            )
        )
    )

    private val etorOrganization = DeepOrganization(
        "etor-test",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "simulatedlab",
                "etor-test",
                Topic.ETOR_TI,
                CustomerStatus.ACTIVE,
                "one"
            )
        )
    )

    private val elimsOrganization = DeepOrganization(
        "elims-test",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "elims-test",
                Topic.ELR_ELIMS,
                CustomerStatus.ACTIVE,
                "one"
            )
        )
    )

    private val etorAndElrOrganizations = DeepOrganization(
        TOPIC_TEST_ORG_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                RECEIVER_NAME,
                TOPIC_TEST_ORG_NAME,
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one",
                jurisdictionalFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
            ),
            Receiver(
                "simulatedlab",
                TOPIC_TEST_ORG_NAME,
                Topic.ETOR_TI,
                CustomerStatus.ACTIVE,
                "one",
                jurisdictionalFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
            ),
            Receiver(
                "full-elr-hl7-inactive",
                TOPIC_TEST_ORG_NAME,
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one",
                jurisdictionalFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
            ),
            Receiver(
                "simulatedlab-inactive",
                TOPIC_TEST_ORG_NAME,
                Topic.ETOR_TI,
                CustomerStatus.INACTIVE,
                "one",
                jurisdictionalFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
            )
        )
    )

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

    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one)
    val report = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)

    private var actionLogger = ActionLogger()

    private fun makeOrgWithJurisdictionalFilter(filter: ReportStreamFilter) = DeepOrganization(
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
                jurisdictionalFilter = filter
            ),
        ),
    )

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider): FHIREngine {
        val rootReport = mockk<ReportFile>(relaxed = true)
        every { rootReport.reportId } returns submittedId
        every { rootReport.sendingOrg } returns "sendingOrg"
        every { rootReport.sendingOrgClient } returns "sendingOrgClient"
        every { reportServiceMock.getRootReport(any()) } returns rootReport
        every { reportServiceMock.getRootReports(any()) } returns listOf(rootReport)
        every { reportServiceMock.getRootItemIndex(any(), any()) } returns 1
        every { accessSpy.fetchReportFile(any()) } returns rootReport

        return FHIREngine.Builder()
            .metadata(metadata)
            .settingsProvider(settings)
            .databaseAccess(accessSpy)
            .blobAccess(blobMock)
            .azureEventService(azureEventService)
            .reportService(reportServiceMock)
            .build(TaskAction.destination_filter)
    }

    @BeforeEach
    fun reset() {
        actionLogger = ActionLogger()
        actionHistory.reportsIn.clear()
        actionHistory.reportsOut.clear()
        actionHistory.actionLogs.clear()
        azureEventService.events.clear()
        clearAllMocks()
    }

    @Test
    fun `fail - jurisfilter does not pass`() {
        // engine set up
        val settings = FileSettings().loadOrganizations(
            makeOrgWithJurisdictionalFilter(FAIL_FILTER)
        )
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRDestinationFilter)
        val message = spyk(
            FhirDestinationFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR
            )
        )

        // mock setup
        mockkObject(BlobAccess)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(File(VALID_FHIR_URL).readText())
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)

        // act + assert
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
        }
    }

    @Test
    fun `success - jurisfilter passes`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(
            makeOrgWithJurisdictionalFilter(PASS_FILTER)
        )
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRDestinationFilter)
        val message = spyk(
            FhirDestinationFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        // mock setup
        mockkObject(BlobAccess)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(File(VALID_FHIR_URL).readText())
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)

        // act + assert
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).hasSize(1)
            assertThat(actionHistory.actionLogs).isEmpty()
            assertThat(actionHistory.reportsIn).hasSize(1)
            assertThat(actionHistory.reportsOut).hasSize(1)

            val azureEvents = azureEventService.events

            assertThat(azureEvents).hasSize(1)
            assertThat(azureEvents.first())
                .isInstanceOf<ReportStreamItemEvent>()
            val event: ReportStreamItemEvent = azureEvents.first() as ReportStreamItemEvent
            assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
                ReportEventData(
                    actionHistory.reportsOut.values.first().reportId,
                    message.reportId,
                    listOf(submittedId),
                    Topic.FULL_ELR,
                    "test",
                    TaskAction.destination_filter,
                    OffsetDateTime.now(),
                    Version.commitId
                ),
                ReportEventData::timestamp,
            )
            assertThat(event.itemEventData).isEqualTo(
                ItemEventData(
                    1,
                    1,
                    1,
                    "1234d1d1-95fe-462c-8ac6-46728dba581c",
                    "sendingOrg.sendingOrgClient"
                )
            )
            assertThat(event.params)
                .isEqualTo(
                    mapOf(
                    ReportStreamEventProperties.RECEIVER_NAME to "co-phd.full-elr-hl7",
                    ReportStreamEventProperties.BUNDLE_DIGEST to BundleDigestLabResult(
                        observationSummaries = listOf(
                            ObservationSummary(
                                listOf(
                                    TestSummary(
                                        listOf(
                                            CodeSummary(
                                                snomedSystem,
                                                "840539006",
                                                @Suppress("ktlint:standard:max-line-length")
                                                "Disease caused by severe acute respiratory syndrome coronavirus 2 (disorder)"
                                            )
                                        ),
                                        loincSystem,
                                        "94558-4",
                                    )
                                ),
                                listOf(
                                    CodeSummary(
                                        system = "http://terminology.hl7.org/CodeSystem/v2-0078",
                                        code = "N",
                                        display = "Normal"
                                    )
                                )
                            ),
                            ObservationSummary(
                                listOf(
                                    TestSummary(
                                        testPerformedCode = "95418-0",
                                        testPerformedSystem = loincSystem
                                    )
                                )
                            ),
                            ObservationSummary(
                                listOf(
                                    TestSummary(
                                        testPerformedCode = "95417-2",
                                        testPerformedSystem = loincSystem
                                    )
                                )
                            ),
                            ObservationSummary(
                                listOf(
                                    TestSummary(
                                        testPerformedCode = "95421-4",
                                        testPerformedSystem = loincSystem
                                    )
                                )
                            ),
                            ObservationSummary(
                                listOf(
                                    TestSummary(
                                        testPerformedCode = "95419-8",
                                        testPerformedSystem = loincSystem
                                    )
                                )
                            ),
                        ),
                        patientState = listOf("CA"),
                        performerSummaries = emptyList(),
                        orderingFacilitySummaries = listOf(
                            OrderingFacilitySummary(
                                orderingFacilityName = "Winchester House",
                                orderingFacilityState = "CA"
                            )
                        ),
                        eventType = "ORU/ACK - Unsolicited transmission of an observation message"
                    )
                )
                )
        }

        // assert
        verify(exactly = 1) {
            BlobAccess.uploadBlob(any(), any(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test a message is queued per receiver that will have the report delivered`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(oneOrganization, secondElrOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRDestinationFilter)
        val message = spyk(
            FhirDestinationFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        // data + mock setup
        val fhirData = File(VALID_FHIR_URL).readText()
        mockkObject(BlobAccess)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)

        // act + assert
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).hasSize(2)
            assertThat(actionHistory.actionLogs).isEmpty()
            assertThat(actionHistory.reportsIn).hasSize(1)
            assertThat(actionHistory.reportsOut).hasSize(2)
        }

        // assert
        verify(exactly = 2) {
            BlobAccess.uploadBlob(any(), any(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test bundle with no receivers is not routed to translate function`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRDestinationFilter)
        val message = spyk(
            FhirDestinationFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR
            )
        )

        // mock setup
        mockkObject(BlobAccess)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(File(VALID_FHIR_URL).readText())
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)
        every { engine.findTopicReceivers(any()) } returns emptyList()

        // act + assert
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            assertThat(actionHistory.reportsIn).hasSize(1)
            assertThat(actionHistory.reportsOut).hasSize(1)

            val azureEvents = azureEventService.events
            assertThat(azureEvents).hasSize(1)
            assertThat(azureEvents.first())
                .isInstanceOf(ReportStreamItemEvent::class)
            val event: ReportStreamItemEvent = azureEvents.first() as ReportStreamItemEvent
                assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
                    ReportEventData(
                        actionHistory.reportsOut.values.first().reportId,
                        message.reportId,
                        listOf(submittedId),
                        Topic.FULL_ELR,
                        "",
                        TaskAction.destination_filter,
                        OffsetDateTime.now(),
                        Version.commitId
                    ),
                    ReportEventData::timestamp
                )
                    assertThat(event.itemEventData).isEqualTo(
                        ItemEventData(
                            1,
                            1,
                            1,
                            "1234d1d1-95fe-462c-8ac6-46728dba581c",
                            "sendingOrg.sendingOrgClient"
                        )
                    )
                    assertThat(event.params).isEqualTo(
                        mapOf(
                        ReportStreamEventProperties.BUNDLE_DIGEST to BundleDigestLabResult(
                            observationSummaries = listOf(
                                ObservationSummary(
                                    listOf(
                                        TestSummary(
                                            listOf(
                                                CodeSummary(
                                                    snomedSystem,
                                                    "840539006",
                                                    @Suppress("ktlint:standard:max-line-length")
                                                    "Disease caused by severe acute respiratory syndrome coronavirus 2 (disorder)"
                                                )
                                            ),
                                            loincSystem,
                                            "94558-4",
                                        )
                                    ),
                                    listOf(
                                        CodeSummary(
                                            system = "http://terminology.hl7.org/CodeSystem/v2-0078",
                                            code = "N",
                                            display = "Normal"
                                        )
                                    )
                                ),
                                ObservationSummary(
                                    listOf(
                                        TestSummary(
                                            testPerformedCode = "95418-0",
                                            testPerformedSystem = loincSystem
                                        )
                                    )
                                ),
                                ObservationSummary(
                                    listOf(
                                        TestSummary(
                                            testPerformedCode = "95417-2",
                                            testPerformedSystem = loincSystem
                                        )
                                    )
                                ),
                                ObservationSummary(
                                    listOf(
                                        TestSummary(
                                            testPerformedCode = "95421-4",
                                            testPerformedSystem = loincSystem
                                        )
                                    )
                                ),
                                ObservationSummary(
                                    listOf(
                                        TestSummary(
                                            testPerformedCode = "95419-8",
                                            testPerformedSystem = loincSystem
                                        )
                                    )
                                ),
                            ),
                            patientState = listOf("CA"),
                            performerSummaries = emptyList(),
                            orderingFacilitySummaries = listOf(
                                OrderingFacilitySummary(
                                    orderingFacilityName = "Winchester House",
                                    orderingFacilityState = "CA"
                                )
                            ),
                            eventType = "ORU/ACK - Unsolicited transmission of an observation message"
                        )
                    )
                    )
        }

        // assert
        verify(exactly = 0) {
            accessSpy.insertTask(any(), any(), any(), any())
            BlobAccess.uploadBlob(any(), any())
        }
    }

    @Test
    fun `test etor topic routing`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(etorOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRDestinationFilter)

        // assert
        // when doing routing for full-elr, verify that etor receiver isn't included (not even in logged results)
        var receivers = engine.findTopicReceivers(Topic.FULL_ELR)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers).isEmpty()
        // when doing routing for etor, verify that etor receiver is included
        receivers = engine.findTopicReceivers(Topic.ETOR_TI)
        assertThat(actionHistory.actionLogs).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0]).isEqualTo(etorOrganization.receivers[0])
    }

    @Test
    fun `test elr topic routing`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRDestinationFilter)

        // assert
        // when doing routing for etor, verify that full-elr receiver isn't included (not even in logged results)
        var receivers = engine.findTopicReceivers(Topic.ETOR_TI)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers).isEmpty()
        // when doing routing for full-elr, verify that full-elr receiver is included
        receivers = engine.findTopicReceivers(Topic.FULL_ELR)
        assertThat(actionHistory.actionLogs).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0]).isEqualTo(oneOrganization.receivers[0])
    }

    @Test
    fun `test elr-elims topic routing`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(elimsOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRDestinationFilter)

        // assert
        // when doing routing for full-elr, verify that elims receiver isn't included (not even in logged results)
        var receivers = engine.findTopicReceivers(Topic.FULL_ELR)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers).isEmpty()
        // when doing routing for elims, verify that elims receiver is included
        receivers = engine.findTopicReceivers(Topic.ELR_ELIMS)
        assertThat(actionHistory.actionLogs).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0]).isEqualTo(elimsOrganization.receivers[0])
    }

    @Test
    fun `test combined topic routing`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(etorAndElrOrganizations)
        val actionHistory = ActionHistory(TaskAction.destination_filter)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRDestinationFilter)

        // assert
        // when routing for etor, verify that only the active etor receiver is included (even in logged results)
        var receivers = engine.findTopicReceivers(Topic.ETOR_TI)
        assertThat(actionHistory.actionLogs).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0].name).isEqualTo("simulatedlab")
        // when routing for full-elr, verify that only the active full-elr receiver is included (even in logged results)
        receivers = engine.findTopicReceivers(Topic.FULL_ELR)
        assertThat(actionHistory.actionLogs).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0].name).isEqualTo(RECEIVER_NAME)

        // verify error when using non-UP topic
        assertFailure {
            engine.doWork(
                FhirDestinationFilterQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.COVID_19
                ),
                actionLogger,
                actionHistory
            )
        }.hasClass(java.lang.IllegalStateException::class.java)
    }
}