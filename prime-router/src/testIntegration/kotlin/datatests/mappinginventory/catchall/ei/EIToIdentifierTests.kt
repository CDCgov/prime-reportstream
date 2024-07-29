package gov.cdc.prime.router.datatests.mappinginventory.ei

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class EIToIdentifierTests {

    @Test
    fun `verify HL7 to FHIR to HL7 default assigner`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ei/EI-to-Identifier-default-assigner").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 organization assigner`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ei/EI-to-Identifier-organization-assigner").passed)
    }
}