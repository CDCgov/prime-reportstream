package gov.cdc.prime.router.datatests.mappinginventory.hd

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

/*
The mapping of HD to Location is typically used within an existing HL7 Data type -> FHIR resource
These tests use MSH.22.6 to test the mapping.
 */
class HDToLocationTests {

    @Test
    fun `test correctly handles ISO universal id type`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-Location-iso.hl7",
                "mappinginventory/hd/HD-to-Location-iso.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-Location-iso.fhir",
                "mappinginventory/hd/HD-to-Location-iso.hl7",
            ).passed
        )
    }

    @Test
    fun `test correctly handles UUID universal id type`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-Location-uuid.hl7",
                "mappinginventory/hd/HD-to-Location-uuid.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-Location-uuid.fhir",
                "mappinginventory/hd/HD-to-Location-uuid.hl7",
            ).passed
        )
    }

    @Test
    fun `test correctly handles and unknown universal id type`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-Location-dns.hl7",
                "mappinginventory/hd/HD-to-Location-dns.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-Location-dns.fhir",
                "mappinginventory/hd/HD-to-Location-dns.hl7",
            ).passed
        )
    }

    @Test
    fun `test correctly handles HD3 being empty`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-Location-empty-hd3.hl7",
                "mappinginventory/hd/HD-to-Location-empty-hd3.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-Location-empty-hd3.fhir",
                "mappinginventory/hd/HD-to-Location-empty-hd3.hl7",
            ).passed
        )
    }
}