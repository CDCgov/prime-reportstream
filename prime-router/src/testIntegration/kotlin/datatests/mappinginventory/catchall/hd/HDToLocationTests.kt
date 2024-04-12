package gov.cdc.prime.router.datatests.mappinginventory.hd

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

/**
 * The mapping of HD to Location is typically used within an existing HL7 Data type -> FHIR resource
 * These tests use MSH.22.6 to test the mapping.
 */
class HDToLocationTests {

    @Test
    fun `test correctly handles ISO universal id type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/hd/HD-to-Location-iso").passed)
    }

    @Test
    fun `test correctly handles UUID universal id type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/hd/HD-to-Location-uuid").passed)
    }

    @Test
    fun `test correctly handles unknown universal id type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/hd/HD-to-Location-dns").passed)
    }

    @Test
    fun `test correctly handles HD3 being empty`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/hd/HD-to-Location-empty-hd3").passed)
    }
}