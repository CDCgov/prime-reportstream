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
    fun `test extension of element`() {
        fun newParent(): ConverterSchemaElement {
            return ConverterSchemaElement(
                "name", condition = "condition1", required = true,
                schema = "schema1", schemaRef = ConverterSchema(), resource = "resource1", resourceIndex = "index1",
                value = listOf("value1"), hl7Spec = listOf("hl7spec1"), constants = sortedMapOf("k1" to "v1")
            )
        }

        val originalElement = newParent()
        val elementA = ConverterSchemaElement("name")
        val mergedElement = elementA.extend(newParent())
        assertThat(mergedElement is ConverterSchemaElement)
        if (mergedElement is ConverterSchemaElement) {
            assertAll {
                assertThat(mergedElement.condition).isEqualTo(originalElement.condition)
                assertThat(mergedElement.required).isEqualTo(originalElement.required)
                assertThat(mergedElement.schema).isEqualTo(originalElement.schema)
                assertThat(mergedElement.schemaRef?.name).isEqualTo(originalElement.schemaRef?.name)
                assertThat(mergedElement.resource).isEqualTo(originalElement.resource)
                assertThat(mergedElement.resourceIndex).isEqualTo(originalElement.resourceIndex)
                assertThat(mergedElement.value.size).isEqualTo(originalElement.value.size)
                assertThat(mergedElement.hl7Spec.size).isEqualTo(originalElement.hl7Spec.size)
                assertThat(mergedElement.constants.size).isEqualTo(originalElement.constants.size)
                mergedElement.value
                    .forEachIndexed { index, value -> assertThat(originalElement.value[index]).isEqualTo(value) }
                mergedElement.hl7Spec
                    .forEachIndexed { index, hl7Spec -> assertThat(originalElement.hl7Spec[index]).isEqualTo(hl7Spec) }
                mergedElement.constants
                    .forEach { (key, value) -> assertThat(originalElement.constants[key]).isEqualTo(value) }
            }
        }

        val elementB = ConverterSchemaElement(
            "name", condition = "condition2", required = false,
            schema = "schema2", schemaRef = ConverterSchema(), resource = "resource2", resourceIndex = "index2"
        )
        val mergedElementB = elementB.extend(newParent())
        assertThat(mergedElementB is ConverterSchemaElement)
        if (mergedElementB is ConverterSchemaElement) {
            assertAll {
                assertThat(mergedElementB.condition).isEqualTo(elementB.condition)
                assertThat(mergedElementB.required).isEqualTo(elementB.required)
                assertThat(mergedElementB.schema).isEqualTo(elementB.schema)
                assertThat(mergedElementB.schemaRef).isEqualTo(elementB.schemaRef)
                assertThat(mergedElementB.resource).isEqualTo(elementB.resource)
                assertThat(mergedElementB.resourceIndex).isEqualTo(elementB.resourceIndex)
                assertThat(mergedElementB.value.size).isEqualTo(originalElement.value.size)
                assertThat(mergedElementB.hl7Spec.size).isEqualTo(originalElement.hl7Spec.size)
                assertThat(mergedElementB.constants.size).isEqualTo(originalElement.constants.size)
                mergedElementB.value
                    .forEachIndexed { index, value -> assertThat(originalElement.value[index]).isEqualTo(value) }
                mergedElementB.hl7Spec
                    .forEachIndexed { index, hl7Spec -> assertThat(originalElement.hl7Spec[index]).isEqualTo(hl7Spec) }
                mergedElementB.constants
                    .forEach { (key, value) -> assertThat(originalElement.constants[key]).isEqualTo(value) }
            }
        }

        val elementC = ConverterSchemaElement(
            "name", condition = "condition3", required = null,
            schema = "schema3", schemaRef = ConverterSchema(), resource = "resource3", resourceIndex = "index3",
            value = listOf("value3"), hl7Spec = listOf("hl7spec3"), constants = sortedMapOf("k3" to "v3")
        )
        val mergedElementC = elementC.extend(newParent())

        assertThat(mergedElementC is ConverterSchemaElement)
        if (mergedElementC is ConverterSchemaElement) {
            assertAll {
                assertThat(mergedElementC.condition).isEqualTo(elementC.condition)
                assertThat(mergedElementC.required).isEqualTo(originalElement.required)
                assertThat(mergedElementC.schema).isEqualTo(elementC.schema)
                assertThat(mergedElementC.schemaRef).isEqualTo(elementC.schemaRef)
                assertThat(mergedElementC.resource).isEqualTo(elementC.resource)
                assertThat(mergedElementC.resourceIndex).isEqualTo(elementC.resourceIndex)
                assertThat(mergedElementC.value.size).isEqualTo(elementC.value.size)
                assertThat(mergedElementC.hl7Spec.size).isEqualTo(elementC.hl7Spec.size)
                assertThat(mergedElementC.constants.size).isEqualTo(elementC.constants.size)
                mergedElementC.value
                    .forEachIndexed { index, value -> assertThat(elementC.value[index]).isEqualTo(value) }
                mergedElementC.hl7Spec
                    .forEachIndexed { index, hl7Spec -> assertThat(elementC.hl7Spec[index]).isEqualTo(hl7Spec) }
                mergedElementC.constants
                    .forEach { (key, value) -> assertThat(elementC.constants[key]).isEqualTo(value) }
            }
        }
    }

    @Test
    fun `test invalid extension of element`() {
        val elementA = ConverterSchemaElement("name")
        val elementB = FhirTransformSchemaElement("name")
        assertThat { elementA.extend(elementB) }.isFailure()
    }

    @Test
    fun `test extension of schema with unnamed element`() {
        val schemaA = ConverterSchema(elements = mutableListOf((ConverterSchemaElement())))
        val schemaB = ConverterSchema(elements = mutableListOf((ConverterSchemaElement())))
        assertThat { schemaA.extend(schemaB) }.isFailure()
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
    fun `test extension of schemas`() {
        val baseSchema = ConverterSchema(
            hl7Class = "ca.uhn.hl7v2.model.v251.message.ORU_R01",
            elements = mutableListOf(
                ConverterSchemaElement("parent1", required = true),
                ConverterSchemaElement("child2", condition = "condition1"),
                ConverterSchemaElement("newElement1"),
            ),
            constants = sortedMapOf(Pair("K1", "baseV1"), Pair("K2", "baseV2"), Pair("K3", "baseV3")),
        )
        baseSchema.name = "baseSchema"

        val parentSchema = ConverterSchema(constants = sortedMapOf(Pair("K2", "parentV2")))
        parentSchema.name = "parentSchema"
        parentSchema.extend(baseSchema)

        val referencedSchema = ConverterSchema(
            elements = mutableListOf(
                ConverterSchemaElement("child1"),
                ConverterSchemaElement("child2"),
                ConverterSchemaElement("child3")
            ),
            constants = sortedMapOf(Pair("K3", "refV3"), Pair("K5", "refV5")),
        )
        val schema = ConverterSchema(
            elements = mutableListOf(
                ConverterSchemaElement("parent1"),
                ConverterSchemaElement("parent2"),
                ConverterSchemaElement("parent3"),
                ConverterSchemaElement("schemaElement", schema = "childSchema", schemaRef = referencedSchema)
            ),
            constants = sortedMapOf(Pair("K1", "testV1"), Pair("K4", "testV4")),
        )
        schema.name = "testSchema"
        schema.extend(parentSchema)

        assertThat((schema.elements[0]).required).isEqualTo((baseSchema.elements[0]).required)
        assertThat(referencedSchema.elements[1].condition).isEqualTo(baseSchema.elements[1].condition)
        assertThat(schema.elements.last().name).isEqualTo(baseSchema.elements[2].name)
        assertThat(schema.name).isEqualTo("testSchema")
        assertThat(schema.hl7Class).isEqualTo("ca.uhn.hl7v2.model.v251.message.ORU_R01")
        assertThat(schema.constants["K1"]).isEqualTo("testV1")
        assertThat(schema.constants["K2"]).isEqualTo("parentV2")
        assertThat(schema.constants["K3"]).isEqualTo("baseV3")
        assertThat(schema.constants["K4"]).isEqualTo("testV4")
        assertThat(schema.constants["K5"]).isNull()
        assertThat(parentSchema.name).isEqualTo("parentSchema")
        assertThat(parentSchema.constants["K1"]).isEqualTo("baseV1")
        assertThat(parentSchema.constants["K2"]).isEqualTo("parentV2")
        assertThat(parentSchema.constants["K4"]).isNull()
        assertThat(parentSchema.constants["K5"]).isNull()
        assertThat(baseSchema.name).isEqualTo("baseSchema")
        assertThat(baseSchema.constants["K1"]).isEqualTo("baseV1")
        assertThat(baseSchema.constants["K2"]).isEqualTo("baseV2")
        assertThat(baseSchema.constants["K4"]).isNull()
        assertThat(baseSchema.constants["K5"]).isNull()
        assertThat(referencedSchema.constants["K1"]).isNull()
        assertThat(referencedSchema.constants["K3"]).isEqualTo("refV3")
        assertThat(referencedSchema.constants["K4"]).isNull()
        assertThat(referencedSchema.constants["K5"]).isEqualTo("refV5")
    }
}