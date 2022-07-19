package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.MessageHeader
import kotlin.test.Test

class FhirToHl7ConverterTests {
    @Test
    fun `test set HL7 value`() {
        val terser = mockk<Terser>()
        var element = ConfigSchemaElement("name", required = true, hl7Spec = listOf("MSH-10"))
        val converter = FhirToHl7Converter(Bundle(), "dummy", "dummy", terser)

        assertThat { converter.setHl7Value(element, "") }.isFailure()
            .hasClass(RequiredElementException::class.java)

        every { terser.set(element.hl7Spec[0], any()) } throws HL7Exception("some text")
        assertThat { converter.setHl7Value(element, "somevalue") }.isFailure()
            .hasClass(HL7ConversionException::class.java)

        clearAllMocks()

        every { terser.set(element.hl7Spec[0], any()) } throws IllegalArgumentException("some text")
        assertThat { converter.setHl7Value(element, "somevalue") }.isFailure()
            .hasClass(SchemaException::class.java)

        clearAllMocks()

        element = ConfigSchemaElement("name", hl7Spec = listOf("MSH-10", "MSH-11", "MSH-12"))
        justRun { terser.set(any(), any()) }
        converter.setHl7Value(element, "somevalue")
        verifySequence {
            terser.set(element.hl7Spec[0], any())
            terser.set(element.hl7Spec[1], any())
            terser.set(element.hl7Spec[2], any())
        }
    }

    @Test
    fun `test can evaluate`() {
        val terser = mockk<Terser>()
        val bundle = Bundle()
        bundle.id = "abc123"

        val converter = FhirToHl7Converter(bundle, "dummy", "dummy", terser)

        var element = ConfigSchemaElement("name")
        assertThat(converter.canEvaluate(element, bundle)).isTrue()

        var pathExpression = FhirPathUtils.parsePath("Bundle.id.exists()")
        assertThat(pathExpression).isNotNull()
        element = ConfigSchemaElement("name", conditionExpression = pathExpression)
        assertThat(converter.canEvaluate(element, bundle)).isTrue()

        pathExpression = FhirPathUtils.parsePath("Bundle.id = 'someothervalue'")
        assertThat(pathExpression).isNotNull()
        element = ConfigSchemaElement("name", conditionExpression = pathExpression)
        assertThat(converter.canEvaluate(element, bundle)).isFalse()
    }

    @Test
    fun `test get focus resource`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = MessageHeader()
        resource.id = "def456"
        val entry = BundleEntryComponent()
        entry.resource = resource
        bundle.addEntry(entry)

        val converter = FhirToHl7Converter(bundle, "dummy", "dummy")

        var element = ConfigSchemaElement("name")
        var focusResources = converter.getFocusResources(element, bundle)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0]).isEqualTo(bundle)

        var pathExpression = FhirPathUtils.parsePath("Bundle.entry.resource.ofType(MessageHeader)")
        assertThat(pathExpression).isNotNull()
        element = ConfigSchemaElement("name", resourceExpression = pathExpression)
        focusResources = converter.getFocusResources(element, bundle)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0]).isEqualTo(resource)

        pathExpression = FhirPathUtils.parsePath("Bundle.entry.resource.ofType(Patient)")
        assertThat(pathExpression).isNotNull()
        element = ConfigSchemaElement("name", resourceExpression = pathExpression)
        focusResources = converter.getFocusResources(element, bundle)
        assertThat(focusResources).isEmpty()

        pathExpression = FhirPathUtils.parsePath("1")
        assertThat(pathExpression).isNotNull()
        element = ConfigSchemaElement("name", resourceExpression = pathExpression)
        assertThat { converter.getFocusResources(element, bundle) }.isFailure().hasClass(SchemaException::class.java)
    }
}