package gov.cdc.prime.router

import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.serializers.ReadResult
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

class SubmissionReceiverTests {
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

    /** companion object tests **/
    // test addDuplicateLogs - duplicate file
    @Test
    fun `test addDuplicateLogs, duplicate file`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val actionLogs = ActionLogger()

        // act
        SubmissionReceiver.addDuplicateLogs(
            actionLogs,
            "Duplicate file",
            null,
            null
        )

        // assert
        assert(actionLogs.hasErrors())
        assert(actionLogs.errors.size == 1)
        assert(actionLogs.errors[0].scope == ActionLogScope.report)
    }

    @Test
    fun `test addDuplicateLogs, all items dupe`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val actionLogs = ActionLogger()

        // act
        SubmissionReceiver.addDuplicateLogs(
            actionLogs,
            "Duplicate submission",
            null,
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
        mockkObject(SubmissionReceiver.Companion)
        val actionLogs = ActionLogger()

        // act
        SubmissionReceiver.addDuplicateLogs(
            actionLogs,
            "Duplicate item",
            1,
            null
        )

        // assert
        assert(actionLogs.hasErrors())
        assert(actionLogs.errors.size == 1)
        assert(actionLogs.errors[0].scope == ActionLogScope.item)
    }

    // doDuplicateDetection, one item is duplicate
    @Test
    fun `test doDuplicateDetection, 2 records, one duplicate`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val report = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()

        every { engine.settings.findSender("Test Sender") } returns sender
        // first call to isDuplicateItem is false, second is true
        every { accessSpy.isDuplicateItem(any(), any()) }
            .returns(false)
            .andThen(true)

        // act
        SubmissionReceiver.doDuplicateDetection(
            engine,
            report,
            actionLogs
        )

        // assert
        verify(exactly = 2) {
            engine.isDuplicateItem(any())
        }
        verify(exactly = 1) {
            SubmissionReceiver.addDuplicateLogs(any(), any(), any(), any())
        }
    }

    // doDuplicateDetection, all items are duplicate
    @Test
    fun `test doDuplicateDetection, 2 records, both duplicate and already in db`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val report = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()

        every { engine.settings.findSender("Test Sender") } returns sender
        every { accessSpy.isDuplicateItem(any(), any()) } returns true

        // act
        SubmissionReceiver.doDuplicateDetection(
            engine,
            report,
            actionLogs
        )

        // assert
        verify(exactly = 1) {
            SubmissionReceiver.addDuplicateLogs(any(), any(), any(), any())
        }
        verify(exactly = 2) {
            engine.isDuplicateItem(any())
        }
        assert(actionLogs.hasErrors())
    }

    // doDuplicateDetection, all items are duplicate
    @Test
    fun `test doDuplicateDetection, 2 records, identical rows, not in db`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = makeEngine(metadata, settings)
        val report = Report(one, listOf(listOf("1", "2"), listOf("1", "2")), source = TestSource, metadata = metadata)

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()

        every { engine.settings.findSender("Test Sender") } returns sender
        every { accessSpy.isDuplicateItem(any(), any()) } returns false

        // act
        SubmissionReceiver.doDuplicateDetection(
            engine,
            report,
            actionLogs
        )

        // assert
        verify(exactly = 1) {
            SubmissionReceiver.addDuplicateLogs(any(), any(), any(), any())
        }
        verify(exactly = 1) {
            engine.isDuplicateItem(any())
        }
        assert(actionLogs.hasErrors())
    }

    /** COVID receiver tests **/
    // parseReport
    @Test
    fun `test covid receiver parseReport`() {
        // TODO: make sure this calls the proper underlying parse method
    }
//
//    // moveToProcessing
//    @Test
//    fun `test covid receiver moveToProcessing, sync`() {
//        // setup
//        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
//        val metadata = Metadata(schema = one)
//        val settings = FileSettings().loadOrganizations(oneOrganization)
//        val engine = makeEngine(metadata, settings)
//        val actionHistory = spyk(ActionHistory(TaskAction.receive))
//        val report = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
//
//        val receiver = CovidReceiver(
//            engine,
//            actionHistory
//        )
//
//        val ret = emptyList<ActionLog>()
//
//        every { engine.routeReport(any(), any(), any(), any(), actionHistory) } returns ret
//
//        // act
//        receiver.moveToProcessing(
//            false,
//            report,
//            Options.None,
//            emptyMap<String, String>(),
//            emptyList<String>()
//        )
//
//        // assert
//        verify(exactly = 1) {
//            engine.routeReport(any(), any(), any(), any(), actionHistory)
//            actionHistory.trackLogs(ret)
//        }
//    }
//
//    @Test
//    fun `test covid receiver moveToProcessing, async`() {
//        // setup
//        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
//        val metadata = Metadata(schema = one)
//        val settings = FileSettings().loadOrganizations(oneOrganization)
//        val engine = makeEngine(metadata, settings)
//        val actionHistory = spyk(ActionHistory(TaskAction.receive))
//        val report = Report(
//            one,
//            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
//            source = ClientSource("ignore", "ignore"),
//            metadata = metadata
//        )
//
//        val receiver = CovidReceiver(
//            engine,
//            actionHistory
//        )
//
//        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
//
//        every { engine.blob.generateBodyAndUploadReport(any(), any(), any()) } returns blobInfo
//        every { actionHistory.trackCreatedReport(any(), any(), any()) } returns Unit
//        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
//
//        // act
//        receiver.moveToProcessing(
//            true,
//            report,
//            Options.None,
//            emptyMap<String, String>(),
//            emptyList<String>()
//        )
//
//        // assert
//        verify(exactly = 1) {
//            engine.blob.generateBodyAndUploadReport(any(), any(), any())
//            actionHistory.trackCreatedReport(any(), any(), any())
//            engine.insertProcessTask(any(), any(), any(), any())
//        }
//    }
//
//    // parseAndMoveToProcessing
//    @Test
//    fun `test covid receiver parseAndMoveToProcessing`() {
//        // setup
//        mockkObject(SubmissionReceiver.Companion)
//        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
//        val metadata = Metadata(schema = one)
//        val settings = FileSettings().loadOrganizations(oneOrganization)
//        val engine = makeEngine(metadata, settings)
//        val actionHistory = spyk(ActionHistory(TaskAction.receive))
//        val actionLogger = spyk(ActionLogger())
//        val report = Report(
//            one,
//            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
//            source = ClientSource("ignore", "ignore"),
//            metadata = metadata
//        )
//
//        val sender = CovidSender(
//            "Test Sender",
//            "test",
//            Sender.Format.CSV,
//            schemaName =
//            "one",
//            allowDuplicates = false
//        )
//
//        val receiver = spyk(
//            CovidReceiver(
//                engine,
//                actionHistory
//            )
//        )
//
//        val readResult = ReadResult(report, actionLogger)
//        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
//        val routeResult = emptyList<ActionLog>()
//
//        every { engine.parseCovidReport(any(), any(), any()) } returns readResult
//        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit
//        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
//        every { actionHistory.trackLogs(emptyList()) } returns Unit
//        every { receiver.moveToProcessing(any(), any(), any(), any(), any()) } returns Unit
//        every { engine.routeReport(any(), any(), any(), any(), actionHistory) } returns routeResult
//
//        // act
//        receiver.validateAndMoveToProcessing(
//            sender,
//            "",
//            emptyMap(),
//            Options.None,
//            emptyList(),
//            isAsync = true,
//            allowDuplicates = false,
//            rawBody = ByteArray(0),
//            payloadName = "testName"
//        )
//
//        // assert
//        verify(exactly = 1) {
//            engine.parseCovidReport(any(), any(), any())
//            SubmissionReceiver.doDuplicateDetection(any(), any(), any())
//            engine.recordReceivedReport(any(), any(), any(), any(), any())
//            actionHistory.trackLogs(emptyList())
//            receiver.moveToProcessing(any(), any(), any(), any(), any())
//        }
//    }

    /** ELR sender tests **/
    // parseReport (calls underlying hl7 parsing)
    // TODO: Implement

    // moveToProcessing, sync (should fail out)
    // TODO: Implement

    // move to processing, async (should succeed)
    // TODO: Implement

    // parseAndMoveToProcessing
    // TODO: Implement
}