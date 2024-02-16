package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
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
import gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE
import gov.cdc.prime.router.azure.db.tables.Task
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.fhirengine.engine.FhirConvertQueueMessage
import gov.cdc.prime.router.fhirengine.engine.FhirRouteQueueMessage
import gov.cdc.prime.router.fhirengine.engine.QueueMessage
import gov.cdc.prime.router.fhirengine.engine.elrRoutingQueueName
import gov.cdc.prime.router.fhirengine.engine.elrTranslationQueueName
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.time.OffsetDateTime
import gov.cdc.prime.router.azure.db.tables.ActionLog as ActionLogTable
import gov.cdc.prime.router.azure.db.tables.pojos.ActionLog as ActionLogModel

private const val MULTIPLE_TARGETS_FHIR_PATH = "src/test/resources/fhirengine/engine/valid_data_multiple_targets.fhir"

private const val VALID_FHIR_PATH = "src/test/resources/fhirengine/engine/valid_data.fhir"

class FhirFunctionTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val timing1 = mockkClass(Receiver.Timing::class)
    val reportServiceMock = mockk<ReportService>()

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
                format = Report.Format.HL7,
            )
        ),
    )

    val hl7_record = "MSH|^~\\&|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|Winchester House^05D2222542^" +
        "ISO|CDPH FL REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|202108031315" +
        "11.0147+0000||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLab" +
        "Report-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO\n" +
        "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210726\n" +
        "PID|1||09d12345-0987-1234-1234-111b1ee0879f^^^Winchester House&05D2222542&ISO^PI^&05D2222542&ISO||Bunny^Bug" +
        "s^C^^^^L||19000101|M||2106-3^White^HL70005^^^^2.5.1|12345 Main St^^San Jose^FL^95125^USA^^^06085||(123)456-" +
        "7890^PRN^PH^^1^123^4567890|||||||||N^Non Hispanic or Latino^HL70189^^^^2.9||||||||N\n" +
        "ORC|RE|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728db" +
        "a581c^Winchester House^05D2222542^ISO|||||||||1679892871^Doolittle^Doctor^^^^^^CMS&2.16.840.1.113883.3.249&" +
        "ISO^^^^NPI||(123)456-7890^WPN^PH^^1^123^4567890|20210802||||||Winchester House|6789 Main St^^San Jose^FL^95" +
        "126^^^^06085|(123)456-7890^WPN^PH^^1^123^4567890|6789 Main St^^San Jose^FL^95126\n" +
        "OBR|1|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728dba" +
        "581c^Winchester House^05D2222542^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by" +
        " Rapid immunoassay^LN^^^^2.68|||202108020000-0500|202108020000-0500||||||||1679892871^Doolittle^Doctor^^^^" +
        "^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|(123)456-7890^WPN^PH^^1^123^4567890|||||202108020000-0500|||F\n" +
        "OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2." +
        "68||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078^^^^2.7|||F|||20210802000" +
        "0-0500|05D2222542^ISO||10811877011290_DIT^10811877011290^99ELR^^^^2.68^^10811877011290_DIT||20" +
        "2108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^" +
        "San Jose^FL^95126^^^^06085\n" +
        "OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||N^No^HL70136||||||F|||202" +
        "108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX" +
        "^^^05D2222542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0500" +
        "|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|" +
        "6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202108020000-05" +
        "00|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D22225" +
        "42|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||2021080" +
        "20000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^" +
        "05D2222542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "SPM|1|1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO^1234d1d1-95fe-462c-8ac6-46728dba581c&&05D22225" +
        "42&ISO||445297001^Swab of internal nose^SCT^^^^2.67||||53342003^Internal nose structure (body structure)^" +
        "SCT^^^^2020-09-01|||||||||202108020000-0500|20210802000006.0000-0500"

    @Suppress("ktlint:standard:max-line-length")
    val fhirRecord =
        """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""

    @Suppress("ktlint:standard:max-line-length")
    val codelessFhirRecord =
        """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""

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
            Report.Format.FHIR,
            emptyList(),
            1,
            itemLineage = listOf(
                ItemLineage()
            ),
            metadata = UnitTestUtils.simpleMetadata,
            topic = Topic.FULL_ELR,
        )
        val routeEvent = ProcessEvent(
            Event.EventAction.ROUTE,
            report.id,
            Options.None,
            emptyMap(),
            emptyList()
        )
        val message = FhirConvertQueueMessage(
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
            queueMock.sendMessage(elrRoutingQueueName, message.serialize())
            workflowEngine.recordAction(any(), any())
        }
    }

    // test route-fhir
    @Test
    fun `test route-fhir`() {
        mockkObject(BlobAccess.Companion)
        mockkObject(QueueMessage.Companion)
        // setup
        commonSetup()
        val metadata = spyk(UnitTestUtils.simpleMetadata)
        every { metadata.findLookupTable("fhirpath_filter_shorthand") } returns LookupTable()
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = spyk(
            FHIRRouter(
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
            Report.Format.FHIR,
            emptyList(),
            1,
            itemLineage = listOf(
                ItemLineage()
            ),
            metadata = UnitTestUtils.simpleMetadata,
            topic = Topic.FULL_ELR,
        )
        val nextEvent = ProcessEvent(
            Event.EventAction.TRANSLATE,
            report.id,
            Options.None,
            emptyMap(),
            emptyList()
        )
        val message = FhirRouteQueueMessage(
            report.id,
            "",
            "",
            "ignore.ignore-full-elr",
            Topic.FULL_ELR
        )
        every { fhirEngine.doWork(any(), any(), any()) } returns listOf(
            FHIREngine.FHIREngineRunResult(
                nextEvent,
                report,
                "",
                message
            )
        )

        val queueMessage = "{\"type\":\"route\",\"reportId\":\"011bb9ab-15c7-4ecd-8fae-0dd21e04d353\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-011bb9ab-15c7-4ecd-8fae-0dd21e04d353-20220729171318.hl7\",\"digest\":\"58ffffffaaffffffc22ffffff" +
            "f044ffffff85ffffffd4ffffffc9ffffffceffffff9bffffffe3ffffff8fffffff86ffffff9a5966fffffff6ffffff87fffff" +
            "fff5bffffffae6015fffffffbffffffdd363037ffffffed51ffffffd3\",\"sender\":\"ignore.ignore-full-elr\"," +
            "\"blobSubFolderName\":\"ignore.ignore-full-elr\",\"topic\":\"full-elr\"}"

        // act
        fhirFunc.doRoute(queueMessage, 1, fhirEngine, actionHistory)

        // assert
        verify(exactly = 1) {
            fhirEngine.doWork(any(), any(), any())
            actionHistory.trackActionParams(queueMessage) // string
            actionHistory.trackLogs(emptyList()) // list actionLog
            workflowEngine.recordAction(any(), any())
            queueMock.sendMessage(elrTranslationQueueName, message.serialize())
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
            Report.Format.FHIR,
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

    @Nested
    @ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
    inner class FhirFunctionIntegrationTests {
        val azuriteContainer =
            GenericContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite"))
                .withEnv("AZURITE_ACCOUNTS", "devstoreaccount1:keydevstoreaccount1")
                .withExposedPorts(10000, 10001, 10002)

        init {
            azuriteContainer.start()
        }

        private fun seedTask(
            fileFormat: Report.Format,
            currentAction: TaskAction,
            nextAction: TaskAction,
            nextEventAction: Event.EventAction,
            topic: Topic,
            taskIndex: Long = 0,
            organization: DeepOrganization,
            childReport: Report? = null,
            bodyURL: String? = null,
        ): Report {
            val report = Report(
                fileFormat,
                listOf(ClientSource(organization = organization.name, client = "Test Sender")),
                1,
                metadata = UnitTestUtils.simpleMetadata,
                nextAction = nextAction,
                topic = topic
            )
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                val action = Action().setActionName(currentAction)
                val actionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
                report.bodyURL = bodyURL ?: "http://${report.id}.${fileFormat.toString().lowercase()}"
                val reportFile = ReportFile().setSchemaTopic(topic).setReportId(report.id)
                    .setActionId(actionId).setSchemaName("").setBodyFormat(fileFormat.toString()).setItemCount(1)
                    .setExternalName("test-external-name")
                    .setBodyUrl(report.bodyURL)
                ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                    reportFile, txn, action
                )
                if (childReport != null) {
                    ReportStreamTestDatabaseContainer.testDatabaseAccess
                        .insertReportLineage(
                            ReportLineage(
                                taskIndex,
                                actionId,
                                report.id,
                                childReport.id,
                                OffsetDateTime.now()
                            ),
                            txn
                        )
                }

                ReportStreamTestDatabaseContainer.testDatabaseAccess.insertTask(
                    report,
                    fileFormat.toString().lowercase(),
                    report.bodyURL,
                    nextAction = ProcessEvent(
                        nextEventAction,
                        report.id,
                        Options.None,
                        emptyMap(),
                        emptyList()
                    ),
                    txn
                )
            }

            return report
        }

        @Test
        fun `test does not update the DB or send messages on an error`() {
            val report = seedTask(
                Report.Format.HL7,
                TaskAction.receive,
                TaskAction.convert,
                Event.EventAction.CONVERT,
                Topic.FULL_ELR,
                0,
                oneOrganization
            )

            mockkObject(BlobAccess.Companion)
            mockkObject(QueueMessage.Companion)
            every { BlobAccess.Companion.downloadBlobAsByteArray(any()) } returns hl7_record.toByteArray()
            every {
                BlobAccess.Companion.uploadBody(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } throws RuntimeException("manual error")
            every { queueMock.sendMessage(any(), any()) } returns Unit

            val settings = FileSettings().loadOrganizations(oneOrganization)
            val fhirEngine = FHIRConverter(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
                blobMock,
            )

            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val workflowEngine =
                makeWorkflowEngine(
                    UnitTestUtils.simpleMetadata,
                    settings,
                    ReportStreamTestDatabaseContainer.testDatabaseAccess
                )

            val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
                "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
                "None-${report.id}.hl7\",\"digest\"" +
                ":\"${BlobAccess.digestToString(BlobAccess.sha256Digest(hl7_record.toByteArray()))}\"," +
                "\"blobSubFolderName\":" +
                "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

            val fhirFunc = FHIRFunctions(
                workflowEngine,
                queueAccess = queueMock,
                databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
            assertThrows<RuntimeException> {
                fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)
            }

            val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
            assertThat(processTask.processedAt).isNull()
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                val routeTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                    .where(Task.TASK.NEXT_ACTION.eq(TaskAction.route))
                    .fetchOneInto(Task.TASK)
                assertThat(routeTask).isNull()
                val convertReportFile =
                    DSL.using(txn).select(REPORT_FILE.asterisk())
                        .from(REPORT_FILE)
                        .where(REPORT_FILE.NEXT_ACTION.eq(TaskAction.route))
                        .fetchOneInto(REPORT_FILE)
                assertThat(convertReportFile).isNull()
            }
            verify(exactly = 0) {
                queueMock.sendMessage(any(), any())
            }
        }

        @Test
        fun `test successfully processes a convert message`() {
            val report = seedTask(
                Report.Format.HL7,
                TaskAction.receive,
                TaskAction.convert,
                Event.EventAction.CONVERT,
                Topic.FULL_ELR,
                0,
                oneOrganization
            )
            val metadata = Metadata(UnitTestUtils.simpleSchema)

            metadata.lookupTableStore += mapOf(
                "observation-mapping" to LookupTable("observation-mapping", emptyList())
            )

            mockkObject(BlobAccess.Companion)
            mockkObject(QueueMessage.Companion)
            every { BlobAccess.Companion.downloadBlobAsByteArray(any()) } returns hl7_record.toByteArray()
            every {
                BlobAccess.Companion.uploadBody(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns BlobAccess.BlobInfo(Report.Format.FHIR, "", "".toByteArray())
            every { queueMock.sendMessage(any(), any()) } returns Unit

            val settings = FileSettings().loadOrganizations(oneOrganization)
            val fhirEngine = FHIRConverter(
                metadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
                blobMock,
            )

            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val workflowEngine =
                makeWorkflowEngine(
                    UnitTestUtils.simpleMetadata,
                    settings,
                    ReportStreamTestDatabaseContainer.testDatabaseAccess
                )

            val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
                "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
                "None-${report.id}.hl7\",\"digest\":" +
                "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(hl7_record.toByteArray()))}\"," +
                "\"blobSubFolderName\":" +
                "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

            val fhirFunc = FHIRFunctions(
                workflowEngine,
                queueAccess = queueMock,
                databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
            fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

            val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
            assertThat(processTask.processedAt).isNotNull()
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                val routeTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                    .where(Task.TASK.NEXT_ACTION.eq(TaskAction.route))
                    .fetchOneInto(Task.TASK)
                assertThat(routeTask).isNotNull()
                val convertReportFile =
                    DSL.using(txn).select(REPORT_FILE.asterisk())
                        .from(REPORT_FILE)
                        .where(REPORT_FILE.NEXT_ACTION.eq(TaskAction.route))
                        .fetchOneInto(REPORT_FILE)
                assertThat(convertReportFile).isNotNull()
            }
            verify(exactly = 1) {
                queueMock.sendMessage(elrRoutingQueueName, any())
            }
        }

        @Test
        fun `test successfully processes a route message`() {
            val report = seedTask(
                Report.Format.HL7,
                TaskAction.receive,
                TaskAction.translate,
                Event.EventAction.TRANSLATE,
                Topic.FULL_ELR,
                0,
                oneOrganization
            )

            mockkObject(BlobAccess.Companion)
            mockkObject(QueueMessage.Companion)
            val routeFhirBytes =
                File(VALID_FHIR_PATH).readBytes()
            every {
                BlobAccess.Companion.downloadBlobAsByteArray(any())
            } returns routeFhirBytes
            every {
                BlobAccess.Companion.uploadBody(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns BlobAccess.BlobInfo(Report.Format.FHIR, "", "".toByteArray())
            every { queueMock.sendMessage(any(), any()) } returns Unit
            every { reportServiceMock.getSenderName(any()) } returns "senderOrg.senderOrgClient"

            val settings = FileSettings().loadOrganizations(oneOrganization)
            val fhirEngine = FHIRRouter(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
                blobMock,
                reportService = reportServiceMock
            )

            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val workflowEngine =
                makeWorkflowEngine(
                    UnitTestUtils.simpleMetadata,
                    settings,
                    ReportStreamTestDatabaseContainer.testDatabaseAccess
                )

            val queueMessage = "{\"type\":\"route\",\"reportId\":\"${report.id}\"," +
                "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
                "None-${report.id}.hl7\",\"digest\":" +
                "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(routeFhirBytes))}\",\"blobSubFolderName\":" +
                "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

            val fhirFunc = FHIRFunctions(
                workflowEngine,
                queueAccess = queueMock,
                databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
            fhirFunc.doRoute(queueMessage, 1, fhirEngine, actionHistory)

            val convertTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
            assertThat(convertTask.routedAt).isNotNull()
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                val routeTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                    .where(Task.TASK.NEXT_ACTION.eq(TaskAction.translate))
                    .fetchOneInto(Task.TASK)
                assertThat(routeTask).isNotNull()
                val convertReportFile =
                    DSL.using(txn).select(REPORT_FILE.asterisk())
                        .from(REPORT_FILE)
                        .where(REPORT_FILE.NEXT_ACTION.eq(TaskAction.translate))
                        .fetchOneInto(REPORT_FILE)
                assertThat(convertReportFile).isNotNull()
            }
            verify(exactly = 1) {
                queueMock.sendMessage(elrTranslationQueueName, any())
            }
        }

        /*
        Send a FHIR message to an HL7v2 receiver and ensure the message receiver receives is translated to HL7v2
         */
        @Test
        fun `test successfully processes a translate message when isSendOriginal is false`() {
            // set up and seed azure blobstore
            val blobConnectionString =
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                    azuriteContainer.getMappedPort(
                        10000
                    )
                }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                    azuriteContainer.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
            val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
                "container1",
                blobConnectionString
            )

            val blobSpy = spyk(BlobAccess())
            mockkObject(BlobAccess.BlobContainerMetadata.Companion)
            every { BlobAccess.BlobContainerMetadata.Companion.build(any(), any()) } returns blobContainerMetadata
            every { BlobAccess.BlobContainerMetadata.Companion.build(any()) } returns blobContainerMetadata

            // upload reports
            val receiveBlobName = "receiveBlobName"
            val translateFhirBytes = File(
                MULTIPLE_TARGETS_FHIR_PATH
            ).readBytes()
            val receiveBlobUrl = BlobAccess.uploadBlob(
                receiveBlobName,
                translateFhirBytes,
                blobContainerMetadata
            )

            // Seed the steps backwards so report lineage can be correctly generated
            val translateReport = seedTask(
                Report.Format.FHIR,
                TaskAction.translate,
                TaskAction.send,
                Event.EventAction.SEND,
                Topic.ELR_ELIMS,
                100,
                oneOrganization
            )
            val routeReport = seedTask(
                Report.Format.FHIR,
                TaskAction.route,
                TaskAction.translate,
                Event.EventAction.TRANSLATE,
                Topic.ELR_ELIMS,
                99,
                oneOrganization,
                translateReport
            )
            val convertReport = seedTask(
                Report.Format.FHIR,
                TaskAction.convert,
                TaskAction.route,
                Event.EventAction.ROUTE,
                Topic.ELR_ELIMS,
                98,
                oneOrganization,
                routeReport
            )
            val receiveReport = seedTask(
                Report.Format.FHIR,
                TaskAction.receive,
                TaskAction.convert,
                Event.EventAction.CONVERT,
                Topic.ELR_ELIMS,
                97,
                oneOrganization,
                convertReport,
                receiveBlobUrl
            )

            val settings = FileSettings().loadOrganizations(oneOrganization)
            val fhirEngine = spyk(
                FHIRTranslator(
                    UnitTestUtils.simpleMetadata,
                    settings,
                    ReportStreamTestDatabaseContainer.testDatabaseAccess,
                    blobSpy,
                    reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess))
                )
            )

            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val workflowEngine =
                makeWorkflowEngine(
                    UnitTestUtils.simpleMetadata,
                    settings,
                    ReportStreamTestDatabaseContainer.testDatabaseAccess
                )

            mockkObject(QueueMessage.Companion)
            every { queueMock.sendMessage(any(), any()) } returns Unit

            // The topic param of queueMessage is what should determine how the Translate function runs
            val queueMessage = "{\"type\":\"translate\",\"reportId\":\"${translateReport.id}\"," +
                "\"blobURL\":\"" + receiveBlobUrl +
                "\",\"digest\":\"${
                    BlobAccess.digestToString(
                        BlobAccess.sha256Digest(
                            translateFhirBytes
                        )
                    )
                }\",\"blobSubFolderName\":" +
                "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"," +
                "\"receiverFullName\":\"phd.elr2\"}"

            val fhirFunc = FHIRFunctions(
                workflowEngine,
                queueAccess = queueMock,
                databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

            fhirFunc.doTranslate(queueMessage, 1, fhirEngine, actionHistory)

            // verify task and report_file tables were updated correctly in the Translate function (new task and new
            // record file created)
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                val queueTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                    .where(Task.TASK.NEXT_ACTION.eq(TaskAction.batch))
                    .fetchOneInto(Task.TASK)
                assertThat(queueTask).isNotNull()

                val sendReportFile =
                    DSL.using(txn).select(REPORT_FILE.asterisk())
                        .from(REPORT_FILE)
                        .where(REPORT_FILE.REPORT_ID.eq(queueTask!!.reportId))
                        .fetchOneInto(REPORT_FILE)
                assertThat(sendReportFile).isNotNull()

                // verify sendReportFile message does not match the original message from receive step
                assertThat(BlobAccess.downloadBlobAsByteArray(sendReportFile!!.bodyUrl, blobContainerMetadata))
                    .isNotEqualTo(BlobAccess.downloadBlobAsByteArray(receiveReport.bodyURL, blobContainerMetadata))
            }

            // verify we did not call the sendOriginal function
            verify(exactly = 0) {
                fhirEngine.sendOriginal(any(), any(), any())
            }

            // verify we called the sendTranslated function
            verify(exactly = 1) {
                fhirEngine.sendTranslated(any(), any(), any())
            }

            // verify sendMessage did not get called because next action should be Batch
            verify(exactly = 0) {
                queueMock.sendMessage(any(), any())
            }
        }

        /*
        Send a FHIR message to an HL7v2 receiver and ensure the message receiver receives is the original FHIR and NOT
        translated to HL7v2
         */
        @Test
        fun `test successfully processes a translate message when isSendOriginal is true`() {
            // set up and seed azure blobstore
            val blobConnectionString =
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                    azuriteContainer.getMappedPort(
                        10000
                    )
                }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                    azuriteContainer.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
            val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
                "container1",
                blobConnectionString
            )

            val blobSpy = spyk(BlobAccess())
            mockkObject(BlobAccess.BlobContainerMetadata.Companion)
            every { BlobAccess.BlobContainerMetadata.Companion.build(any(), any()) } returns blobContainerMetadata
            every { BlobAccess.BlobContainerMetadata.Companion.build(any()) } returns blobContainerMetadata

            // upload reports
            val receiveBlobName = "receiveBlobName"
            val translateFhirBytes = File(
                MULTIPLE_TARGETS_FHIR_PATH
            ).readBytes()
            val receiveBlobUrl = BlobAccess.uploadBlob(
                receiveBlobName,
                translateFhirBytes,
                blobContainerMetadata
            )

            // Seed the steps backwards so report lineage can be correctly generated
            val translateReport = seedTask(
                Report.Format.FHIR,
                TaskAction.translate,
                TaskAction.send,
                Event.EventAction.SEND,
                Topic.ELR_ELIMS,
                100,
                oneOrganization
            )
            val routeReport = seedTask(
                Report.Format.FHIR,
                TaskAction.route,
                TaskAction.translate,
                Event.EventAction.TRANSLATE,
                Topic.ELR_ELIMS,
                99,
                oneOrganization,
                translateReport
            )
            val convertReport = seedTask(
                Report.Format.FHIR,
                TaskAction.convert,
                TaskAction.route,
                Event.EventAction.ROUTE,
                Topic.ELR_ELIMS,
                98,
                oneOrganization,
                routeReport
            )
            val receiveReport = seedTask(
                Report.Format.FHIR,
                TaskAction.receive,
                TaskAction.convert,
                Event.EventAction.CONVERT,
                Topic.ELR_ELIMS,
                97,
                oneOrganization,
                convertReport,
                receiveBlobUrl
            )

            val settings = FileSettings().loadOrganizations(oneOrganization)
            val fhirEngine = spyk(
                FHIRTranslator(
                    UnitTestUtils.simpleMetadata,
                    settings,
                    ReportStreamTestDatabaseContainer.testDatabaseAccess,
                    blobSpy,
                    reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess))
                )
            )

            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val workflowEngine =
                makeWorkflowEngine(
                    UnitTestUtils.simpleMetadata,
                    settings,
                    ReportStreamTestDatabaseContainer.testDatabaseAccess
                )

            mockkObject(QueueMessage.Companion)
            every { queueMock.sendMessage(any(), any()) } returns Unit

            // The topic param of queueMessage is what should determine how the Translate function runs
            val queueMessage = "{\"type\":\"translate\",\"reportId\":\"${translateReport.id}\"," +
                "\"blobURL\":\"" + receiveBlobUrl +
                "\",\"digest\":\"${
                    BlobAccess.digestToString(
                        BlobAccess.sha256Digest(
                            translateFhirBytes
                        )
                    )
                }\",\"blobSubFolderName\":" +
                "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"elr-elims\"," +
                "\"receiverFullName\":\"phd.elr2\"}"

            val fhirFunc = FHIRFunctions(
                workflowEngine,
                queueAccess = queueMock,
                databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

            fhirFunc.doTranslate(queueMessage, 1, fhirEngine, actionHistory)

            // verify task and report_file tables were updated correctly in the Translate function
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                val sendTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                    .where(Task.TASK.NEXT_ACTION.eq(TaskAction.send))
                    .fetchOneInto(Task.TASK)
                assertThat(sendTask).isNotNull()

                val sendReportFile =
                    DSL.using(txn).select(REPORT_FILE.asterisk())
                        .from(REPORT_FILE)
                        .where(REPORT_FILE.REPORT_ID.eq(sendTask!!.reportId))
                        .fetchOneInto(REPORT_FILE)
                assertThat(sendReportFile).isNotNull()

                // verify sendReportFile message matches the original message from receive step
                assertThat(BlobAccess.downloadBlobAsByteArray(sendReportFile!!.bodyUrl, blobContainerMetadata))
                    .isEqualTo(BlobAccess.downloadBlobAsByteArray(receiveReport.bodyURL, blobContainerMetadata))
            }

            // verify we called the sendOriginal function
            verify(exactly = 1) {
                fhirEngine.sendOriginal(any(), any(), any())
            }

            // verify we did not call the sendTranslated function
            verify(exactly = 0) {
                fhirEngine.sendTranslated(any(), any(), any())
            }

            // verify sendMessage did get called because next action should be Send since isOriginal skips the batch
            // step
            verify(exactly = 1) {
                queueMock.sendMessage(any(), any())
            }
        }

        @Test
        fun `test unmapped observation error messages`() {
            val report = seedTask(
                Report.Format.FHIR,
                TaskAction.receive,
                TaskAction.convert,
                Event.EventAction.CONVERT,
                Topic.FULL_ELR,
                0,
                oneOrganization
            )
            val metadata = Metadata(UnitTestUtils.simpleSchema)
            val fhirRecordBytes = fhirRecord.toByteArray()

            metadata.lookupTableStore += mapOf(
                "observation-mapping" to LookupTable("observation-mapping", emptyList())
            )

            mockkObject(BlobAccess.Companion)
            mockkObject(QueueMessage.Companion)
            every { BlobAccess.Companion.downloadBlobAsByteArray(any()) } returns fhirRecordBytes
            every {
                BlobAccess.Companion.uploadBody(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns BlobAccess.BlobInfo(Report.Format.FHIR, "", "".toByteArray())
            every { queueMock.sendMessage(any(), any()) } returns Unit

            val settings = FileSettings().loadOrganizations(oneOrganization)
            val fhirEngine = FHIRConverter(
                metadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
                blobMock,
            )

            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val workflowEngine =
                makeWorkflowEngine(
                    metadata,
                    settings,
                    ReportStreamTestDatabaseContainer.testDatabaseAccess
                )

            val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
                "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
                "None-${report.id}.fhir\",\"digest\":" +
                "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(fhirRecordBytes))}\"," +
                "\"blobSubFolderName\":" +
                "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

            val fhirFunc = FHIRFunctions(
                workflowEngine,
                queueAccess = queueMock,
                databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
            fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

            val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
            assertThat(processTask.processedAt).isNotNull()
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                val actionLogs = DSL
                    .using(txn)
                    .select(ActionLogTable.ACTION_LOG.asterisk())
                    .from(ActionLogTable.ACTION_LOG)
                    .fetchMany()
                    .map { it.into(ActionLogModel::class.java) }
                assertThat(actionLogs.size).isEqualTo(1)
                assertThat(actionLogs[0].size).isEqualTo(2)
                assertThat(actionLogs[0].map { it.detail.message }).isEqualTo(
                    listOf(
                        "Missing mapping for code(s): 80382-5",
                        "Missing mapping for code(s): 260373001"
                    )
                )
            }
        }

        @Test
        fun `test codeless observation error message`() {
            val report = seedTask(
                Report.Format.FHIR,
                TaskAction.receive,
                TaskAction.convert,
                Event.EventAction.CONVERT,
                Topic.FULL_ELR,
                0,
                oneOrganization
            )
            val metadata = Metadata(UnitTestUtils.simpleSchema)
            val fhirRecordBytes = codelessFhirRecord.toByteArray()

            metadata.lookupTableStore += mapOf(
                "observation-mapping" to LookupTable("observation-mapping", emptyList())
            )

            mockkObject(BlobAccess.Companion)
            mockkObject(QueueMessage.Companion)
            every { BlobAccess.Companion.downloadBlobAsByteArray(any()) } returns fhirRecordBytes
            every {
                BlobAccess.Companion.uploadBody(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns BlobAccess.BlobInfo(Report.Format.FHIR, "", "".toByteArray())
            every { queueMock.sendMessage(any(), any()) } returns Unit

            val settings = FileSettings().loadOrganizations(oneOrganization)
            val fhirEngine = FHIRConverter(
                metadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
                blobMock,
            )

            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val workflowEngine =
                makeWorkflowEngine(
                    metadata,
                    settings,
                    ReportStreamTestDatabaseContainer.testDatabaseAccess
                )

            val queueMessage = "{\"type\":\"convert\",\"reportId\":\"${report.id}\"," +
                "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
                "None-${report.id}.fhir\",\"digest\":" +
                "\"${BlobAccess.digestToString(BlobAccess.sha256Digest(fhirRecordBytes))}\"," +
                "\"blobSubFolderName\":" +
                "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"}"

            val fhirFunc = FHIRFunctions(
                workflowEngine,
                queueAccess = queueMock,
                databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
            fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

            val processTask = ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchTask(report.id)
            assertThat(processTask.processedAt).isNotNull()
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                val actionLogs = DSL
                    .using(txn)
                    .select(ActionLogTable.ACTION_LOG.asterisk())
                    .from(ActionLogTable.ACTION_LOG).fetchMany()
                    .map { it.into(ActionLogModel::class.java) }
                assertThat(actionLogs.size).isEqualTo(1)
                assertThat(actionLogs[0].size).isEqualTo(1)
                assertThat(actionLogs[0][0].detail.message).isEqualTo("Observation missing code")
            }
        }
    }
}