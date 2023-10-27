package gov.cdc.prime.router.datatests.mappinginventory.cwe

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CodeableConceptTests {

    @Test
    fun `test OBR-39`() {
        assert(verifyHL7ToFHIRToHL7Mapping("cwe-obr-39").passed)
    }
}