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
    fun `test uses MSH6 when MSH5 and MSH25 are not valued`() {
    }

    @Test
    fun `test stores MSH6 and MSH23 when when MSH5 and MSH25 are not valued`() {
    }

    @Test
    fun `does not create a second destination if the contents of MSH5 and MSH25 are equal`() {
    }

    @Test
    fun `test correctly stores MSH8 security in meta`() {
    }

    @Test
    fun `test stores info from SFT when MSH3 and MSH24 are not valued`() {
    }

    @Test
    fun `test stores multiple character sets correctly`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/MSH-to-MessageHeader-handles-multiple-msh18").passed)
    }
}