package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.ServiceRequest
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
        assertThat { converter.setHl7Value(element, "", CustomContext()) }.isFailure()
            .hasClass(RequiredElementException::class.java)

        // Test the value is set for all specified HL7 fields
        element = ConfigSchemaElement("name", hl7Spec = listOf("MSH-10", "MSH-11", "MSH-12"))
        justRun { mockTerser.set(any(), any()) }
        converter.setHl7Value(element, fieldValue, CustomContext())
        verifySequence {
            mockTerser.set(element.hl7Spec[0], fieldValue)
            mockTerser.set(element.hl7Spec[1], fieldValue)
            mockTerser.set(element.hl7Spec[2], fieldValue)
        }

        // Not strict errors
        every { mockTerser.set(any(), any()) } throws HL7Exception("some text")
        assertThat { converter.setHl7Value(element, fieldValue, CustomContext()) }.isSuccess()

        clearAllMocks()

        every { mockTerser.set(any(), any()) } throws IllegalArgumentException("some text")
        assertThat { converter.setHl7Value(element, fieldValue, CustomContext()) }.isSuccess()

        clearAllMocks()

        // Strict errors
        converter = FhirToHl7Converter(Bundle(), mockSchema, true, mockTerser)
        every { mockTerser.set(element.hl7Spec[0], any()) } throws HL7Exception("some text")
        assertThat { converter.setHl7Value(element, fieldValue, CustomContext()) }.isFailure()
            .hasClass(HL7ConversionException::class.java)

        every { mockTerser.set(element.hl7Spec[0], any()) } throws IllegalArgumentException("some text")
        assertThat { converter.setHl7Value(element, fieldValue, CustomContext()) }.isFailure()
            .hasClass(SchemaException::class.java)

        every { mockTerser.set(element.hl7Spec[0], any()) } throws Exception("some text")
        assertThat { converter.setHl7Value(element, fieldValue, CustomContext()) }.isFailure()
            .hasClass(HL7ConversionException::class.java)
    }

    @Test
    fun `test can evaluate`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConfigSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"

        val converter = FhirToHl7Converter(bundle, mockSchema, terser = mockTerser)

        var element = ConfigSchemaElement("name")
        assertThat(converter.canEvaluate(element, bundle, CustomContext())).isTrue()

        element = ConfigSchemaElement("name", condition = "Bundle.id.exists()")
        assertThat(converter.canEvaluate(element, bundle, CustomContext())).isTrue()

        element = ConfigSchemaElement("name", condition = "Bundle.id = 'someothervalue'")
        assertThat(converter.canEvaluate(element, bundle, CustomContext())).isFalse()

        element = ConfigSchemaElement("name", condition = "Bundle.id")
        assertThat(converter.canEvaluate(element, bundle, CustomContext())).isFalse()
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
        bundle.addEntry().resource = resource

        val converter = FhirToHl7Converter(bundle, mockSchema)

        var element = ConfigSchemaElement("name")
        var focusResources = converter.getFocusResources(element, bundle, CustomContext())
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0]).isEqualTo(bundle)

        element = ConfigSchemaElement("name", resource = "Bundle.entry.resource.ofType(MessageHeader)")
        focusResources = converter.getFocusResources(element, bundle, CustomContext())
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0]).isEqualTo(resource)

        element = ConfigSchemaElement("name", resource = "Bundle.entry.resource.ofType(Patient)")
        focusResources = converter.getFocusResources(element, bundle, CustomContext())
        assertThat(focusResources).isEmpty()

        element = ConfigSchemaElement("name", resource = "1")
        assertThat { converter.getFocusResources(element, bundle, CustomContext()) }.isFailure()
            .hasClass(SchemaException::class.java)

        element = ConfigSchemaElement(
            "name", resource = "Bundle.entry.resource.ofType(MessageHeader).destination[0].name"
        )
        assertThat { converter.getFocusResources(element, bundle, CustomContext()) }.isFailure()
            .hasClass(SchemaException::class.java)

        // Let's test how the FHIR library handles the focus resource
        element = ConfigSchemaElement(
            "name", resource = "Bundle.entry.resource.ofType(MessageHeader).destination",
            value = listOf("%resource.name")
        )
        focusResources = converter.getFocusResources(element, bundle, CustomContext())
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources.size).isEqualTo(1)
        assertThat(focusResources).isEqualTo(resource.destination)
        assertThat(converter.getValue(element, focusResources[0], CustomContext()))
            .isEqualTo(resource.destination[0].name)
    }

    @Test
    fun `test get value`() {
        val mockSchema = mockk<ConfigSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val converter = FhirToHl7Converter(bundle, mockSchema)

        var element = ConfigSchemaElement("name", value = listOf("Bundle.id", "Bundle.timestamp"))
        assertThat(converter.getValue(element, bundle, CustomContext())).isEqualTo(bundle.id)

        element = ConfigSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.id"))
        assertThat(converter.getValue(element, bundle, CustomContext())).isEqualTo(bundle.id)

        element = ConfigSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.timestamp"))
        assertThat(converter.getValue(element, bundle, CustomContext())).isEmpty()
    }

    @Test
    fun `test process element with single focus resource`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConfigSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val converter = FhirToHl7Converter(bundle, mockSchema, terser = mockTerser)
        val pathWithValue = "Bundle.id"
        val pathNoValue = "Bundle.timestamp"
        val conditionTrue = "Bundle.id.exists()"
        val conditionFalse = "Bundle.timestamp.exists()"

        // Condition is false and was not required
        var element = ConfigSchemaElement(
            "name", condition = conditionFalse,
            value = listOf(pathNoValue)
        )
        assertThat { converter.processElement(element, bundle, CustomContext()) }.isSuccess()

        // Condition is false and was required
        element = ConfigSchemaElement(
            "name", required = true, condition = conditionFalse,
            value = listOf(pathNoValue)
        )
        assertThat { converter.processElement(element, bundle, CustomContext()) }.isFailure()
            .hasClass(RequiredElementException::class.java)

        // Illegal states
        element = ConfigSchemaElement(
            "name", condition = conditionTrue,
            value = listOf(pathNoValue)
        )
        assertThat { converter.processElement(element, bundle, CustomContext()) }.isFailure()
            .hasClass(java.lang.IllegalStateException::class.java)
        element = ConfigSchemaElement(
            "name", condition = conditionTrue,
            value = listOf(pathWithValue)
        )
        assertThat { converter.processElement(element, bundle, CustomContext()) }.isFailure()
            .hasClass(java.lang.IllegalStateException::class.java)

        // Process a value
        element = ConfigSchemaElement(
            "name", condition = conditionTrue,
            value = listOf(pathWithValue), hl7Spec = listOf("MSH-10")
        )
        justRun { mockTerser.set(any(), any()) }
        converter.processElement(element, bundle, CustomContext())
        verify(exactly = 1) { mockTerser.set(element.hl7Spec[0], any()) }

        // Process a schema
        element = ConfigSchemaElement(
            "name", condition = conditionTrue,
            value = listOf(pathWithValue), hl7Spec = listOf("MSH-11")
        )
        val schema = ConfigSchema("schema name", elements = listOf(element))
        val elementWithSchema = ConfigSchemaElement("name", schemaRef = schema)
        converter.processElement(elementWithSchema, bundle, CustomContext())
        verify(exactly = 1) { mockTerser.set(element.hl7Spec[0], any()) }

        // Test when a resource has no value
        element = ConfigSchemaElement(
            "name", resource = pathNoValue, required = true
        )
        assertThat { converter.processElement(element, bundle, CustomContext()) }.isFailure()
            .hasClass(RequiredElementException::class.java)
    }

    @Test
    fun `test resource index`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConfigSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val servRequest1 = ServiceRequest()
        servRequest1.id = "def456"
        val servRequest2 = ServiceRequest()
        servRequest2.id = "ghi789"
        bundle.addEntry().resource = servRequest1
        bundle.addEntry().resource = servRequest2

        val converter = FhirToHl7Converter(bundle, mockSchema, terser = mockTerser)

        val childElement = ConfigSchemaElement(
            "childElement", value = listOf("1"),
            hl7Spec = listOf("/PATIENT_RESULT/ORDER_OBSERVATION(%{myindexvar})/OBX-1")
        )
        val childSchema = ConfigSchema("childSchema", elements = listOf(childElement))
        val element = ConfigSchemaElement(
            "name", resource = "Bundle.entry", resourceIndex = "myindexvar", schemaRef = childSchema
        )
        justRun { mockTerser.set(any(), any()) }
        converter.processElement(element, bundle, CustomContext())
        verifySequence {
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION(0)/OBX-1", any())
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION(1)/OBX-1", any())
        }
    }

    @Test
    fun `test convert`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        val pathWithValue = "Bundle.id"

        var element = ConfigSchemaElement(
            "name", value = listOf(pathWithValue), hl7Spec = listOf("MSH-11")
        )
        var schema = ConfigSchema("schema name", hl7Type = "ORU_R01", hl7Version = "2.5.1", elements = listOf(element))
        val message = FhirToHl7Converter(bundle, schema).convert()
        assertThat(message.isEmpty).isFalse()
        assertThat(Terser(message).get(element.hl7Spec[0])).isEqualTo(bundle.id)

        // Hit the sanity checks
        element = ConfigSchemaElement(
            "name", value = listOf(pathWithValue), hl7Spec = listOf("MSH-11")
        )
        schema = ConfigSchema("schema name", hl7Type = "ORU_R01", elements = listOf(element))
        assertThat { FhirToHl7Converter(bundle, schema).convert() }.isFailure()
        element = ConfigSchemaElement(
            "name", value = listOf(pathWithValue), hl7Spec = listOf("MSH-11")
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