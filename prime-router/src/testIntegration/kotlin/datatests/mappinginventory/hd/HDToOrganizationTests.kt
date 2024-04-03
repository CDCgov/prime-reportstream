package gov.cdc.prime.router.datatests.mappinginventory.hd

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class HDToOrganizationTests {

    @Test
    fun `test that an ISO HD HL7 datatype can be mapped to a FHIR Organization`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "hd/HD-to-Organization-iso",
                additionalProfiles = listOf("./metadata/HL7/v251-elr")
            ).passed
        )
    }

    @Test
    fun `test that an UUID HD HL7 datatype can be mapped to a FHIR Organization`() {
        assert(verifyHL7ToFHIRToHL7Mapping("hd/HD-to-Organization-uuid").passed)
    }

    @Test
    fun `test that identifier2 system is not set for unknown system type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("hd/HD-to-Organization-clia").passed)
    }

    @Test
    fun `test that the mapping handles a missing HD3 value`() {
        assert(verifyHL7ToFHIRToHL7Mapping("hd/HD-to-Organization-empty-hd3").passed)
    }
}