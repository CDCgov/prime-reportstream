package gov.cdc.prime.router.datatests.mappinginventory.msh.hd

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

class `MSH-HDTests` {
    @Test
    fun `MSH HD use type ISO`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/msh/hd/hd_use_type_ISO.hl7",
                "mappinginventory/msh/hd/hd_use_type_ISO.FHIR",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/msh/hd/hd_use_type_ISO.FHIR",
                "mappinginventory/msh/hd/hd_use_type_ISO.hl7",
            ).passed
        )
    }

    @Test
    fun `MSH HD use no type`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/msh/hd/hd_use_no_type.hl7",
                "mappinginventory/msh/hd/hd_use_no_type.FHIR",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/msh/hd/hd_use_no_type.FHIR",
                "mappinginventory/msh/hd/hd_use_no_type.hl7",
            ).passed
        )
    }

    @Test
    fun `MSH HD use type DNS`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/msh/hd/hd_use_type_DNS.hl7",
                "mappinginventory/msh/hd/hd_use_type_DNS.FHIR",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/msh/hd/hd_use_type_DNS.FHIR",
                "mappinginventory/msh/hd/hd_use_type_DNS.hl7",
            ).passed
        )
    }

    @Test
    fun `MSH HD use type TEST`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/msh/hd/hd_use_type_TEST.hl7",
                "mappinginventory/msh/hd/hd_use_type_TEST.FHIR",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/msh/hd/hd_use_type_TEST.FHIR",
                "mappinginventory/msh/hd/hd_use_type_TEST.hl7",
            ).passed
        )
    }

    @Test
    fun `MSH HD use type URI`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/msh/hd/hd_use_type_URI.hl7",
                "mappinginventory/msh/hd/hd_use_type_URI.FHIR",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/msh/hd/hd_use_type_URI.FHIR",
                "mappinginventory/msh/hd/hd_use_type_URI.hl7",
            ).passed
        )
    }

    @Test
    fun `MSH HD use type UUID`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/msh/hd/hd_use_type_UUID.hl7",
                "mappinginventory/msh/hd/hd_use_type_UUID.FHIR",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/msh/hd/hd_use_type_UUID.FHIR",
                "mappinginventory/msh/hd/hd_use_type_UUID.hl7",
            ).passed
        )
    }
}