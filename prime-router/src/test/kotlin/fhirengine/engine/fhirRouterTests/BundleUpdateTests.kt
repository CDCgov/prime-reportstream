package gov.cdc.prime.router.fhirengine.engine.fhirRouterTests

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import io.mockk.clearAllMocks
import io.mockk.mockkClass
import io.mockk.spyk
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
class BundleUpdateTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val metadata = Metadata(schema = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList()))
    private val shorthandLookupTable = emptyMap<String, String>().toMutableMap()
    private val organizationName = "co-phd"

    val bodyUrl = "https://anyblob.com"
    private val defaultReceivers = listOf(
        Receiver(
            "full-elr-hl7",
            organizationName,
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            "one"
        ),
        Receiver(
            "full-elr-hl7-2",
            organizationName,
            Topic.FULL_ELR,
            CustomerStatus.INACTIVE,
            "one"
        )
    )
    val oneOrganization = DeepOrganization(
        organizationName,
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = defaultReceivers
    )

    val message =
        spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender", topic = Topic.FULL_ELR))

    private val validFhirWithProvenance = """
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
    }
    """

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test adding receivers to bundle`() {
        // set up
        val bundle = FhirTranscoder.decode(validFhirWithProvenance)
        val receiversIn = listOf(oneOrganization.receivers[0])

        // act
        FHIRBundleHelpers.addReceivers(bundle, receiversIn, shorthandLookupTable)

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isNotEmpty())
        assert(receiversOut[0] == "$organizationName.full-elr-hl7")
    }

    @Test
    fun `test skipping inactive receivers (only inactive)`() {
        // set up
        val bundle = FhirTranscoder.decode(validFhirWithProvenance)
        val receiversIn = listOf(oneOrganization.receivers[1])

        // act
        FHIRBundleHelpers.addReceivers(bundle, receiversIn, shorthandLookupTable)

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isEmpty())
    }

    @Test
    fun `test skipping inactive receivers (mixed)`() {
        // set up
        val bundle = FhirTranscoder.decode(validFhirWithProvenance)
        val receiversIn = oneOrganization.receivers

        // act
        FHIRBundleHelpers.addReceivers(bundle, receiversIn, shorthandLookupTable)

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isNotEmpty())
        assert(receiversOut[0] == "$organizationName.full-elr-hl7")
    }
}