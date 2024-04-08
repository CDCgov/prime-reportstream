package gov.cdc.prime.router.datatests.mappinginventory.pid

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class PIDToPatient {
    @Test
    fun `test PID fully populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pid/pid-to-patient-pid-fully-populated").passed)
    }

    @Test
    fun `test PID-25 not populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pid/pid-to-patient-pid-25-not-populated").passed)
    }

    @Test
    fun `test PID-25 populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pid/pid-to-patient-pid-25-populated").passed)
    }

    @Test
    fun `test PID-29 not populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pid/pid-to-patient-pid-29-not-populated").passed)
    }

    @Test
    fun `test PID-29 populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pid/pid-to-patient-pid-29-populated").passed)
    }
}