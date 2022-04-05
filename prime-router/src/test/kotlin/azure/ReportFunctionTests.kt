package gov.cdc.prime.router.azure

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.PrincipalLevel
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

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
                .blobAccess(blobMock).queueAccess(queueMock).build()
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

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

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val sender = Sender("default", "simple_report", Sender.Format.CSV, "test", schemaName = "one")
        val req = MockHttpRequestMessage("test")
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))
        val resp = HttpUtilities.okResponse(req, "fakeOkay")
        every { engine.db } returns accessSpy
        val oktaAuth = spyk(OktaAuthentication(PrincipalLevel.USER))
        mockkObject(AuthenticationStrategy.Companion)
        every { AuthenticationStrategy.authStrategy(any(), any(), any()) } returns oktaAuth
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt)
        every { oktaAuth.authenticate(any()) } returns claims
        every { reportFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender(any()) } returns sender // This test only works with org = simple_report
        return Pair(reportFunc, req)
    }

    /**
     * Test that header of the form client:simple_report.default works with the auth code.
     */
    @Test
    fun `test the waters function with dot-notation client header - basic happy path`() {
        val (reportFunc, req) = setupForDotNotationTests()
        // This is the most common way our customers use the client string
        req.httpHeaders += mapOf(
            "client" to "simple_report",
            "authentication-type" to "okta",
            "content-length" to "4"
        )
        // Invoke the waters function run
        reportFunc.report(req)
        // processFunction should be called
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test the waters function with dot-notation client header - full dotted name`() {
        val (reportFunc, req) = setupForDotNotationTests()
        // Now try it with a full client name
        req.httpHeaders += mapOf(
            "client" to "simple_report.default",
            "authentication-type" to "okta",
            "content-length" to "4"
        )
        reportFunc.report(req)
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test the waters function with dot-notation client header - dotted but not default`() {
        val (reportFunc, req) = setupForDotNotationTests()
        // Now try it with a full client name but not with "default"
        // The point of these tests is that the call to the auth code only contains the org prefix 'simple_report'
        req.httpHeaders += mapOf(
            "client" to "simple_report.foobar",
            "authentication-type" to "okta",
            "content-length" to "4"
        )
        reportFunc.report(req)
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    // Hits processRequest, tracks 'receive' action in actionHistory
    @Test
    fun `test reportFunction 'report' endpoint`() {
        // Setup
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val sender = Sender("Test Sender", "test", Sender.Format.CSV, "test", schemaName = "one")

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
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val sender = Sender("Test Sender", "test", Sender.Format.CSV, "test", schemaName = "one")

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
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
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

    // duplicate file can run more than once
    @Test
    fun `test processFunction duplicate when allowed`() {
        // setup
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))
        val sender = Sender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            "test",
            schemaName =
            "one",
            allowDuplicates = true
        )

        val req = MockHttpRequestMessage("test")
        req.parameters += mapOf("option" to Options.ValidatePayload.toString())

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest("test", sender = sender)
        every { actionHistory.insertAction(any()) } returns 1

        // act
        reportFunc.processRequest(req, sender)
        reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 0) { engine.verifyNoDuplicateFile(any(), any(), any()) }
        verify(exactly = 2) { actionHistory.trackActionSenderInfo(any(), any()) }
    }

    // request is rejected when duplicate
    @Test
    fun `test processFunction duplicate when not allowed`() {
        // setup
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))
        val sender = Sender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            "test",
            schemaName =
            "one",
            allowDuplicates = false
        )

        val req = MockHttpRequestMessage("test")
        req.parameters += mapOf("option" to Options.ValidatePayload.toString())

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest("test", sender = sender)
        every { actionHistory.insertAction(any()) } returns 1
        every { actionHistory.insertAll(any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "org"
        every { actionHistory.action.sendingOrgClient } returns "client"

        // act
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false
        reportFunc.processRequest(req, sender)
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns true
        reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 2) { engine.verifyNoDuplicateFile(any(), any(), any()) }
        verify(exactly = 2) { actionHistory.trackActionSenderInfo(any(), any()) }
    }

    // test duplicate override = true
    @Test
    fun `test processFunction duplicate when allowed via override`() {
        // setup
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))
        val sender = Sender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            "test",
            schemaName =
            "one",
            allowDuplicates = false
        )

        val req = MockHttpRequestMessage("test")
        req.parameters += mapOf(
            "option" to Options.ValidatePayload.toString(),
            "allowDuplicate" to "true"
        )

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest("test", sender = sender)
        every { actionHistory.insertAction(any()) } returns 1
        every { actionHistory.insertAll(any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "org"
        every { actionHistory.action.sendingOrgClient } returns "client"

        // act
        reportFunc.processRequest(req, sender)
        reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 0) { engine.verifyNoDuplicateFile(any(), any(), any()) }
        verify(exactly = 2) { actionHistory.trackActionSenderInfo(any(), any()) }
    }

    // test duplicate override = false
    @Test
    fun `test processFunction duplicate when not allowed via override`() {
        // setup
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))
        val sender = Sender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            "test",
            schemaName =
            "one",
            allowDuplicates = true
        )

        val req = MockHttpRequestMessage("test")
        req.parameters += mapOf(
            "option" to Options.ValidatePayload.toString(),
            "allowDuplicate" to "false"
        )

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest("test", sender = sender)
        every { actionHistory.insertAction(any()) } returns 1
        every { actionHistory.insertAll(any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "org"
        every { actionHistory.action.sendingOrgClient } returns "client"

        // act
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false
        reportFunc.processRequest(req, sender)
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns true
        reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 2) { engine.verifyNoDuplicateFile(any(), any(), any()) }
        verify(exactly = 2) { actionHistory.trackActionSenderInfo(any(), any()) }
    }
}