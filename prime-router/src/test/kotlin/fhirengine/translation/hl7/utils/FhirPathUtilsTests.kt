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
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.TimeType
import java.util.Date
import kotlin.test.Test

class FhirPathUtilsTests {
    private var hl7DateTimePattern = "^((?:19|20)\\d{2})(?:(1[0-2]|0[1-9])" +
        "(?:(3[0-1]|[1-2]\\d|0[1-9])(?:([0-1]\\d|2[0-3])" +
        "(?:([0-5]\\d)(?:([0-5]\\d(?:\\.\\d{1,4})?)?)?)?)?)?)?([+-](?:[0-1]\\d|2[0-3])[0-5]\\d)?"
    private var regex = hl7DateTimePattern.toRegex()

    private var hl7TimePattern = "^(?:([0-1]\\d|2[0-3])(?:([0-5]\\d)(?:([0-5]\\d(?:\\.\\d{1,4})?)?)?)?)?"
    private var regexTime = hl7TimePattern.toRegex()
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

    @Test
    fun `test evaluateString`() {
        val bundle = Bundle()
        bundle.timestamp = Date()

        val observation = Observation()
        observation.effective = DateTimeType("2015-04-11T12:22:01-04:00")
        bundle.addEntry().resource = observation

        // Test timestamp of java Date
        var path = FhirPathUtils.parsePath("Bundle.timestamp")
        var result = FhirPathUtils.evaluateString(null, bundle, bundle, path!!)
        assertThat(result).isNotEmpty()
        assertThat(result).isInstanceOf(String::class.java)
        assertThat(regex.matches(result)).isTrue()

        // Test DateTimeType
        path = FhirPathUtils.parsePath("Bundle.entry.resource.effective")
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path!!)

        assertThat(result).isNotEmpty()
        assertThat(result).isInstanceOf(String::class.java)
        assertThat(regex.matches(result)).isTrue()

        // Test InstanceType (which boils down to a DateTimeType)
        observation.effective = InstantType("2015-04-11T12:22:01-04:00")
        path = FhirPathUtils.parsePath("Bundle.entry.resource.effective")
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path!!)

        assertThat(result).isNotEmpty()
        assertThat(result).isInstanceOf(String::class.java)
        assertThat(regex.matches(result)).isTrue()

        val ext = Extension()
        ext.url = "http://example.com/extensions#someext"
        ext.setValue(DateType("2011-01-02"))
        observation.addExtension(ext)
        // Test DateType
        path = FhirPathUtils.parsePath("Bundle.entry.resource.extension[0].value")
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path!!)
        assertThat(result).isNotEmpty()
        assertThat(result).isInstanceOf(String::class.java)
        assertThat(regex.matches(result)).isTrue()

        // Test TimeType
        ext.setValue(TimeType("13:04:05.098"))
        path = FhirPathUtils.parsePath("Bundle.entry.resource.extension[0].value")
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path!!)
        assertThat(result).isNotEmpty()
        assertThat(result).isInstanceOf(String::class.java)
        assertThat(regexTime.matches(result)).isTrue()

        // Test regular string
        bundle.id = "super special id"
        path = FhirPathUtils.parsePath("Bundle.id")
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path!!)
        assertThat(result).isNotEmpty()
        assertThat(result).isInstanceOf(String::class.java)
        assertThat(result).isEqualTo(bundle.id)
    }

    @Test
    fun `test convertDateTimeToHL7`() {
        val dateTime = DateTimeType("2015-04-11T12:22:01-04:00")
        val result = FhirPathUtils.convertDateTimeToHL7(dateTime)
        assertThat(result).isNotEmpty()
        assertThat(result).isInstanceOf(String::class.java)
        assertThat(regex.matches(result)).isTrue()
    }

    @Test
    fun `test convertTimeToHL7`() {
        val time = TimeType("13:04:05.098")
        val result = FhirPathUtils.convertTimeToHL7(time)
        assertThat(result).isNotEmpty()
        assertThat(result).isInstanceOf(String::class.java)
        assertThat(regexTime.matches(result)).isTrue()
    }

    @Test
    fun `test convertDateToHL7`() {
        val date = DateType("2011-01-02")
        val result = FhirPathUtils.convertDateToHL7(date)
        assertThat(result).isNotEmpty()
        assertThat(result).isInstanceOf(String::class.java)
        assertThat(regex.matches(result)).isTrue()
    }
}