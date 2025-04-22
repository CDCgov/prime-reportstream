package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.blob.models.BlobItemProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.ajalt.clikt.core.CliktError
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.ReportStreamFilters
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.SubmissionReceiver
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.TopicReceiver
import gov.cdc.prime.router.UniversalPipelineReceiver
import gov.cdc.prime.router.UniversalPipelineSender
import gov.cdc.prime.router.azure.BlobAccess.BlobContainerMetadata
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.cli.PIIRemovalCommands
import gov.cdc.prime.router.cli.ProcessFhirCommands
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.azure.SubmissionsFacade
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.ktor.utils.io.core.toByteArray
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID

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

    @Suppress("ktlint:standard:max-line-length")
    val fhirReport = """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347",
        |"meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}
""".trimMargin()

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine = spyk(
        WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).hl7Serializer(serializer).build()
    )

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
        val sender = CovidSender("default", "simple_report", MimeFormat.CSV, schemaName = "one")
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
            MimeFormat.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val blobInfo = BlobAccess.BlobInfo(MimeFormat.CSV, "test", ByteArray(0))
        val report1 = Report(
            Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b"))), listOf(),
            sources = listOf(ClientSource("myOrg", "myClient")),
            metadata = UnitTestUtils.simpleMetadata
        )
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
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) } returns Unit
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
        } returns report1

        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns ""
        val bodyBytes = "".toByteArray()
        mockkObject(ReportWriter)
        every { ReportWriter.getBodyBytes(any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(any(), any(), any()) }.returns(blobInfo)
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { accessSpy.isDuplicateItem(any(), any()) } returns false
        return Triple(reportFunc, req, sender)
    }

    @Nested
    inner class SftpSubmission {

        val sender = UniversalPipelineSender(
            "full-elr",
            "phd",
            MimeFormat.HL7,
            customerStatus = CustomerStatus.ACTIVE,
            topic = Topic.FULL_ELR
        )
        val mockReport = mockk<Report>(relaxed = true)

        @BeforeEach
        fun setUp() {
            mockkConstructor(UniversalPipelineReceiver::class)

            every {
                anyConstructed<UniversalPipelineReceiver>().validateAndMoveToProcessing(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns mockReport
        }

        val senderOrg = DeepOrganization(
            "phd",
            "test",
            Organization.Jurisdiction.FEDERAL,
            senders = listOf(sender)
        )

        @Test
        fun `test submitSFTP success`() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(senderOrg)
            val engine = makeEngine(metadata, settings)
            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val reportFunc = spyk(ReportFunction(engine, actionHistory))
            every { accessSpy.insertAction(any(), any()) } returns 0
            every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

            reportFunc.submitViaSftp(hl7_valid, "${senderOrg.name}/${sender.name}/valid", "hl7")
            verify {
                anyConstructed<UniversalPipelineReceiver>().validateAndMoveToProcessing(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
        }

        @Test
        fun `test submitSFTP no sender`() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(senderOrg)
            val engine = makeEngine(metadata, settings)
            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val reportFunc = spyk(ReportFunction(engine, actionHistory))
            every { accessSpy.insertAction(any(), any()) } returns 0
            every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

            assertThrows<ReportFunction.SftpSubmissionException> {
                reportFunc.submitViaSftp(
                    hl7_valid,
                    "${senderOrg.name}/foo/valid",
                    "hl7"
                )
            }
        }

        @Test
        fun `test submitSFTP invalid extension`() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(senderOrg)
            val engine = makeEngine(metadata, settings)
            val actionHistory = spyk(ActionHistory(TaskAction.receive))
            val reportFunc = spyk(ReportFunction(engine, actionHistory))
            every { accessSpy.insertAction(any(), any()) } returns 0
            every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

            assertThrows<ReportFunction.SftpSubmissionException> {
                reportFunc.submitViaSftp(
                    hl7_valid,
                    "${senderOrg.name}/foo/valid",
                    "png"
                )
            }
        }
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

        val sender = UniversalPipelineSender("Test ELR Sender", "test", MimeFormat.HL7, topic = Topic.FULL_ELR)

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
        val sender = CovidSender("Test Sender", "test", MimeFormat.CSV, schemaName = "one")

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
        val sender = CovidSender("Test Sender", "test", MimeFormat.CSV, schemaName = "one")

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
            MimeFormat.CSV,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val blobInfo = BlobAccess.BlobInfo(MimeFormat.CSV, "test", ByteArray(0))

        val req = MockHttpRequestMessage(csvString_2Records)

        every { reportFunc.validateRequest(any()) } returns RequestFunction.ValidatedRequest(
            csvString_2Records,
            sender = sender
        )
        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"
        every { actionHistory.action.actionName } returns TaskAction.receive
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns ""
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
            MimeFormat.HL7,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val blobInfo = BlobAccess.BlobInfo(MimeFormat.HL7, "test", ByteArray(0))

        val req = MockHttpRequestMessage(hl7_valid)

        every { reportFunc.validateRequest(any()) } returns RequestFunction.ValidatedRequest(
            hl7_valid,
            sender = sender
        )
        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"
        every { actionHistory.action.actionName } returns TaskAction.receive
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns ""
        val bodyBytes = "".toByteArray()
        mockkObject(ReportWriter)
        every { ReportWriter.getBodyBytes(any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(any(), any(), any()) }.returns(blobInfo)
        every { engine.insertProcessTask(any(), any(), any(), any()) } returns Unit
        every { serializer.checkLIVDValueExists(any(), any()) } returns true

        every { accessSpy.isDuplicateItem(any(), any()) } returns false

        val mockSubmissionsFacade = mockk<SubmissionsFacade>()
        mockkObject(SubmissionsFacade)
        every { SubmissionsFacade.instance } returns mockSubmissionsFacade
        val reportId = UUID.randomUUID()
        val mockHistory = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            reports = mutableListOf(),
            logs = emptyList()
        )
        mockHistory.reportId = reportId.toString()
        every { mockSubmissionsFacade.findDetailedSubmissionHistory(any(), any(), any()) } returns mockHistory

        // act
        val resp = reportFunc.processRequest(req, sender)

        // assert
        verify(exactly = 1) { engine.isDuplicateItem(any()) }
        verify(exactly = 1) { actionHistory.trackActionSenderInfo(any(), any()) }
        assert(resp.status.equals(HttpStatus.CREATED))
        assertThat(resp.getHeader(HttpHeaders.LOCATION))
            .isEqualTo("http://localhost/api/waters/report/$reportId/history")
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
            MimeFormat.HL7,
            schemaName =
            "one",
            allowDuplicates = false
        )
        val blobInfo = BlobAccess.BlobInfo(MimeFormat.HL7, "test", ByteArray(0))

        val req = MockHttpRequestMessage(hl7_5_separator)

        every { reportFunc.validateRequest(any()) } returns RequestFunction.ValidatedRequest(
            hl7_5_separator,
            sender = sender
        )
        every { accessSpy.insertAction(any(), any()) } returns 0
        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit

        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) } returns Unit
        every { actionHistory.action.actionId } returns 1
        every { actionHistory.action.sendingOrg } returns "Test Sender"
        every { actionHistory.action.actionName } returns TaskAction.receive
        every { engine.recordReceivedReport(any(), any(), any(), any(), any()) } returns blobInfo
        every { engine.queue.sendMessage(any(), any(), any()) } returns ""
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

    @Test
    fun `test throws an error for an invalid payloadname`() {
        // 2052 character
        val longpayloadname = "test".repeat(513)
        val mockHttpRequest = MockHttpRequestMessage()
        mockHttpRequest.httpHeaders["payloadname"] = longpayloadname
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val (reportFunc, _, _) = setupForProcessRequestTests(actionHistory)

        assertThrows<RequestFunction.InvalidExternalPayloadException> {
            reportFunc.extractPayloadName(mockHttpRequest)
        }
    }

    @Test
    fun `Test Bank Message Retrieval - Unauthorized `() {
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.authenticate(any()) } returns null

        val result = ReportFunction(
            makeEngine(metadata, settings),
            actionHistory
        ).getMessagesFromTestBank(MockHttpRequestMessage())
        assert(result.status == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `Test Bank Message Retrieval - Authorized, but no reports`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        mockkConstructor(BlobServiceClientBuilder::class)
        every { anyConstructed<BlobServiceClientBuilder>().connectionString(any()) } answers
            { BlobServiceClientBuilder() }
        every { anyConstructed<BlobServiceClientBuilder>().buildClient() } returns mockk()
        val testBlobMetadata = BlobContainerMetadata.build("testcontainer", "test")
        every { BlobAccess.Companion.listBlobs("", any()) } returns listOf()
        every { BlobAccess.Companion.getBlobContainer(any()) } returns mockk()

        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val result = ReportFunction(makeEngine(metadata, settings), actionHistory).processGetMessageFromTestBankRequest(
            MockHttpRequestMessage(),
            BlobAccess.Companion,
            testBlobMetadata
        )
        assert(result.status == HttpStatus.OK)
        assert(result.body == "[]")
    }

    @Test
    fun `Test Bank Message Retrieval - Authorized, reports`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        mockkConstructor(BlobServiceClientBuilder::class)
        every { anyConstructed<BlobServiceClientBuilder>().connectionString(any()) } answers
            { BlobServiceClientBuilder() }
        every { anyConstructed<BlobServiceClientBuilder>().buildClient() } returns mockk()
        val testBlobMetadata = BlobContainerMetadata.build("testcontainer", "test")
        val mockedBlobClient = mockkClass(BlobClient::class)
        val mockedBlobContainerClient = mockkClass(BlobContainerClient::class)
        every { mockedBlobContainerClient.getBlobClient(any()) } returns mockedBlobClient
        every { BlobAccess.Companion.getBlobContainer(any()) } returns mockedBlobContainerClient

        val dateCreated = OffsetDateTime.now()
        val fileName = "testOrg.default/" + UUID.randomUUID().toString() + ".fhir"
        val blob1 = BlobItem()
        blob1.name = fileName
        blob1.properties = BlobItemProperties()
        blob1.properties.creationTime = dateCreated

        every { mockedBlobClient.downloadContent() } returns BinaryData.fromBytes(fhirReport.toByteArray())

        every { BlobAccess.Companion.listBlobs("", any()) } returns listOf(
            BlobAccess.Companion.BlobItemAndPreviousVersions(
            currentBlobItem = blob1, null
        )
        )

        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val result = ReportFunction(makeEngine(metadata, settings), actionHistory).processGetMessageFromTestBankRequest(
            MockHttpRequestMessage(),
            BlobAccess.Companion,
            testBlobMetadata
        )
        assert(result.status == HttpStatus.OK)
        val mapper: ObjectMapper = JsonMapper.builder()
            .addModule(JavaTimeModule())
            .build()
        assert(
            result.body == "[" + mapper.writeValueAsString(
            ReportFunction.TestReportInfo(
                dateCreated.toString(),
                fileName,
                fhirReport,
                "testOrg.default"
            )
        ) + "]"
        )
    }

    @Nested
    inner class GetSendersForTestingTool {
        @Test
        fun `happy path`() {
            val sender = UniversalPipelineSender(
                "full-elr",
                "phd",
                MimeFormat.HL7,
                customerStatus = CustomerStatus.ACTIVE,
                topic = Topic.FULL_ELR,
                schemaName = "/testSchema.yml"
            )
            val senderOrg = DeepOrganization(
                "phd",
                "test",
                Organization.Jurisdiction.FEDERAL,
                senders = listOf(sender)
            )

            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(senderOrg)
            val engine = makeEngine(metadata, settings)
            val reportFunc = spyk(ReportFunction(engine))

            val result = reportFunc.getSenders(MockHttpRequestMessage())

            val senders = listOf(
                ReportFunction.SenderResponse("None", null, null),
                ReportFunction.SenderResponse("phd.full-elr", "HL7", "/testSchema.yml")
            )
            val sendersString = JacksonMapperUtilities.allowUnknownsMapper.writeValueAsString(senders)

            assert(result.status.value() == 200)
            assert(result.body.toString() == sendersString)
        }

        @Test
        fun `unauthorized `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.authenticate(any()) } returns null

            val result = ReportFunction(
                makeEngine(metadata, settings),
            ).getSenders(MockHttpRequestMessage())

            assert(result.status == HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `always returns None option `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            val engine = makeEngine(metadata, settings)
            val reportFunc = spyk(ReportFunction(engine))
            every { engine.settings.senders } throws (IllegalStateException())

            val result = reportFunc.getSenders(MockHttpRequestMessage())

            val senders = listOf(
                ReportFunction.SenderResponse("None", null, null),
            )
            val sendersString = JacksonMapperUtilities.allowUnknownsMapper.writeValueAsString(senders)
            assert(result.status.value() == 200)
            assert(result.body.toString() == sendersString)
        }
    }

    @Nested
    inner class ProcessFhirDataRequest {

        @Test
        fun `unauthorized `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.authenticate(any()) } returns null

            val result = ReportFunction(
                makeEngine(metadata, settings),
            ).processFhirDataRequest(MockHttpRequestMessage())

            assert(result.status == HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `unauthorized, non admin `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            mockkObject(AuthenticatedClaims)
            val claims = mockk<AuthenticatedClaims>()
            every { AuthenticatedClaims.authenticate(any()) } returns claims
            every { claims.authorized(setOf(Scope.primeAdminScope)) } returns false

            val result = ReportFunction(
                makeEngine(metadata, settings),
            ).processFhirDataRequest(MockHttpRequestMessage())

            assert(result.status == HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `missing receiver name `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            val req = MockHttpRequestMessage(hl7_valid)
            req.queryParameters["organizationName"] = "test"

            val result = ReportFunction(
                makeEngine(metadata, settings),
            ).processFhirDataRequest(req)

            assert(result.status == HttpStatus.BAD_REQUEST)
            assert(result.body == "The receiver name is required")
        }

        @Test
        fun `missing organization name `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            val req = MockHttpRequestMessage(hl7_valid)
            req.queryParameters["receiverName"] = "test"
            req.queryParameters["organizationName"] = ""

            val result = ReportFunction(
                makeEngine(metadata, settings),
            ).processFhirDataRequest(req)

            assert(result.status == HttpStatus.BAD_REQUEST)
            assert(result.body == "The organization name is required")
        }

        @Test
        fun `missing request body `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            val req = MockHttpRequestMessage()
            req.queryParameters["receiverName"] = "test"
            req.queryParameters["organizationName"] = "test"

            val result = ReportFunction(
                makeEngine(metadata, settings),
            ).processFhirDataRequest(req)

            assert(result.status == HttpStatus.OK)
            assert(result.body.toString().contains("Input message is blank."))
        }

        @Test
        fun `body is not HL7 or FHIR `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            val engine = makeEngine(metadata, settings)
            val req = MockHttpRequestMessage("test")
            req.queryParameters["receiverName"] = "test"
            req.queryParameters["organizationName"] = "test"
            req.queryParameters["senderId"] = "testOrg.sender"

            val result = ReportFunction(engine).processFhirDataRequest(req)

            assert(result.status == HttpStatus.OK)
            assert(result.body.toString().contains("Input not recognized as FHIR or HL7."))
        }

        @Test
        fun `senderId not blank and sender not found `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            val engine = makeEngine(metadata, settings)
            val req = MockHttpRequestMessage(hl7_valid)
            req.queryParameters["receiverName"] = "test"
            req.queryParameters["organizationName"] = "test"
            val senderId = "testOrg.sender"
            req.queryParameters["senderId"] = senderId
            every { engine.settings.findSender(any()) } returns null

            val result = ReportFunction(engine).processFhirDataRequest(req)

            assert(result.status == HttpStatus.OK)
            assert(result.body.toString().contains("No sender found for $senderId."))
        }

        @Test
        fun `FHIR sender with HL7 input `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            val engine = makeEngine(metadata, settings)
            val req = MockHttpRequestMessage(hl7_valid)
            req.queryParameters["receiverName"] = "test"
            req.queryParameters["organizationName"] = "test"
            val senderId = "testOrg.sender"
            req.queryParameters["senderId"] = senderId
            val sender = mockk<Sender>()
            every { engine.settings.findSender(any()) } returns sender
            every { sender.format.ext } returns "fhir"

            val result = ReportFunction(engine).processFhirDataRequest(req)

            assert(result.status == HttpStatus.OK)
            assert(result.body.toString().contains("Expected FHIR input for selected sender."))
        }

        @Test
        fun `Process fhir data request - HL7 sender with FHIR input `() {
            val metadata = UnitTestUtils.simpleMetadata
            val settings = FileSettings().loadOrganizations(oneOrganization)
            val engine = makeEngine(metadata, settings)
            val req = MockHttpRequestMessage(fhirReport)
            req.queryParameters["receiverName"] = "test"
            req.queryParameters["organizationName"] = "test"
            val senderId = "testOrg.sender"
            req.queryParameters["senderId"] = senderId
            val sender = mockk<Sender>()
            every { engine.settings.findSender(any()) } returns sender
            every { sender.format.ext } returns "hl7"

            val result = ReportFunction(engine).processFhirDataRequest(req)

            assert(result.status == HttpStatus.OK)
            assert(result.body.toString().contains("Expected HL7 input for selected sender."))
        }
    }

    @Test
    fun `No report`() {
        val mockDb = mockk<DatabaseAccess>()
        val reportId = UUID.randomUUID()
        every { mockDb.fetchReportFile(reportId, null, null) } throws (IllegalStateException())
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val result = ReportFunction(
            makeEngine(metadata, settings),
            actionHistory
        ).processDownloadReport(
            MockHttpRequestMessage(),
            reportId,
            true,
            "local",
            mockDb
        )
        assert(result.status.value() == 400)
    }

    @Test
    fun `Report found, PII removal`() {
        val reportFile = ReportFile()
        reportFile.bodyUrl = "fakeurl.fhir"
        mockkObject(AuthenticatedClaims)
        val mockDb = mockk<DatabaseAccess>()
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val blobConnectionInfo = mockk<BlobContainerMetadata>()
        every { blobConnectionInfo.getBlobEndpoint() } returns "http://endpoint/metadata"
        every { BlobAccess.downloadBlobAsByteArray(any<String>()) } returns fhirReport.toByteArray(Charsets.UTF_8)
        val reportId = UUID.randomUUID()
        every { mockDb.fetchReportFile(reportId, null, null) } returns reportFile
        val piiRemovalCommands = mockkClass(PIIRemovalCommands::class)
        every { piiRemovalCommands.removePii(any()) } returns fhirReport

        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val result = ReportFunction(makeEngine(metadata, settings), actionHistory).processDownloadReport(
            MockHttpRequestMessage(),
            reportId,
            true,
            "local",
            mockDb,
            piiRemovalCommands
        )

        assert(result.body.toString().contains("1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347"))
    }

    @Test
    fun `Report found, asked for no removal on prod`() {
        val reportFile = ReportFile()
        reportFile.bodyUrl = "fakeurl.fhir"
        mockkObject(AuthenticatedClaims)
        val mockDb = mockk<DatabaseAccess>()
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val blobConnectionInfo = mockk<BlobContainerMetadata>()
        every { blobConnectionInfo.getBlobEndpoint() } returns "http://endpoint/metadata"
        every { BlobAccess.downloadBlobAsByteArray(any<String>()) } returns fhirReport.toByteArray(Charsets.UTF_8)
        every { mockDb.fetchReportFile(reportId = any(), null, null) } returns reportFile

        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val result = ReportFunction(makeEngine(metadata, settings), actionHistory).processDownloadReport(
            MockHttpRequestMessage(),
            UUID.randomUUID(),
            false,
            "prod",
            mockDb
        )

        assert(result.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `valid access token, report found, no PII removal`() {
        val reportFile = ReportFile()
        reportFile.bodyUrl = "fakeurl.fhir"
        mockkObject(AuthenticatedClaims)
        val mockDb = mockk<DatabaseAccess>()
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val blobConnectionInfo = mockk<BlobContainerMetadata>()
        every { blobConnectionInfo.getBlobEndpoint() } returns "http://endpoint/metadata"
        every { BlobAccess.downloadBlobAsByteArray(any<String>()) } returns fhirReport.toByteArray(Charsets.UTF_8)
        every { mockDb.fetchReportFile(reportId = any(), null, null) } returns reportFile

        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val result = ReportFunction(makeEngine(metadata, settings), actionHistory).processDownloadReport(
            MockHttpRequestMessage(),
            UUID.randomUUID(),
            false,
            "local",
            mockDb
        )

        assert(result.body.toString().contains("1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347"))
    }

    @Test
    fun `valid access token, report found, body URL not FHIR`() {
        val reportFile = ReportFile()
        reportFile.bodyUrl = "fakeurl.hl7"
        mockkObject(AuthenticatedClaims)
        val mockDb = mockk<DatabaseAccess>()
        every { mockDb.fetchReportFile(reportId = any(), null, null) } returns reportFile

        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))

        val result = ReportFunction(makeEngine(metadata, settings), actionHistory).processDownloadReport(
            MockHttpRequestMessage(),
            UUID.randomUUID(),
            false,
            "local",
            mockDb
        )

        assert(result.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `processFhirDataRequest blank file`() {
        val file = File("filename.txt")
        file.createNewFile()
        assertThrows<CliktError> {
            ProcessFhirCommands().processFhirDataRequest(
                file,
                Environment.get("staging"),
                "full-elr",
                "me-phd",
                "classpath:/metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml",
                false,
                ""
            )
        }
        file.delete()
    }

    @Test
    fun `processFhirDataRequest receiver name, or org name and output format blank`() {
        val file = File("filename.txt")
        file.createNewFile()
        assertThrows<CliktError> {
            ProcessFhirCommands().processFhirDataRequest(
                file,
                Environment.get("local"),
                "",
                "",
                "classpath:/metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml",
                false,
                ""
            )
        }
        file.delete()
    }

    @Test
    fun `processFhirDataRequest nonCLI request in staging without access token should fail`() {
        val file = File("src/testIntegration/resources/datatests/FHIR_to_HL7/sample_ME_20240806-0001.fhir")
        assertThrows<CliktError> {
            ProcessFhirCommands().processFhirDataRequest(
                file,
                Environment.get("staging"),
                "full-elr",
                "me-phd",
                "classpath:/metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml",
                false,
                ""
            )
        }
    }

    @Suppress("ktlint:standard:max-line-length")
    val jurisdictionalFilter: ReportStreamFilter =
        listOf("(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'ME') or (Bundle.entry.resource.ofType(Patient).address.state = 'ME')")
    val qualityFilter: ReportStreamFilter = listOf("Bundle.entry.resource.ofType(MessageHeader).id.exists()")

    @Suppress("ktlint:standard:max-line-length")
    val conditionFilter: ReportStreamFilter =
        listOf("%resource.where(interpretation.coding.code = 'A').code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code in ('840539006'|'895448002')).exists()")
    val filters = listOf<ReportStreamFilter>(jurisdictionalFilter, qualityFilter, conditionFilter)
    val organization = Organization(
        "me-phd",
        "This is my description",
        Organization.Jurisdiction.STATE,
        "ME",
        "Cumberland",
        listOf(
            ReportStreamFilters(
            Topic.FULL_ELR,
            jurisdictionalFilter,
            qualityFilter,
            null,
            null,
            conditionFilter,
            null
        )
        )
    )
    val sender = UniversalPipelineSender(
        "full-elr",
        "me-phd",
        MimeFormat.HL7,
        CustomerStatus.ACTIVE,
        "classpath:/metadata/hl7_mapping/receivers/STLTs/ME/ME-receiver-transform.yml",
        Sender.ProcessingType.async,
        true,
        Sender.SenderType.facility,
        Sender.PrimarySubmissionMethod.manual,
        false,
        Topic.FULL_ELR,
    )
    val receiver = Receiver(
        "full-elr",
        "me-phd",
        Topic.FULL_ELR,
        CustomerStatus.ACTIVE,
        Hl7Configuration(
            schemaName = "classpath:/metadata/hl7_mapping/receivers/STLTs/ME/ME-receiver-transform.yml",
            useTestProcessingMode = true,
            useBatchHeaders = true,
            messageProfileId = "",
            receivingApplicationName = "",
            receivingFacilityOID = "",
            receivingFacilityName = "",
            receivingApplicationOID = "",
            receivingOrganization = ""
        ),
        jurisdictionalFilter,
        qualityFilter
    )

    @Test
    fun `return ack if requested and enabled`() {
        val mockEngine = mockk<WorkflowEngine>()
        val mockActionHistory = mockk<ActionHistory>(relaxed = true)
        val mockReportStreamEventService = mockk<ReportStreamEventService>(relaxed = true)
        val mockSettings = mockk<SettingsProvider>()
        val mockReceiver = mockk<UniversalPipelineReceiver>()
        val mockAction = mockk<Action>()
        val mockDb = mockk<DatabaseAccess>()
        mockkObject(BlobAccess.Companion)
        mockkObject(SubmissionReceiver.Companion)

        val sender = UniversalPipelineSender(
            name = "Test Sender",
            organizationName = "org",
            format = MimeFormat.HL7,
            hl7AcknowledgementEnabled = true,
            topic = Topic.FULL_ELR,
        )
        val report = Report(
            Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b"))), listOf(),
            sources = listOf(ClientSource("myOrg", "myClient")),
            metadata = UnitTestUtils.simpleMetadata
        )
        val submission = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            reports = mutableListOf(),
            logs = emptyList()
        )

        every { mockEngine.settings } returns mockSettings
        every { mockSettings.findSender(any()) } returns sender
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        every { SubmissionReceiver.getSubmissionReceiver(any(), any(), any()) } returns mockReceiver
        every {
            mockReceiver.validateAndMoveToProcessing(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns report
        every { mockEngine.recordAction(any()) } just Runs
        every { mockActionHistory.action } returns mockAction
        every { mockAction.actionId } returns 5
        every { mockEngine.db } returns mockDb
        // I don't agree with ktlint on this one
        every {
            mockDb.transactReturning(
                any<
                        (
                    DataAccessTransaction,
                ) -> DetailedSubmissionHistory?
                    >()
            )
        } returns submission

        val reportFunction = ReportFunction(mockEngine, mockActionHistory, mockReportStreamEventService)

        val body = """
            MSH|^~\&|Epic|Hospital|LIMS|StatePHL|20241003000000||ORM^O01^ORM_O01|4AFA57FE-D41D-4631-9500-286AAAF797E4|T|2.5.1|||AL|NE
        """.trimIndent()

        val req = MockHttpRequestMessage(body)
        req.httpHeaders += mapOf(
            "client" to "Test Sender",
            "content-length" to body.length.toString(),
            "content-type" to "application/hl7-v2"
        )

        val response = reportFunction.run(req)

        assertThat(response.status).isEqualTo(HttpStatus.CREATED)
        assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(HttpUtilities.hl7V2MediaType)
    }
}