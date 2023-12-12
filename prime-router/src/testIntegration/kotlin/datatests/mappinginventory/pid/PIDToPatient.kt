package gov.cdc.prime.router.datatests.mappinginventory.pid

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class PIDToPatient {
    @Test
    fun `test PID populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("pid/PID").passed)
    }
}