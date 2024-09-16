package gov.cdc.prime.router.datatests.mappinginventory.v251

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class XADtoAddressTests {
    @Test
    fun `xad to address`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251/xad/xad-to-address",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }
}