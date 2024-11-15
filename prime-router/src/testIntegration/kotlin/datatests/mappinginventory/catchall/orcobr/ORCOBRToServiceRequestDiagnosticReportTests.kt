package gov.cdc.prime.router.datatests.mappinginventory.orcobr

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class ORCOBRToServiceRequestDiagnosticReportTests {

    @Test
    fun `test ORC populated`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-orc-populated"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName).passed)
    }

    @Test
    fun `test OBR populated`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-obr-populated"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName).passed)
    }

    @Test
    fun `test OBR and ORC populated`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-orc-obr-populated"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName).passed)
    }

    @Test
    fun `test handles different specimen action codes`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-obr11-g").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-obr11-a").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-obr11-l").passed)
    }

    @Test
    fun `test prefers ORC for identifiers`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-prefers-orc-for-identifiers"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName).passed)
    }

    @Test
    fun `test will correctly set authoredOn`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-sets-authored-on"
            ).passed
        )
    }

    @Test
    fun `test prefers ORC12 for the requester`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-prefers-orc-12-for-requester"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName).passed)
    }

    @Test
    fun `test will create a practitioner only when ORC21, ORC22, ORC23 are not populated`() {
        val missingOrc212223 =
            "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-requester-practitioner-missing-orc212223"
        assert(verifyHL7ToFHIRToHL7Mapping(missingOrc212223).passed)
        val orc21 =
            "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-requester-practitionerrole-orc21-populated"
        assert(verifyHL7ToFHIRToHL7Mapping(orc21).passed)
        val orc22 =
            "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-requester-practitionerrole-orc22-populated"
        assert(verifyHL7ToFHIRToHL7Mapping(orc22).passed)
        val orc23 =
            "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-requester-practitionerrole-orc23-populated"
        assert(verifyHL7ToFHIRToHL7Mapping(orc23).passed)
    }

    @Test
    fun `test correctly handles the effectiveDateTime when OBR8 is not populated`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-obr8-not-populated"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName).passed)
    }

    @Test
    fun `test correctly handles the effectivePeriod when OBR8 is populated`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-obr8-populated"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName).passed)
    }

    @Test
    fun `test handles mapped HL7 values to OBRExtension from OBR25`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-obr25"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName).passed)
    }

    @Test
    fun `test handles unmapped HL7 values to OBRExtension from OBR25`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-obr25-not-mapped"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName).passed)
    }

    @Test
    fun `test does not map OBR25 when OBRExtension is not populated`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-obr25-obrext-not-populated"
        assert(verifyHL7ToFHIRToHL7Mapping(testFileName, true, false, true).passed)
    }

    @Test
    fun `test links ServiceRequest to Observation through SupportingInfo`() {
        val testFileName = "catchall/orcobr/orc_obr-to-servicerequest_diagnosticreport-OML-message"
        assert(
            verifyHL7ToFHIRToHL7Mapping(
            testFileName,
            outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
        ).passed
        )
    }
}