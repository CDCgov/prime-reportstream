package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.azure.db.enums.TaskAction
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
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
    // Hits processRequest, tracks 'receive' action in actionHistory
    @Test
    fun `test reportFunction 'report' endpoint`() {
        // Setup
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
        var res = reportFunc.run(req)

        // verify
        assert(res.statusCode == 400)
    }

    // Returns a 400 bad request
    @Test
    fun `test reportFunction 'report' endpoint with unknown sender`() {
        // Setup
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

    /** addDuplicateLogs tests **/
    // test addDuplicateLogs - duplicate file, skipInvalid = false
    @Test
    fun `test addDuplicateLogs, duplicate file, no skipInvalidItems`() {
        // setup
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))

        val actionLogs = ActionLogger()

        // act
        reportFunc.addDuplicateLogs(
            Options.None,
            actionLogs,
            "Duplicate file",
            null
        )

        // assert
        assert(actionLogs.hasErrors())
        assert(actionLogs.errors.size == 1)
        assert(actionLogs.errors[0].scope == ActionLogScope.report)
    }

    // test addDuplicateLogs - duplicate file, skipInvalid = true
    @Test
    fun `test addDuplicateLogs, duplicate file, yes skipInvalidItems`() {
        // setup
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))

        val actionLogs = ActionLogger()

        // act
        reportFunc.addDuplicateLogs(
            Options.SkipInvalidItems,
            actionLogs,
            "Duplicate file",
            null
        )

        // assert
        assert(actionLogs.hasErrors())
        assert(actionLogs.errors.size == 1)
        assert(actionLogs.errors[0].scope == ActionLogScope.report)
    }

    // test addDuplicateLogs - duplicate item, skipInvalid = false
    @Test
    fun `test addDuplicateLogs, duplicate item, no skipInvalidItems`() {
        // setup
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))

        val actionLogs = ActionLogger()

        // act
        reportFunc.addDuplicateLogs(
            Options.None,
            actionLogs,
            "Duplicate item",
            1
        )

        // assert
        assert(actionLogs.hasErrors())
        assert(actionLogs.errors.size == 1)
        assert(actionLogs.errors[0].scope == ActionLogScope.item)
    }

    // test addDuplicateLogs - duplicate item, skipInvalid = true
    @Test
    fun `test addDuplicateLogs, duplicate item, yes skipInvalidItems`() {
        // setup
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val reportFunc = spyk(ReportFunction(engine, actionHistory))

        val actionLogs = ActionLogger()

        // act
        reportFunc.addDuplicateLogs(
            Options.SkipInvalidItems,
            actionLogs,
            "Duplicate item",
            1
        )

        // assert
        assert(!actionLogs.hasErrors())
        assert(actionLogs.warnings.size == 1)
        assert(actionLogs.warnings[0].scope == ActionLogScope.item)
    }

    /** doDuplicateDetection tests **/
    // entire file is duplicate, should not check for item duplication
    @Test
    fun `test doDuplicateDetection, entire file`() {
        // setup
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val report = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
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
        val actionLogs = ActionLogger()

        val req = MockHttpRequestMessage("test")
        every { engine.settings.findSender("Test Sender") } returns sender
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns true

        // act
        reportFunc.doDuplicateDetection(
            req.content!!.toByteArray(),
            sender,
            "payload.txt",
            Options.None,
            report,
            actionLogs
        )

        // assert
        verify(exactly = 1) {
            engine.isDuplicateFile(any(), any())
            reportFunc.addDuplicateLogs(any(), any(), any(), any())
        }
        verify(exactly = 0) { engine.isDuplicateItem(any()) }
    }

    // doDuplicateDetection, one item is duplicate
    @Test
    fun `test doDuplicateDetection, 2 records, one duplicate`() {
        // setup
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val report = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
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
        val actionLogs = ActionLogger()

        val req = MockHttpRequestMessage(csvString_2Records)

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest(
            csvString_2Records,
            sender = sender
        )

        every { engine.settings.findSender("Test Sender") } returns sender
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false
        // first call to isDuplicateItem is false, second is true
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(false)
            .andThen(true)

        // act
        reportFunc.doDuplicateDetection(
            req.content!!.toByteArray(),
            sender,
            "payload.txt",
            Options.SkipInvalidItems,
            report,
            actionLogs
        )

        // assert
        verify(exactly = 2) {
            engine.isDuplicateItem(any())
        }
        verify(exactly = 1) {
            reportFunc.addDuplicateLogs(any(), any(), any(), any())
            engine.isDuplicateFile(any(), any())
        }
        assert(report.itemCount == 1)
    }

    // doDuplicateDetection, all items are duplicate
    @Test
    fun `test doDuplicateDetection, 2 records, both duplicate (sanity check)`() {
        // setup
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val report = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
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
        val actionLogs = ActionLogger()

        val req = MockHttpRequestMessage(csvString_2Records)

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest(
            csvString_2Records,
            sender = sender
        )

        every { engine.settings.findSender("Test Sender") } returns sender
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false
        // first call to isDuplicateItem is false, second is true
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(true)
            .andThen(true)

        // act
        reportFunc.doDuplicateDetection(
            req.content!!.toByteArray(),
            sender,
            "payload.txt",
            Options.SkipInvalidItems,
            report,
            actionLogs
        )

        // assert
        verify(exactly = 3) { // one for each dupe item, one for the file as a whole
            reportFunc.addDuplicateLogs(any(), any(), any(), any())
        }
        verify(exactly = 2) {
            engine.isDuplicateItem(any())
        }
        verify(exactly = 1) {
            engine.isDuplicateFile(any(), any())
        }
        assert(actionLogs.hasErrors())
    }

    /** processFunction tests **/
    // duplicate file can run more than once
    @Test
    fun `test processFunction duplicate when allowed (entire file)`() {
        // setup
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
        every { actionHistory.insertAction(any()) } returns 0

        // act
        reportFunc.processRequest(req, sender)
        reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 0) { engine.isDuplicateFile(any(), any()) }
        verify(exactly = 2) { actionHistory.trackActionSenderInfo(any(), any()) }
    }

    // test duplicate override = true
    @Test
    fun `test processFunction duplicate override false to true`() {
        // setup
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
            "allowDuplicate" to "true"
        )

        val blobInfo = BlobAccess.BlobInfo(Report.Format.CSV, "test", ByteArray(0))

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest("test", sender = sender)
        every { actionHistory.insertAction(any()) } returns 0
        every { actionHistory.insertAll(any()) } returns Unit
        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { actionHistory.createResponseBody(any(), any(), any()) } returns "test"

        // act
        reportFunc.processRequest(req, sender)
        reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 0) { engine.isDuplicateFile(any(), any()) }
    }

    // test duplicate override = false
    @Test
    fun `test processFunction duplicate override true to false`() {
        // setup
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
            "allowDuplicate" to "false"
        )

        val blobInfo = BlobAccess.BlobInfo(Report.Format.CSV, "test", ByteArray(0))

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest("test", sender = sender)
        every { actionHistory.insertAction(any()) } returns 0
        every { actionHistory.insertAll(any()) } returns Unit
        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { actionHistory.createResponseBody(any(), any(), any()) } returns "test"
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns true

        // act
        reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) { reportFunc.doDuplicateDetection(any(), any(), any(), any(), any(), any()) }
    }

    // test processFunction when an error is added to ActionLogs
    @Test
    fun `test processFunction when ActionLogs has an error`() {
        // setup
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
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { actionHistory.createResponseBody(any(), any(), any()) } returns "test"

        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns true

        // act
        var resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) { engine.isDuplicateFile(any(), any()) }
        verify(exactly = 0) { engine.isDuplicateItem(any()) }
        verify(exactly = 1) { actionHistory.trackActionSenderInfo(any(), any()) }
        assert(resp.status.equals(HttpStatus.BAD_REQUEST))
    }

    //  test processFunction when an error is added to ActionLogs
    @Test
    fun `test processFunction when ActionLogs has no error`() {
        // setup
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
        val blobInfo = BlobAccess.BlobInfo(Report.Format.CSV, "test", ByteArray(0))

        val req = MockHttpRequestMessage(csvString_2Records)
        req.parameters += mapOf(
            "option" to Options.SkipInvalidItems.toString()
        )

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest(
            csvString_2Records,
            sender = sender
        )
        every { actionHistory.insertAction(any()) } returns 0
        every { actionHistory.insertAll(any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { actionHistory.createResponseBody(any(), any(), any()) } returns "test"

        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(false)
            .andThen(true)

        // act
        var resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) { engine.isDuplicateFile(any(), any()) }
        verify(exactly = 2) { engine.isDuplicateItem(any()) }
        verify(exactly = 1) { actionHistory.trackActionSenderInfo(any(), any()) }
        assert(resp.status.equals(HttpStatus.CREATED))
    }
}