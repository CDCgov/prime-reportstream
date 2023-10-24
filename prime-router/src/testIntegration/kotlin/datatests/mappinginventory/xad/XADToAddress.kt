package gov.cdc.prime.router.datatests.mappinginventory.xad

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class XADToAddress {

    @Test
    fun `test translate HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address").passed)
    }
}