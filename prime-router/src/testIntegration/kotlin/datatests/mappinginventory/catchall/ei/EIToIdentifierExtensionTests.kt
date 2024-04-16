package gov.cdc.prime.router.datatests.mappinginventory.ei

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class EIToIdentifierExtensionTests {

    @Test
    fun `EI to Identifier Extension`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ei/EI-to-Identifier-Extension").passed)
    }
}