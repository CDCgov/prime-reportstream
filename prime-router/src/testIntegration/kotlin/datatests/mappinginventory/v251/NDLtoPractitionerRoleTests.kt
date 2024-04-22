package gov.cdc.prime.router.datatests.mappinginventory.v251

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class NDLtoPractitionerRoleTests {
    @Test
    fun `test translate to NDL to PractitionerRole to NDL`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251/ndl/NDL-to-PractitionerRole",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }
}