package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ServiceRequest
import kotlin.test.Test

class FhirPathUtilsTests {
    @Test
    fun `test parse fhir path`() {
        // We can do some level of validation on a FHIR path string without an actual bundle

        // Good ones
        assertThat(FhirPathUtils.parsePath("Bundle.entry.resource.ofType(MessageHeader)")).isNotNull()
        assertThat(FhirPathUtils.parsePath("%resource.contact.relationship.first().coding.exists()")).isNotNull()

        // Bad ones
        assertThat { FhirPathUtils.parsePath("Bundle.entry.resource.BADMETHOD(MessageHeader)") }.isFailure()
        assertThat { FhirPathUtils.parsePath("Bundle...entry.resource.ofType(MessageHeader)") }.isFailure()

        // Null
        assertThat(FhirPathUtils.parsePath(null)).isNull()
    }

    @Test
    fun `test evaluate condition`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        var path = FhirPathUtils.parsePath("Bundle.id.exists()")
        assertThat(path).isNotNull()
        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, path!!)).isTrue()

        path = FhirPathUtils.parsePath("Bundle.timestamp.exists()")
        assertThat(path).isNotNull()
        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, path!!)).isFalse()

        path = FhirPathUtils.parsePath("Bundle.id")
        assertThat(path).isNotNull()
        assertThat { FhirPathUtils.evaluateCondition(null, bundle, bundle, path!!) }.isFailure()
    }

    @Test
    fun `test evaluate`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val servRequest = ServiceRequest()
        servRequest.id = "def456"
        val reference = Reference()
        reference.resource = servRequest
        val diagReport = DiagnosticReport()
        diagReport.id = "ghi789"
        diagReport.basedOn = listOf(reference)
        val entry1 = Bundle.BundleEntryComponent()
        entry1.resource = diagReport
        val entry2 = Bundle.BundleEntryComponent()
        entry2.resource = servRequest
        bundle.addEntry(entry1)
        bundle.addEntry(entry2)

        // First a non reference
        var path = FhirPathUtils.parsePath("Bundle.entry.resource.ofType(DiagnosticReport)[0]")
        assertThat(path).isNotNull()
        var result = FhirPathUtils.evaluate(null, bundle, bundle, path!!)
        assertThat(result).isNotEmpty()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isInstanceOf(DiagnosticReport::class.java)
        assertThat((result[0] as DiagnosticReport).id).isEqualTo(diagReport.id)

        // Now a reference
        path = FhirPathUtils.parsePath("Bundle.entry.resource.ofType(DiagnosticReport)[0].basedOn")
        assertThat(path).isNotNull()
        result = FhirPathUtils.evaluate(null, bundle, bundle, path!!)
        assertThat(result).isNotEmpty()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isNotInstanceOf(Reference::class.java)
        assertThat(result[0]).isInstanceOf(ServiceRequest::class.java)
        assertThat((result[0] as ServiceRequest).id).isEqualTo(servRequest.id)
    }
}