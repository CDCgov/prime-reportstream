package gov.cdc.prime.router.datatests.mappinginventory.orcobr

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class ORCOBRToServiceRequestDiagnosticReportTests {

    @Test
    fun `test ORC populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("orcobr/orc_obr-to-servicerequest_diagnosticreport-orc-populated").passed)
    }

    @Test
    fun `test OBR populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("orcobr/orc_obr-to-servicerequest_diagnosticreport-obr-populated").passed)
    }
}