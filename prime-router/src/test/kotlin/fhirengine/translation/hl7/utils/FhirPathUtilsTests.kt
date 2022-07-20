package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.hl7.fhir.r4.model.Bundle
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
        assertThat(FhirPathUtils.evaluateCondition("", bundle, bundle, path!!)).isTrue()

        path = FhirPathUtils.parsePath("Bundle.timestamp.exists()")
        assertThat(path).isNotNull()
        assertThat(FhirPathUtils.evaluateCondition("", bundle, bundle, path!!)).isFalse()

        path = FhirPathUtils.parsePath("Bundle.id")
        assertThat(path).isNotNull()
        assertThat { FhirPathUtils.evaluateCondition("", bundle, bundle, path!!) }.isFailure()
    }
}