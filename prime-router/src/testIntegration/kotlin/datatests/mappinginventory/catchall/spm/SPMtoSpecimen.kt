package gov.cdc.prime.router.datatests.mappinginventory.spm

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class SPMtoSpecimen {
    @Test
    fun `test SPM fully populated with 17-2 populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/spm/SPM-with-17-2-populated").passed)
    }

    @Test
    fun `test SPM fully populated with 17-2 not populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/spm/SPM-with-17-2-not-populated").passed)
    }
}