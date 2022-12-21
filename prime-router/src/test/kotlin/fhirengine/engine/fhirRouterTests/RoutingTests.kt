package gov.cdc.prime.router.fhirengine.engine.fhirRouterTests

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutingTests {
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

    val twoOrganization = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "full-elr-hl7",
                "co-phd",
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one",
                reverseTheQualityFilter = true
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
            test-dash,Bundle.test.dash
            test_underscore,Bundle.test.underscore
            test'apostrophe,Bundle.test.apostrophe
    """.trimIndent()

    val shorthandTable = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one).loadLookupTable("fhirpath_filter_shorthand", shorthandTable)
    val report = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)
    val receiver = Receiver("myRcvr", "topic", Topic.TEST, CustomerStatus.ACTIVE, "mySchema")

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider, taskAction: TaskAction): FHIREngine {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build(taskAction)
    }

    @BeforeEach
    fun reset() {
        report.filteringResults.clear()
        clearAllMocks()
    }

    @Test
    fun `test replaceShorthand with simple string including fhir resource`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%resource.%patient")

        // assert
        assertThat(result).isEqualTo("%resource.Bundle.entry.resource.ofType(Patient)")
    }

    @Test
    fun `test replaceShorthand with simple string`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%patient")

        // assert
        assertThat(result).isEqualTo("Bundle.entry.resource.ofType(Patient)")
    }

    @Test
    fun `test replaceShorthand with simple string in quotes`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%'patient'")

        // assert
        assertThat(result).isEqualTo("Bundle.entry.resource.ofType(Patient)")
    }

    @Test
    fun `test replaceShorthand with simple string in backticks`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%`patient`")

        // assert
        assertThat(result).isEqualTo("Bundle.entry.resource.ofType(Patient)")
    }

    @Test
    fun `test replaceShorthand with two variable string`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%patient or %messageId")

        // assert
        assertThat(result).isEqualTo(
            "Bundle.entry.resource.ofType(Patient) or Bundle.entry.resource.ofType(MessageHeader).id"
        )
    }

    @Test
    fun `test replaceShorthand with dashed string`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%test-dash")

        // assert
        assertThat(result).isEqualTo("Bundle.test.dash")
    }

    @Test
    fun `test replaceShorthand with underscored string`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%test_underscore")

        // assert
        assertThat(result).isEqualTo("Bundle.test.underscore")
    }

    @Test
    fun `test replaceShorthand with apostrophe string`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%test'apostrophe")

        // assert
        assertThat(result).isEqualTo("Bundle.test.apostrophe")
    }

    @Test
    fun `test replaceShorthand with multi parts string`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%patient.name.first")

        // assert
        assertThat(result).isEqualTo("Bundle.entry.resource.ofType(Patient).name.first")
    }

    @Test
    fun `test replaceShorthand with multi part while in middle`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("myVariable.%patient.name.first")

        // assert
        assertThat(result).isEqualTo("myVariable.Bundle.entry.resource.ofType(Patient).name.first")
    }

    @Test
    fun `test replaceShorthand with similar strings, order 1`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%patient or %patientState")

        // assert
        assertThat(result).isEqualTo(
            "Bundle.entry.resource.ofType(Patient) or Bundle.entry.resource.ofType(Patient).address.state"
        )
    }

    @Test
    fun `test replaceShorthand with similar strings, order 2`() {
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val result = engine.replaceShorthand("%patientState or %patient")

        // assert
        assertThat(result).isEqualTo(
            "Bundle.entry.resource.ofType(Patient).address.state or Bundle.entry.resource.ofType(Patient)"
        )
    }

    @Test
    fun `test applyFilters receiver setting - (reverseTheQualityFilter = true) `() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(twoOrganization)
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 10")
        val routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val procModeFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter
        // act
        val receivers = engine.applyFilters(bundle, report)

        // assert
        assertThat(receivers).isNotEmpty()
    }

    @Test
    fun `test applyFilters receiver setting - (reverseTheQualityFilter = false) `() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val procModeFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        val receivers = engine.applyFilters(bundle, report)

        // assert
        assertThat(receivers).isNotEmpty()
    }

    @Test
    fun `test reverseQualityFilter flag only reverses the quality filter`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
        // Using receiver with reverQualityFilter set to true
        val settings = FileSettings().loadOrganizations(twoOrganization)
        // This Jurisdictional filter evaluates to true.
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        // This quality filter evaluates to true, but is reversed to false
        val qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        // This routing filter evaluates to true
        val routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        // This processing mode filter evaluates to true
        val procModeFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        val receivers = engine.applyFilters(bundle, report)

        // assert only the quality filter didn't pass
        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.QUALITY_FILTER)
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `0 test qualFilter default - succeed, basic covid FHIR`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
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
    fun `fail - jurisfilter does not pass`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 10")
        var qualFilter = emptyList<String>()
        var routingFilter = emptyList<String>()
        var procModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter does not`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 10")
        var routingFilter = emptyList<String>()
        var procModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter passes, routing filter does not`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 10")
        var procModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter passes, routing filter passes, proc mode does not`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var procModeFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 10")

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `success - jurisfilter passes, qual filter passes, routing filter passes, proc mode passes`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var procModeFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 1) {
            actionHistory.trackExistingInputReport(any())
            actionHistory.trackCreatedReport(any(), any(), any())
            BlobAccess.Companion.uploadBlob(any(), any())
            queueMock.sendMessage(any(), any())
            accessSpy.insertTask(any(), any(), any(), any())
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun ` test constantResolver for routing constants - succeed`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val filter = listOf(
            "(%performerState.exists() and %performerState = 'CA') or (%patientState.exists() " +
                "and %patientState = 'CA')"
        )
        val result = engine.evaluateFilterCondition(
            filter,
            bundle,
            false
        )
        // act & assert
        assertThat(result).isTrue()
    }

    @Test
    fun ` test constants - succeed, no patient state`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/no_patient_state.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val filter = listOf(
            "(%performerState.exists() and %performerState = 'CA') or (%patientState.exists() " +
                "and %patientState = 'CA')"
        )
        val result = engine.evaluateFilterCondition(
            filter,
            bundle,
            false
        )
        // act & assert
        assertThat(result).isTrue()
    }

    @Test
    fun ` test constants - succeed, no performer state`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/no_performer_state.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val filter = listOf(
            "(%performerState.exists() and %performerState = 'CA') or (%patientState.exists() " +
                "and %patientState = 'CA')"
        )
        val result = engine.evaluateFilterCondition(
            filter,
            bundle,
            false
        )
        // act & assert
        assertThat(result).isTrue()
    }

    @Test
    fun `test logFilterResults`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val report = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        var qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        assertThat(report.filteringResults.count()).isEqualTo(0)
        engine.logFilterResults(qualFilter, bundle, report, receiver, ReportStreamFilterType.QUALITY_FILTER)
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterName).isEqualTo(qualFilter.toString())
    }

    @Test
    fun `test evaluateFilterAndLogResult`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val report = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val filter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val type = ReportStreamFilterType.QUALITY_FILTER

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        every { engine.evaluateFilterCondition(any(), any(), true, any()) } returns true
        every { engine.evaluateFilterCondition(any(), any(), false, any()) } returns false

        engine.evaluateFilterAndLogResult(filter, bundle, report, receiver, type, true)
        verify(exactly = 0) {
            engine.logFilterResults(any(), any(), any(), any(), any())
        }
        engine.evaluateFilterAndLogResult(filter, bundle, report, receiver, type, false)
        verify(exactly = 1) {
            engine.logFilterResults(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test applyFilters logs results for routing filters`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
        // Using receiver with reverQualityFilter set to true
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // This Jurisdictional filter evaluates to true.
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        // This quality filter evaluates to true
        val qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        // This routing filter evaluates to false
        val routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 10")
        // This processing mode filter evaluates to true
        val procModeFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        val receivers = engine.applyFilters(bundle, report)

        // assert only the quality filter didn't pass

        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.ROUTING_FILTER)
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `test applyFilters logs results for processing mode filters`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
        // Using receiver with reverQualityFilter set to true
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // This Jurisdictional filter evaluates to true.
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        // This quality filter evaluates to true
        val qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        // This routing filter evaluates to true
        val routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        // This processing mode filter evaluates to false
        val procModeFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 10")
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        val receivers = engine.applyFilters(bundle, report)

        // assert only the quality filter didn't pass

        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        assertThat(receivers).isEmpty()
    }
}