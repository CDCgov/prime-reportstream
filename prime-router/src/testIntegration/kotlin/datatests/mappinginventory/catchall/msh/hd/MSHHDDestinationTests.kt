package gov.cdc.prime.router.datatests.mappinginventory.msh.hd

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class MSHHDDestinationTests {

    @Test
    fun `test handles UUID universal ID type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/msh/hd/HD-to-Destination-uuid").passed)
    }

    @Test
    fun `test handles ISO universal ID type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/msh/hd/HD-to-Destination-iso").passed)
    }

    @Test
    fun `test handles DNS universal ID type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/msh/hd/HD-to-Destination-dns").passed)
    }

    @Test
    fun `test handles missing HD3`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/msh/hd/HD-to-Destination-missing-hd3").passed)
    }

    @Test
    fun `test prefers MSH6 for the receiver`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/msh/hd/HD-to-Destination-prefers-msh6").passed)
    }

    @Test
    fun `test uses MSH23 for the receiver when MSH6 is not present`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/msh/hd/HD-to-Destination-msh6-not-valued-msh23-valued").passed)
    }
}