package gov.cdc.prime.router.datatests.mappinginventory.dtm

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class DTMTests {

    @Test
    fun `test dtm to instant`() {
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-year-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-zero-year-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-month-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-day-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-minute-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-second-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-dsec-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-csec-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-msec-precision-to-instant").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/instant/dtm-mmsec-precision-to-instant").passed)
    }

    @Test
    fun `test dtm to dateTime`() {
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-year-precision-to-dateTime").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-zero-year-to-dateTime").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-month-precision-to-dateTime").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-day-precision-to-dateTime").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-minute-precision-to-dateTime").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-second-precision-to-dateTime").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-dsec-precision-to-dateTime").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-csec-precision-to-dateTime").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-msec-precision-to-dateTime").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/dateTime/dtm-mmsec-precision-to-dateTime").passed)
    }

    @Test
    fun `test dtm to date`() {
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-year-precision-to-date").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-zero-year-to-date").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-month-precision-to-date").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-day-precision-to-date").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-minute-precision-to-date").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-second-precision-to-date").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-dsec-precision-to-date").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-csec-precision-to-date").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-msec-precision-to-date").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("dtm/date/dtm-mmsec-precision-to-date").passed)
    }
}