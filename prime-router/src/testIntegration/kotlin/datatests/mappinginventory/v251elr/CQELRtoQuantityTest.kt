package gov.cdc.prime.router.datatests.mappinginventory.v251elr

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CQELRtoQuantityTest {
    @Test
    fun `test value for CQ_ELR`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cq/cq-to-quantity",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }
}