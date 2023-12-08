package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import ca.uhn.fhir.context.FhirContext
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.cli.ObservationMappingConstants
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.metadata.LookupTable
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.UUID
import kotlin.test.Test

private const val BLOB_URL = "http://blobstore.example/file.hl7"
private const val BLOB_SUB_FOLDER_NAME = "test-sender"
private const val SCHEMA_NAME = "test-schema"
private const val VALID_DATA_URL = "src/test/resources/fhirengine/engine/valid_data.fhir"
private const val BLOB_FHIR_URL = "http://blobstore.example/file.fhir"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FhirConverterTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val oneOrganization = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(Receiver("elr", "co-phd", Topic.TEST, CustomerStatus.INACTIVE, "one"))
    )

    val settings = FileSettings().loadOrganizations(oneOrganization)
    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one)

    private val validHl7 = "" +
        "MSH|^~\\&|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|Winchester House^05D2222542^ISO|CDPH FL " +
        "REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|20210803131511.0147+0000" +
        "||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLabReport-NoAck" +
        "^ELR_Receiver^2.16.840.1.113883.9.11^ISO\n" +
        "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210726\n" +
        "PID|1||09d12345-0987-1234-1234-111b1ee0879f^^^Winchester House&05D2222542&ISO^PI^&05D2222542&ISO||Bunny^" +
        "Bugs^C^^^^L||19000101|M||2106-3^White^HL70005^^^^2.5.1|12345 Main St^^San Jose^FL^95125^USA^^^06085||(123" +
        ")456-7890^PRN^PH^^1^123^4567890|||||||||N^Non Hispanic or Latino^HL70189^^^^2.9||||||||N\n" +
        "ORC|RE|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728db" +
        "a581c^Winchester House^05D2222542^ISO|||||||||1679892871^Doolittle^Doctor^^^^^^CMS&2.16.840.1.113883.3.249&I" +
        "SO^^^^NPI||(123)456-7890^WPN^PH^^1^123^4567890|20210802||||||Winchester House|6789 Main St^^San Jose^FL" +
        "^95126^^^^06085|(123)456-7890^WPN^PH^^1^123^4567890|6789 Main St^^San Jose^FL^95126\n" +
        "OBR|1|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728db" +
        "a581c^Winchester House^05D2222542^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specim" +
        "en by Rapid immunoassay^LN^^^^2.68|||202108020000-0500|202108020000-0500||||||||1679892871^Doolittle^Doc" +
        "tor^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|(123)456-7890^WPN^PH^^1^123^4567890|||||202108020000-050" +
        "0|||F\n" +
        "OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN" +
        "^^^2.68||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078^^^^2.7|||F|||202" +
        "108020000-0500|05D2222542^ISO||10811877011290_DIT^10811877011290^99ELR^^^^2.68^^10811877011290_DIT||2021" +
        "08020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^S" +
        "an Jose^FL^95126^^^^06085\n" +
        "OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||N^No^HL70136||||||F|" +
        "||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4." +
        "6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0" +
        "00|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D222" +
        "2542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202108020000-" +
        "0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222" +
        "542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||2021" +
        "08020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&I" +
        "SO^XX^^^05D2222542|6789 Main St^^San Jose^FL^95126-5285^^^^06085|||||QST\n" +
        "SPM|1|1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO^1234d1d1-95fe-462c-8ac6-46728dba581c&&05D" +
        "2222542&ISO||445297001^Swab of internal nose^SCT^^^^2.67||||53342003^Internal nose structure (body " +
        "structure)^SCT^^^^2020-09-01|||||||||202108020000-0500|20210802000006.0000-0500"

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider, taskAction: TaskAction): FHIREngine {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).build(taskAction)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    // good hl7, check actionHistory, item lineages, upload was called, task, queue message
    @Test
    fun `test processHl7 happy path`() {
        mockkObject(BlobAccess)
        mockkObject(Report)

        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val transformer = mockk<FhirTransformer>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.process) as FHIRConverter)
        val message = spyk(
            FhirConvertQueueMessage(
                UUID.randomUUID(), BLOB_URL, "test", BLOB_SUB_FOLDER_NAME, topic = Topic.FULL_ELR,
                SCHEMA_NAME
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "https://anyblob.com"

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(validHl7)
        every { Report.getFormatFromBlobURL(message.blobURL) } returns Report.Format.HL7
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { engine.getTransformerFromSchema(SCHEMA_NAME) }.returns(transformer)
        every { transformer.transform(any()) } returnsArgument (0)

        // act
        accessSpy.transact { txn ->
            engine.run(message, actionLogger, actionHistory, txn)
        }

        // assert
        verify(exactly = 1) {
            engine.getContentFromHL7(any(), any())
            actionHistory.trackExistingInputReport(any())
            transformer.transform(any())
            actionHistory.trackCreatedReport(any(), any(), blobInfo = any())
            BlobAccess.Companion.uploadBlob(any(), any(), any())
        }
    }

    @Test
    fun `test processFhir happy path`() {
        mockkObject(BlobAccess)
        mockkObject(Report)

        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val transformer = mockk<FhirTransformer>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.process) as FHIRConverter)
        val message = spyk(
            FhirConvertQueueMessage(
                UUID.randomUUID(),
                BLOB_FHIR_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
                SCHEMA_NAME
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "https://anyblob.com"

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File(VALID_DATA_URL).readText())
        every { Report.getFormatFromBlobURL(message.blobURL) } returns Report.Format.FHIR
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { engine.getTransformerFromSchema(SCHEMA_NAME) }.returns(transformer)
        every { transformer.transform(any()) } returnsArgument (0)

        // act
        accessSpy.transact { txn ->
            engine.run(message, actionLogger, actionHistory, txn)
        }

        // assert
        verify(exactly = 1) {
            engine.getContentFromFHIR(any(), any())
            actionHistory.trackExistingInputReport(any())
            transformer.transform(any())
            actionHistory.trackCreatedReport(any(), any(), blobInfo = any())
            BlobAccess.Companion.uploadBlob(any(), any(), any())
        }
    }

    @Test
    fun `test getContentFromHL7`() {
        val actionLogger = mockk<ActionLogger>()
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.process) as FHIRConverter)
        val message = spyk(
            FhirConvertQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR
            )
        )

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(validHl7)

        assertThat(engine.getContentFromHL7(message, actionLogger)).isNotEmpty()
    }

    @Test
    fun `test getContentFromHL7 invalid HL7`() {
        val actionLogger = spyk(ActionLogger())
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.process) as FHIRConverter)
        val message = spyk(
            FhirConvertQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR
            )
        )

        every { message.downloadContent() }
            .returns(File(VALID_DATA_URL).readText())

        assertThat(engine.getContentFromHL7(message, actionLogger)).isEmpty()

        verify(atLeast = 1) {
            actionLogger.error(any<ActionLogDetail>())
        }
    }

    @Test
    fun `test getContentFromFHIR`() {
        val actionLogger = spyk(ActionLogger())
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.process) as FHIRConverter)
        val message =
            spyk(
                FhirConvertQueueMessage(
                    UUID.randomUUID(),
                    BLOB_FHIR_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR
                )
            )

        every { message.downloadContent() }
            .returns(File(VALID_DATA_URL).readText())

        val result = engine.getContentFromFHIR(message, actionLogger)
        assertThat(result).isNotEmpty()
    }

    @Test
    fun `test getTransformerFromSchema`() {
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.process) as FHIRConverter)

        assertThat(
            engine.getTransformerFromSchema("")
        ).isNull()

        assertThat(
            engine.getTransformerFromSchema("src/test/resources/fhir_sender_transforms/sample_schema")
        ).isNotNull()
    }

    @Test
    fun `test queue messages sent after all processing`() {
        mockkObject(BlobAccess)
        mockkObject(Report)

        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val transformer = mockk<FhirTransformer>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.process) as FHIRConverter)
        val message = spyk(
            FhirConvertQueueMessage(
                UUID.randomUUID(), BLOB_FHIR_URL, "test", BLOB_SUB_FOLDER_NAME, topic = Topic.FULL_ELR,
                SCHEMA_NAME
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File("src/test/resources/fhirengine/engine/bundle_multiple_bundles.fhir").readText())
        every { Report.getFormatFromBlobURL(message.blobURL) } returns Report.Format.FHIR
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        // Throw an exception the second time trackCreatedReport is called to exit processing early and demonstrate sendMessage is not called
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }
            .returns(Unit) andThenThrows (RuntimeException())
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { engine.getTransformerFromSchema(SCHEMA_NAME) }.returns(transformer)
        every { transformer.transform(any()) } returnsArgument (0)

        // act
        assertThrows<RuntimeException> {
            accessSpy.transact { txn ->
                engine.run(message, actionLogger, actionHistory, txn)
            }
        }

        // assert
        verify(exactly = 1) {
            engine.getContentFromFHIR(any(), any())
            actionHistory.trackExistingInputReport(any())
        }
        verify(exactly = 2) {
            transformer.transform(any())
            BlobAccess.Companion.uploadBlob(any(), any(), any())
            actionHistory.trackCreatedReport(any(), any(), blobInfo = any())
        }
    }

    @Test
    fun `test condition code stamping`() {
        @Suppress("ktlint:standard:max-line-length")
        val fhirRecord = """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""

        val conditionCodeExtensionURL = "https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code"
        mockkObject(BlobAccess)
        mockkObject(Report)
        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable(
                "observation-mapping",
                listOf(
                listOf(
                    ObservationMappingConstants.TEST_CODE_KEY,
                    ObservationMappingConstants.CONDITION_CODE_KEY,
                    ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY,
                    ObservationMappingConstants.CONDITION_NAME_KEY
                ),
                listOf(
                    "80382-5",
                    "6142004",
                    "SNOMEDCT",
                    "Influenza (disorder)"
                ),
                listOf(
                    "260373001",
                    "Some Condition Code",
                    "Condition Code System",
                    "Condition Name"
                )
            )
            )
        )

        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val transformer = mockk<FhirTransformer>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.process) as FHIRConverter)
        val message = spyk(
            FhirConvertMessage(
                UUID.randomUUID(),
                BLOB_FHIR_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
                SCHEMA_NAME
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "https://anyblob.com"

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.getItemLogger(any(), any()) } returns actionLogger
        every { actionLogger.log(any<ActionLogDetail>(), any()) } just runs
        every { actionLogger.log(any<List<ActionLogDetail>>(), any()) } just runs
        every { actionLogger.log(emptyList(), any()) } just runs
        every { actionLogger.error(any<ActionLogDetail>()) } just runs
        every { message.downloadContent() } returns(fhirRecord)
        every { Report.getFormatFromBlobURL(message.blobURL) } returns Report.Format.FHIR
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { engine.getTransformerFromSchema(SCHEMA_NAME) }.returns(transformer)
        every { transformer.transform(any()) } returnsArgument (0)

        // act
        accessSpy.transact { txn ->
            engine.run(message, actionLogger, actionHistory, txn)
        }

        val bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle::class.java, fhirRecord)
        bundle.entry.filter { it.resource is Observation }.forEach {
            val observation = (it.resource as Observation)
            observation.code.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding(
                "SNOMEDCT",
                "6142004",
                "Influenza (disorder)"
            )
            )
            observation.valueCodeableConcept.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding(
                "Condition Code System",
                "Some Condition Code",
                "Condition Name"
            )
            )
        }

        // assert
        verify(exactly = 1) {
            engine.getContentFromFHIR(any(), any())
            actionHistory.trackExistingInputReport(any())
            transformer.transform(any())
            actionHistory.trackCreatedReport(any(), any(), blobInfo = any())
            BlobAccess.Companion.uploadBlob(any(), FhirTranscoder.encode(bundle).toByteArray(), any())
        }
    }
}