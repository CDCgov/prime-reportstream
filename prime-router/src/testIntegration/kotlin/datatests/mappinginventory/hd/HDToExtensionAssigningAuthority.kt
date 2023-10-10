package gov.cdc.prime.router.datatests.mappinginventory.hd

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

class HDToExtensionAssigningAuthority {

    @Test
    fun `test correctly handles ISO universal id type`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-iso.hl7",
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-iso.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-iso.fhir",
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-iso.hl7",
            ).passed
        )
    }

    @Test
    fun `test correctly handles UUID universal id type`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-uuid.hl7",
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-uuid.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-uuid.fhir",
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-uuid.hl7",
            ).passed
        )
    }

    @Test
    fun `test correctly handles unknown universal id type`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-clia.hl7",
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-clia.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-clia.fhir",
                "mappinginventory/hd/HD-to-extensionAssigningAuthority-clia.hl7",
            ).passed
        )
    }
}