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
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.DO_OKTA_AUTH
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
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val timing1 = mockkClass(Receiver.Timing::class)

    val oneOrganization = DeepOrganization(
        "phd", "test", Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "phd",
                "topic",
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            )
        ),
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
    val csvString_2Records_diff = "senderId,processingModeCode,testOrdered,testName,testResult,testPerformed," +
        "testResultDate,testReportDate,deviceIdentifier,deviceName,specimenId,testId,patientAge,patientRace," +
        "patientEthnicity,patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname," +
        "orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName," +
        "performingFacilityState,performingFacilityZip,specimenSource,patientUniqueId,patientUniqueIdHash," +
        "patientState,firstTest,previousTestType,previousTestResult,healthcareEmployee,symptomatic,symptomsList," +
        "hospitalized,symptomsIcu,congregateResident,congregateResidentType,pregnant\n" +
        "abbottt,P,95209-3,SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay,419984006,95209-3,202112181841-0500,202112151325-0500,LumiraDx SARS-CoV-2 Ag Test_LumiraDx " +
        "UK Ltd.,LumiraDx SARS-CoV-2 Ag Test*,SomeEntityID,SomeEntityID,3,2131-1,2135-2,F,19931,Sussex,1404270765," +
        "ReichertA,NormanBA,19931,97D0667471,Any lab USA,DE,19931,122554006,esyuj9,vhd3cfvvt,DE,NO,bgq0b2e,840533007," +
        "NO,NO,h8jev96rc,YES,YES,YES,257628001,60001007\n" +
        "abbott,P,95209-3,SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassayy,419984006,95209-3,202112181841-0500,202112151325-0500,LumiraDx SARS-CoV-2 Ag Test_LumiraDx " +
        "UK Ltd.,LumiraDx SARS-CoV-2 Ag Test*,SomeEntityID,SomeEntityID,3,2131-1,2135-2,F,19931,Sussex,1404270765," +
        "Reicherts,NormanB,19931,97D0667471,Any lab USA,DE,19931,122554006,esyuj9,vhd3cfvvt,DE,NO,bgq0b2e," +
        "840533007,NO,NO,h8jev96rc,YES,YES,YES,257628001,60001007"

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
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
        mockkObject(AuthenticationStrategy.Companion)
        every { reportFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender(any()) } returns sender // This test only works with org = simple_report
        return Pair(reportFunc, req)
    }

    /** basic /submitToWaters endpoint tests **/

    @Test
    fun `test submitToWaters with missing client`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("foo" to "bar", "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, "simple_report")
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
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
        val jwt = mapOf("foo" to "bar", "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, "simple_report")
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
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
        val jwt = mapOf("foo" to "bar", "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, "bogus_org")
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
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
        val claims = AuthenticatedClaims(jwt)
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
        // This is the most common way our customers use the client string
        req.httpHeaders += mapOf(
            "client" to "simple_report",
            "authentication-type" to "okta",
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
        val claims = AuthenticatedClaims(jwt)
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
        // Now try it with a full client name
        req.httpHeaders += mapOf(
            "client" to "simple_report.default",
            "authentication-type" to DO_OKTA_AUTH,
            "content-length" to "4"
        )
        reportFunc.submitToWaters(req)
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test submitToWaters with okta dot-notation client header - dotted but not default`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt)
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
        // Now try it with a full client name but not with "default"
        // The point of these tests is that the call to the auth code only contains the org prefix 'simple_report'
        req.httpHeaders += mapOf(
            "client" to "simple_report.foobar",
            "authentication-type" to DO_OKTA_AUTH,
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
        var res = reportFunc.run(req)

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
        // setup
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

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest(
            csvString_2Records,
            sender = sender
        )
        every { actionHistory.insertAction(any()) } returns 0
        every { actionHistory.insertAll(any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        every { accessSpy.isDuplicateItem(any(), any()) } returns true

        // act
        var resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 2) { engine.isDuplicateItem(any()) }
        verify(exactly = 1) { actionHistory.trackActionSenderInfo(any(), any()) }
        assert(resp.status.equals(HttpStatus.BAD_REQUEST))
    }
}