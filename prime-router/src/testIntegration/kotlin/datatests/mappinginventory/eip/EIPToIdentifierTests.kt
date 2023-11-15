package gov.cdc.prime.router.datatests.mappinginventory.eip

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class EIPToIdentifierTests {

    @Test
    fun `verify HL7 to FHIR to HL7 from Placer Assigned Identifier`() {
        assert(verifyHL7ToFHIRToHL7Mapping("eip/EIP-to-Identifier-PlacerAssignedIdentifier").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 from Filler Assigned Identifier`() {
        assert(verifyHL7ToFHIRToHL7Mapping("eip/EIP-to-Identifier-FillerAssignedIdentifier").passed)
    }
}