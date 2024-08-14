package gov.cdc.prime.router.datatests.mappinginventory.cq

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CQTests {
    @Test
    fun `test CQ to quantity`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cq/quantity/cq_all_fields").passed)
    }

    @Test
    fun `test CQ to quantity when CQ-2-9 null`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cq/quantity/cq_29_null").passed)
    }
}