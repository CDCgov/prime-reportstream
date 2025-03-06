package gov.cdc.prime.router.datatests.mappinginventory.obx

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

class OBXToObservationTests {

    @Test
    fun `test converts with ST for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-st-value").passed)
    }

    @Test
    fun `test converts with FT for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-ft-value").passed)
    }

    @Test
    fun `test converts with TX for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-tx-value").passed)
    }

    @Test
    fun `test converts with VR for obx2`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/obx/OBX-to-Observation-vr-value",
            ).passed
        )
    }

    @Test
    fun `test converts with SN as a string for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-sn-as-string-value").passed)
    }

    @Test
    fun `test converts with CWE for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-cwe-value").passed)
    }

    @Test
    fun `test converts with CE for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-ce-value").passed)
    }

    @Test
    fun `test converts with IS for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-is-value").passed)
    }

    @Test
    fun `test converts with DR for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-dr-value").passed)
    }

    @Test
    fun `test converts with DT for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-dt-value").passed)
    }

    @Test
    fun `test converts with DTM for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-dtm-value").passed)
    }

    @Test
    fun `test converts with TM for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-tm-value").passed)
    }

    @Test
    fun `test converts with NR for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-nr-value").passed)
    }

    @Test
    fun `test converts SN as range for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-sn-as-range-value").passed)
    }

    @Test
    fun `test converts with SN as ratio for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-sn-as-ratio-value-division").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-sn-as-ratio-value-colon").passed)
    }

    @Test
    fun `test converts with NM for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-nm-value").passed)
    }

    @Test
    fun `test converts with SN as quantity for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-sn-as-quantity-value").passed)
    }

    @Test
    fun `test converts NM as value for obx2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-nm-value").passed)
    }

    @Test
    fun `test converts with ED for the value type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-ed-value").passed)
    }

    @Test
    fun `test converts with data absent`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-data-absent-x-value").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-data-absent-n-value").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-data-absent-st-but-empty-value").passed)
    }

    @Test
    fun `test creates an organization as performer when OBX25 not populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-obx25-not-valued").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-obx25-valued").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-obx25-obx24-not-valued").passed)
    }

    @Test
    fun `test correctly handles OBX-18 device identifier missing values`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/obx/OBX-to-Observation-obx-18-extra-device-identifier",
                skipHl7ToFhir = true
            ).passed
        )
    }

    @Test
    fun `test correctly handles OBX-33`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/obx/OBX-to-Observation-obx-33").passed)
    }

    // TODO https://github.com/CDCgov/prime-reportstream/issues/16522
    @Test
    @Ignore
    fun `test correctly handles OBX-33 without extension`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/obx/OBX-to-Observation-obx-33-no-extension",
                skipHl7ToFhir = true
            ).passed
        )
    }
}