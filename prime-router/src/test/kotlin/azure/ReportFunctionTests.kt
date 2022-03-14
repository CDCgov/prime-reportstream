package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ActionLog
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
import gov.cdc.prime.router.azure.db.enums.TaskAction
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

    val csvString_3Records = "senderId,processingModeCode,testOrdered,testName,testResult," +
        "testPerformed,testResultDate," +
        "testReportDate,deviceIdentifier,deviceName,specimenId,testId,patientAge,patientRace,patientEthnicity," +
        "patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname,orderingProviderFname," +
        "orderingProviderZip,performingFacility,performingFacilityName,performingFacilityState," +
        "performingFacilityZip,specimenSource,patientUniqueId,patientUniqueIdHash,patientState,firstTest," +
        "previousTestType,previousTestResult,healthcareEmployee,symptomatic,symptomsList,hospitalized," +
        "symptomsIcu,congregateResident,congregateResidentType,pregnant\n" +
        "abbott,P,95209-3,SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay,419984006,95209-3,202112181841-0500,202112151325-0500,LumiraDx SARS-CoV-2 Ag " +
        "Test_LumiraDx UK Ltd.,LumiraDx SARS-CoV-2 Ag Test*,SomeEntityID,SomeEntityID,3,2131-1,2135-2,F," +
        "19931,Sussex,1404270765,Reichert,NormanA,19931,97D0667471,Any lab USA,DE,19931,122554006,esyuj9," +
        "vhd3cfvvt,DE,NO,bgq0b2e,840533007,NO,NO,h8jev96rc,YES,YES,YES,257628001,60001007\n" +
        "abbott,P,95209-3,SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid" +
        " immunoassay,419984006,95209-3,202112181841-0500,202112151325-0500,LumiraDx SARS-CoV-2 Ag Test_LumiraDx " +
        "UK Ltd.,LumiraDx SARS-CoV-2 Ag Test*,SomeEntityID,SomeEntityID,3,2131-1,2135-2,F,19931,Sussex," +
        "1404270765,Reicherts,NormanB,19931,97D0667471,Any lab USA,DE,19931,122554006,esyuj9,vhd3cfvvt,DE," +
        "NO,bgq0b2e,840533007,NO,NO,h8jev96rc,YES,YES,YES,257628001,60001007\n" +
        "abbott,P,95209-3,SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid " +
        "immunoassay,419984006,95209-3,202112181841-0500,202112151325-0500,LumiraDx SARS-CoV-2 Ag Test_LumiraDx" +
        " UK Ltd.,LumiraDx SARS-CoV-2 Ag Test*,SomeEntityID,SomeEntityID,3,2131-1,2135-2,F,19931,Sussex," +
        "1404270765,Reicherta,NormanB,19931,97D0667471,Any lab USA,DE,19931,122554006,esyuj9,vhd3cfvvt,DE," +
        "NO,bgq0b2e,840533007,NO,NO,h8jev96rc,YES,YES,YES,257628001,60001007"

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
        verify(exactly = 0) { engine.verifyNoDuplicateFile(any(), any(), any()) }
        verify(exactly = 2) { actionHistory.trackActionSenderInfo(any(), any()) }
    }

    // request is rejected when duplicate
    @Test
    fun `test processFunction duplicate when not allowed (entire file)`() {
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

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest("test", sender = sender)
        every { actionHistory.insertAction(any()) } returns 0
        every { actionHistory.insertAll(any()) } returns Unit

        // act
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns true
        val resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) { engine.verifyNoDuplicateFile(any(), any(), any()) }
        verify(exactly = 0) { engine.verifyNoDuplicateItem(any()) }
        verify(exactly = 1) { actionHistory.trackActionSenderInfo(any(), any()) }
        assert(resp.status.equals(com.microsoft.azure.functions.HttpStatus.BAD_REQUEST))
    }

    // test duplicate override = true
    @Test
    fun `test processFunction duplicate when allowed via override (entire file)`() {
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
        verify(exactly = 0) { engine.verifyNoDuplicateFile(any(), any(), any()) }
        verify(exactly = 0) { engine.verifyNoDuplicateItem(any()) }
        verify(exactly = 2) { actionHistory.trackActionSenderInfo(any(), any()) }
    }

    // test duplicate override = false
    @Test
    fun `test processFunction duplicate when not allowed via override (entire file)`() {
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

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest("test", sender = sender)
        every { actionHistory.insertAction(any()) } returns 0
        every { actionHistory.insertAll(any()) } returns Unit

        // act
        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns true
        val resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) { engine.verifyNoDuplicateFile(any(), any(), any()) }
        verify(exactly = 0) { engine.verifyNoDuplicateItem(any()) }
        verify(exactly = 1) { actionHistory.trackActionSenderInfo(any(), any()) }
        assert(resp.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `test processFunction no duplicate, all new items`() {
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

        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false

        // act
        every { accessSpy.isDuplicateItem(any(), any()) } returns false
        var resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) {
            engine.verifyNoDuplicateFile(any(), any(), any())
            actionHistory.trackActionSenderInfo(any(), any())
        }
        verify(exactly = 2) { engine.verifyNoDuplicateItem(any()) }
        assert(resp.status.equals(HttpStatus.CREATED))
    }

    @Test
    fun `test processFunction no duplicate, 1 new 1 dupe with SkipInvalidItems`() {
        mockkObject(BlobAccess.Companion)
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

        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        every {
            BlobAccess.Companion.uploadBody(
                Report.Format.CSV,
                any(),
                any(),
                sender.fullName,
                Event.EventAction.RECEIVE
            )
        }.returns(BlobAccess.BlobInfo(Report.Format.CSV, "http://anyblob.com", "".toByteArray()))

        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false

        // first call to isDuplicateItem is false, second is true
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(false)
            .andThen(true)
        // act
        var resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) {
            engine.verifyNoDuplicateFile(any(), any(), any())
            actionHistory.trackActionSenderInfo(any(), any())
        }
        verify(exactly = 2) { engine.verifyNoDuplicateItem(any()) }
        assert(resp.status.equals(HttpStatus.CREATED))
        val respStr = resp.body.toString()
        assert(respStr.contains("Item 2 is a duplicate"))
    }

    @Test
    fun `test processFunction no duplicate, 1 new 1 dupe no SkipInvalidItems`() {
        mockkObject(BlobAccess.Companion)
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

        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        every {
            BlobAccess.Companion.uploadBody(
                Report.Format.CSV,
                any(),
                any(),
                sender.fullName,
                Event.EventAction.RECEIVE
            )
        }.returns(BlobAccess.BlobInfo(Report.Format.CSV, "http://anyblob.com", "".toByteArray()))

        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false

        // first call to isDuplicateItem is false, second is true
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(false)
            .andThen(true)
        // act
        var resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) {
            engine.verifyNoDuplicateFile(any(), any(), any())
            actionHistory.trackActionSenderInfo(any(), any())
        }
        verify(exactly = 2) { engine.verifyNoDuplicateItem(any()) }
        assert(resp.status.equals(HttpStatus.BAD_REQUEST))
        val respStr = resp.body.toString()
        assert(respStr.contains("Item 2 is a duplicate"))
    }

    @Test
    fun `test processFunction no duplicate, both new, identical with SkipInvalidItems`() {
        mockkObject(BlobAccess.Companion)
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

        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        every {
            BlobAccess.Companion.uploadBody(
                Report.Format.CSV,
                any(),
                any(),
                sender.fullName,
                Event.EventAction.RECEIVE
            )
        }.returns(BlobAccess.BlobInfo(Report.Format.CSV, "http://anyblob.com", "".toByteArray()))

        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false

        // first call to isDuplicateItem is false, second is true
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(true)
            .andThen(true)
        // act
        var resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) {
            engine.verifyNoDuplicateFile(any(), any(), any())
            actionHistory.trackActionSenderInfo(any(), any())
        }
        verify(exactly = 2) { engine.verifyNoDuplicateItem(any()) }
        assert(resp.status.equals(HttpStatus.BAD_REQUEST))
        val respStr = resp.body.toString()
        assert(respStr.contains("Duplicate file detected"))
    }

    @Test
    fun `test processFunction no duplicate, 3 identical new rows with SkipInvalidItems`() {
        mockkObject(BlobAccess.Companion)
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

        val req = MockHttpRequestMessage(csvString_3Records)
        req.parameters += mapOf(
            "option" to Options.SkipInvalidItems.toString()
        )

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest(
            csvString_3Records,
            sender = sender
        )
        every { actionHistory.insertAction(any()) } returns 0
        every { actionHistory.insertAll(any()) } returns Unit

        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        every {
            BlobAccess.Companion.uploadBody(
                Report.Format.CSV,
                any(),
                any(),
                sender.fullName,
                Event.EventAction.RECEIVE
            )
        }.returns(BlobAccess.BlobInfo(Report.Format.CSV, "http://anyblob.com", "".toByteArray()))

        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false

        // first call to isDuplicateItem is false, second is true
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(false)
            .andThen(true)
            .andThen(true)
        // act
        var resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) {
            engine.verifyNoDuplicateFile(any(), any(), any())
            actionHistory.trackActionSenderInfo(any(), any())
        }
        verify(exactly = 3) { engine.verifyNoDuplicateItem(any()) }
        assert(resp.status.equals(HttpStatus.CREATED))
        val respStr = resp.body.toString()
        assert(respStr.contains("Item 2 is a duplicate"))
        assert(respStr.contains("Item 3 is a duplicate"))
    }

    @Test
    fun `test processFunction no duplicate, 3 identical new rows no SkipInvalidItems`() {
        mockkObject(BlobAccess.Companion)
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

        val req = MockHttpRequestMessage(csvString_3Records)

        every { reportFunc.validateRequest(any()) } returns ReportFunction.ValidatedRequest(
            csvString_3Records,
            sender = sender
        )
        every { actionHistory.insertAction(any()) } returns 0
        every { actionHistory.insertAll(any()) } returns Unit

        every { engine.queue.sendMessage(any(), any(), any()) } returns Unit
        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        every {
            BlobAccess.Companion.uploadBody(
                Report.Format.CSV,
                any(),
                any(),
                sender.fullName,
                Event.EventAction.RECEIVE
            )
        }.returns(BlobAccess.BlobInfo(Report.Format.CSV, "http://anyblob.com", "".toByteArray()))

        every { accessSpy.isDuplicateReportFile(any(), any(), any(), any()) } returns false

        // first call to isDuplicateItem is false, second is true
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(false)
            .andThen(true)
            .andThen(true)
        // act
        var resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) {
            engine.verifyNoDuplicateFile(any(), any(), any())
            actionHistory.trackActionSenderInfo(any(), any())
        }
        verify(exactly = 3) { engine.verifyNoDuplicateItem(any()) }
        assert(resp.status.equals(HttpStatus.BAD_REQUEST))
        val respStr = resp.body.toString()
        assert(respStr.contains("Item 2 is a duplicate"))
        assert(respStr.contains("Item 3 is a duplicate"))
    }
}