package gov.cdc.prime.router.datatests.mappinginventory.pt

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class PTToMetaTests {

    @Test
    fun `test translate to PT to Meta to PT`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pt/PT-to-Meta").passed)
    }
}