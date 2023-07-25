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
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.metadata.LivdLookup
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifySequence
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.ServiceRequest
import java.io.File
import kotlin.test.Test

class FhirToHl7ConverterTests {
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

        val valueSet = InlineValueSet(
            sortedMapOf(
                Pair("Stagnatious", "S"), // casing should not matter
                Pair("grompfle", "G")
            )
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
        val element = ConverterSchemaElement("name", value = listOf("Bundle.entry"))
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo("")
    }

    @Test
    fun `test set HL7 value`() {
        val fieldValue = "somevalue"
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        var element = ConverterSchemaElement("name", required = true, hl7Spec = listOf("MSH-10"))
        var converter = FhirToHl7Converter(mockSchema, terser = mockTerser)
        val customContext = CustomContext(Bundle(), Bundle())

        // Required element
        assertThat { converter.setHl7Value(element, "", customContext) }.isFailure()
            .hasClass(RequiredElementException::class.java)

        // Test the value is set for all specified HL7 fields
        element = ConverterSchemaElement("name", hl7Spec = listOf("MSH-10", "MSH-11", "MSH-12"))
        justRun { mockTerser.set(any(), any()) }
        converter.setHl7Value(element, fieldValue, customContext)
        verifySequence {
            mockTerser.set(element.hl7Spec[0], fieldValue)
            mockTerser.set(element.hl7Spec[1], fieldValue)
            mockTerser.set(element.hl7Spec[2], fieldValue)
        }

        // Not strict errors
        every { mockTerser.set(any(), any()) } throws HL7Exception("some text")
        assertThat { converter.setHl7Value(element, fieldValue, customContext) }.isSuccess()

        clearAllMocks()

        every { mockTerser.set(any(), any()) } throws IllegalArgumentException("some text")
        assertThat { converter.setHl7Value(element, fieldValue, customContext) }.isSuccess()

        clearAllMocks()

        // Strict errors
        converter = FhirToHl7Converter(mockSchema, true, mockTerser)
        every { mockTerser.set(element.hl7Spec[0], any()) } throws HL7Exception("some text")
        assertThat { converter.setHl7Value(element, fieldValue, customContext) }.isFailure()
            .hasClass(HL7ConversionException::class.java)

        every { mockTerser.set(element.hl7Spec[0], any()) } throws IllegalArgumentException("some text")
        assertThat { converter.setHl7Value(element, fieldValue, customContext) }.isFailure()
            .hasClass(SchemaException::class.java)

        every { mockTerser.set(element.hl7Spec[0], any()) } throws Exception("some text")
        assertThat { converter.setHl7Value(element, fieldValue, customContext) }.isFailure()
            .hasClass(HL7ConversionException::class.java)
    }

    @Test
    fun `test process element with single focus resource`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(mockSchema, terser = mockTerser)
        val pathWithValue = "Bundle.id"
        val pathNoValue = "Bundle.timestamp"
        val conditionTrue = "Bundle.id.exists()"
        val conditionFalse = "Bundle.timestamp.exists()"

        // Condition is false and was not required
        var element = ConverterSchemaElement(
            "name",
            condition = conditionFalse,
            value = listOf(pathNoValue)
        )
        assertThat { converter.processElement(element, bundle, bundle, customContext) }.isSuccess()

        // Condition is false and was required
        element = ConverterSchemaElement(
            "name",
            required = true,
            condition = conditionFalse,
            value = listOf(pathNoValue)
        )
        assertThat { converter.processElement(element, bundle, bundle, customContext) }.isFailure()
            .hasClass(RequiredElementException::class.java)

        // Illegal states
        element = ConverterSchemaElement(
            "name",
            condition = conditionTrue,
            value = listOf(pathNoValue)
        )
        assertThat { converter.processElement(element, bundle, bundle, customContext) }.isFailure()
            .hasClass(java.lang.IllegalStateException::class.java)
        element = ConverterSchemaElement(
            "name",
            condition = conditionTrue,
            value = listOf(pathWithValue)
        )
        assertThat { converter.processElement(element, bundle, bundle, customContext) }.isFailure()
            .hasClass(java.lang.IllegalStateException::class.java)

        // Process a value
        element = ConverterSchemaElement(
            "name",
            condition = conditionTrue,
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-10")
        )
        justRun { mockTerser.set(any(), any()) }
        converter.processElement(element, bundle, bundle, customContext)
        verify(exactly = 1) { mockTerser.set(element.hl7Spec[0], any()) }

        // Process a schema
        element = ConverterSchemaElement(
            "name",
            condition = conditionTrue,
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-11")
        )
        val schema = ConverterSchema(elements = mutableListOf(element))
        val elementWithSchema = ConverterSchemaElement("name", schemaRef = schema)
        converter.processElement(elementWithSchema, bundle, bundle, customContext)
        verify(exactly = 1) { mockTerser.set(element.hl7Spec[0], any()) }

        // Test when a resource has no value
        element = ConverterSchemaElement(
            "name",
            resource = pathNoValue,
            required = true
        )
        assertThat { converter.processElement(element, bundle, bundle, customContext) }.isFailure()
            .hasClass(RequiredElementException::class.java)
    }

    @Test
    fun `test resource index`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val servRequest1 = ServiceRequest()
        servRequest1.id = "def456"
        val servRequest2 = ServiceRequest()
        servRequest2.id = "ghi789"
        bundle.addEntry().resource = servRequest1
        bundle.addEntry().resource = servRequest2

        val converter = FhirToHl7Converter(mockSchema, terser = mockTerser)

        val childElement = ConverterSchemaElement(
            "childElement",
            value = listOf("1"),
            hl7Spec = listOf("/PATIENT_RESULT/ORDER_OBSERVATION(%{myindexvar})/OBX-1")
        )
        val childSchema = ConverterSchema(elements = mutableListOf(childElement))
        val element = ConverterSchemaElement(
            "name",
            resource = "Bundle.entry",
            resourceIndex = "myindexvar",
            schemaRef = childSchema
        )
        justRun { mockTerser.set(any(), any()) }
        converter.processElement(element, bundle, bundle, CustomContext(bundle, bundle))
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

        var element = ConverterSchemaElement(
            "name",
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-11")
        )
        var schema = ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(element)
        )
        val message = FhirToHl7Converter(schema).convert(bundle)
        assertThat(message.isEmpty).isFalse()
        assertThat(Terser(message).get(element.hl7Spec[0])).isEqualTo(bundle.id)

        // Hit the sanity checks
        element = ConverterSchemaElement(
            "name",
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-11")
        )
        schema =
            ConverterSchema(elements = mutableListOf(element))
        assertThat { FhirToHl7Converter(schema).convert(bundle) }.isFailure()

        // Use a file based schema which will fail as we do not have enough data in the bundle
        assertThat {
            FhirToHl7Converter(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01"
            ).convert(bundle)
        }.isFailure()

        // check that duplicate names trigger an exception when attempting to convert
        element = ConverterSchemaElement(
            "iMustBeUnique",
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-11")
        )

        val dupe = ConverterSchemaElement(
            "iMustBeUnique",
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-12")
        )
        schema = ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(element, dupe)
        )

        assertThat { FhirToHl7Converter(schema).convert(bundle) }.isFailure()
            .hasClass(SchemaException::class.java)
    }

    @Test
    fun `test convert with context`() {
        mockkObject(LivdLookup, Metadata)
        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
        val fhirData = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()

        val bundle = FhirTranscoder.decode(fhirData)
        val loincCode = "906-1"
        val pathWithValue = "Bundle.entry.resource.ofType(Observation)[0].livdTableLookup(1)"

        var element = ConverterSchemaElement(
            "name",
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-11")
        )
        var schema = ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(element)
        )

        every { LivdLookup.find(any(), any(), any(), any(), any(), any(), any(), any()) } returns loincCode
        val message = FhirToHl7Converter(schema, context = FhirToHl7Context(CustomFhirPathFunctions())).convert(bundle)
        assertThat(message.isEmpty).isFalse()
        assertThat(Terser(message).get(element.hl7Spec[0])).isEqualTo(loincCode)
        unmockkObject(LivdLookup, Metadata)
    }

    @Test
    fun `test convert with nested schemas`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        // check for dupes in various scenarios:
        // root -> A -> C
        //      -> B
        val elemB = ConverterSchemaElement("elementB", value = listOf("Bundle.id"), hl7Spec = listOf("MSH-11"))
        val elemC = ConverterSchemaElement("elementC", value = listOf("Bundle.id"), hl7Spec = listOf("MSH-11"))

        val childSchema = ConverterSchema(elements = mutableListOf(elemC))
        val elemA = ConverterSchemaElement("elementA", schema = "elementC", schemaRef = childSchema)

        val rootSchema =
            ConverterSchema(
                hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
                elements = mutableListOf(elemA, elemB)
            )

        // nobody sharing the same name
        assertThat(FhirToHl7Converter(rootSchema).convert(bundle).isEmpty).isFalse()

        // B/C sharing the same name
        elemC.name = "elementB"
        assertThat { FhirToHl7Converter(rootSchema).convert(bundle) }.isFailure()
            .hasClass(SchemaException::class.java)

        // A/B sharing the same name
        elemC.name = "elementC"
        elemA.name = "elementB"
        assertThat { FhirToHl7Converter(rootSchema).convert(bundle) }.isFailure()
            .hasClass(SchemaException::class.java)

        // A/C sharing the same name
        elemA.name = "elementC"
        assertThat { FhirToHl7Converter(rootSchema).convert(bundle) }.isFailure()
            .hasClass(SchemaException::class.java)
    }
}