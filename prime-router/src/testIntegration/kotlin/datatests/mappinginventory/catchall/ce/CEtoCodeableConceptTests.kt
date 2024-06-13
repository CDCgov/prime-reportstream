package gov.cdc.prime.router.datatests.mappinginventory.catchall.ce

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CEtoCodeableConceptTests {
    @Test
    fun `test value in CE-1`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ce/ce-3-test-value-ce1").passed)
    }

    @Test
    fun `test value in CE-2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ce/ce-4-test-value-ce2").passed)
    }

    @Test
    fun `test value in CE-3`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ce/ce-5-test-value-ce3").passed)
    }

    @Test
    fun `test value in CE-4`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ce/ce-6-test-value-ce4").passed)
    }

    @Test
    fun `test value in CE-5`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ce/ce-7-test-value-ce5").passed)
    }

    @Test
    fun `test value in CE-6`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ce/ce-8-test-value-ce6").passed)
    }
}