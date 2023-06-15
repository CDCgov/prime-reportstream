package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.LegacyPipelineSender
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.AuthenticatedClaims
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
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.InvalidParameterException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
                Topic.TEST,
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
        mockkObject(AuthenticatedClaims.Companion)
        every { validateFunc.processRequest(any(), any()) } returns resp
        every { engine.settings.findSender(any()) } returns sender // This test only works with org = simple_report
        return Pair(validateFunc, req)
    }

    /** basic /validate endpoint tests **/

    @Test
    fun `test validate endpoint with missing client`() {
        val (validateFunc, req) = setupForDotNotationTests()
        req.httpHeaders += mapOf(
            "content-length" to "4"
        )
        // Invoke the waters function run
        validateFunc.validate(req)
        // processFunction should never be called
        verify(exactly = 0) { validateFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test validate endpoint with schemaName and format`() {
        val (validateFunc, req) = setupForDotNotationTests()
        req.httpHeaders += mapOf(
            "content-length" to "4"
        )
        req.parameters += mapOf(
            "schema" to "one",
            "format" to "CSV"
        )
        // Invoke the waters function run
        validateFunc.validate(req)
        // processFunction should never be called
        verify(exactly = 1) { validateFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test validate endpoint with schemaName but missing format`() {
        val (validateFunc, req) = setupForDotNotationTests()
        req.httpHeaders += mapOf(
            "content-length" to "4"
        )
        req.parameters += mapOf(
            "schema" to "one"
        )
        // Invoke the waters function run
        validateFunc.validate(req)
        // processFunction should never be called
        verify(exactly = 0) { validateFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test validate endpoint with format but missing schemaName`() {
        val (validateFunc, req) = setupForDotNotationTests()
        req.httpHeaders += mapOf(
            "content-length" to "4"
        )
        req.parameters += mapOf(
            "format" to "CSV"
        )
        // Invoke the waters function run
        validateFunc.validate(req)
        // processFunction should never be called
        verify(exactly = 0) { validateFunc.processRequest(any(), any()) }
    }

    @Test
    fun `test validate endpoint with schemaName but schema not found`() {
        val (validateFunc, req) = setupForDotNotationTests()
        req.httpHeaders += mapOf(
            "content-length" to "4"
        )
        req.parameters += mapOf(
            "schema" to "does-not-exist",
            "format" to "CSV"
        )
        // Invoke the waters function run
        validateFunc.validate(req)
        // processFunction should never be called
        verify(exactly = 0) { validateFunc.processRequest(any(), any()) }
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
        validateFunc.validate(req)

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
        val res = validateFunc.validate(req)

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
        val res = validateFunc.validate(req)

        // verify
        assert(res.statusCode == 400)
    }

    @Test
    fun `test processFunction with disallowed duplicate records`() {
        // setup steps
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName = "one",
            allowDuplicates = false
        )

        val req = MockHttpRequestMessage(csvString_2Records)

        every { validateFunc.validateRequest(any()) } returns RequestFunction.ValidatedRequest(
            csvString_2Records,
            sender = sender
        )

        every { accessSpy.isDuplicateItem(any(), any()) } returns true

        // act
        val resp = validateFunc.processRequest(req, sender)

        // assert
        verify(exactly = 2) { engine.isDuplicateItem(any()) }
        assert(resp.status.equals(HttpStatus.OK))
    }

    @Test
    fun `test processFunction with only validation errors`() {
        // setup steps
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName = "one",
            allowDuplicates = true
        )

        val req = MockHttpRequestMessage("")

        every { validateFunc.validateRequest(any()) }.throws(
            ActionError(
                ActionLog(
                    InvalidReportMessage("Your file is bad, please fix"),
                    null,
                    null,
                    null,
                    null,
                    ActionLogLevel.error
                )
            )
        )

        // act
        val resp = validateFunc.processRequest(req, sender)

        // assert
        assert(resp.status.equals(HttpStatus.OK))
        val body = JSONObject(resp.body.toString())
        assertEquals(body.getString("overallStatus"), "Error")

        val errors = body.getJSONArray("errors")
        assertEquals(errors.length(), 1)
        assertEquals((errors[0] as JSONObject).getString("message"), "Your file is bad, please fix")
    }

    @Test
    fun `test processFunction with only validation warnings`() {
        // setup steps
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName = "one",
            allowDuplicates = true
        )

        val req = MockHttpRequestMessage("")

        every { validateFunc.validateRequest(any()) } returns RequestFunction.ValidatedRequest(
            "", // empty string results in a 'no reports' warning
            sender = sender
        )

        // act
        val resp = validateFunc.processRequest(req, sender)

        // assert
        assert(resp.status.equals(HttpStatus.OK))
        val body = JSONObject(resp.body.toString())
        assertEquals(body.getString("overallStatus"), "Valid")

        val errors = body.getJSONArray("errors")
        assertEquals(errors.length(), 0)

        val warnings = body.getJSONArray("warnings")
        assertEquals(warnings.length(), 1)
        assertEquals((warnings[0] as JSONObject).getString("message"), "No reports were found in CSV content")
    }

    @Test
    fun `test processFunction with validation errors and warnings`() {
        // setup steps
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName = "one",
            allowDuplicates = true
        )

        val req = MockHttpRequestMessage("")

        every { validateFunc.validateRequest(any()) }.throws(
            ActionError(
                ActionLog(
                    InvalidReportMessage("Your file is bad, please fix"),
                    null,
                    null,
                    null,
                    null,
                    ActionLogLevel.error
                )
            )
        )

        // need to explicitly add a warning-level log since we're throwing an ActionError
        actionHistory.trackLogs(
            ActionLog(
                InvalidReportMessage("Your file could be better"),
                null,
                null,
                null,
                null,
                ActionLogLevel.warning
            )
        )

        // act
        val resp = validateFunc.processRequest(req, sender)

        // assert
        assert(resp.status.equals(HttpStatus.OK))
        val body = JSONObject(resp.body.toString())
        assertEquals(body.getString("overallStatus"), "Error")

        val errors = body.getJSONArray("errors")
        assertEquals(errors.length(), 1)
        assertEquals((errors[0] as JSONObject).getString("message"), "Your file is bad, please fix")

        val warnings = body.getJSONArray("warnings")
        assertEquals(warnings.length(), 1)
        assertEquals((warnings[0] as JSONObject).getString("message"), "Your file could be better")
    }

    @Test
    fun `test RequestFunction getDummySender`() {
        // setup steps
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))

        // act
        val expectedSender = LegacyPipelineSender(
            "ValidationSender",
            "Internal",
            Sender.Format.CSV,
            CustomerStatus.TESTING,
            "One",
            Topic.TEST
        )
        val sender = validateFunc.getDummySender("One", "CSV")

        // assert
        assertEquals(expectedSender.name, sender.name)
        assertEquals(expectedSender.organizationName, sender.organizationName)
        assertEquals(expectedSender.format, sender.format)
        assertEquals(expectedSender.customerStatus, sender.customerStatus)
        assertEquals(expectedSender.schemaName, sender.schemaName)
        assertEquals(expectedSender.topic, sender.topic)
    }

    @Test
    fun `test RequestFunction getDummySender bad params`() {
        // setup steps
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val validateFunc = spyk(ValidateFunction(engine, actionHistory))

        // act
        assertFailsWith<InvalidParameterException> {
            validateFunc.getDummySender(null, "CSV")
        }
        assertFailsWith<InvalidParameterException> {
            validateFunc.getDummySender("One", null)
        }
        assertFailsWith<InvalidParameterException> {
            validateFunc.getDummySender("DoesNotExist", "CSV")
        }
        assertFailsWith<InvalidParameterException> {
            validateFunc.getDummySender("One", "DoesNotExist")
        }
    }
}