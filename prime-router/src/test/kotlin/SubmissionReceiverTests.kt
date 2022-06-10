package gov.cdc.prime.router

import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.azure.elrProcessQueueName
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
import java.lang.IllegalStateException
import kotlin.test.assertTrue

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
    @Test
    fun `test covid receiver processAsync`() {
        // setup
        val one = Schema(name = "None", topic = "full-elr", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val receiver = CovidReceiver(
            engine,
            actionHistory
        )

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val bodyFormat = Report.Format.CSV
        val bodyUrl = "http://anyblob.com"

        every { blobMock.generateBodyAndUploadReport(any(), any(), any()) }
            .returns(BlobAccess.BlobInfo(bodyFormat, bodyUrl, "".toByteArray()))
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        // act
        receiver.processAsync(
            report,
            Options.None,
            emptyMap(),
            emptyList()
        )

        // assert
        verify(exactly = 1) {
            blobMock.generateBodyAndUploadReport(report = any(), any(), any())
            actionHistory.trackCreatedReport(any(), any(), any())
            engine.insertProcessTask(any(), any(), any(), any())
        }
    }

    @Test
    fun `test covid receiver processAsync, incorrect format`() {
        // setup
        val one = Schema(name = "None", topic = "full-elr", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val receiver = CovidReceiver(
            engine,
            actionHistory
        )

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata,
            bodyFormat = Report.Format.HL7
        )

        val bodyFormat = Report.Format.CSV
        val bodyUrl = "http://anyblob.com"

        every { blobMock.generateBodyAndUploadReport(any(), any(), any()) }
            .returns(BlobAccess.BlobInfo(bodyFormat, bodyUrl, "".toByteArray()))
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit

        // act
        var exceptionThrown = false
        try {
            receiver.processAsync(
                report,
                Options.None,
                emptyMap(),
                emptyList()
            )
        } catch (ex: IllegalStateException) {
            exceptionThrown = true
        }

        // assert
        assertTrue(exceptionThrown)
    }

    // validateAndMoveToProcessing
    @Test
    fun `test COVID receiver validateAndMoveToProcessing, async`() {
        // setup
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            CovidReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))

        every { engine.parseCovidReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { receiver.processAsync(any(), any(), any(), any()) } returns Unit

        // act
        receiver.validateAndMoveToProcessing(
            sender,
            csvString_2Records,
            emptyMap(),
            Options.None,
            emptyList(),
            true,
            true,
            ByteArray(0),
            "test.csv",
            metadata = metadata
        )

        // assert
        verify(exactly = 1) {
            engine.parseCovidReport(any(), any(), any())
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            actionHistory.trackLogs(emptyList())
            receiver.processAsync(any(), any(), any(), any())
        }
    }

    @Test
    fun `test COVID receiver validateAndMoveToProcessing, sync, with dupe check`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            CovidReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseCovidReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit

        // act
        receiver.validateAndMoveToProcessing(
            sender,
            csvString_2Records,
            emptyMap(),
            Options.None,
            emptyList(),
            false,
            false,
            ByteArray(0),
            "test.csv",
            metadata = metadata
        )

        // assert
        verify(exactly = 1) {
            engine.parseCovidReport(any(), any(), any())
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            engine.routeReport(any(), any(), any(), any(), any())
            SubmissionReceiver.doDuplicateDetection(any(), any(), any())
        }

        verify(exactly = 2) {
            actionHistory.trackLogs(emptyList())
        }
    }

    @Test
    fun `test ELR receiver validateAndMoveToProcessing, async, with dupe check`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ELRReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseCovidReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { SubmissionReceiver.doDuplicateDetection(any(), any(), any()) } returns Unit
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { queueMock.sendMessage(elrProcessQueueName, any()) } returns Unit

        // act
        receiver.validateAndMoveToProcessing(
            sender,
            hl7_record,
            emptyMap(),
            Options.None,
            emptyList(),
            true,
            false,
            ByteArray(0),
            "test.csv",
            metadata = metadata
        )

        // assert
        verify(exactly = 1) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
            SubmissionReceiver.doDuplicateDetection(any(), any(), any())
            actionHistory.trackLogs(emptyList())
            engine.insertProcessTask(any(), any(), any(), any())
            queueMock.sendMessage(elrProcessQueueName, any())
        }
    }

    @Test
    fun `test ELR receiver validateAndMoveToProcessing, invalid hl7`() {
        // setup
        mockkObject(SubmissionReceiver.Companion)
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val report = Report(
            one,
            mapOf<String, List<String>>(Pair("test", listOf("1,2"))),
            source = ClientSource("ignore", "ignore"),
            metadata = metadata
        )

        val receiver = spyk(
            ELRReceiver(
                engine,
                actionHistory
            )
        )

        val sender = CovidSender(
            "Test Sender",
            "test",
            Sender.Format.HL7,
            schemaName =
            "one",
            allowDuplicates = true
        )
        val actionLogs = ActionLogger()
        val readResult = ReadResult(report, actionLogs)
        val blobInfo = BlobAccess.BlobInfo(Report.Format.HL7, "test", ByteArray(0))
        val routeResult = emptyList<ActionLog>()

        every { engine.parseCovidReport(any(), any(), any()) } returns readResult
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.routeReport(any(), any(), any(), any(), any()) } returns routeResult
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { queueMock.sendMessage(elrProcessQueueName, any()) } returns Unit

        // act
        var exceptionThrown = false
        try {
            receiver.validateAndMoveToProcessing(
                sender,
                "bad_data",
                emptyMap(),
                Options.None,
                emptyList(),
                true,
                true,
                ByteArray(0),
                "test.csv",
                metadata = metadata
            )
        } catch (ex: ActionError) {
            exceptionThrown = true
        }

        // assert
        assertTrue(exceptionThrown)

        verify(exactly = 1) {
            engine.recordReceivedReport(any(), any(), any(), any(), any())
        }

        verify(exactly = 0) {
            actionHistory.trackLogs(emptyList())
            engine.insertProcessTask(any(), any(), any(), any())
            queueMock.sendMessage(elrProcessQueueName, any())
        }
    }
}