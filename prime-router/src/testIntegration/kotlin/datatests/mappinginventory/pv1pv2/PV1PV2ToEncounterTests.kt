package gov.cdc.prime.router.datatests.mappinginventory.pv1pv2

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class PV1PV2ToEncounterTests {

    @Test
    fun `test converts PV1 and PV2 to Encounter`() {
        assert(verifyHL7ToFHIRToHL7Mapping("pv1pv2/PV1-PV2-to-Encounter").passed)
    }

    @Test
    fun `test converts PV1 to Encounter without PV2`() {
    }
}