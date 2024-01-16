package gov.cdc.prime.router.datatests.mappinginventory.spm

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class SPMtoSpecimen {
    @Test
    fun `test SPM fully populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("spm/spm").passed)
    }
}