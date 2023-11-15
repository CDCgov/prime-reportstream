package gov.cdc.prime.router.datatests.mappinginventory.msh

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class MSHToMessageHeaderTests {

    // This differs from the official inventory (which does mention that it's partially up to the implementor
    // since from RS experience MSH3 more often contains values to use
    @Test
    fun `test prefers MSH3 over MSH24`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-prefers-msh3").passed)
    }

    @Test
    fun `test uses MSH24 when MSH3 is not valued`() {
    }

    @Test
    fun `test uses MSH23 when MSH5 MSH6 not valued and MSH25 valued`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-msh23-msh25-valued-msh5-msh6-not-valued").passed)
    }

    @Test
    fun `test uses MSH6 when MSH5 not valued and MSH25 valued`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-msh6-msh25-valued-msh5-not-valued").passed)
    }

    @Test
    fun `test stores MSH6 and MSH23 when when MSH5 and MSH25 are not valued`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-msh6-msh23-valued-msh5-msh25-not-valued").passed)
    }

    @Test
    fun `test stores MSH5 and MSH25 when valued`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-msh5-msh25-different-value").passed)
    }

    @Test
    fun `test does not create a second destination if the contents of MSH5 and MSH25 are equal`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-msh5-msh25-same-value").passed)
    }

    @Test
    fun `test correctly stores MSH8 security in meta`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-stores-msh8-msh11-not-valued").passed)
    }

    @Test
    fun `test stores info from SFT when MSH3 and MSH24 are not valued`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-stores-sft-without-msh3-msh24").passed)
    }

    @Test
    fun `test stores multiple character sets correctly`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-handles-multiple-msh18").passed)
    }

    @Test
    fun `test all MSH valued`() {
    }

    @Test
    fun `test all MSH and SFT valued`() {
    }

    @Test
    fun `test MSH17 valued and MSH4 not valued`() {
    }

    @Test
    fun `test language prefers MSH191 over MSH194`() {
    }
}