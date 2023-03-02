package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import io.mockk.mockk
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.StringType
import kotlin.test.Test

class ConfigSchemaProcessorTests {

    @Test
    fun `test can evaluate`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val customContext = CustomContext(bundle, bundle)

        val converter = FhirToHl7Converter(mockSchema, terser = mockTerser)

        var element = ConverterSchemaElement("name")
        assertThat(converter.canEvaluate(element, bundle, bundle, customContext)).isTrue()

        element = ConverterSchemaElement("name", condition = "Bundle.id.exists()")
        assertThat(converter.canEvaluate(element, bundle, bundle, customContext)).isTrue()

        element = ConverterSchemaElement("name", condition = "Bundle.id = 'someothervalue'")
        assertThat(converter.canEvaluate(element, bundle, bundle, customContext)).isFalse()

        element = ConverterSchemaElement("name", condition = "Bundle.id")
        assertThat(converter.canEvaluate(element, bundle, bundle, customContext)).isFalse()
    }

    @Test
    fun `test get focus resource`() {
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = MessageHeader()
        resource.id = "def456"
        resource.destination = listOf(MessageHeader.MessageDestinationComponent())
        resource.destination[0].name = "a destination"
        bundle.addEntry().resource = resource
        val customContext = CustomContext(bundle, bundle)

        val converter = FhirToHl7Converter(mockSchema)

        var element = ConverterSchemaElement("name")
        var focusResources = converter.getFocusResources(element.resource, bundle, bundle, customContext)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0]).isEqualTo(bundle)

        element = ConverterSchemaElement("name", resource = "Bundle.entry.resource.ofType(MessageHeader)")
        focusResources = converter.getFocusResources(element.resource, bundle, bundle, customContext)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0]).isEqualTo(resource)

        element = ConverterSchemaElement("name", resource = "Bundle.entry.resource.ofType(Patient)")
        focusResources = converter.getFocusResources(element.resource, bundle, bundle, customContext)
        assertThat(focusResources).isEmpty()

        element = ConverterSchemaElement("name", resource = "1")
        focusResources = converter.getFocusResources(element.resource, bundle, bundle, customContext)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0].isPrimitive).isTrue()
        assertThat(focusResources[0].primitiveValue()).isEqualTo("1")

        element = ConverterSchemaElement(
            "name",
            resource = "Bundle.entry.resource.ofType(MessageHeader).destination[0].name"
        )
        focusResources = converter.getFocusResources(element.resource, bundle, bundle, customContext)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources[0].isEmpty).isFalse()

        // Let's test how the FHIR library handles the focus resource
        element = ConverterSchemaElement(
            "name",
            resource = "Bundle.entry.resource.ofType(MessageHeader).destination",
            value = listOf("%resource.name")
        )
        focusResources = converter.getFocusResources(element.resource, bundle, bundle, customContext)
        assertThat(focusResources).isNotEmpty()
        assertThat(focusResources.size).isEqualTo(1)
        assertThat(focusResources).isEqualTo(resource.destination)
        assertThat(converter.getValueAsString(element, bundle, focusResources[0], customContext))
            .isEqualTo(resource.destination[0].name)
    }

    @Test
    fun `test get value as string`() {
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(mockSchema)

        var element = ConverterSchemaElement("name", value = listOf("Bundle.id", "Bundle.timestamp"))
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo(bundle.id)

        element = ConverterSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.id"))
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo(bundle.id)

        element = ConverterSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.timestamp"))
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEmpty()
    }

    @Test
    fun `test get value as string from set`() {
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "stagnatious"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(mockSchema)

        val valueSet = sortedMapOf(
            Pair("Stagnatious", "S"), // casing should not matter
            Pair("grompfle", "G")
        )

        var element = ConverterSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo("S")

        bundle.id = "grompfle"
        element = ConverterSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo("G")

        bundle.id = "GRompfle" // verify case insensitivity
        element = ConverterSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo("G")

        bundle.id = "unmapped"
        element = ConverterSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo("unmapped")

        element = ConverterSchemaElement("name", value = listOf("unmapped"), valueSet = valueSet)
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEmpty()
    }

    @Test
    fun `test get value`() {
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(mockSchema)

        var element = ConverterSchemaElement("name", value = listOf("Bundle.id", "Bundle.timestamp"))
        assertThat(converter.getValue(element, bundle, bundle, customContext)).isEqualTo(IdType(bundle.id))

        element = ConverterSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.id"))
        assertThat(converter.getValue(element, bundle, bundle, customContext)).isEqualTo(IdType(bundle.id))

        element = ConverterSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.timestamp"))
        assertThat(converter.getValue(element, bundle, bundle, customContext)).isNull()
    }

    @Test
    fun `test get value from set`() {
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "stagnatious"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(mockSchema)

        val valueSet = sortedMapOf(
            Pair("Stagnatious", "'S'"), // casing should not matter
            Pair("grompfle", "'G'")
        )

        var element = ConverterSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        var value = converter.getValue(element, bundle, bundle, customContext)
        assertThat(value).isNotNull().isInstanceOf(StringType::class.java)
        assertThat(value?.primitiveValue()).isEqualTo("S")

        bundle.id = "grompfle"
        element = ConverterSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        value = converter.getValue(element, bundle, bundle, customContext)
        assertThat(value).isNotNull().isInstanceOf(StringType::class.java)
        assertThat(value?.primitiveValue()).isEqualTo("G")

        bundle.id = "GRompfle" // verify case insensitivity
        element = ConverterSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        value = converter.getValue(element, bundle, bundle, customContext)
        assertThat(value).isNotNull().isInstanceOf(StringType::class.java)
        assertThat(value?.primitiveValue()).isEqualTo("G")

        bundle.id = "unmapped"
        element = ConverterSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        value = converter.getValue(element, bundle, bundle, customContext)
        assertThat(value).isNotNull().isInstanceOf(IdType::class.java)
        assertThat(value?.primitiveValue()).isEqualTo("unmapped")

        element = ConverterSchemaElement("name", value = listOf("unmapped"), valueSet = valueSet)
        assertThat(converter.getValue(element, bundle, bundle, customContext)).isNull()
    }

    @Test
    fun `test get value as string with error`() {
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = MessageHeader()
        resource.id = "def456"
        resource.destination = listOf(MessageHeader.MessageDestinationComponent())
        resource.destination[0].name = "a destination"
        bundle.addEntry().resource = resource
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(mockSchema)

        // Non-primitive values should return an empty string and log an error
        var element = ConverterSchemaElement("name", value = listOf("Bundle.entry"))
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo("")
    }
}