package cli

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.github.ajalt.clikt.core.CliktError
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.cli.NoopReportStreamEventService
import gov.cdc.prime.router.cli.ProcessFhirCommands
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkObject
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.Test

 class ProcessFhirCommandsTests {

     private val jurisdictionalFilter: ReportStreamFilter = listOf(
         "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state " +
         "= 'ME') or (Bundle.entry.resource.ofType(Patient).address.state = 'ME')"
     )
     private val qualityFilter: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(Patient).birthDate.exists()",
         "Bundle.entry.resource.ofType(Patient).name.family.exists()"
     )
     private val receiverHl7Configuration = Hl7Configuration(
         schemaName = "classpath:/metadata/hl7_mapping/receivers/STLTs/ME/ME-receiver-transform.yml",
         useTestProcessingMode = true,
         useBatchHeaders = true,
         messageProfileId = "",
         receivingApplicationName = "",
         receivingFacilityOID = "",
         receivingFacilityName = "",
         receivingApplicationOID = "",
         receivingOrganization = ""
     )
     private val receiver = Receiver(
         "full-elr",
         "me-phd",
         Topic.FULL_ELR,
         CustomerStatus.ACTIVE,
         receiverHl7Configuration,
         jurisdictionalFilter = jurisdictionalFilter,
         qualityFilter = qualityFilter
     )

     private val testFile = File("src/testIntegration/resources/datatests/FHIR_to_HL7/sample_ME_20240806-0001.fhir")
     private val fhirString = testFile.inputStream().readBytes().toString(Charsets.UTF_8)

     @Test
     fun handleReceiverFilters() {
         mockkObject(Metadata)
         every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
         val bundle = FhirTranscoder.decode(fhirString)
         val messageOrBundle = ProcessFhirCommands.MessageOrBundle()
         messageOrBundle.bundle = bundle

         // test missing bundle id
         messageOrBundle.bundle?.identifier?.setValue(null)
         ProcessFhirCommands().handleReceiverFilters(receiver, messageOrBundle, false)
         assertThat(messageOrBundle.bundle?.identifier?.value).isNotNull()

         // remove birthdate to make quality filter fail
         val patient = bundle.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient
         patient.birthDate = null

         // reset filterErrors and test CliKtError thrown when isCli = true
         messageOrBundle.filterErrors = mutableListOf()
         val cliError = assertThrows<CliktError> {
             ProcessFhirCommands().handleReceiverFilters(receiver, messageOrBundle, true)
         }
         assertThat(cliError.message).isEqualTo(
             "QUALITY_FILTER - Filter failed :  \n Bundle.entry.resource.ofType(Patient).birthDate.exists()"
         )
     }

     @Test
     fun evaluateReceiverFilters() {
         mockkObject(Metadata)
         every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
         val bundle = FhirTranscoder.decode(fhirString)
         val messageOrBundle = ProcessFhirCommands.MessageOrBundle()
         messageOrBundle.bundle = bundle
         val fhirReceiverFilter = FHIRReceiverFilter(reportStreamEventService = NoopReportStreamEventService())

         ProcessFhirCommands().evaluateReceiverFilters(receiver, messageOrBundle, fhirReceiverFilter)

         assertThat(messageOrBundle.filterErrors).isEmpty()
     }

     @Test
     fun `evaluateReceiverFilters - with filter errors`() {
         mockkObject(Metadata)
         every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
         val bundle = FhirTranscoder.decode(fhirString)

         // remove birthdate to make quality filter fail
         val patient = bundle.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient
         patient.birthDate = null

         val messageOrBundle = ProcessFhirCommands.MessageOrBundle()
         messageOrBundle.bundle = bundle

         val fhirReceiverFilter = FHIRReceiverFilter(reportStreamEventService = NoopReportStreamEventService())
        // Test filter error fields
         ProcessFhirCommands().evaluateReceiverFilters(receiver, messageOrBundle, fhirReceiverFilter)
         assertThat(messageOrBundle.filterErrors).isNotEmpty()
         val qualityFilterError = messageOrBundle.filterErrors.filter {
             it.filterType == ReportStreamFilterType.QUALITY_FILTER.toString()
         }
         assertThat(qualityFilterError.count()).isEqualTo(1)
         assertThat(qualityFilterError.first().filter)
             .isEqualTo("Bundle.entry.resource.ofType(Patient).birthDate.exists()")
         assertThat(qualityFilterError.first().message).isEqualTo("Filter failed")

         // Test multiple filters failed
         val receiver2 = Receiver(
             "full-elr",
             "me-phd",
             Topic.FULL_ELR,
             CustomerStatus.ACTIVE,
             receiverHl7Configuration,
             jurisdictionalFilter = listOf("Bundle.entry.resource.ofType(Patient).address.state = 'IG'"),
             qualityFilter = qualityFilter,
         )

         messageOrBundle.filterErrors = mutableListOf()
         ProcessFhirCommands().evaluateReceiverFilters(receiver2, messageOrBundle, fhirReceiverFilter)
         assertThat(
             messageOrBundle.filterErrors.filter {
             it.filterType == ReportStreamFilterType.QUALITY_FILTER.toString()
         }
         ).isNotEmpty()
         assertThat(
             messageOrBundle.filterErrors.filter {
             it.filterType == ReportStreamFilterType.JURISDICTIONAL_FILTER.toString()
         }
         ).isNotEmpty()

         // Test invalid filter
         val invalidFilter = "Bundle.entry.resource.ofType(MessageHeader).meta.tag"
         val receiver3 = Receiver(
             "full-elr",
             "me-phd",
             Topic.FULL_ELR,
             CustomerStatus.ACTIVE,
             receiverHl7Configuration,
             qualityFilter = qualityFilter,
             processingModeFilter = listOf(invalidFilter)
         )

         messageOrBundle.filterErrors = mutableListOf()
         ProcessFhirCommands().evaluateReceiverFilters(receiver3, messageOrBundle, fhirReceiverFilter)

         val processingFilterError = messageOrBundle.filterErrors.filter {
             it.filterType == ReportStreamFilterType.PROCESSING_MODE_FILTER.toString()
         }
         assertThat(processingFilterError).isNotEmpty()
         assertThat(processingFilterError.first().filter).isEqualTo(invalidFilter)
         assertThat(processingFilterError.first().message).isEqualTo(
             "Invalid filter - FHIR Path expression did not evaluate to a boolean type: Bundle.entry.resource." +
                 "ofType(MessageHeader).meta.tag"
         )
     }

     @Test
     fun applyConditionFilters() {
         mockkObject(Metadata)
         every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
         val bundle = FhirTranscoder.decode(fhirString)
         val messageOrBundle = ProcessFhirCommands.MessageOrBundle()
         messageOrBundle.bundle = bundle
         val fhirReceiverFilter = FHIRReceiverFilter(reportStreamEventService = NoopReportStreamEventService())

         ProcessFhirCommands().applyConditionFilter(receiver, messageOrBundle, fhirReceiverFilter)
         assertThat(messageOrBundle.filterErrors).isEmpty()

         val conditionFilter: ReportStreamFilter = listOf(
             "%resource.code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code')" +
             ".value.where(code in ('840539006')).exists()"
         )

         val receiver2 = Receiver(
             "full-elr",
             "me-phd",
             Topic.FULL_ELR,
             CustomerStatus.ACTIVE,
             receiverHl7Configuration,
             conditionFilter = conditionFilter
         )
         ProcessFhirCommands().applyConditionFilter(receiver2, messageOrBundle, fhirReceiverFilter)
         assertThat(messageOrBundle.filterErrors).isEmpty()
     }

     @Test
     fun `applyConditionFilters - with filter errors`() {
         mockkObject(Metadata)
         every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
         val bundle = FhirTranscoder.decode(fhirString)
         val messageOrBundle = ProcessFhirCommands.MessageOrBundle()
         messageOrBundle.bundle = bundle
         val fhirReceiverFilter = FHIRReceiverFilter(reportStreamEventService = NoopReportStreamEventService())

         val zeroConditionFilter: ReportStreamFilter = listOf(
             "%resource.code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code')" +
             ".value.where(code in ('0000000')).exists()"
         )

         val receiver2 = Receiver(
             "full-elr",
             "me-phd",
             Topic.FULL_ELR,
             CustomerStatus.ACTIVE,
             receiverHl7Configuration,
             conditionFilter = zeroConditionFilter
         )

         ProcessFhirCommands().applyConditionFilter(receiver2, messageOrBundle, fhirReceiverFilter)

         val conditionFilterError = messageOrBundle.filterErrors.filter {
             it.filterType == ReportStreamFilterType.CONDITION_FILTER.toString()
         }
         assertThat(conditionFilterError).isNotEmpty()
         assertThat(conditionFilterError.first().filter).isEqualTo(zeroConditionFilter.first())
         assertThat(conditionFilterError.first().message).isEqualTo("Filter failed")

         val invalidConditionFilter: ReportStreamFilter = listOf("%resource.code.coding.")

         val receiver3 = Receiver(
             "full-elr",
             "me-phd",
             Topic.FULL_ELR,
             CustomerStatus.ACTIVE,
             receiverHl7Configuration,
             conditionFilter = invalidConditionFilter
         )
         messageOrBundle.filterErrors = mutableListOf()

         ProcessFhirCommands().applyConditionFilter(receiver3, messageOrBundle, fhirReceiverFilter)

         val conditionFilterError2 = messageOrBundle.filterErrors.filter {
             it.filterType == ReportStreamFilterType.CONDITION_FILTER.toString()
         }
         assertThat(conditionFilterError2).isNotEmpty()
         assertThat(conditionFilterError2.first().filter).isEqualTo(invalidConditionFilter.first())
         assertThat(conditionFilterError2.first().message).isEqualTo(
             "Invalid filter - Syntax error: Error in ?? at 1, 1: Expression terminated unexpectedly in " +
                 "FHIR Path expression %resource.code.coding."
         )
     }
}