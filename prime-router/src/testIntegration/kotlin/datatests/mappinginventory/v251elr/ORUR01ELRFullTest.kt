package gov.cdc.prime.router.datatests.mappinginventory.v251elr

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class ORUR01ELRFullTest {
    @Test
    fun `test ORU_R01 all segments`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/orur01/oru_r01-full",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }
}