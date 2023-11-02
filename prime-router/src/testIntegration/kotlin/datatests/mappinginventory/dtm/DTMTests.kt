package gov.cdc.prime.router.datatests.mappinginventory.dtm

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class DTMTests {

    @Test
    fun `test dtm to instant`() {
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dtm-year-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dtm-month-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dtm-day-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dtm-minute-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dtm-second-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dtm-dsec-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dtm-csec-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dtm-msec-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dtm-mmsec-precision-to-instant").passed)
    }
}