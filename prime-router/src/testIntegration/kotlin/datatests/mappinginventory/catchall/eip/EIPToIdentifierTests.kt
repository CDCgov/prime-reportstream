package gov.cdc.prime.router.datatests.mappinginventory.eip

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class EIPToIdentifierTests {

    @Test
    fun `verify HL7 to FHIR to HL7 from both Placer Assigned Identifier and Filler Assigned Identifier`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/eip/EIP-to-Identifier-Both").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 from only Placer Assigned Identifier`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/eip/EIP-to-Identifier-FillerAssignedIdentifier").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 from only Filler Assigned Identifier`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/eip/EIP-to-Identifier-FillerAssignedIdentifier").passed)
    }
}