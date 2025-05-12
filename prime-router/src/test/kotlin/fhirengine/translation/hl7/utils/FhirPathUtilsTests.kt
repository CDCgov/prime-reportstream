package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.verify
import org.apache.logging.log4j.kotlin.KotlinLogger
import org.hl7.fhir.exceptions.PathEngineException
import org.hl7.fhir.r4.fhirpath.ExpressionNode
import org.hl7.fhir.r4.fhirpath.FHIRLexer
import org.hl7.fhir.r4.fhirpath.FHIRPathEngine
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TimeType
import org.junit.jupiter.api.BeforeEach
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FhirPathUtilsTests {

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test parse fhir path`() {
        // We can do some level of validation on a FHIR path string without an actual bundle

        // Good ones
        assertThat(FhirPathUtils.parsePath("Bundle.entry.resource.ofType(MessageHeader)")).isNotNull()
        assertThat(FhirPathUtils.parsePath("%resource.contact.relationship.first().coding.exists()")).isNotNull()

        // Bad ones
        assertFailure { FhirPathUtils.parsePath("Bundle.entry.resource.BADMETHOD(MessageHeader)") }
        assertFailure { FhirPathUtils.parsePath("Bundle...entry.resource.ofType(MessageHeader)") }

        // Null
        assertThat(FhirPathUtils.parsePath(null)).isNull()

        // Empty string
        assertThat(FhirPathUtils.parsePath("")).isNull()

        // Invalid fhir path syntax
        assertFailsWith<FHIRLexer.FHIRLexerException> { FhirPathUtils.parsePath("Bundle.#*($&id.exists()") }
    }

    @Test
    fun `test evaluate condition`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        var path = "Bundle.id.exists()"

        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, bundle, path)).isTrue()

        path = "Bundle.timestamp.exists()"
        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, bundle, path)).isFalse()

        path = "Bundle.entry[0].resource.extension('blah')"
        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, bundle, path)).isFalse()

        // Empty string
        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, bundle, "")).isFalse()
    }

    @Test
    fun `test evaluate condition error thrown`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        val path = "Bundle.entry[0].resource.blah('blah')"
        try {
            FhirPathUtils.evaluateCondition(null, bundle, bundle, bundle, path)
        } catch (e: Exception) {
            assertThat(e).isInstanceOf<SchemaException>()
            assertThat(e.cause).isNotNull().isInstanceOf<FHIRLexer.FHIRLexerException>()
        }
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
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isInstanceOf(DiagnosticReport::class.java)
        assertThat((result[0] as DiagnosticReport).id).isEqualTo(diagReport.id)

        // Extension value does not exist
        path = "Bundle.extension('blah').value"
        assertThat(FhirPathUtils.evaluate(null, bundle, bundle, path)).isEmpty()

        // Empty string
        path = ""
        assertThat(FhirPathUtils.evaluate(null, bundle, bundle, path)).isEmpty()

        // Invalid resource, throws uncaught PathEngineException
        path = "Bundle.entry.resource.ofType(Messi)"
        assertFailure { FhirPathUtils.evaluate(null, bundle, bundle, path) }.all {
            hasClass(PathEngineException::class.java)
        }
    }

    @Test
    fun `test evaluate invalid extension exception`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val diagReport = DiagnosticReport()
        diagReport.id = "ghi789"
        val entry1 = Bundle.BundleEntryComponent()
        entry1.resource = diagReport
        bundle.addEntry(entry1)

        val fhirPathUtils = spyk(FhirPathUtils)
        val mockedLogger = mockk<KotlinLogger>()

        every { fhirPathUtils.logger } returns mockedLogger
        every { mockedLogger.error(any<String>()) } returns Unit
        every { mockedLogger.trace(any<String>()) } returns Unit

        // Extension provided with a non-string value, throws IndexOutOfBoundsException
        val path = "Bundle.extension(blah).value"
        assertThat(fhirPathUtils.evaluate(null, bundle, bundle, path)).isEmpty()

        verify {
            mockedLogger.error(
                "java.lang.IndexOutOfBoundsException: " +
                "FHIR path could not find a specified field in Bundle.extension(blah).value."
            )
        }
    }

    @Test
    fun `test evaluate invalid fhirpath syntax exception`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val diagReport = DiagnosticReport()
        diagReport.id = "ghi789"
        val entry1 = Bundle.BundleEntryComponent()
        entry1.resource = diagReport
        bundle.addEntry(entry1)

        val fhirPathUtils = spyk(FhirPathUtils)
        val mockedLogger = mockk<KotlinLogger>()

        every { fhirPathUtils.logger } returns mockedLogger
        every { mockedLogger.error(any<String>()) } returns Unit
        every { mockedLogger.trace(any<String>()) } returns Unit

        // Invalid fhirpath syntax, throws FHIRLexerException
        val path = "Bundle.#*($&id.exists()"
        assertThat(fhirPathUtils.evaluate(null, bundle, bundle, path)).isEmpty()

        verify {
            mockedLogger.error(
                "FHIRLexerException: Error in ?? at 1, 1: Found # expecting a token name. " +
                    "Trying to evaluate: Bundle.#*(\$&id.exists()."
            )
        }
    }

    @Test
    fun `test evaluate logs error when zip code lookup fails`() {
        val path = "%resource.postalCode.getStateFromZipCode()"

        val bundle = Bundle()
        bundle.id = "abc123"
        val address = Address()
        val missingZipCode = "66666"
        address.postalCode = missingZipCode

        val patient = Patient()
        patient.id = "ghi789"
        patient.address.add(address)
        val entry1 = Bundle.BundleEntryComponent()
        entry1.resource = patient
        bundle.addEntry(entry1)

        // pathEngine.evaluate() will return an empty list when zip lookup fails
        val zipLookupResults: MutableList<Base> = mutableListOf(StringType(""))

        // need an ExpressionNode to let the mock resolve the evaluate call
        val expressionNode = ExpressionNode(1)
        // setting the kind allows the ExpressionNode to properly init
        expressionNode.kind = ExpressionNode.Kind.Name
        expressionNode.name = "expname"

        mockkConstructor(FHIRPathEngine::class)
        every { anyConstructed<FHIRPathEngine>().parse(path) } returns expressionNode
        every {
            anyConstructed<FHIRPathEngine>().evaluate(null, address, bundle, bundle, expressionNode)
        } returns zipLookupResults

        val fhirPathUtils = spyk(FhirPathUtils)
        val mockedLogger = mockk<KotlinLogger>()

        every { fhirPathUtils.logger } returns mockedLogger
        every { mockedLogger.error(any<String>()) } returns Unit
        every { mockedLogger.trace(any<String>()) } returns Unit

        assertThat(fhirPathUtils.evaluate(null, address, bundle, path).first().isEmpty)

        verify {
            mockedLogger.error(
                "getStateFromZipCode() lookup failed for zip code: $missingZipCode"
            )
        }
    }

    @Test
    fun `test evaluateString`() {
        val a = ORU_R01()
        val terser = Terser(a)
        val bundle = Bundle()

        val receiver = mockkClass(Receiver::class)
        val appContext = mockkClass(CustomContext::class)
        val config = UnitTestUtils.createConfig(
            useHighPrecisionHeaderDateTimeFormat = true,
            convertPositiveDateTimeOffsetToNegative = false
        )

        every { appContext.constants.contains(any()) }.returns(false)
        every { appContext.customFhirFunctions }.returns(CustomFhirPathFunctions())
        every { appContext.translationFunctions }.returns(CustomTranslationFunctions())
        every { appContext.config }.returns(HL7TranslationConfig(config, receiver))
        every { receiver.dateTimeFormat }.returns(null)
        every { receiver.translation }.returns(config)

        val observation = Observation()
        observation.effective = DateTimeType("2015-04-11T12:22:01-04:00")
        bundle.timestamp = Date()
        bundle.addEntry().resource = observation

        // Test timestamp of java Date
        var path = "Bundle.timestamp"
        var result = FhirPathUtils.evaluateString(appContext, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat(terser.set("MSH-7", result))

        // Test DateTimeType
        path = "Bundle.entry.resource.effective"
        result = FhirPathUtils.evaluateString(appContext, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat(terser.set("MSH-7", result))

        // Test InstanceType (which boils down to a DateTimeType)
        observation.effective = InstantType("2015-04-11T12:22:01-04:00")
        path = "Bundle.entry.resource.effective"
        result = FhirPathUtils.evaluateString(appContext, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat(terser.set("MSH-7", result))

        val ext = Extension()
        ext.url = "http://example.com/extensions#someext"
        ext.setValue(DateType("2011-01-02"))
        observation.addExtension(ext)
        // Test DateType
        path = "Bundle.entry.resource.extension.value"
        result = FhirPathUtils.evaluateString(appContext, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat(terser.set("MSH-7", result))

        // Test TimeType
        ext.setValue(TimeType("13:04:05.098"))
        path = "Bundle.entry.resource.extension.value"
        result = FhirPathUtils.evaluateString(appContext, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        // OBX-2 is one of the few HL7 fields that accepts a TM
        assertThat(terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-2", result))

        // Test regular string
        bundle.id = "super special id"
        path = "Bundle.id"
        result = FhirPathUtils.evaluateString(appContext, bundle, bundle, path)
        assertThat(result).isNotEmpty()
        assertThat(terser.set("MSH-10", result))

        // Empty string
        assertThat(FhirPathUtils.evaluateString(appContext, bundle, bundle, "")).isEmpty()
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
        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, bundle, expression)).isTrue()

        // verify it throws exception for bad syntax
        expression = "Bundle.#*($&id.exists()"
        assertFailure { FhirPathUtils.evaluateCondition(null, bundle, bundle, bundle, expression) }.all {
            hasClass(SchemaException::class.java)
        }

        // verify it throws exception for non-boolean expression
        expression = "Bundle.id"
        assertFailure { FhirPathUtils.evaluateCondition(null, bundle, bundle, bundle, expression) }.all {
            hasClass(SchemaException::class.java)
        }
    }

    @Test
    fun `test evaluateCondition with empty focus resource`() {
        val bundle = Bundle()
        val path = "Bundle.timestamp.is(dateTime)"
        assertThat(FhirPathUtils.evaluateCondition(null, bundle, bundle, bundle, path)).isFalse()
    }
}