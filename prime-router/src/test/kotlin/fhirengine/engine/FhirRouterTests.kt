package gov.cdc.prime.router.fhirengine.engine

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
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Provenance
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FhirRouterTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val oneOrganization = DeepOrganization(
        "co-phd", "test", Organization.Jurisdiction.FEDERAL,
        receivers = listOf(Receiver("full-elr-hl7", "co-phd", "topic", CustomerStatus.INACTIVE, "one"))
    )

    val valid_fhir = "{\n" +
        "\t\"resourceType\": \"Bundle\",\n" +
        "\t\"id\": \"d848a959-e466-42c8-aec7-44c8f1024f91\",\n" +
        "\t\"meta\": {\n" +
        "\t\t\"lastUpdated\": \"2022-07-20T19:33:13.087+00:00\"\n" +
        "\t},\n" +
        "\t\"identifier\": {\n" +
        "\t\t\"value\": \"1234d1d1-95fe-462c-8ac6-46728dba581c\"\n" +
        "\t},\n" +
        "\t\"type\": \"message\",\n" +
        "\t\"timestamp\": \"2021-08-03T13:15:11.015+00:00\",\n" +
        "\t\"entry\": [\n" +
        "\t\t{\n" +
        "\t\t\t\"fullUrl\": \"MessageHeader/c03f1b6b-cfc3-3477-89c0-d38316cd1a38\",\n" +
        "\t\t\t\"resource\": {\n" +
        "\t\t\t\t\"resourceType\": \"MessageHeader\",\n" +
        "\t\t\t\t\"id\": \"c03f1b6b-cfc3-3477-89c0-d38316cd1a38\",\n" +
        "\t\t\t\t\"meta\": {\n" +
        "\t\t\t\t\t\"extension\": [\n" +
        "\t\t\t\t\t\t{\n" +
        "\t\t\t\t\t\t\t\"url\": \"https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id\",\n" +
        "\t\t\t\t\t\t\t\"valueCodeableConcept\": {\n" +
        "\t\t\t\t\t\t\t\t\"coding\": [\n" +
        "\t\t\t\t\t\t\t\t\t{\n" +
        "\t\t\t\t\t\t\t\t\t\t\"system\": \"https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html\",\n" +
        "\t\t\t\t\t\t\t\t\t\t\"code\": \"P\"\n" +
        "\t\t\t\t\t\t\t\t\t}\n" +
        "\t\t\t\t\t\t\t\t]\n" +
        "\t\t\t\t\t\t\t}\n" +
        "\t\t\t\t\t\t},\n" +
        "\t\t\t\t\t\t{\n" +
        "\t\t\t\t\t\t\t\"url\": \"http://ibm.com/fhir/cdm/StructureDefinition/source-record-id\",\n" +
        "\t\t\t\t\t\t\t\"valueId\": \"1234d1d1-95fe-462c-8ac6-46728dba581c\"\n" +
        "\t\t\t\t\t\t},\n" +
        "\t\t\t\t\t\t{\n" +
        "\t\t\t\t\t\t\t\"url\": \"http://ibm.com/fhir/cdm/StructureDefinition/source-data-model-version\",\n" +
        "\t\t\t\t\t\t\t\"valueString\": \"2.5.1\"\n" +
        "\t\t\t\t\t\t}\n" +
        "\t\t\t\t\t]\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t\"eventCoding\": {\n" +
        "\t\t\t\t\t\"system\": \"http://terminology.hl7.org/CodeSystem/v2-0003\",\n" +
        "\t\t\t\t\t\"code\": \"R01\",\n" +
        "\t\t\t\t\t\"display\": \"ORU/ACK - Unsolicited transmission of an observation message\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t\"destination\": [\n" +
        "\t\t\t\t\t{\n" +
        "\t\t\t\t\t\t\"name\": \"CDPH FL REDIE\",\n" +
        "\t\t\t\t\t\t\"endpoint\": \"CDPH_CID\"\n" +
        "\t\t\t\t\t}\n" +
        "\t\t\t\t],\n" +
        "\t\t\t\t\"sender\": {\n" +
        "\t\t\t\t\t\"reference\": \"Organization/257d65ea-c714-4041-9e82-5c0562034cf0\"\n" +
        "\t\t\t\t},\n" +
        "\t\t\t\t\"source\": {\n" +
        "\t\t\t\t\t\"extension\": [\n" +
        "\t\t\t\t\t\t{\n" +
        "\t\t\t\t\t\t\t\"url\": \"https://reportstream.cdc.gov/fhir/StructureDefinition/" +
        "source-software-vendor-org\",\n" +
        "\t\t\t\t\t\t\t\"valueReference\": {\n" +
        "\t\t\t\t\t\t\t\t\"reference\": \"Organization/c3a639a8-d624-455f-844d-ad5aea717c56\"\n" +
        "\t\t\t\t\t\t\t}\n" +
        "\t\t\t\t\t\t},\n" +
        "\t\t\t\t\t\t{\n" +
        "\t\t\t\t\t\t\t\"url\": \"https://reportstream.cdc.gov/fhir/StructureDefinition/" +
        "source-software-install-date\",\n" +
        "\t\t\t\t\t\t\t\"valueDateTime\": \"2021-07-26\"\n" +
        "\t\t\t\t\t\t},\n" +
        "\t\t\t\t\t\t{\n" +
        "\t\t\t\t\t\t\t\"url\": \"https://reportstream.cdc.gov/fhir/StructureDefinition/" +
        "source-software-binary-id\",\n" +
        "\t\t\t\t\t\t\t\"valueString\": \"0.1-SNAPSHOT\"\n" +
        "\t\t\t\t\t\t},\n" +
        "\t\t\t\t\t\t{\n" +
        "\t\t\t\t\t\t\t\"url\": \"https://reportstream.cdc.gov/fhir/StructureDefinition/" +
        "source-identifier-system\",\n" +
        "\t\t\t\t\t\t\t\"valueOid\": \"urn:oid:2.16.840.1.114222.4.1.237821\"\n" +
        "\t\t\t\t\t\t}\n" +
        "\t\t\t\t\t],\n" +
        "\t\t\t\t\t\"name\": \"CDC PRIME - Atlanta,\",\n" +
        "\t\t\t\t\t\"software\": \"PRIME Data Hub\",\n" +
        "\t\t\t\t\t\"version\": \"0.1-SNAPSHOT\"\n" +
        "\t\t\t\t}\n" +
        "\t\t\t}\n" +
        "\t\t}\n" +
        "\t]\n" +
        "}"

    val valid_fhir_with_provenance = """
    {
        "resourceType": "Bundle",
        "id": "1666038428133786000.94addcb6-835c-4883-a095-0c50cf113744",
        "meta": {
        "lastUpdated": "2022-10-17T20:27:08.149+00:00",
        "security": [
            {
                "code": "SECURITY",
                "display": "SECURITY"
            }
        ]
    },
        "identifier": {
        "value": "MT_COCAA_ORU_AAPHELR.1.6214638"
    },
        "type": "message",
        "timestamp": "2028-08-08T15:28:05.000+00:00",
        "entry": [
            {
                "fullUrl": "Provenance/1666038430962443000.9671377b-8f2b-4f5c-951c-b43ca8fd1a25",
                "resource": {
                    "resourceType": "Provenance",
                    "id": "1666038430962443000.9671377b-8f2b-4f5c-951c-b43ca8fd1a25",
                    "occurredDateTime": "2028-08-08T09:28:05-06:00",
                    "recorded": "2028-08-08T09:28:05-06:00",
                    "activity": {
                        "coding": [
                            {
                                "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                "code": "R01",
                                "display": "ORU_R01"
                            }
                        ]
                    }
                }
            }
        ]
    }"""

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider, taskAction: TaskAction): FHIREngine {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build(taskAction)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    // valid fhir, read file, update with destinations, write file, put message on queue
    @Test
    fun `test full elr routing happy path`() {
        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = "full-elr", elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route))
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(valid_fhir)
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { (engine as FHIRRouter).addReceivers(any(), any()) } returns Unit

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 1) {
            actionHistory.trackExistingInputReport(any())
            actionHistory.trackCreatedReport(any(), any(), any())
            BlobAccess.Companion.uploadBlob(any(), any())
            queueMock.sendMessage(any(), any())
            accessSpy.insertTask(any(), any(), any(), any())
            (engine as FHIRRouter).addReceivers(any(), any())
        }
    }

    @Test
    fun `test adding receivers to bundle`() {
        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = "full-elr", elements = emptyList())
        val metadata = Metadata(schema = one)
        val engine = makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter

        val bundle = FhirTranscoder.decode(valid_fhir_with_provenance)
        val receiversIn = listOf(oneOrganization.receivers[0])

        // act
        engine.addReceivers(bundle, receiversIn)

        val stringDoc = FhirTranscoder.encode(bundle)
        val reparse = FhirTranscoder.decode(stringDoc)

        // assert
        val provenance = reparse.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isNotEmpty())
        assert(receiversOut[0] == "co-phd.full-elr-hl7")
    }
}