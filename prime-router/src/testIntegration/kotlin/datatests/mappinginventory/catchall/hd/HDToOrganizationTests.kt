package gov.cdc.prime.router.datatests.mappinginventory.hd

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class HDToOrganizationTests {

    @Test
    fun `test that an ISO HD HL7 datatype can be mapped to a FHIR Organization`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/hd/HD-to-Organization-iso"
            ).passed
        )
    }

    @Test
    fun `test that an UUID HD HL7 datatype can be mapped to a FHIR Organization`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/hd/HD-to-Organization-uuid"
            ).passed
        )
    }

    @Test
    fun `test that identifier2 system is not set for unknown system type`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/hd/HD-to-Organization-clia"
            ).passed
        )
    }

    @Test
    fun `test that the mapping handles a missing HD3 value`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/hd/HD-to-Organization-empty-hd3"
            ).passed
        )
    }

    @Test
    fun `test that Organization country uses MSH17 when MSH17 present`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/hd/HD-to-Organization-country-msh"
            ).passed
        )
    }

    @Test
    fun `test that Organization country is null when MSH17 absent`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/hd/HD-to-Organization-country-null"
            ).passed
        )
    }
}