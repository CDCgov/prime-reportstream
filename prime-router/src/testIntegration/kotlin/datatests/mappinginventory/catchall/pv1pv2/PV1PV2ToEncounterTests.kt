package gov.cdc.prime.router.datatests.mappinginventory.pv1pv2

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class PV1PV2ToEncounterTests {

    @Test
    fun `test converts PV1 and PV2 to Encounter`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pv1pv2/PV1-PV2-to-Encounter").passed)
    }

    @Test
    fun `test converts PV1 to Encounter without PV2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pv1pv2/PV1-to-Encounter").passed)
    }

    @Test
    fun `test sets the status correctly when PV145 is valued`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pv1pv2/PV1-to-Encounter-pv145-valued").passed)
    }

    @Test
    fun `test correctly handles PV2-22`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pv1pv2/PV1-PV2-to-Encounter-pv22-y").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pv1pv2/PV1-PV2-to-Encounter-pv22-n").passed)
    }

    @Test
    fun `test correctly handles PV12`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pv1pv2/PV1-to-Encounter-pv12-p").passed)
    }
}