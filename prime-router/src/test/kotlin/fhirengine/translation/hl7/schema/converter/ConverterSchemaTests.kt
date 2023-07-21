package gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import kotlin.test.Test

class ConverterSchemaTests {
    @Test
    fun `test validate schema`() {
        var schema = ConverterSchema()
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConverterSchema("ca.uhn.hl7v2.model.v251.message.ORU_R01")
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        val goodElement = ConverterSchemaElement(value = listOf("Bundle"), hl7Spec = listOf("MSH-7"))
        schema = ConverterSchema("ca.uhn.hl7v2.model.v251.message.ORU_R01", mutableListOf(goodElement))
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()

        // A child schema
        schema = ConverterSchema("ca.uhn.hl7v2.model.v251.message.ORU_R01", mutableListOf(goodElement))
        assertThat(schema.validate(true)).isNotEmpty()

        // A bad type
        schema = ConverterSchema("some bad class", mutableListOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConverterSchema(null, mutableListOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        // Check on constants
        schema = ConverterSchema(
            "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            mutableListOf(goodElement),
            constants = sortedMapOf("const1" to "")
        )
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()

        schema = ConverterSchema(
            "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            mutableListOf(goodElement),
            constants = sortedMapOf("const1" to "value")
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
        assertThat(errors).isEmpty()

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
        var topSchema = ConverterSchema("ca.uhn.hl7v2.model.v251.message.ORU_R01", mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isTrue()
        assertThat(topSchema.errors).isEmpty()

        goodElement = ConverterSchemaElement("name", value = listOf("Bundle")) // No HL7Spec = error
        childSchema = ConverterSchema(elements = mutableListOf(goodElement))
        elementWithSchema = ConverterSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConverterSchema("ca.uhn.hl7v2.model.v251.message.ORU_R01", mutableListOf(elementWithSchema))
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
                assertThat(parentElement.value).isEqualTo(originalElement.value)
                assertThat(parentElement.valueSet).isEqualTo(originalElement.valueSet)
                assertThat(parentElement.hl7Spec).isEqualTo(originalElement.hl7Spec)
                assertThat(parentElement.constants).isEqualTo(originalElement.constants)
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
                assertThat(parentElementB.value).isEqualTo(originalElement.value)
                assertThat(parentElementB.valueSet).isEqualTo(originalElement.valueSet)
                assertThat(parentElementB.hl7Spec).isEqualTo(originalElement.hl7Spec)
                assertThat(parentElementB.constants).isEqualTo(originalElement.constants)
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
                assertThat(parentElementC.value).isEqualTo(elementC.value)
                assertThat(parentElementC.valueSet).isEqualTo(elementC.valueSet)
                assertThat(parentElementC.hl7Spec).isEqualTo(elementC.hl7Spec)
                assertThat(parentElementC.constants).isEqualTo(elementC.constants)
            }
        }
    }

    @Test
    fun `test invalid merge of element`() {
        val elementA = ConverterSchemaElement("name")
        val elementB = FhirTransformSchemaElement("name")
        assertThat { elementA.merge(elementB) }.isFailure()
    }

    @Test
    fun `test merge of schema with unnamed element`() {
        val schemaA = ConverterSchema(elements = mutableListOf((ConverterSchemaElement())))
        val schemaB = ConverterSchema(elements = mutableListOf((ConverterSchemaElement())))
        assertThat { schemaA.merge(schemaB) }.isFailure()
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
        val referencedSchema = ConverterSchema(
            elements = mutableListOf(
                ConverterSchemaElement("child1"),
                ConverterSchemaElement("child2"),
                ConverterSchemaElement("child3")
            ),
            constants = sortedMapOf(Pair("K3", "refV3"), Pair("K5", "refV5")),
        )
        referencedSchema.name = "referencedSchema"

        val baseSchema = ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(
                ConverterSchemaElement("parent1"),
                ConverterSchemaElement("parent2"),
                ConverterSchemaElement("parent3"),
                ConverterSchemaElement("schemaElement", schema = "childSchema", schemaRef = referencedSchema)
            ),
            constants = sortedMapOf(Pair("K1", "baseV1"), Pair("K2", "baseV2"), Pair("K3", "baseV3")),
        )
        baseSchema.name = "baseSchema"

        val parentSchema = ConverterSchema(constants = sortedMapOf(Pair("K2", "parentV2")))
        parentSchema.name = "parentSchema"

        val schema = ConverterSchema(
            elements = mutableListOf(
                ConverterSchemaElement("parent1", required = true),
                ConverterSchemaElement("child2", condition = "condition1"),
                ConverterSchemaElement("newElement"),
            ),
            constants = sortedMapOf(Pair("K1", "testV1"), Pair("K4", "testV4")),
        )
        schema.name = "testSchema"

        baseSchema.merge(parentSchema).merge(schema)
        assertThat((baseSchema.elements[0]).required).isEqualTo((schema.elements[0]).required)
        assertThat(referencedSchema.elements[1].condition).isEqualTo(schema.elements[1].condition)
        assertThat(baseSchema.elements.last().name).isEqualTo(schema.elements[2].name)
        assertThat(baseSchema.hl7Class).isEqualTo("ca.uhn.hl7v2.model.v251.message.ORU_R01")
        assertThat(baseSchema.name).isEqualTo("testSchema")
        assertThat(baseSchema.constants["K1"]).isEqualTo("testV1")
        assertThat(baseSchema.constants["K2"]).isEqualTo("parentV2")
        assertThat(baseSchema.constants["K3"]).isEqualTo("baseV3")
        assertThat(baseSchema.constants["K4"]).isEqualTo("testV4")
        assertThat(baseSchema.constants["K5"]).isNull()
    }
}