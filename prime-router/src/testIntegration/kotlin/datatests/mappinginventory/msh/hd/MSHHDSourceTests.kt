package gov.cdc.prime.router.datatests.mappinginventory.msh.hd

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class MSHHDSourceTests {

    @Test
    fun `test handles ISO universal ID type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/hd/HD-to-Source-iso").passed)
    }

    @Test
    fun `test handles DNS universal ID type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/hd/HD-to-Source-dns").passed)
    }

    @Test
    fun `test handles UUID universal ID type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/hd/HD-to-Source-uuid").passed)
    }

    @Test
    fun `test handles URI universal ID type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/hd/HD-to-Source-uri").passed)
    }

    @Test
    fun `test handles CLIA universal ID type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/hd/HD-to-Source-clia").passed)
    }

    @Test
    fun `test missing HD2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/hd/HD-to-Source-missing-hd2").passed)
    }

    @Test
    fun `test adds in the correct SFT fields`() {
        assert(verifyHL7ToFHIRToHL7Mapping("msh/hd/HD-to-Source-with-SFT").passed)
    }
}