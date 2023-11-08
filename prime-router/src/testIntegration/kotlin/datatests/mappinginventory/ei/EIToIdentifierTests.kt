package gov.cdc.prime.router.datatests.mappinginventory.ei

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import kotlin.test.Test

class EIToIdentifierTests {

    @Test
    fun `EI to Identifier`() {
        assert(verifyHL7ToFHIRToHL7Mapping("ei/EI-to-Identifier").passed)
    }
}