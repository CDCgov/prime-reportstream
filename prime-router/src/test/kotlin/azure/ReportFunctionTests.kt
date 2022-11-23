package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.FullELRSender
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.SubmissionReceiver
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.TopicReceiver
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
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

class ReportFunctionTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val metadata = UnitTestUtils.simpleMetadata // mockkClass(Metadata::class)
    val settings = mockkClass(SettingsProvider::class)
    val serializer = spyk(Hl7Serializer(metadata, settings))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val timing1 = mockkClass(Receiver.Timing::class)

    val oneOrganization = DeepOrganization(
        "phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "phd",
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            )
        )
    )

    val csvString_2Records = "senderId,processingModeCode,testOrdered,testName,testResult,testPerformed," +
        "testResultDate,testReportDate,deviceIdentifier,deviceName,specimenId,testId,patientAge,patientRace," +
        "patientEthnicity,patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname," +
        "orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName," +
        "performingFacilityState,performingFacilityZip,specimenSource,patientUniqueId,patientUniqueIdHash," +
        "patientState,firstTest,previousTestType,previousTestResult,healthcareEmployee,symptomatic,symptomsList," +
        "hospitalized,symptomsIcu,congregateResident,congregateResidentType,pregnant\n" +
        "abbott,P,95209-3,SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay,419984006,95209-3,202112181841-0500,202112151325-0500,LumiraDx SARS-CoV-2 Ag Test_LumiraDx " +
        "UK Ltd.,LumiraDx SARS-CoV-2 Ag Test*,SomeEntityID,SomeEntityID,3,2131-1,2135-2,F,19931,Sussex,1404270765," +
        "Reichert,NormanA,19931,97D0667471,Any lab USA,DE,19931,122554006,esyuj9,vhd3cfvvt,DE,NO,bgq0b2e,840533007," +
        "NO,NO,h8jev96rc,YES,YES,YES,257628001,60001007\n" +
        "abbott,P,95209-3,SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay,419984006,95209-3,202112181841-0500,202112151325-0500,LumiraDx SARS-CoV-2 Ag Test_LumiraDx " +
        "UK Ltd.,LumiraDx SARS-CoV-2 Ag Test*,SomeEntityID,SomeEntityID,3,2131-1,2135-2,F,19931,Sussex,1404270765," +
        "Reicherts,NormanB,19931,97D0667471,Any lab USA,DE,19931,122554006,esyuj9,vhd3cfvvt,DE,NO,bgq0b2e," +
        "840533007,NO,NO,h8jev96rc,YES,YES,YES,257628001,60001007"

    val hl7_valid = "MSH|^~\\&|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|Winchester House^05D2222542^" +
        "ISO|CDPH CA REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|" +
        "20210803131511.0147+0000||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|" +
        "UNICODE UTF-8|||PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO\n" +
        "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210726\n" +
        "PID|1||09d12345-0987-1234-1234-111b1ee0879f^^^Winchester House&05D2222542&ISO^PI^&05D2222542&ISO||" +
        "Bunny^Bugs^C^^^^L||19000101|M||2106-3^White^HL70005^^^^2.5.1|12345 Main St^^San Jose^CA^95125^USA^^^06085||" +
        "(123)456-7890^PRN^PH^^1^123^4567890|||||||||N^Non Hispanic or Latino^HL70189^^^^2.9||||||||N\n" +
        "ORC|RE|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|" +
        "1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|||||||||1679892871^Doolittle^Doctor^^^^" +
        "^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||(123)456-7890^WPN^PH^^1^123^4567890|20210802||||||Winchester " +
        "House|6789 Main St^^San Jose^CA^95126^^^^06085|(123)456-7890^WPN^PH^^1^123^4567890|6789 Main St^^San " +
        "Jose^CA^95126\n" +
        "OBR|1|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|" +
        "1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|94558-4^SARS-CoV-2 " +
        "(COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.68|||" +
        "202108020000-0500|202108020000-0500||||||||1679892871^Doolittle^Doctor^^^^^^" +
        "CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|(123)456-7890^WPN^PH^^1^123^4567890|||||202108020000-0500|||F\n" +
        "OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay^LN^^^^2.68||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^" +
        "HL70078^^^^2.7|||F|||202108020000-0500|05D2222542^ISO||10811877011290_DIT^10811877011290^99ELR^^^" +
        "^2.68^^10811877011290_DIT||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&" +
        "ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126^^^^06085\n" +
        "OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||N^No^HL70136||" +
        "||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^" +
        "ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST\n" +
        "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||N^No^HL70136||||" +
        "||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^" +
        "^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085||" +
        "|||QST\n" +
        "OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|" +
        "||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO" +
        "^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST\n" +
        "OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136|||" +
        "|||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&" +
        "2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST\n" +
        "SPM|1|1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO^1234d1d1-95fe-462c-8ac6-46728dba581c&&" +
        "05D2222542&ISO||445297001^Swab of internal nose^SCT^^^^2.67||||53342003^Internal nose structure" +
        " (body structure)^SCT^^^^2020-09-01|||||||||202108020000-0500|20210802000006.0000-0500"

    val hl7_5_separator = "MSH|^~\\&#|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|Winchester" +
        " House^05D2222542^" +
        "ISO|CDPH CA REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|" +
        "20210803131511.0147+0000||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|" +
        "UNICODE UTF-8|||PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO\n" +
        "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210726\n" +
        "PID|1||09d12345-0987-1234-1234-111b1ee0879f^^^Winchester House&05D2222542&ISO^PI^&05D2222542&ISO||" +
        "Bunny^Bugs^C^^^^L||19000101|M||2106-3^White^HL70005^^^^2.5.1|12345 Main St^^San Jose^CA^95125^USA^^^06085||" +
        "(123)456-7890^PRN^PH^^1^123^4567890|||||||||N^Non Hispanic or Latino^HL70189^^^^2.9||||||||N\n" +
        "ORC|RE|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|" +
        "1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|||||||||1679892871^Doolittle^Doctor^^^^" +
        "^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||(123)456-7890^WPN^PH^^1^123^4567890|20210802||||||Winchester " +
        "House|6789 Main St^^San Jose^CA^95126^^^^06085|(123)456-7890^WPN^PH^^1^123^4567890|6789 Main St^^San " +
        "Jose^CA^95126\n" +
        "OBR|1|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|" +
        "1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|94558-4^SARS-CoV-2 " +
        "(COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.68|||" +
        "202108020000-0500|202108020000-0500||||||||1679892871^Doolittle^Doctor^^^^^^" +
        "CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|(123)456-7890^WPN^PH^^1^123^4567890|||||202108020000-0500|||F\n" +
        "OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay^LN^^^^2.68||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^" +
        "HL70078^^^^2.7|||F|||202108020000-0500|05D2222542^ISO||10811877011290_DIT^10811877011290^99ELR^^^" +
        "^2.68^^10811877011290_DIT||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&" +
        "ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126^^^^06085\n" +
        "OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||N^No^HL70136||" +
        "||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^" +
        "ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST\n" +
        "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||N^No^HL70136||||" +
        "||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^" +
        "^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085||" +
        "|||QST\n" +
        "OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|" +
        "||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO" +
        "^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST\n" +
        "OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136|||" +
        "|||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&" +
        "2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST\n" +
        "SPM|1|1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO^1234d1d1-95fe-462c-8ac6-46728dba581c&&" +
        "05D2222542&ISO||445297001^Swab of internal nose^SCT^^^^2.67||||53342003^Internal nose structure" +
        " (body structure)^SCT^^^^2020-09-01|||||||||202108020000-0500|20210802000006.0000-0500"

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
                .blobAccess(blobMock).queueAccess(queueMock).hl7Serializer(serializer).build()
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

    /** basic /reports endpoint tests **/
    /**
     * Do all the zany setup work needed to run the 'waters' endpoint as a test.
     * Written specifically for the 'client header' tests below, to start.  But could probably
     * be generalized for all 'waters' endpoint tests in the future.
     */
    private fun setupForDotNotationTests(): Pair<ReportFunction, MockHttpRequestMessage> {
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // does not matter what type of Sender it is for this test
        val sender = CovidSender("default", "simple_report", Sender.Format.CSV, schemaName = "one")
        val req = MockHttpRequestMessage("test")
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))
        val resp = HttpUtilities.okResponse(req, "fakeOkay")
        every { engine.db } returns accessSpy
        mockkObject(AuthenticatedClaims.Companion)
        every { reportFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender(any()) } returns sender // This test only works with org = simple_report
        return Pair(reportFunc, req)
    }

    /** Do all the setup required to be able to run any process request tests**/
    private fun setupForProcessRequestTests(actionHistory: ActionHistory):
        Triple<ReportFunction, MockHttpRequestMessage, Sender> {
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val reportFunc = spyk(ReportFunction(engine, actionHistory))
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val blobInfo = BlobAccess.BlobInfo(Report.Format.CSV, "test", ByteArray(0))

        val req = MockHttpRequestMessage(csvString_2Records)
        req.httpHeaders += mapOf(
            "client" to "Test Sender",
            "content-length" to "4"
        )
        every { reportFunc.validateRequest(any()) } returns RequestFunction.ValidatedRequest(
            csvString_2Records,
            sender = sender
        )
        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit
        every { actionHistory.trackLogs(any<ActionLog>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.actionName } returns TaskAction.receive
        every { actionHistory.action.sendingOrg } returns "Test Sender"
        mockkObject(SubmissionReceiver.Companion)
        var mockReceiver = spyk(TopicReceiver(engine, actionHistory))
        every { SubmissionReceiver.getSubmissionReceiver(any(), any(), any()) } returns mockReceiver

        every {
            mockReceiver.validateAndMoveToProcessing(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns Unit

        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        val bodyBytes = "".toByteArray()
        mockkObject(ReportWriter)
        every { ReportWriter.getBodyBytes(any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(any(), any(), any()) }.returns(blobInfo)
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { accessSpy.isDuplicateItem(any(), any()) } returns false
        return Triple(reportFunc, req, sender)
    }

    /** basic /submitToWaters endpoint tests **/

    @Test
    fun `test submitToWaters with missing client`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("scope" to "simple_report.default.report", "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Server2Server)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
        req.httpHeaders += mapOf(
            "content-length" to "4"
        )
        // Invoke the waters function run
        reportFunc.submitToWaters(req)
        // processFunction should never be called
        verify(exactly = 0) { reportFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test submitToWaters with server2server auth - basic happy path`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("scope" to "simple_report.default.report", "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Server2Server)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
        req.httpHeaders += mapOf(
            "client" to "simple_report",
            "content-length" to "4"
        )
        // Invoke the waters function run
        reportFunc.submitToWaters(req)
        // processFunction should be called
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test submitToWaters with server2server auth - claim does not match`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("scope" to "bogus_org.default.report", "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Server2Server)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
        req.httpHeaders += mapOf(
            "client" to "simple_report",
            "content-length" to "4"
        )
        // Invoke the waters function run
        reportFunc.submitToWaters(req)
        // processFunction should never be called
        verify(exactly = 0) { reportFunc.processRequest(any(), any()) }
    }

    /**
     * Test that header of the form client:simple_report.default works with the auth code.
     */
    @Test
    fun `test submitToWaters with okta dot-notation client header - basic happy path`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
        // This is the most common way our customers use the client string
        req.httpHeaders += mapOf(
            "client" to "simple_report",
            "content-length" to "4"
        )
        // Invoke the waters function run
        reportFunc.submitToWaters(req)
        // processFunction should be called
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test submitToWaters with okta dot-notation client header - full dotted name`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
        // Now try it with a full client name
        req.httpHeaders += mapOf(
            "client" to "simple_report.default",
            "content-length" to "4"
        )
        reportFunc.submitToWaters(req)
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test submitToWaters with okta dot-notation client header - dotted but not default`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
        // Now try it with a full client name but not with "default"
        // The point of these tests is that the call to the auth code only contains the org prefix 'simple_report'
        req.httpHeaders += mapOf(
            "client" to "simple_report.foobar",
            "content-length" to "4"
        )
        reportFunc.submitToWaters(req)
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test reportFunction 'report' endpoint for full ELR sender`() {
        // Setup
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val sender = FullELRSender("Test ELR Sender", "test", Sender.Format.HL7)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))

        val req = MockHttpRequestMessage("test")
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { reportFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender(any()) } returns sender

        req.httpHeaders += mapOf(
            "client" to "Test ELR Sender",
            "content-length" to "4"
        )

        // Invoke function run
        reportFunc.run(req)

        // processFunction should be called
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    // TODO: Will need to copy this test for Full ELR senders once receiving full ELR is implemented (see #5051)
    // Hits processRequest, tracks 'receive' action in actionHistory
    @Test
    fun `test reportFunction 'report' endpoint`() {
        // Setup
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val sender = CovidSender("Test Sender", "test", Sender.Format.CSV, schemaName = "one")

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))

        val req = MockHttpRequestMessage("test")
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { reportFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender("Test Sender") } returns sender

        req.httpHeaders += mapOf(
            "client" to "Test Sender",
            "content-length" to "4"
        )

        // Invoke function run
        reportFunc.run(req)

        // processFunction should be called
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    // Returns 400 bad request
    @Test
    fun `test reportFunction 'report' endpoint with no sender name`() {
        // Setup
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val sender = CovidSender("Test Sender", "test", Sender.Format.CSV, schemaName = "one")

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))

        val req = MockHttpRequestMessage("test")
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { reportFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender("Test Sender") } returns sender

        req.httpHeaders += mapOf(
            "content-length" to "4"
        )

        // Invoke function run
        val res = reportFunc.run(req)

        // verify
        assert(res.statusCode == 400)
    }

    // Returns a 400 bad request
    @Test
    fun `test reportFunction 'report' endpoint with unknown sender`() {
        // Setup
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))

        val req = MockHttpRequestMessage("test")
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { reportFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender("Test Sender") } returns null

        req.httpHeaders += mapOf(
            "client" to "Test Sender",
            "content-length" to "4"
        )

        // Invoke function run
        val res = reportFunc.run(req)

        // verify
        assert(res.statusCode == 400)
    }

    // test processFunction when an error is added to ActionLogs
    @Test
    fun `test processFunction when ActionLogs has an error`() {
        // setup steps
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val blobInfo = BlobAccess.BlobInfo(Report.Format.CSV, "test", ByteArray(0))

        val req = MockHttpRequestMessage(csvString_2Records)

        every { reportFunc.validateRequest(any()) } returns RequestFunction.ValidatedRequest(
            csvString_2Records,
            sender = sender
        )
        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"
        every { actionHistory.action.actionName } returns TaskAction.receive
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        val bodyBytes = "".toByteArray()
        mockkObject(ReportWriter)
        every { ReportWriter.getBodyBytes(any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(any(), any(), any()) }.returns(blobInfo)
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        every { accessSpy.isDuplicateItem(any(), any()) } returns true

        // act
        val resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 2) { engine.isDuplicateItem(any()) }
        verify(exactly = 1) { actionHistory.trackActionSenderInfo(any(), any()) }
        assert(resp.status.equals(HttpStatus.BAD_REQUEST))
    }

    // test processFunction when basic hl7 message is passed
    @Test
    fun `test processFunction when basic hl7 message is passed`() {
        // setup steps

        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))

        val req = MockHttpRequestMessage(hl7_valid)

        every { reportFunc.validateRequest(any()) } returns RequestFunction.ValidatedRequest(
            hl7_valid,
            sender = sender
        )
        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"
        every { actionHistory.action.actionName } returns TaskAction.receive
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        val bodyBytes = "".toByteArray()
        mockkObject(ReportWriter)
        every { ReportWriter.getBodyBytes(any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(any(), any(), any()) }.returns(blobInfo)
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { serializer.checkLIVDValueExists(any(), any()) } returns true

        every { accessSpy.isDuplicateItem(any(), any()) } returns false

        // act
        val resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) { engine.isDuplicateItem(any()) }
        verify(exactly = 1) { actionHistory.trackActionSenderInfo(any(), any()) }
        assert(resp.status.equals(HttpStatus.CREATED))
    }

    // test processFunction when basic hl7 message with 5 separators is passed
    @Test
    fun `test processFunction when basic hl7 message with 5 separators is passed`() {
        // setup steps

        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))

        val req = MockHttpRequestMessage(hl7_5_separator)

        every { reportFunc.validateRequest(any()) } returns RequestFunction.ValidatedRequest(
            hl7_5_separator,
            sender = sender
        )
        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"
        every { actionHistory.action.actionName } returns TaskAction.receive
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        val bodyBytes = "".toByteArray()
        mockkObject(ReportWriter)
        every { ReportWriter.getBodyBytes(any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(any(), any(), any()) }.returns(blobInfo)
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { serializer.checkLIVDValueExists(any(), any()) } returns true

        every { accessSpy.isDuplicateItem(any(), any()) } returns false

        // act
        val resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) { engine.isDuplicateItem(any()) }
        verify(exactly = 1) { actionHistory.trackActionSenderInfo(any(), any()) }
        assert(resp.status.equals(HttpStatus.CREATED))
    }

    @Test
    fun `test processRequest invalid Option`() {
        // setup steps
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        var (reportFunc, req, sender) = setupForProcessRequestTests(actionHistory)
        req.queryParameters["option"] = "INVALID OPTION"

        // act on invalid options
        var resp = reportFunc.processRequest(req, sender)

        // No report is created and an error is returned to explain the issue
        assert(resp.status.equals(HttpStatus.BAD_REQUEST))
        verify(exactly = 1) { actionHistory.trackLogs(any<ActionLog>()) }
    }

    @Test
    fun `test processRequest Options deprecated`() {
        // setup steps
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        var (reportFunc, req, sender) = setupForProcessRequestTests(actionHistory)
        req.queryParameters["option"] = "SkipInvalidItems"

        // act on invalid options
        var resp = reportFunc.processRequest(req, sender)

        // Report still created but a warning is returned
        assert(resp.status.equals(HttpStatus.CREATED))
        verify(exactly = 1) { actionHistory.trackLogs(any<ActionLog>()) }
    }

    @Test
    fun `test processRequest Options happy path`() {
        // setup steps
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        var (reportFunc, req, sender) = setupForProcessRequestTests(actionHistory)
        req.queryParameters["option"] = "ValidatePayload"

        // act on invalid options
        var resp = reportFunc.processRequest(req, sender)

        // Report Validated and no warnings returned
        assert(resp.status.equals(HttpStatus.OK))
        verify(exactly = 0) { actionHistory.trackLogs(any<ActionLog>()) }
    }
}