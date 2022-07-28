package gov.cdc.prime.router.azure

import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
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

class ValidateFunctionTests {
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

        // setup
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()
    }

    /** basic /validate endpoint tests **/
    /**
     * Do all the zany setup work needed to run the 'waters' endpoint as a test.
     * Written specifically for the 'client header' tests below, to start.  But could probably
     * be generalized for all 'waters' endpoint tests in the future.
     */
    private fun setupForDotNotationTests(): Pair<ValidateFunction, MockHttpRequestMessage> {
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
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))
        val resp = HttpUtilities.okResponse(req, "fakeOkay")
        every { engine.db } returns accessSpy
        mockkObject(AuthenticationStrategy.Companion)
        every { validateFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender(any()) } returns sender // This test only works with org = simple_report
        return Pair(validateFunc, req)
    }

    /** basic /validate endpoint tests **/

    @Test
    fun `test validate endpoint with missing client`() {
        val (validateFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("foo" to "bar", "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, "simple_report")
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
        req.httpHeaders += mapOf(
            "content-length" to "4"
        )
        // Invoke the waters function run
        validateFunc.run(req)
        // processFunction should never be called
        verify(exactly = 0) { validateFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test validate endpoint with server2server auth - basic happy path`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("foo" to "bar", "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, "simple_report")
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
        req.httpHeaders += mapOf(
            "client" to "simple_report",
            "content-length" to "4"
        )
        // Invoke the waters function run
        reportFunc.run(req)
        // processFunction should be called
        verify(exactly = 1) { reportFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test validate endpoint with server2server auth - claim does not match`() {
        val (reportFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("foo" to "bar", "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, "bogus_org")
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
        req.httpHeaders += mapOf(
            "client" to "simple_report",
            "content-length" to "4"
        )
        // Invoke the waters function run
        reportFunc.run(req)
        // processFunction should never be called
        verify(exactly = 0) { reportFunc.processRequest(any(), any()) }
    }

    /**
     * Test that header of the form client:simple_report.default works with the auth code.
     */
    @Test
    fun `test validate endpoint with okta dot-notation client header - basic happy path`() {
        val (validateFunc, req) = setupForDotNotationTests()
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
        validateFunc.run(req)
        // processFunction should be called
        verify(exactly = 1) { validateFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test validate endpoint with okta dot-notation client header - full dotted name`() {
        val (validateFunc, req) = setupForDotNotationTests()
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt)
        every { AuthenticationStrategy.Companion.authenticate(any()) } returns claims
        // Now try it with a full client name
        req.httpHeaders += mapOf(
            "client" to "simple_report.default",
            "authentication-type" to DO_OKTA_AUTH,
            "content-length" to "4"
        )
        validateFunc.run(req)
        verify(exactly = 1) { validateFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test validate endpoint with okta dot-notation client header - dotted but not default`() {
        val (validateFunc, req) = setupForDotNotationTests()
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
        validateFunc.run(req)
        verify(exactly = 1) { validateFunc.processRequest(any(), any()) }
    }

    // TODO: Will need to copy this test for Full ELR senders once receiving full ELR is implemented (see #5051)
    // Hits processRequest, tracks 'receive' action in actionHistory
    @Test
    fun `test validate endpoint`() {
        // Setup
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val sender = CovidSender("Test Sender", "test", Sender.Format.CSV, schemaName = "one")

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))

        val req = MockHttpRequestMessage("test")
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { validateFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender("Test Sender") } returns sender

        req.httpHeaders += mapOf(
            "client" to "Test Sender",
            "content-length" to "4"
        )

        // Invoke function run
        validateFunc.run(req)

        // processFunction should be called
        verify(exactly = 1) { validateFunc.processRequest(any(), any()) }
    }

    // Returns 400 bad request
    @Test
    fun `test validate endpoint with no sender name`() {
        // Setup
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val sender = CovidSender("Test Sender", "test", Sender.Format.CSV, schemaName = "one")

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))

        val req = MockHttpRequestMessage("test")
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { validateFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender("Test Sender") } returns sender

        req.httpHeaders += mapOf(
            "content-length" to "4"
        )

        // Invoke function run
        val res = validateFunc.run(req)

        // verify
        assert(res.statusCode == 400)
    }

    // Returns a 400 bad request
    @Test
    fun `test validate endpoint with unknown sender`() {
        // Setup
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))

        val req = MockHttpRequestMessage("test")
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { validateFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender("Test Sender") } returns null

        req.httpHeaders += mapOf(
            "client" to "Test Sender",
            "content-length" to "4"
        )

        // Invoke function run
        val res = validateFunc.run(req)

        // verify
        assert(res.statusCode == 400)
    }
}