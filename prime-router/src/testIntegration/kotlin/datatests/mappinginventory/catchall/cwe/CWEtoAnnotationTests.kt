package gov.cdc.prime.router.datatests.mappinginventory.cwe

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

class CWEtoAnnotationTests {

    @Test
    fun `test value in CWE-1`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/annotation/cwe-4-test-value-cwe1").passed)
    }

    @Ignore
    @Test
    fun `test value in CWE-2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/annotation/cwe-5-test-value-cwe2").passed)
    }

    @Ignore
    @Test
    fun `test value in CWE-3`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/annotation/cwe-6-test-value-cwe3").passed)
    }

    @Test
    @Ignore
    fun `test value in CWE-4`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/annotation/cwe-7-test-value-cwe4").passed)
    }

    @Test
    @Ignore
    fun `test value in CWE-5`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/annotation/cwe-8-test-value-cwe5").passed)
    }

    @Test
    @Ignore
    fun `test value in CWE-6`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/annotation/cwe-9-test-value-cwe6").passed)
    }

    @Test
    @Ignore
    fun `test value in CWE-7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/annotation/cwe-10-test-value-cwe7").passed)
    }

    @Test
    @Ignore
    fun `test value in CWE-8`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/annotation/cwe-11-test-value-cwe8").passed)
    }

    @Test
    @Ignore
    fun `test value in CWE-9`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/annotation/cwe-12-test-value-cwe9").passed)
    }
}