package gov.cdc.prime.router.fhirengine.azure

import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.fhirengine.engine.Message
import gov.cdc.prime.router.metadata.LookupTable
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

    private fun makeWorkflowEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
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
        mockkObject(Message.Companion)
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
                queueMock
            )
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine = makeWorkflowEngine(metadata, settings)

        val fhirFunc = spyk(FHIRFunctions(workflowEngine))

        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"

        every { fhirEngine.doWork(any(), any(), any()) } returns Unit

        val queueMessage = "{\"type\":\"raw\",\"reportId\":\"011bb9ab-15c7-4ecd-8fae-0dd21e04d353\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-011bb9ab-15c7-4ecd-8fae-0dd21e04d353-20220729171318.hl7\",\"digest\":\"58ffffffaaffffffc22ffffff" +
            "f044ffffff85ffffffd4ffffffc9ffffffceffffff9bffffffe3ffffff8fffffff86ffffff9a5966fffffff6ffffff87fffff" +
            "fff5bffffffae6015fffffffbffffffdd363037ffffffed51ffffffd3\",\"sender\":\"ignore.ignore-full-elr\"," +
            "\"blobSubFolderName\":\"ignore.ignore-full-elr\"}"

        // act
        fhirFunc.doConvert(queueMessage, 1, fhirEngine, actionHistory)

        // assert
        verify(exactly = 1) {
            fhirEngine.doWork(any(), any(), any())
            actionHistory.trackActionParams(queueMessage) // string
            actionHistory.trackLogs(emptyList()) // list actionLog
            workflowEngine.recordAction(any())
        }
    }

    // test route-fhir
    @Test
    fun `test route-fhir`() {
        mockkObject(BlobAccess.Companion)
        mockkObject(Message.Companion)
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
                queueMock
            )
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine = makeWorkflowEngine(metadata, settings)

        val fhirFunc = spyk(FHIRFunctions(workflowEngine))
        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"

        every { fhirEngine.doWork(any(), any(), any()) } returns Unit

        val queueMessage = "{\"type\":\"raw\",\"reportId\":\"011bb9ab-15c7-4ecd-8fae-0dd21e04d353\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-011bb9ab-15c7-4ecd-8fae-0dd21e04d353-20220729171318.hl7\",\"digest\":\"58ffffffaaffffffc22ffffff" +
            "f044ffffff85ffffffd4ffffffc9ffffffceffffff9bffffffe3ffffff8fffffff86ffffff9a5966fffffff6ffffff87fffff" +
            "fff5bffffffae6015fffffffbffffffdd363037ffffffed51ffffffd3\",\"sender\":\"ignore.ignore-full-elr\"," +
            "\"blobSubFolderName\":\"ignore.ignore-full-elr\"}"

        // act
        fhirFunc.doRoute(queueMessage, 1, fhirEngine, actionHistory)

        // assert
        verify(exactly = 1) {
            fhirEngine.doWork(any(), any(), any())
            actionHistory.trackActionParams(queueMessage) // string
            actionHistory.trackLogs(emptyList()) // list actionLog
            workflowEngine.recordAction(any())
        }
    }

    // test translate-fhir
    @Test
    fun `test translate-fhir`() {
        mockkObject(BlobAccess.Companion)
        mockkObject(Message.Companion)
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
                queueMock
            )
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine = makeWorkflowEngine(metadata, settings)

        val fhirFunc = spyk(FHIRFunctions(workflowEngine))

        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"

        every { fhirEngine.doWork(any(), any(), any()) } returns Unit

        val queueMessage = "{\"type\":\"raw\",\"reportId\":\"011bb9ab-15c7-4ecd-8fae-0dd21e04d353\"," +
            "\"blobURL\":\"http://azurite:10000/devstoreaccount1/reports/receive%2Fignore.ignore-full-elr%2F" +
            "None-011bb9ab-15c7-4ecd-8fae-0dd21e04d353-20220729171318.hl7\",\"digest\":\"58ffffffaaffffffc22ffffff" +
            "f044ffffff85ffffffd4ffffffc9ffffffceffffff9bffffffe3ffffff8fffffff86ffffff9a5966fffffff6ffffff87fffff" +
            "fff5bffffffae6015fffffffbffffffdd363037ffffffed51ffffffd3\",\"sender\":\"ignore.ignore-full-elr\"," +
            "\"blobSubFolderName\":\"ignore.ignore-full-elr\"}"

        // act
        fhirFunc.doTranslate(queueMessage, 1, fhirEngine, actionHistory)

        // assert
        verify(exactly = 1) {
            fhirEngine.doWork(any(), any(), any())
            actionHistory.trackActionParams(queueMessage) // string
            actionHistory.trackLogs(emptyList()) // list actionLog
            workflowEngine.recordAction(any())
        }
    }
}