package gov.cdc.prime.router.datatests.mappinginventory.v251elr

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class XPNELRtoHumanNameTests {
    @Test
    fun `test translate to HL7 to FHIR to HL7`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/xpn/xpn-to-humanname-xpn12-13-populated-xpn10-empty",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }
}