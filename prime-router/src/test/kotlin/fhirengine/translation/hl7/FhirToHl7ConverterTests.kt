package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.v251.message.OML_O21
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.HL7ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.helpers.SchemaReferenceResolverHelper
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
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.ServiceRequest
import org.junit.jupiter.api.Nested
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FhirToHl7ConverterTests {
    @Test
    fun `test can evaluate`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<HL7ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val customContext = CustomContext(bundle, bundle)

        val converter = FhirToHl7Converter(
            mockSchema,
            terser = mockTerser,
            warnings = mutableListOf(),
            errors = mutableListOf()
        )

        var element = ConverterSchemaElement("name")
        assertThat(converter.canEvaluate(element, bundle, bundle, bundle, customContext)).isTrue()

        element = ConverterSchemaElement("name", condition = "Bundle.id.exists()")
        assertThat(converter.canEvaluate(element, bundle, bundle, bundle, customContext)).isTrue()

        element = ConverterSchemaElement("name", condition = "Bundle.id = 'someothervalue'")
        assertThat(converter.canEvaluate(element, bundle, bundle, bundle, customContext)).isFalse()

        element = ConverterSchemaElement("name", condition = "Bundle.id")
        assertThat(converter.canEvaluate(element, bundle, bundle, bundle, customContext)).isFalse()
    }

    @Test
    fun `test get focus resource`() {
        val mockSchema = mockk<HL7ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = MessageHeader()
        resource.id = "def456"
        resource.destination = listOf(MessageHeader.MessageDestinationComponent())
        resource.destination[0].name = "a destination"
        bundle.addEntry().resource = resource
        val customContext = CustomContext(bundle, bundle)

        val converter = FhirToHl7Converter(mockSchema, warnings = mutableListOf(), errors = mutableListOf())

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
        val mockSchema = mockk<HL7ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(mockSchema, warnings = mutableListOf(), errors = mutableListOf())

        var element = ConverterSchemaElement("name", value = listOf("Bundle.id", "Bundle.timestamp"))
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo(bundle.id)

        element = ConverterSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.id"))
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo(bundle.id)

        element = ConverterSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.timestamp"))
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEmpty()
    }

    @Test
    fun `test get value as string from set`() {
        val mockSchema = mockk<HL7ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "stagnatious"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(mockSchema, warnings = mutableListOf(), errors = mutableListOf())

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
        val mockSchema = mockk<HL7ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = MessageHeader()
        resource.id = "def456"
        resource.destination = listOf(MessageHeader.MessageDestinationComponent())
        resource.destination[0].name = "a destination"
        bundle.addEntry().resource = resource
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(mockSchema, warnings = mutableListOf(), errors = mutableListOf())

        // Non-primitive values should return an empty string and log an error
        val element = ConverterSchemaElement("name", value = listOf("Bundle.entry"))
        assertThat(converter.getValueAsString(element, bundle, bundle, customContext)).isEqualTo("")
    }

    @Test
    fun `test set HL7 value`() {
        val fieldValue = "somevalue"
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<HL7ConverterSchema>() // Just a dummy schema to pass around
        var element = ConverterSchemaElement("name", required = true, hl7Spec = listOf("MSH-10"))
        var converter =
            FhirToHl7Converter(mockSchema, terser = mockTerser, warnings = mutableListOf(), errors = mutableListOf())
        val customContext = CustomContext(Bundle(), Bundle())

        // Required element
        assertFailure { converter.setHl7Value(element, "", customContext) }
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
        assertThat(converter.setHl7Value(element, fieldValue, customContext))

        clearAllMocks()

        every { mockTerser.set(any(), any()) } throws IllegalArgumentException("some text")
        assertThat(converter.setHl7Value(element, fieldValue, customContext))

        clearAllMocks()

        // Strict errors
        converter = FhirToHl7Converter(
            mockSchema,
            true,
            mockTerser,
            warnings = mutableListOf(),
            errors = mutableListOf()
        )
        every { mockTerser.set(element.hl7Spec[0], any()) } throws HL7Exception("some text")
        assertFailure { converter.setHl7Value(element, fieldValue, customContext) }
            .hasClass(HL7ConversionException::class.java)

        every { mockTerser.set(element.hl7Spec[0], any()) } throws IllegalArgumentException("some text")
        assertFailure { converter.setHl7Value(element, fieldValue, customContext) }
            .hasClass(SchemaException::class.java)

        every { mockTerser.set(element.hl7Spec[0], any()) } throws Exception("some text")
        assertFailure { converter.setHl7Value(element, fieldValue, customContext) }
            .hasClass(HL7ConversionException::class.java)
    }

    @Test
    fun `test process element with single focus resource`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<HL7ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirToHl7Converter(
            mockSchema,
            terser = mockTerser,
            warnings = mutableListOf(),
            errors = mutableListOf()
        )
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
        assertThat(converter.processElement(element, bundle, bundle, customContext))

        // Condition is false and was required
        element = ConverterSchemaElement(
            "name",
            required = true,
            condition = conditionFalse,
            value = listOf(pathNoValue)
        )
        assertFailure { converter.processElement(element, bundle, bundle, customContext) }
            .hasClass(RequiredElementException::class.java)

        // Illegal states
        element = ConverterSchemaElement(
            "name",
            condition = conditionTrue,
            value = listOf(pathNoValue)
        )
        assertFailure { converter.processElement(element, bundle, bundle, customContext) }
            .hasClass(java.lang.IllegalStateException::class.java)
        element = ConverterSchemaElement(
            "name",
            condition = conditionTrue,
            value = listOf(pathWithValue)
        )
        assertFailure { converter.processElement(element, bundle, bundle, customContext) }
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
        val schema = HL7ConverterSchema(elements = mutableListOf(element))
        val elementWithSchema = ConverterSchemaElement("name", schemaRef = schema)
        converter.processElement(elementWithSchema, bundle, bundle, customContext)
        verify(exactly = 1) { mockTerser.set(element.hl7Spec[0], any()) }

        // Test when a resource has no value
        element = ConverterSchemaElement(
            "name",
            resource = pathNoValue,
            required = true
        )
        assertFailure { converter.processElement(element, bundle, bundle, customContext) }
            .hasClass(RequiredElementException::class.java)
    }

    @Test
    fun `test resource index`() {
        val mockTerser = mockk<Terser>()
        val mockSchema = mockk<HL7ConverterSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val servRequest1 = ServiceRequest()
        servRequest1.id = "def456"
        val servRequest2 = ServiceRequest()
        servRequest2.id = "ghi789"
        bundle.addEntry().resource = servRequest1
        bundle.addEntry().resource = servRequest2

        val converter =
            FhirToHl7Converter(mockSchema, terser = mockTerser, warnings = mutableListOf(), errors = mutableListOf())

        val childElement = ConverterSchemaElement(
            "childElement",
            value = listOf("1"),
            hl7Spec = listOf("/PATIENT_RESULT/ORDER_OBSERVATION(%{myindexvar})/OBX-1")
        )
        val childSchema = HL7ConverterSchema(elements = mutableListOf(childElement))
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
    fun `test context constant`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val messageHeader = MessageHeader()
        val source = MessageHeader.MessageSourceComponent()
        source.name = "Epic"
        messageHeader.source = source
        val servRequest1 = ServiceRequest()
        servRequest1.id = "def456"
        bundle.addEntry().resource = servRequest1
        bundle.addEntry().resource = messageHeader

        val element = ConverterSchemaElement(
            "name",
            resource = "Bundle.entry.resource.ofType(MessageHeader).source.name",
            condition = "%context.entry.resource.ofType(ServiceRequest).id='def456'",
            value = listOf("%resource"),
            hl7Spec = listOf("MSH-3-1")
        )
        val schema = HL7ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v27.message.ORU_R01",
            elements = listOf(element).toMutableList()
        )
        val converter = FhirToHl7Converter(schema, warnings = mutableListOf(), errors = mutableListOf())
        val message = converter.process(bundle)
        assertThat(Terser(message).get("MSH-3-1")).isEqualTo("Epic")
    }

    @Test
    fun `test convert`() {
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

        val bundle = Bundle()
        bundle.id = "abc123"

        val pathWithValue = "Bundle.id"

        var element = ConverterSchemaElement(
            "name",
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-11")
        )
        var schema = HL7ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(element)
        )
        val message = FhirToHl7Converter(schema, warnings = mutableListOf(), errors = mutableListOf()).process(bundle)
        assertThat(message.isEmpty).isFalse()
        assertThat(Terser(message).get(element.hl7Spec[0])).isEqualTo(bundle.id)

        // Hit the sanity checks
        element = ConverterSchemaElement(
            "name",
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-11")
        )
        schema =
            HL7ConverterSchema(elements = mutableListOf(element))
        assertFailure {
            FhirToHl7Converter(
            schema,
            warnings = mutableListOf(),
            errors = mutableListOf()
        ).process(bundle)
        }

        // Use a file based schema which will fail as we do not have enough data in the bundle
        val transformer = FhirToHl7Converter(
            SchemaReferenceResolverHelper.retrieveHl7SchemaReference(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-01/ORU_R01.yml",
                mockk<BlobAccess.BlobContainerMetadata>()
            ),
            warnings = mutableListOf(),
            errors = mutableListOf()
        )

        transformer.process(bundle)

        assertThat(transformer.errors[0]).isEqualTo(
            "Error encountered while applying: message-headers in" +
                " /fhirengine/translation/hl7/schema/schema-read-test-01/ORU_R01.yml to FHIR bundle. \n" +
                "Error was: Required element message-headers conditional was false or value was empty."
        )
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
        var schema = HL7ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(element)
        )

        every { LivdLookup.find(any(), any(), any(), any(), any(), any(), any(), any()) } returns loincCode
        val message = FhirToHl7Converter(
            schema,
            context = FhirToHl7Context(
                CustomFhirPathFunctions(),
                null,
                CustomTranslationFunctions()
            ),
                warnings = mutableListOf(), errors = mutableListOf()
        ).process(bundle)
        assertThat(message.isEmpty).isFalse()
        assertThat(Terser(message).get(element.hl7Spec[0])).isEqualTo(loincCode)
        unmockkObject(LivdLookup, Metadata)
    }

    @Test
    fun `test convert with context timestamp`() {
        mockkObject(LivdLookup, Metadata)
        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
        val fhirData = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()

        val bundle = FhirTranscoder.decode(fhirData)
        val expectedDate = "20220803060705-0400"
        val pathWithValue = "Bundle.timestamp"

        var element = ConverterSchemaElement(
            "name",
            value = listOf(pathWithValue),
            hl7Spec = listOf("MSH-7")
        )
        var schema = HL7ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(element)
        )

        val message = FhirToHl7Converter(
            schema,
            context = FhirToHl7Context(CustomFhirPathFunctions(), null, CustomTranslationFunctions()),
            warnings = mutableListOf(), errors = mutableListOf()
        ).process(bundle)
        assertThat(message.isEmpty).isFalse()
        assertThat(Terser(message).get(element.hl7Spec[0])).isEqualTo(expectedDate)
    }

    @Test
    fun `test convert with nested schemas`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        val elemB = ConverterSchemaElement("elementB", value = listOf("'654321'"), hl7Spec = listOf("MSH-11"))
        val elemC = ConverterSchemaElement("elementC", value = listOf("'fedcba'"), hl7Spec = listOf("MSH-12"))

        val childSchema = HL7ConverterSchema(elements = mutableListOf(elemB, elemC))
        val elemA = ConverterSchemaElement("elementA", schema = "schema", schemaRef = childSchema)

        val rootSchema = HL7ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(elemA)
        )

        val message = FhirToHl7Converter(
            rootSchema,
            warnings = mutableListOf(),
            errors = mutableListOf()
        ).process(bundle)
        assertThat(Terser(message).get("MSH-11")).isEqualTo("654321")
        assertThat(Terser(message).get("MSH-12")).isEqualTo("fedcba")
    }

    @Test
    fun `test convert with nested schemas and override duplicate elements`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        // root: A -> child: B
        //       B           B
        val elemB1 = ConverterSchemaElement("elementB", value = listOf("'654321'"), hl7Spec = listOf("MSH-11"))
        val elemB2 = ConverterSchemaElement("elementB", value = listOf("'fedcba'"), hl7Spec = listOf("MSH-12"))

        val childSchema = HL7ConverterSchema(elements = mutableListOf(elemB1, elemB2))
        val elemA = ConverterSchemaElement("elementA", schema = "schema", schemaRef = childSchema)

        val rootSchema = HL7ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(elemA)
        )

        val elemBOverride = ConverterSchemaElement("elementB", value = listOf("'overrideVal'"))
        val overrideSchema = HL7ConverterSchema(elements = mutableListOf(elemBOverride))
        rootSchema.override(overrideSchema)

        val message = FhirToHl7Converter(
            rootSchema,
            warnings = mutableListOf(),
            errors = mutableListOf()
        ).process(bundle)
        assertThat(Terser(message).get("MSH-11")).isEqualTo("overrideVal")
        assertThat(Terser(message).get("MSH-12")).isEqualTo("overrideVal")
    }

    @Test
    fun `test truncation logic for ORU_R01`() {
        val mockBundle = mockk<Bundle>()
        val mockSchema = mockk<HL7ConverterSchema>()
        val terser = Terser(ORU_R01())

        // dummy config with just truncation config set up
        val hl7Config = UnitTestUtils.createConfig(
            truncateHl7Fields = "PID-5-1",
            truncateHDNamespaceIds = true
        )

        val config = HL7TranslationConfig(
            hl7Config,
            receiver = null
        )

        val contextWithConfig = FhirToHl7Context(
            CustomFhirPathFunctions(),
            config,
            CustomTranslationFunctions()
        )

        val customContext = CustomContext(
            mockBundle,
            mockBundle,
            config = config,
            translationFunctions = CustomTranslationFunctions()
        )

        val converter = FhirToHl7Converter(
            mockSchema,
            terser = terser,
            context = contextWithConfig, warnings = mutableListOf(), errors = mutableListOf()
        )

        // should truncate to 194
        val value = "x".repeat(500)

        val element = ConverterSchemaElement(
            "name",
            required = true,
            hl7Spec = listOf("/PATIENT_RESULT/PATIENT/PID-5-1")
        )
        converter.setHl7Value(element, value, customContext)

        val shouldBeTruncated = terser.get("/PATIENT_RESULT/PATIENT/PID-5-1")
        assertEquals(shouldBeTruncated.length, 194)
    }

    @Test
    fun `test truncation logic for OML_O21`() {
        val mockBundle = mockk<Bundle>()
        val mockSchema = mockk<HL7ConverterSchema>()
        val terser = Terser(OML_O21())

        // dummy config with just truncation config set up
        val hl7Config = UnitTestUtils.createConfig(
            truncateHl7Fields = "OBX-18-1",
            truncateHDNamespaceIds = true
        )

        val config = HL7TranslationConfig(
            hl7Config,
            receiver = null
        )

        val contextWithConfig = FhirToHl7Context(
            CustomFhirPathFunctions(),
            config,
            CustomTranslationFunctions()
        )

        val customContext = CustomContext(
            mockBundle,
            mockBundle,
            config = config,
            translationFunctions = CustomTranslationFunctions()
        )

        val converter = FhirToHl7Converter(
            mockSchema,
            terser = terser,
            context = contextWithConfig, warnings = mutableListOf(), errors = mutableListOf()
        )

        // should truncate to 199
        val value = "x".repeat(500)

        val element = ConverterSchemaElement(
            "name",
            required = true,
            hl7Spec = listOf("/ORDER/OBSERVATION_REQUEST/OBSERVATION/OBX-18-1")
        )
        converter.setHl7Value(element, value, customContext)

        val shouldBeTruncated = terser.get("/ORDER/OBSERVATION_REQUEST/OBSERVATION/OBX-18-1")
        assertEquals(shouldBeTruncated.length, 199)
    }

    @Nested
    inner class TestOverrides {
        private val mockBlobContainerMetadata = mockk<BlobAccess.BlobContainerMetadata>()

        val baseSchema = ConfigSchemaReader.fromFile(
            """
                    classpath:/fhirengine/translation/hl7/schema/schema-test-overrides/ORU_R01.yml
            """.trimIndent(),
            schemaClass = HL7ConverterSchema::class.java,
            blobConnectionInfo = mockBlobContainerMetadata
        )

        val extendedSchema = ConfigSchemaReader.fromFile(
            """
                    classpath:/fhirengine/translation/hl7/schema/schema-test-overrides/ORU_R01_extended.yml
            """.trimIndent(),
            schemaClass = HL7ConverterSchema::class.java,
            blobConnectionInfo = mockBlobContainerMetadata
        )

        val extendedSchemaOverridesSoftware = ConfigSchemaReader.fromFile(
            """
                    classpath:/fhirengine/translation/hl7/schema/schema-test-overrides/ORU_R01_extended_overrides_software.yml
            """.trimIndent(),
            schemaClass = HL7ConverterSchema::class.java,
            blobConnectionInfo = mockBlobContainerMetadata
        )

        val extendedSchemaOverridesXon = ConfigSchemaReader.fromFile(
            "classpath:/fhirengine/translation/hl7/schema/schema-test-overrides/ORU_R01_extended_overrides_xon.yml",
            schemaClass = HL7ConverterSchema::class.java,
            blobConnectionInfo = mockBlobContainerMetadata
        )

        val extendedExtendedSchema = ConfigSchemaReader.fromFile(
            "classpath:/fhirengine/translation/hl7/schema/schema-test-overrides/ORU_R01_extended_extended.yml",
            schemaClass = HL7ConverterSchema::class.java,
            blobConnectionInfo = mockBlobContainerMetadata
        )

        @Test
        fun `test overrides an existing element`() {
            val bundle = Bundle()
            bundle.id = "abc123"

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("MSH-11")).isEqualTo("abc123")
            // Confirms that we can override an element that exists in the base
            assertThat(Terser(message).get("MSH-11")).isEqualTo("override")
        }

        @Test
        fun `test override uses a constant`() {
            val bundle = Bundle()

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("MSH-10")).isEqualTo("10")
            // Assert that we can create an override element that uses a constant from the base schema
            assertThat(Terser(message).get("MSH-10")).isEqualTo("baseValue")
        }

        @Test
        fun `test override overrides a constant`() {
            val bundle = Bundle()

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("MSH-8")).isEqualTo("otherValue")
            // Assert that a constant can get overridden
            assertThat(Terser(message).get("MSH-8")).isEqualTo("overriddenOtherConstant")
        }

        @Test
        fun `test the overriding schema takes priority when setting the same HL7 field`() {
            val bundle = Bundle()

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("MSH-14")).isEqualTo("14")
            // A new element in the overriding schema sets MSH-14 as well
            // Assert that the element from extended schema is executed last and that value is set
            assertThat(Terser(message).get("MSH-14")).isEqualTo("not14")
        }

        @Test
        fun `test overriding an element in a nested schema at the root`() {
            val bundle = Bundle()
            val messageHeader = MessageHeader()
            messageHeader.definition = "definition"
            bundle.addEntry().resource = messageHeader

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("SFT-2")).isEqualTo("1")
            // Assert that an element can be overridden in a nested schema
            // from the base of the overriding schema
            assertThat(Terser(message).get("SFT-2")).isEqualTo("not1")
        }

        @Test
        fun `test overriding an element and constant in a nested schema`() {
            val bundle = Bundle()
            val messageHeader = MessageHeader()
            messageHeader.definition = "definition"
            messageHeader.id = "idSft"
            bundle.addEntry().resource = messageHeader

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("SFT-3")).isEqualTo("definition")

            // Asserts that this override does not work since the constant that is trying
            // to be overridden is defined at the top of used nested schema
            assertThat(Terser(message).get("SFT-3")).isEqualTo("definition")
            assertThat(Terser(message).get("SFT-4")).isNotEqualTo("definition")
        }

        @Test
        fun `test overriding element can use constant defined in nested schema`() {
            val bundle = Bundle()
            val messageHeader = MessageHeader()
            messageHeader.definition = "definition"
            messageHeader.id = "idSft"
            messageHeader.event = Coding("system", "noEvent", "displayCode")
            bundle.addEntry().resource = messageHeader

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("SFT-5")).isEqualTo("idSft")
            // Asserts that the base extending schema can provide for a nested schema element
            // with a constant in the nested schema
            assertThat(Terser(message).get("SFT-5")).isEqualTo("sftValue")
        }

        @Test
        fun `test overriding deeply nested schema`() {
            val bundle = Bundle()
            val messageHeader = MessageHeader()
            messageHeader.definition = "definition"
            messageHeader.id = "idSft"
            messageHeader.event = Coding("system", "code", "displayCode")
            bundle.addEntry().resource = messageHeader

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("SFT-1-1")).isEqualTo("system")
            // Assert that a deeply nested schema element can be overridden, using a
            // constant that is provided in the parent schema
            assertThat(Terser(message).get("SFT-1-2")).isEqualTo("system")
        }

        @Test
        fun `test overrides add new field in nested schema`() {
            val bundle = Bundle()
            val messageHeader = MessageHeader()
            messageHeader.definition = "definition"
            messageHeader.id = "idSft"
            messageHeader.event = Coding("system", "xon3", "displayCode")
            bundle.addEntry().resource = messageHeader

            // Assert that a new element that would be "part" of an existing schema cannot
            // reference a constant from the nested schema
            val transformer = FhirToHl7Converter(
                extendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            )
            transformer.process(bundle)
            assertThat(transformer.errors.size > 0)
        }

        @Test
        fun `test overrides with a different schema`() {
            val bundle = Bundle()
            val messageHeader = MessageHeader()
            messageHeader.definition = "definition"
            messageHeader.id = "idSft"
            messageHeader.event = Coding("system", "xon3", "displayCode")
            bundle.addEntry().resource = messageHeader

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchemaOverridesSoftware,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("SFT-5")).isEqualTo("idSft")
            // Assert that the override sets a different schema for software that
            // does not define SFT-5
            assertThat(Terser(message).get("SFT-5")).isNull()
        }

        @Test
        fun `test nested overrides with a different schema`() {
            val bundle = Bundle()
            val messageHeader = MessageHeader()
            messageHeader.definition = "definition"
            messageHeader.id = "idSft"
            messageHeader.event = Coding("system", "aCode", "displayCode")
            bundle.addEntry().resource = messageHeader

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchemaOverridesXon,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("SFT-1-1")).isEqualTo("system")
            // Assert that an extending schema can override the schema for a nested element
            assertThat(Terser(message).get("SFT-1-1")).isEqualTo("aCode")
        }

        @Test
        fun `test nested overrides with a different schema and then adds an element`() {
            val bundle = Bundle()
            val messageHeader = MessageHeader()
            messageHeader.definition = "definition"
            messageHeader.id = "idSft"
            messageHeader.event = Coding("system", "aCode", "displayCode")
            bundle.addEntry().resource = messageHeader

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedSchemaOverridesXon,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("SFT-1-10")).isNull()
            // Assert that a new element can get added when overriding a nested schema
            assertThat(Terser(message).get("SFT-1-10")).isEqualTo("system")
        }

        @Test
        fun `test overrides in an extended schema`() {
            val bundle = Bundle()
            bundle.id = "abc123"

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedExtendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("MSH-11")).isEqualTo("abc123")
            // Assert that a schema that extends a schema overriding MSH-11 can further
            // override it
            assertThat(Terser(message).get("MSH-11")).isEqualTo("overrideOverridden")
        }

        @Test
        fun `test nested schemas cannot extend`() {
            val bundle = Bundle()
            val messageHeader = MessageHeader()
            messageHeader.definition = "definition"
            messageHeader.id = "idSft"
            messageHeader.event = Coding("system", "aCode", "displayCode")
            bundle.addEntry().resource = messageHeader

            val baseMessage = FhirToHl7Converter(
                baseSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)
            val message = FhirToHl7Converter(
                extendedExtendedSchema,
                warnings = mutableListOf(), errors = mutableListOf()
            ).process(bundle)

            assertThat(Terser(baseMessage).get("SFT-1-1")).isEqualTo("system")
            // Asserts that a nested schema cannot use an extends clause
            assertThat(Terser(message).get("SFT-1-1")).isNull()
            assertThat(Terser(message).get("SFT-1-2")).isEqualTo("2")
        }
    }
}