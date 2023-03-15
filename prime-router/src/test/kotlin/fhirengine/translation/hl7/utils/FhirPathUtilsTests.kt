package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.all
import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.TimeType
import java.util.Date
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

        // Empty string
        assertThat(FhirPathUtils.parsePath("")).isNull()
    }

    @Test
    fun `test evaluate condition`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        var path = "Bundle.id.exists()"

        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, path)).isTrue()

        path = "Bundle.timestamp.exists()"
        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, path)).isFalse()

        // Bad extension names throw an out of bound exception (a bug in the library)
        path = "Bundle.entry[0].resource.extension('blah')"
        assertThat { FhirPathUtils.evaluateCondition(null, bundle, bundle, path) }.isFailure()

        // Empty string
        assertThat { FhirPathUtils.evaluateCondition(null, bundle, bundle, "") }.isFailure()
    }

    @Test
    fun `test evaluate`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val servRequest = ServiceRequest()
        servRequest.id = "def456"
        val diagReport = DiagnosticReport()
        diagReport.id = "ghi789"
        val entry1 = Bundle.BundleEntryComponent()
        entry1.resource = diagReport
        val entry2 = Bundle.BundleEntryComponent()
        entry2.resource = servRequest
        bundle.addEntry(entry1)
        bundle.addEntry(entry2)

        var path = "Bundle.entry.resource.ofType(DiagnosticReport)[0]"
        val result = FhirPathUtils.evaluate(null, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isInstanceOf(DiagnosticReport::class.java)
        assertThat((result[0] as DiagnosticReport).id).isEqualTo(diagReport.id)

        // Bad extension names throw an out of bound exception (a bug in the library)
        path = "Bundle.extension('blah').value"
        assertThat { FhirPathUtils.evaluate(null, bundle, bundle, path) }.isSuccess()
        assertThat(FhirPathUtils.evaluate(null, bundle, bundle, path)).isEmpty()

        // Empty string
        assertThat(FhirPathUtils.evaluate(null, bundle, bundle, "")).isEmpty()
    }

    @Test
    fun `test evaluateString`() {
        val a = ORU_R01()
        val terser = Terser(a)
        val bundle = Bundle()
        bundle.timestamp = Date()

        val observation = Observation()
        observation.effective = DateTimeType("2015-04-11T12:22:01-04:00")
        bundle.addEntry().resource = observation

        // Test timestamp of java Date
        var path = "Bundle.timestamp"
        var result = FhirPathUtils.evaluateString(null, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat { terser.set("MSH-7", result) }.isSuccess()

        // Test DateTimeType
        path = "Bundle.entry.resource.effective"
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat { terser.set("MSH-7", result) }.isSuccess()

        // Test InstanceType (which boils down to a DateTimeType)
        observation.effective = InstantType("2015-04-11T12:22:01-04:00")
        path = "Bundle.entry.resource.effective"
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat { terser.set("MSH-7", result) }.isSuccess()

        val ext = Extension()
        ext.url = "http://example.com/extensions#someext"
        ext.setValue(DateType("2011-01-02"))
        observation.addExtension(ext)
        // Test DateType
        path = "Bundle.entry.resource.extension.value"
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat { terser.set("MSH-7", result) }.isSuccess()

        // Test TimeType
        ext.setValue(TimeType("13:04:05.098"))
        path = "Bundle.entry.resource.extension.value"
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        // OBX-2 is one of the few HL7 fields that accepts a TM
        assertThat { terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-2", result) }.isSuccess()

        // Test regular string
        bundle.id = "super special id"
        path = "Bundle.id"
        result = FhirPathUtils.evaluateString(null, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat { terser.set("MSH-10", result) }.isSuccess()

        // Empty string
        assertThat(FhirPathUtils.evaluateString(null, bundle, bundle, "")).isEmpty()
    }

    @Test
    fun `test convertDateTimeToHL7`() {
        assertThat(FhirPathUtils.convertDateTimeToHL7(DateTimeType("2015")))
            .isEqualTo("2015")
        assertThat(FhirPathUtils.convertDateTimeToHL7(DateTimeType("2015-04")))
            .isEqualTo("201504")
        assertThat(FhirPathUtils.convertDateTimeToHL7(DateTimeType("2015-04-05")))
            .isEqualTo("20150405")
        // Hour only or hour and minute only is not supported by FHIR type
        assertThat(FhirPathUtils.convertDateTimeToHL7(DateTimeType("2015-04-05T12:22:11")))
            .isEqualTo("20150405122211")
//              TODO: There's no way to turn this off at the moment.
//                 Need to add support to configure Date precision.
//                 Ticket: https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/8694

//        assertThat(FhirPathUtils.convertDateTimeToHL7(DateTimeType("2015-04-05T12:22:11.567")))
//            .isEqualTo("20150405122211.567")
//        assertThat(FhirPathUtils.convertDateTimeToHL7(DateTimeType("2015-04-05T12:22:11.567891")))
//            .isEqualTo("20150405122211.5679") // Note the rounding
        assertThat(FhirPathUtils.convertDateTimeToHL7(DateTimeType("2015-04-11T12:22:01-04:00")))
            .isEqualTo("20150411122201-0400")
    }

    @Test
    fun `test convertTimeToHL7`() {
        val time = TimeType("13:04:05.098")
        assertThat(FhirPathUtils.convertTimeToHL7(time)).isEqualTo("130405.0980")
    }

    @Test
    fun `test convertDateToHL7`() {
        assertThat(FhirPathUtils.convertDateToHL7(DateType("2011-01-02"))).isEqualTo("20110102")
        assertThat(FhirPathUtils.convertDateToHL7(DateType("2011-01"))).isEqualTo("201101")
        assertThat(FhirPathUtils.convertDateToHL7(DateType("2011"))).isEqualTo("2011")
    }

    @Test
    fun `test evaluateCondition exceptions`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        // first verify that good syntax is accepted
        var expression = "Bundle.id.exists()"
        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, expression)).isTrue()

        // verify it throws exception for bad syntax
        expression = "Bundle.#*($&id.exists()"
        assertThat { FhirPathUtils.evaluateCondition(null, bundle, bundle, expression) }.isFailure().all {
            hasClass(SchemaException::class.java)
        }

        // verify it throws exception for non-boolean expression
        expression = "Bundle.id"
        assertThat { FhirPathUtils.evaluateCondition(null, bundle, bundle, expression) }.isFailure().all {
            hasClass(SchemaException::class.java)
        }
    }
}