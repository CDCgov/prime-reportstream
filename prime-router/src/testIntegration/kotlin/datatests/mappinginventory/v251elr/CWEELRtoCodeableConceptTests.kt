package gov.cdc.prime.router.datatests.mappinginventory.v251elr

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CWEELRtoCodeableConceptTests {
    @Test
    fun `test value in CWE-1`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-3-test-value-cwe1",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-2`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-4-test-value-cwe2",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-3`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-5-test-value-cwe3",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-4`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-6-test-value-cwe4",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-5`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-7-test-value-cwe5",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-6`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-8-test-value-cwe6",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-7`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-9-test-value-cwe7",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-8`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-10-test-value-cwe8",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-9`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-11-test-value-cwe9",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-10`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-12-test-value-cwe10",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-11`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-13-test-value-cwe11",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-12`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-14-test-value-cwe12",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-13`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-15-test-value-cwe13",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }

    @Test
    fun `test value in CWE-14`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "v251-elr/cwe/codeable-concept/cwe-16-test-value-cwe14",
                profile = "./metadata/HL7/v251-elr",
                outputSchema = "classpath:/metadata/hl7_mapping/v251-elr/ORU_R01.yml",
            ).passed
        )
    }
}