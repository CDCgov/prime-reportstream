package gov.cdc.prime.router.fhirengine.engine.fhirRouterTests

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.mockkClass
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultFilterTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val oneOrganization = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "full-elr-hl7",
                "co-phd",
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one"
            ),
            Receiver(
                "full-elr-hl7-2",
                "co-phd",
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one"
            )
        )
    )
    val csv = """
            variable,fhirPath
            processingId,Bundle.entry.resource.ofType(MessageHeader).meta.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id').value.coding.code
            messageId,Bundle.entry.resource.ofType(MessageHeader).id
            patient,Bundle.entry.resource.ofType(Patient)
            performerState,Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state
            patientState,Bundle.entry.resource.ofType(Patient).address.state
            specimen,Bundle.entry.resource.ofType(Specimen)
            serviceRequest,Bundle.entry.resource.ofType(ServiceRequest)
            observation,Bundle.entry.resource.ofType(Observation)
    """.trimIndent()

    val shorthandTable = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one).loadLookupTable("fhirpath_filter_shorthand", shorthandTable)
    val report = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)
    val receiver = Receiver("myRcvr", "topic", Topic.TEST, CustomerStatus.ACTIVE, "mySchema")

    private val fhirCodeP = """
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
                "fullUrl": "MessageHeader/88a50cd6-72bf-34a1-9025-708e7c29cc32",
                "resource": {
                    "resourceType": "MessageHeader",
                    "id": "88a50cd6-72bf-34a1-9025-708e7c29cc32",
                    "meta": {
                        "extension": [
                            {
                                "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html",
                                            "code": "P"
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                }
            }
        ]
    }"""

    private val fhirCodeT = """
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
                "fullUrl": "MessageHeader/88a50cd6-72bf-34a1-9025-708e7c29cc32",
                "resource": {
                    "resourceType": "MessageHeader",
                    "id": "88a50cd6-72bf-34a1-9025-708e7c29cc32",
                    "meta": {
                        "extension": [
                            {
                                "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id",
                                "valueCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "https://terminology.hl7.org/3.1.0/CodeSystem-v2-0103.html",
                                            "code": "T"
                                        }
                                    ]
                                }
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

    @Test
    fun `test evaluate default - false`() {
        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val bundle = FhirTranscoder.decode(fhirCodeP)
        // act
        val result = engine.evaluateFilterCondition(
            emptyList(),
            bundle,
            false
        )

        // assert
        assertThat(result).isFalse()
    }

    @Test
    fun `test evaluateFilterCondition reverse filter`() {
        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val bundle = FhirTranscoder.decode(fhirCodeP)

        // act
        val useDefaultResult = engine.evaluateFilterCondition(
            emptyList(),
            bundle,
            false,
            true
        )
        // assert
        assertThat(useDefaultResult).isTrue()

        // act
        val useBundleResult = engine.evaluateFilterCondition(
            engine.processingModeFilterDefault,
            bundle,
            false,
            false
        )
        // assert
        assertThat(useBundleResult).isTrue()
    }

    @Test
    fun `test evaluate default - true`() {
        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val bundle = FhirTranscoder.decode(fhirCodeP)

        // act
        val result = engine.evaluateFilterCondition(
            emptyList(),
            bundle,
            true
        )

        // assert
        assertThat(result).isTrue()
    }

    @Test
    fun `test processModeFilter default pass (bundle mode = 'P')`() {
        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val bundle = FhirTranscoder.decode(fhirCodeP)

        // act
        val procModeResult = engine.evaluateFilterCondition(
            engine.processingModeFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(procModeResult).isTrue()
    }

    @Test
    fun `test processModeFilter default fail (mode = 'T')`() {
        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val bundle = FhirTranscoder.decode(fhirCodeT)

        // act
        val procModeResult = engine.evaluateFilterCondition(
            engine.processingModeFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(procModeResult).isFalse()
    }

    @Test
    fun `0 test qualFilter default - succeed, basic covid FHIR`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isTrue()
    }

    @Test
    fun `1 test qualFilter default - fail, missing item from first line`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_1.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isFalse()
    }

    @Test
    fun `2 test qualFilter default - fail, full first line, nothing second line`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_2.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isFalse()
    }

    @Test
    fun `3 test qualFilter default - fail, full first line, full second line, nothing third line`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_3.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isFalse()
    }

    @Test
    fun `4 test qualFilter default - succeed, full first line, patient street, order test date`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_4.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isTrue()
    }

    @Test
    fun `5 test qualFilter default - succeed, full first line, patient zip, order test date`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_5.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isTrue()
    }

    @Test
    fun `6 test qualFilter default - succeed, full first line, patient telecom, order test date`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_6.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isTrue()
    }

    @Test
    fun `7 test qualFilter default - succeed, full first line, patient street, specimen collection date`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_7.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isTrue()
    }

    @Test
    fun `8 test qualFilter default - succeed, full first line, patient street, test result date`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_8.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isTrue()
    }

    @Test
    fun `9 test qualFilter default - succeed, full first line, patient street, occurrence`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_9.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(
            engine.qualityFilterDefault,
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult).isTrue()
    }
}