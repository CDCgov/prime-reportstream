package gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FHIRTransformSchemaElement
import kotlin.test.Test

class ConverterSchemaTests {
    @Test
    fun `test validate schema`() {
        var schema = ConverterSchema()
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConverterSchema("ORU_R01")
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConverterSchema("ORU_R01", "2.5.1")
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        val goodElement = ConverterSchemaElement(value = listOf("Bundle"), hl7Spec = listOf("MSH-7"))
        schema = ConverterSchema("ORU_R01", "2.5.1", mutableListOf(goodElement))
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()

        // A child schema
        schema = ConverterSchema("ORU_R01", "2.5.1", mutableListOf(goodElement))
        assertThat(schema.validate(true)).isNotEmpty()

        // A bad type
        schema = ConverterSchema("VAT", "2.5.1", mutableListOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConverterSchema(null, "2.5.1", mutableListOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConverterSchema("ORU_R01", null, mutableListOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.isValid()).isFalse() // We check again to make sure we get the same value
        assertThat(schema.errors).isNotEmpty()

        // Check on constants
        schema = ConverterSchema(
            "ORU_R01", "2.5.1", mutableListOf(goodElement), constants = sortedMapOf("const1" to "")
        )
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()
        assertThat(schema.errors.size).isEqualTo(1)
        assertThat(schema.errors[0]).contains(schema.constants.firstKey())

        schema = ConverterSchema(
            "ORU_R01", "2.5.1", mutableListOf(goodElement), constants = sortedMapOf("const1" to "value")
        )
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()
    }

    @Test
    fun `test validate schema element`() {
        var element = ConverterSchemaElement()
        assertThat(element.validate()).isNotEmpty()

        element = ConverterSchemaElement("name")
        assertThat(element.validate()).isNotEmpty()

        element = ConverterSchemaElement("name", value = listOf("Bundle"))
        assertThat(element.validate()).isNotEmpty()

        element = ConverterSchemaElement("name", hl7Spec = listOf("MSH-7"))
        assertThat(element.validate()).isNotEmpty()

        element = ConverterSchemaElement("name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7"))
        assertThat(element.validate()).isEmpty()

        element = ConverterSchemaElement("name", value = listOf("Bundle", "Bundle.id"), hl7Spec = listOf("MSH-7"))
        assertThat(element.validate()).isEmpty()

        element = ConverterSchemaElement(
            "name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7"), schema = "schema"
        )
        assertThat(element.validate()).isNotEmpty()

        element = ConverterSchemaElement("name", hl7Spec = listOf("MSH-7"), schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = ConverterSchemaElement("name", value = listOf("Bundle"), schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = ConverterSchemaElement("name", schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = ConverterSchemaElement(
            "name", value = listOf("Bundle"), resource = "Bundle", condition = "Bundle",
            hl7Spec = listOf("MSH-7")
        )
        assertThat(element.validate()).isEmpty()

        // Check on resource index
        val aSchema = ConverterSchema(
            elements = mutableListOf(
                ConverterSchemaElement(
                    "name",
                    value = listOf("Bundle"),
                    hl7Spec = listOf("MSH-7")
                )
            )
        )
        element = ConverterSchemaElement(
            "name", schema = "someschema", schemaRef = aSchema, resourceIndex = "someindex"
        )
        assertThat(element.validate()).isNotEmpty()
        element = ConverterSchemaElement(
            "name", schema = "someschema", schemaRef = aSchema, resourceIndex = "someindex",
            resource = "someresource"
        )
        assertThat(element.validate()).isEmpty()
        element = ConverterSchemaElement(
            "name", value = listOf("somevalue"), hl7Spec = listOf("MSH-10"), resourceIndex = "someindex",
            resource = "someresource"
        )
        assertThat(element.validate()).isNotEmpty()

        // Check on constants
        element = ConverterSchemaElement(
            "name", value = listOf("somevalue"), hl7Spec = listOf("MSH-10"), constants = sortedMapOf("const1" to "")
        )
        val errors = element.validate()
        assertThat(errors).isNotEmpty()
        assertThat(errors.size).isEqualTo(1)
        assertThat(errors[0]).contains(element.constants.firstKey())
        element = ConverterSchemaElement(
            "name", value = listOf("somevalue"), hl7Spec = listOf("MSH-10"),
            constants = sortedMapOf("const1" to "value")
        )
        assertThat(element.validate()).isEmpty()
    }

    @Test
    fun `test validate schema with schemas`() {
        var goodElement = ConverterSchemaElement("name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7"))
        var childSchema = ConverterSchema(elements = mutableListOf(goodElement))
        var elementWithSchema = ConverterSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        var topSchema = ConverterSchema("ORU_R01", "2.5.1", mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isTrue()
        assertThat(topSchema.errors).isEmpty()

        goodElement = ConverterSchemaElement("name", value = listOf("Bundle")) // No HL7Spec = error
        childSchema = ConverterSchema(elements = mutableListOf(goodElement))
        elementWithSchema = ConverterSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConverterSchema("ORU_R01", "2.5.1", mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()

        childSchema = ConverterSchema(hl7Version = "2.5.1", elements = mutableListOf(goodElement))
        elementWithSchema = ConverterSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConverterSchema("ORU_R01", "2.5.1", mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()

        childSchema = ConverterSchema(hl7Type = "ORU_R01", elements = mutableListOf(goodElement))
        elementWithSchema = ConverterSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConverterSchema("ORU_R01", "2.5.1", mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()
    }

    @Test
    fun `test merge of element`() {
        fun newParent(): ConverterSchemaElement {
            return ConverterSchemaElement(
                "name", condition = "condition1", required = true,
                schema = "schema1", schemaRef = ConverterSchema(), resource = "resource1", resourceIndex = "index1",
                value = listOf("value1"), hl7Spec = listOf("hl7spec1"), constants = sortedMapOf("k1" to "v1")
            )
        }

        val originalElement = newParent()
        val elementA = ConverterSchemaElement("name")
        val parentElement = newParent().merge(elementA)
        assertThat(parentElement is ConverterSchemaElement)
        if (parentElement is ConverterSchemaElement) {
            assertAll {
                assertThat(parentElement.condition).isEqualTo(originalElement.condition)
                assertThat(parentElement.required).isEqualTo(originalElement.required)
                assertThat(parentElement.schema).isEqualTo(originalElement.schema)
                assertThat(parentElement.schemaRef?.name).isEqualTo(originalElement.schemaRef?.name)
                assertThat(parentElement.resource).isEqualTo(originalElement.resource)
                assertThat(parentElement.resourceIndex).isEqualTo(originalElement.resourceIndex)
                assertThat(parentElement.value.size).isEqualTo(originalElement.value.size)
                assertThat(parentElement.hl7Spec.size).isEqualTo(originalElement.hl7Spec.size)
                assertThat(parentElement.constants.size).isEqualTo(originalElement.constants.size)
                parentElement.value
                    .forEachIndexed { index, value -> assertThat(originalElement.value[index]).isEqualTo(value) }
                parentElement.hl7Spec
                    .forEachIndexed { index, hl7Spec -> assertThat(originalElement.hl7Spec[index]).isEqualTo(hl7Spec) }
                parentElement.constants
                    .forEach { (key, value) -> assertThat(originalElement.constants[key]).isEqualTo(value) }
            }
        }

        val elementB = ConverterSchemaElement(
            "name", condition = "condition2", required = false,
            schema = "schema2", schemaRef = ConverterSchema(), resource = "resource2", resourceIndex = "index2"
        )
        val parentElementB = newParent().merge(elementB)
        assertThat(parentElementB is ConverterSchemaElement)
        if (parentElementB is ConverterSchemaElement) {
            assertAll {
                assertThat(parentElementB.condition).isEqualTo(elementB.condition)
                assertThat(parentElementB.required).isEqualTo(elementB.required)
                assertThat(parentElementB.schema).isEqualTo(elementB.schema)
                assertThat(parentElementB.schemaRef).isEqualTo(elementB.schemaRef)
                assertThat(parentElementB.resource).isEqualTo(elementB.resource)
                assertThat(parentElementB.resourceIndex).isEqualTo(elementB.resourceIndex)
                assertThat(parentElementB.value.size).isEqualTo(originalElement.value.size)
                assertThat(parentElementB.hl7Spec.size).isEqualTo(originalElement.hl7Spec.size)
                assertThat(parentElementB.constants.size).isEqualTo(originalElement.constants.size)
                parentElementB.value
                    .forEachIndexed { index, value -> assertThat(originalElement.value[index]).isEqualTo(value) }
                parentElementB.hl7Spec
                    .forEachIndexed { index, hl7Spec -> assertThat(originalElement.hl7Spec[index]).isEqualTo(hl7Spec) }
                parentElementB.constants
                    .forEach { (key, value) -> assertThat(originalElement.constants[key]).isEqualTo(value) }
            }
        }

        val elementC = ConverterSchemaElement(
            "name", condition = "condition3", required = null,
            schema = "schema3", schemaRef = ConverterSchema(), resource = "resource3", resourceIndex = "index3",
            value = listOf("value3"), hl7Spec = listOf("hl7spec3"), constants = sortedMapOf("k3" to "v3")
        )
        val parentElementC = newParent().merge(elementC)

        assertThat(parentElementC is ConverterSchemaElement)
        if (parentElementC is ConverterSchemaElement) {
            assertAll {
                assertThat(parentElementC.condition).isEqualTo(elementC.condition)
                assertThat(parentElementC.required).isEqualTo(originalElement.required)
                assertThat(parentElementC.schema).isEqualTo(elementC.schema)
                assertThat(parentElementC.schemaRef).isEqualTo(elementC.schemaRef)
                assertThat(parentElementC.resource).isEqualTo(elementC.resource)
                assertThat(parentElementC.resourceIndex).isEqualTo(elementC.resourceIndex)
                assertThat(parentElementC.value.size).isEqualTo(elementC.value.size)
                assertThat(parentElementC.hl7Spec.size).isEqualTo(elementC.hl7Spec.size)
                assertThat(parentElementC.constants.size).isEqualTo(elementC.constants.size)
                parentElementC.value
                    .forEachIndexed { index, value -> assertThat(elementC.value[index]).isEqualTo(value) }
                parentElementC.hl7Spec
                    .forEachIndexed { index, hl7Spec -> assertThat(elementC.hl7Spec[index]).isEqualTo(hl7Spec) }
                parentElementC.constants
                    .forEach { (key, value) -> assertThat(elementC.constants[key]).isEqualTo(value) }
            }
        }
    }

    @Test
    fun `test invalid merge of element`() {
        val elementA = ConverterSchemaElement("name")
        val elementB = FHIRTransformSchemaElement("name")
        assertThat { elementA.merge(elementB) }.isFailure()
    }

    @Test
    fun `test find element`() {
        val childSchema = ConverterSchema(
            elements = mutableListOf(
                ConverterSchemaElement("child1"),
                ConverterSchemaElement("child2"),
                ConverterSchemaElement("child3")
            )
        )
        val schema = ConverterSchema(
            elements = mutableListOf(
                ConverterSchemaElement("parent1"),
                ConverterSchemaElement("parent2"),
                ConverterSchemaElement("parent3"),
                ConverterSchemaElement("schemaElement", schema = "childSchema", schemaRef = childSchema)
            )
        )

        assertThat(schema.findElement("parent2")).isEqualTo(schema.elements[1])
        assertThat(schema.findElement("child2")).isEqualTo(childSchema.elements[1])
    }

    @Test
    fun `test merge of schemas`() {
        val childSchema = ConverterSchema(
            elements = mutableListOf(
                ConverterSchemaElement("child1"),
                ConverterSchemaElement("child2"),
                ConverterSchemaElement("child3")
            )
        )
        val schema = ConverterSchema(
            hl7Type = "ORU_R01",
            hl7Version = "2.5.1",
            elements = mutableListOf(
                ConverterSchemaElement("parent1"),
                ConverterSchemaElement("parent2"),
                ConverterSchemaElement("parent3"),
                ConverterSchemaElement("schemaElement", schema = "childSchema", schemaRef = childSchema)
            )
        )

        val extendedSchema = ConverterSchema(
            hl7Type = "ORU_R01",
            hl7Version = "2.7",
            elements = mutableListOf(
                ConverterSchemaElement("parent1", required = true),
                ConverterSchemaElement("child2", condition = "condition1"),
                ConverterSchemaElement("newElement1"),
            )
        )

        schema.merge(extendedSchema)
        assertThat((schema.elements[0]).required).isEqualTo((extendedSchema.elements[0]).required)
        assertThat(childSchema.elements[1].condition).isEqualTo(extendedSchema.elements[1].condition)
        assertThat(schema.elements.last().name).isEqualTo(extendedSchema.elements[2].name)
        assertThat(schema.hl7Version).isEqualTo("2.7")
    }
}