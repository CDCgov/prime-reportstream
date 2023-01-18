package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import kotlin.test.Test

class ConfigSchemaTests {
    @Test
    fun `test validate schema`() {
        var schema = ConfigSchema()
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema()
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema("ORU_R01")
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema("ORU_R01", "2.5.1")
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        val goodElement = ConfigSchemaElement(value = listOf("Bundle"), hl7Spec = listOf("MSH-7"))
        schema = ConfigSchema("ORU_R01", "2.5.1", mutableListOf(goodElement))
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()

        // A child schema
        schema = ConfigSchema("ORU_R01", "2.5.1", mutableListOf(goodElement))
        assertThat(schema.validate(true)).isNotEmpty()

        // A bad type
        schema = ConfigSchema("VAT", "2.5.1", mutableListOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema(null, "2.5.1", mutableListOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema("ORU_R01", null, mutableListOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.isValid()).isFalse() // We check again to make sure we get the same value
        assertThat(schema.errors).isNotEmpty()

        // Check on constants
        schema = ConfigSchema(
            "ORU_R01", "2.5.1", mutableListOf(goodElement), constants = sortedMapOf("const1" to "")
        )
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()
        assertThat(schema.errors.size).isEqualTo(1)
        assertThat(schema.errors[0]).contains(schema.constants.firstKey())

        schema = ConfigSchema(
            "ORU_R01", "2.5.1", mutableListOf(goodElement), constants = sortedMapOf("const1" to "value")
        )
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()
    }

    @Test
    fun `test validate schema element`() {
        var element = ConfigSchemaElement()
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement("name")
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement("name", value = listOf("Bundle"))
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement("name", hl7Spec = listOf("MSH-7"))
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement("name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7"))
        assertThat(element.validate()).isEmpty()

        element = ConfigSchemaElement("name", value = listOf("Bundle", "Bundle.id"), hl7Spec = listOf("MSH-7"))
        assertThat(element.validate()).isEmpty()

        element = ConfigSchemaElement(
            "name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7"), schema = "schema"
        )
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement("name", hl7Spec = listOf("MSH-7"), schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement("name", value = listOf("Bundle"), schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement("name", schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement(
            "name", value = listOf("Bundle"), resource = "Bundle", condition = "Bundle",
            hl7Spec = listOf("MSH-7")
        )
        assertThat(element.validate()).isEmpty()

        // FHIR Path errors
        element = ConfigSchemaElement(
            "name", value = listOf("Bundle..."), resource = "Bundle", condition = "Bundle",
        )
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement(
            "name", value = listOf("Bundle"), resource = "Bundle...", condition = "Bundle",
        )
        assertThat(element.validate()).isNotEmpty()

        element = ConfigSchemaElement(
            "name", value = listOf("Bundle"), resource = "Bundle", condition = "Bundle...",
        )
        assertThat(element.validate()).isNotEmpty()

        // Check on resource index
        val aSchema = ConfigSchema(
            elements = mutableListOf(ConfigSchemaElement("name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7")))
        )
        element = ConfigSchemaElement(
            "name", schema = "someschema", schemaRef = aSchema, resourceIndex = "someindex"
        )
        assertThat(element.validate()).isNotEmpty()
        element = ConfigSchemaElement(
            "name", schema = "someschema", schemaRef = aSchema, resourceIndex = "someindex",
            resource = "someresource"
        )
        assertThat(element.validate()).isEmpty()
        element = ConfigSchemaElement(
            "name", value = listOf("somevalue"), hl7Spec = listOf("MSH-10"), resourceIndex = "someindex",
            resource = "someresource"
        )
        assertThat(element.validate()).isNotEmpty()

        // Check on constants
        element = ConfigSchemaElement(
            "name", value = listOf("somevalue"), hl7Spec = listOf("MSH-10"), constants = sortedMapOf("const1" to "")
        )
        val errors = element.validate()
        assertThat(errors).isNotEmpty()
        assertThat(errors.size).isEqualTo(1)
        assertThat(errors[0]).contains(element.constants.firstKey())
        element = ConfigSchemaElement(
            "name", value = listOf("somevalue"), hl7Spec = listOf("MSH-10"),
            constants = sortedMapOf("const1" to "value")
        )
        assertThat(element.validate()).isEmpty()
    }

    @Test
    fun `test validate schema with schemas`() {
        var goodElement = ConfigSchemaElement("name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7"))
        var childSchema = ConfigSchema(elements = mutableListOf(goodElement))
        var elementWithSchema = ConfigSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        var topSchema = ConfigSchema("ORU_R01", "2.5.1", mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isTrue()
        assertThat(topSchema.errors).isEmpty()

        goodElement = ConfigSchemaElement("name", value = listOf("Bundle")) // No HL7Spec = error
        childSchema = ConfigSchema(elements = mutableListOf(goodElement))
        elementWithSchema = ConfigSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConfigSchema("ORU_R01", "2.5.1", mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()

        childSchema = ConfigSchema(hl7Version = "2.5.1", elements = mutableListOf(goodElement))
        elementWithSchema = ConfigSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConfigSchema("ORU_R01", "2.5.1", mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()

        childSchema = ConfigSchema(hl7Type = "ORU_R01", elements = mutableListOf(goodElement))
        elementWithSchema = ConfigSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConfigSchema("ORU_R01", "2.5.1", mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()
    }

    @Test
    fun `test merge of element`() {
        fun newParent(): ConfigSchemaElement {
            return ConfigSchemaElement(
                "name", condition = "condition1", required = true,
                schema = "schema1", schemaRef = ConfigSchema(), resource = "resource1", resourceIndex = "index1",
                value = listOf("value1"), hl7Spec = listOf("hl7spec1"), constants = sortedMapOf("k1" to "v1")
            )
        }

        val originalElement = newParent()
        val elementA = ConfigSchemaElement("name")
        var parentElement = newParent().merge(elementA)
        assertAll {
            assertThat(parentElement.condition).isEqualTo(originalElement.condition)
            assertThat(parentElement.required).isEqualTo(originalElement.required)
            assertThat(parentElement.schema).isEqualTo(originalElement.schema)
            assertThat(parentElement.schemaRef).isEqualTo(originalElement.schemaRef)
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

        val elementB = ConfigSchemaElement(
            "name", condition = "condition2", required = false,
            schema = "schema2", schemaRef = ConfigSchema(), resource = "resource2", resourceIndex = "index2"
        )
        parentElement = newParent().merge(elementB)
        assertAll {
            assertThat(parentElement.condition).isEqualTo(elementB.condition)
            assertThat(parentElement.required).isEqualTo(elementB.required)
            assertThat(parentElement.schema).isEqualTo(elementB.schema)
            assertThat(parentElement.schemaRef).isEqualTo(elementB.schemaRef)
            assertThat(parentElement.resource).isEqualTo(elementB.resource)
            assertThat(parentElement.resourceIndex).isEqualTo(elementB.resourceIndex)
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

        val elementC = ConfigSchemaElement(
            "name", condition = "condition3", required = null,
            schema = "schema3", schemaRef = ConfigSchema(), resource = "resource3", resourceIndex = "index3",
            value = listOf("value3"), hl7Spec = listOf("hl7spec3"), constants = sortedMapOf("k3" to "v3")
        )
        parentElement = newParent().merge(elementC)
        assertAll {
            assertThat(parentElement.condition).isEqualTo(elementC.condition)
            assertThat(parentElement.required).isEqualTo(originalElement.required)
            assertThat(parentElement.schema).isEqualTo(elementC.schema)
            assertThat(parentElement.schemaRef).isEqualTo(elementC.schemaRef)
            assertThat(parentElement.resource).isEqualTo(elementC.resource)
            assertThat(parentElement.resourceIndex).isEqualTo(elementC.resourceIndex)
            assertThat(parentElement.value.size).isEqualTo(elementC.value.size)
            assertThat(parentElement.hl7Spec.size).isEqualTo(elementC.hl7Spec.size)
            assertThat(parentElement.constants.size).isEqualTo(elementC.constants.size)
            parentElement.value
                .forEachIndexed { index, value -> assertThat(elementC.value[index]).isEqualTo(value) }
            parentElement.hl7Spec
                .forEachIndexed { index, hl7Spec -> assertThat(elementC.hl7Spec[index]).isEqualTo(hl7Spec) }
            parentElement.constants
                .forEach { (key, value) -> assertThat(elementC.constants[key]).isEqualTo(value) }
        }
    }

    @Test
    fun `test find element`() {
        val childSchema = ConfigSchema(
            elements = mutableListOf(
                ConfigSchemaElement("child1"),
                ConfigSchemaElement("child2"),
                ConfigSchemaElement("child3")
            )
        )
        val schema = ConfigSchema(
            elements = mutableListOf(
                ConfigSchemaElement("parent1"),
                ConfigSchemaElement("parent2"),
                ConfigSchemaElement("parent3"),
                ConfigSchemaElement("schemaElement", schema = "childSchema", schemaRef = childSchema)
            )
        )

        assertThat(schema.findElement("parent2")).isEqualTo(schema.elements[1])
        assertThat(schema.findElement("child2")).isEqualTo(childSchema.elements[1])
    }

    @Test
    fun `test merge of schemas`() {
        val childSchema = ConfigSchema(
            elements = mutableListOf(
                ConfigSchemaElement("child1"),
                ConfigSchemaElement("child2"),
                ConfigSchemaElement("child3")
            )
        )
        val schema = ConfigSchema(
            hl7Type = "ORU_R01",
            hl7Version = "2.5.1",
            elements = mutableListOf(
                ConfigSchemaElement("parent1"),
                ConfigSchemaElement("parent2"),
                ConfigSchemaElement("parent3"),
                ConfigSchemaElement("schemaElement", schema = "childSchema", schemaRef = childSchema)
            )
        )

        val extendedSchema = ConfigSchema(
            hl7Type = "ORU_R01",
            hl7Version = "2.7",
            elements = mutableListOf(
                ConfigSchemaElement("parent1", required = true),
                ConfigSchemaElement("child2", condition = "condition1"),
                ConfigSchemaElement("newElement1"),
            )
        )

        schema.merge(extendedSchema)
        assertThat(schema.elements[0].required).isEqualTo(extendedSchema.elements[0].required)
        assertThat(childSchema.elements[1].condition).isEqualTo(extendedSchema.elements[1].condition)
        assertThat(schema.elements.last().name).isEqualTo(extendedSchema.elements[2].name)
        assertThat(schema.hl7Version).isEqualTo("2.7")
    }
}