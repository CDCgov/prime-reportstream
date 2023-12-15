package gov.cdc.prime.router.datatests.mappinginventory

import org.junit.jupiter.api.Test

class OBXToObservationTests {

    @Test
    fun `test converts with ST for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("obx/OBX-to-Observation-st-value").passed)
    }
}