package gov.cdc.prime.router.datatests.mappinginventory.xad

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class XADToAddress {

    @Test
    fun `test translate HL7 to FHIR to HL7 m-address-type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address-type-m").passed)
    }

    @Test
    fun `test translate HL7 to FHIR to HL7 sh-address-type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address-type-sh").passed)
    }

    @Test
    fun `test translate HL7 to FHIR to HL7 ba-address-type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address-type-ba").passed)
    }

    @Test
    fun `test translate HL7 to FHIR to HL7 bi-address-type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address-type-bi").passed)
    }

    @Test
    fun `test translate HL7 to FHIR to HL7 c-address-type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address-type-c").passed)
    }

    @Test
    fun `test translate HL7 to FHIR to HL7 b-address-type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address-type-b").passed)
    }

    @Test
    fun `test translate HL7 to FHIR to HL7 h-address-type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address-type-h").passed)
    }

    @Test
    fun `test translate HL7 to FHIR to HL7 o-address-type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address-type-o").passed)
    }

    @Test
    fun `test translate HL7 to FHIR to HL7 hv-address-type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xad/xad-to-address-type-hv").passed)
    }
}