package gov.cdc.prime.router.datatests.mappinginventory.cwe

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CWEtoCodeableConceptTests {

    @Test
    fun `test value in CWE-1`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-3-test-value-cwe1").passed)
    }

    @Test
    fun `test value in CWE-2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-4-test-value-cwe2").passed)
    }

    @Test
    fun `test value in CWE-3`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-5-test-value-cwe3").passed)
    }

    @Test
    fun `test value in CWE-4`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-6-test-value-cwe4").passed)
    }

    @Test
    fun `test value in CWE-5`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-7-test-value-cwe5").passed)
    }

    @Test
    fun `test value in CWE-6`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-8-test-value-cwe6").passed)
    }

    @Test
    fun `test value in CWE-7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-9-test-value-cwe7").passed)
    }

    @Test
    fun `test value in CWE-8`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-10-test-value-cwe8").passed)
    }

    @Test
    fun `test value in CWE-9`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-11-test-value-cwe9").passed)
    }

    @Test
    fun `test value in CWE-10`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-12-test-value-cwe10").passed)
    }

    @Test
    fun `test value in CWE-11`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-13-test-value-cwe11").passed)
    }

    @Test
    fun `test value in CWE-12`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-14-test-value-cwe12").passed)
    }

    @Test
    fun `test value in CWE-13`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-15-test-value-cwe13").passed)
    }

    @Test
    fun `test value in CWE-14`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/codeable-concept/cwe-16-test-value-cwe14").passed)
    }
}