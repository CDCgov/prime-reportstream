package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.MessageHeader
import kotlin.test.Test

class FhirToHl7ConverterTests {
    @Test
    fun `test set HL7 value`() {
        val fieldValue = "somevalue"
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConfigSchema>() // Just a dummy schema to pass around
        var element = ConfigSchemaElement("name", required = true, hl7Spec = listOf("MSH-10"))
        var converter = FhirToHl7Converter(Bundle(), mockSchema, terser = mockTerser)

        // Required element
        assertThat { converter.setHl7Value(element, "") }.isFailure()
            .hasClass(RequiredElementException::class.java)

        // Test the value is set for all specified HL7 fields
        element = ConfigSchemaElement("name", hl7Spec = listOf("MSH-10", "MSH-11", "MSH-12"))
        justRun { mockTerser.set(any(), any()) }
        converter.setHl7Value(element, fieldValue)
        verifySequence {
            mockTerser.set(element.hl7Spec[0], fieldValue)
            mockTerser.set(element.hl7Spec[1], fieldValue)
            mockTerser.set(element.hl7Spec[2], fieldValue)
        }

        // Not strict errors
        every { mockTerser.set(any(), any()) } throws HL7Exception("some text")
        assertThat { converter.setHl7Value(element, fieldValue) }.isSuccess()

        clearAllMocks()

        every { mockTerser.set(any(), any()) } throws IllegalArgumentException("some text")
        assertThat { converter.setHl7Value(element, fieldValue) }.isSuccess()

        clearAllMocks()

        // Strict errors
        converter = FhirToHl7Converter(Bundle(), mockSchema, true, mockTerser)
        every { mockTerser.set(element.hl7Spec[0], any()) } throws HL7Exception("some text")
        assertThat { converter.setHl7Value(element, fieldValue) }.isFailure()
            .hasClass(HL7ConversionException::class.java)

        every { mockTerser.set(element.hl7Spec[0], any()) } throws IllegalArgumentException("some text")
        assertThat { converter.setHl7Value(element, fieldValue) }.isFailure()
            .hasClass(SchemaException::class.java)
    }

    @Test
    fun `test can evaluate`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConfigSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"

        val converter = FhirToHl7Converter(bundle, mockSchema, terser = mockTerser)

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

        pathExpression = FhirPathUtils.parsePath("Bundle.id") // Not a boolean
        assertThat(pathExpression).isNotNull()
        element = ConfigSchemaElement("name", conditionExpression = pathExpression)
        assertThat(converter.canEvaluate(element, bundle)).isFalse()
    }

    @Test
    fun `test get focus resource`() {
        val mockSchema = mockk<ConfigSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = MessageHeader()
        resource.id = "def456"
        resource.destination = listOf(MessageHeader.MessageDestinationComponent())
        resource.destination[0].name = "a destination"
        val entry = BundleEntryComponent()
        entry.resource = resource
        bundle.addEntry(entry)

        val converter = FhirToHl7Converter(bundle, mockSchema)

        var element = ConfigSchemaElement("name")
        var focusResources = converter.getFocusResources(element, bundle)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0]).isEqualTo(bundle)

        var resourceExpression = FhirPathUtils.parsePath("Bundle.entry.resource.ofType(MessageHeader)")
        assertThat(resourceExpression).isNotNull()
        element = ConfigSchemaElement("name", resourceExpression = resourceExpression)
        focusResources = converter.getFocusResources(element, bundle)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0]).isEqualTo(resource)

        resourceExpression = FhirPathUtils.parsePath("Bundle.entry.resource.ofType(Patient)")
        assertThat(resourceExpression).isNotNull()
        element = ConfigSchemaElement("name", resourceExpression = resourceExpression)
        focusResources = converter.getFocusResources(element, bundle)
        assertThat(focusResources).isEmpty()

        resourceExpression = FhirPathUtils.parsePath("1")
        assertThat(resourceExpression).isNotNull()
        element = ConfigSchemaElement("name", resourceExpression = resourceExpression)
        assertThat { converter.getFocusResources(element, bundle) }.isFailure().hasClass(SchemaException::class.java)

        resourceExpression = FhirPathUtils.parsePath("Bundle.entry.resource.ofType(MessageHeader).destination[0].name")
        assertThat(resourceExpression).isNotNull()
        element = ConfigSchemaElement("name", resourceExpression = resourceExpression)
        assertThat { converter.getFocusResources(element, bundle) }.isFailure().hasClass(SchemaException::class.java)

        // Let's test how the FHIR library handles the focus resource
        resourceExpression = FhirPathUtils.parsePath("Bundle.entry.resource.ofType(MessageHeader).destination")
        val valueExpression = FhirPathUtils.parsePath("%resource.name")
        assertThat(resourceExpression).isNotNull()
        assertThat(valueExpression).isNotNull()
        element = ConfigSchemaElement(
            "name", resourceExpression = resourceExpression, valueExpressions = listOf(valueExpression!!)
        )
        focusResources = converter.getFocusResources(element, bundle)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources.size).isEqualTo(1)
        assertThat(focusResources).isEqualTo(resource.destination)
        assertThat(converter.getValue(element, focusResources[0])).isEqualTo(resource.destination[0].name)
    }

    @Test
    fun `test get value`() {
        val mockSchema = mockk<ConfigSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val converter = FhirToHl7Converter(bundle, mockSchema)

        val pathWithValue = FhirPathUtils.parsePath("Bundle.id")
        val pathNoValue = FhirPathUtils.parsePath("Bundle.timestamp")
        assertThat(pathWithValue).isNotNull()
        assertThat(pathNoValue).isNotNull()

        var element = ConfigSchemaElement("name", valueExpressions = listOf(pathWithValue!!, pathNoValue!!))
        assertThat(converter.getValue(element, bundle)).isEqualTo(bundle.id)

        element = ConfigSchemaElement("name", valueExpressions = listOf(pathNoValue, pathWithValue))
        assertThat(converter.getValue(element, bundle)).isEqualTo(bundle.id)

        element = ConfigSchemaElement("name", valueExpressions = listOf(pathNoValue, pathNoValue))
        assertThat(converter.getValue(element, bundle)).isEmpty()
    }

    @Test
    fun `test process element with single focus resource`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConfigSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val converter = FhirToHl7Converter(bundle, mockSchema, terser = mockTerser)
        val pathWithValue = FhirPathUtils.parsePath("Bundle.id")
        val pathNoValue = FhirPathUtils.parsePath("Bundle.timestamp")
        val conditionTrue = FhirPathUtils.parsePath("Bundle.id.exists()")
        val conditionFalse = FhirPathUtils.parsePath("Bundle.timestamp.exists()")
        assertThat(pathWithValue).isNotNull()
        assertThat(pathNoValue).isNotNull()
        assertThat(conditionTrue).isNotNull()
        assertThat(conditionFalse).isNotNull()

        // Condition is false and was not required
        var element = ConfigSchemaElement(
            "name", conditionExpression = conditionFalse,
            valueExpressions = listOf(pathNoValue!!)
        )
        assertThat { converter.processElement(element, bundle) }.isSuccess()

        // Condition is false and was required
        element = ConfigSchemaElement(
            "name", required = true, conditionExpression = conditionFalse,
            valueExpressions = listOf(pathNoValue)
        )
        assertThat { converter.processElement(element, bundle) }.isFailure()
            .hasClass(RequiredElementException::class.java)

        // Illegal states
        element = ConfigSchemaElement(
            "name", conditionExpression = conditionTrue,
            valueExpressions = listOf(pathNoValue)
        )
        assertThat { converter.processElement(element, bundle) }.isFailure()
            .hasClass(java.lang.IllegalStateException::class.java)
        element = ConfigSchemaElement(
            "name", conditionExpression = conditionTrue,
            valueExpressions = listOf(pathWithValue!!)
        )
        assertThat { converter.processElement(element, bundle) }.isFailure()
            .hasClass(java.lang.IllegalStateException::class.java)

        // Process a value
        element = ConfigSchemaElement(
            "name", conditionExpression = conditionTrue,
            valueExpressions = listOf(pathWithValue), hl7Spec = listOf("MSH-10")
        )
        justRun { mockTerser.set(any(), any()) }
        converter.processElement(element, bundle)
        verify(exactly = 1) { mockTerser.set(element.hl7Spec[0], any()) }

        // Process a schema
        element = ConfigSchemaElement(
            "name", conditionExpression = conditionTrue,
            valueExpressions = listOf(pathWithValue), hl7Spec = listOf("MSH-11")
        )
        val schema = ConfigSchema("schema name", elements = listOf(element))
        val elementWithSchema = ConfigSchemaElement("name", schemaRef = schema)
        converter.processElement(elementWithSchema, bundle)
        verify(exactly = 1) { mockTerser.set(element.hl7Spec[0], any()) }
    }

    @Test
    fun `test convert`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        val pathWithValue = FhirPathUtils.parsePath("Bundle.id")
        assertThat(pathWithValue).isNotNull()

        var element = ConfigSchemaElement(
            "name", valueExpressions = listOf(pathWithValue!!), hl7Spec = listOf("MSH-11")
        )
        var schema = ConfigSchema("schema name", hl7Type = "ORU_R01", hl7Version = "2.5.1", elements = listOf(element))
        val message = FhirToHl7Converter(bundle, schema).convert()
        assertThat(message.isEmpty).isFalse()
        assertThat(Terser(message).get(element.hl7Spec[0])).isEqualTo(bundle.id)

        // Hit the sanity checks
        element = ConfigSchemaElement(
            "name", valueExpressions = listOf(pathWithValue), hl7Spec = listOf("MSH-11")
        )
        schema = ConfigSchema("schema name", hl7Type = "ORU_R01", elements = listOf(element))
        assertThat { FhirToHl7Converter(bundle, schema).convert() }.isFailure()
        element = ConfigSchemaElement(
            "name", valueExpressions = listOf(pathWithValue), hl7Spec = listOf("MSH-11")
        )
        schema = ConfigSchema("schema name", hl7Version = "2.5.1", elements = listOf(element))
        assertThat { FhirToHl7Converter(bundle, schema).convert() }.isFailure()

        // Use a file based schema which will fail as we do not have enough data in the bundle
        assertThat {
            FhirToHl7Converter(
                bundle, "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01",
            ).convert()
        }.isFailure()
    }
}