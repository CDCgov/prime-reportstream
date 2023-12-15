package gov.cdc.prime.router.datatests.mappinginventory.pd1

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class PD1ToPatient {
    @Test
    fun `test PD1 populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("pd1/pd1").passed)
    }
}