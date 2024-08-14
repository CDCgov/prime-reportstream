package gov.cdc.prime.router.datatests.mappinginventory.pd1

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class PD1ToPatient {
    @Test
    fun `test PD1 populated with PD1-14-1 populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pd1/pd1-to-patient-pd1-14-1-populated").passed)
    }

    @Test
    fun `test PD1 populated with PD1-14-1 blank and PD1-14-10 populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pd1/pd1-to-patient-pd1-14-1-blank-14-10-populated").passed)
    }
}