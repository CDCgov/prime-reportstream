package gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import kotlin.test.Test

class FhirTransformSchemaTests {
    @Test
    fun `test validate schema`() {
        var schema = FhirTransformSchema()
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        val goodElement = FhirTransformSchemaElement(value = listOf("final"), bundleProperty = "%resource.status")
        schema = FhirTransformSchema(mutableListOf(goodElement))
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()

        schema = FhirTransformSchema(mutableListOf(goodElement))
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.isValid()).isTrue() // We check again to make sure we get the same value
        assertThat(schema.errors).isEmpty()

        // Check on constants
        schema = FhirTransformSchema(
            mutableListOf(goodElement), constants = sortedMapOf("const1" to "")
        )
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()

        schema = FhirTransformSchema(
            mutableListOf(goodElement), constants = sortedMapOf("const1" to "value")
        )
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()
    }

    @Test
    fun `test validate schema element`() {
        var element = FhirTransformSchemaElement()
        assertThat(element.validate()).isNotEmpty()

        element = FhirTransformSchemaElement("name")
        assertThat(element.validate()).isNotEmpty()

        element = FhirTransformSchemaElement("name", value = listOf("final"))
        assertThat(element.validate()).isNotEmpty()

        element = FhirTransformSchemaElement("name", value = listOf("final"), bundleProperty = "%resource.status")
        assertThat(element.validate()).isEmpty()

        element =
            FhirTransformSchemaElement("name", value = listOf("final", "partial"), bundleProperty = "%resource.status")
        assertThat(element.validate()).isEmpty()

        element = FhirTransformSchemaElement(
            "name", value = listOf("final"), schema = "schema", bundleProperty = "%resource.status"
        )
        assertThat(element.validate()).isNotEmpty()

        element = FhirTransformSchemaElement("name", schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = FhirTransformSchemaElement("name", value = listOf("final"), schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = FhirTransformSchemaElement(
            "name",
            value = listOf("final"),
            resource = "%resource.status",
            condition = "%resource.status=final",
            bundleProperty = "%resource.status"
        )
        assertThat(element.validate()).isEmpty()

        // Check on resource index
        val aSchema = FhirTransformSchema(
            elements = mutableListOf(
                FhirTransformSchemaElement(
                    "name",
                    value = listOf("final"),
                    bundleProperty = "%resource.status"
                )
            )
        )
        element = FhirTransformSchemaElement(
            "name", schema = "someSchema", schemaRef = aSchema, resourceIndex = "someIndex"
        )
        assertThat(element.validate()).isNotEmpty()
        element = FhirTransformSchemaElement(
            "name", schema = "someSchema", schemaRef = aSchema, resourceIndex = "someIndex",
            resource = "someResource"
        )
        assertThat(element.validate()).isEmpty()
        element = FhirTransformSchemaElement(
            "name", value = listOf("someValue"), resourceIndex = "someIndex",
            resource = "someResource"
        )
        assertThat(element.validate()).isNotEmpty()

        // Check on constants
        element = FhirTransformSchemaElement(
            "name", value = listOf("someValue"), constants = sortedMapOf("const1" to ""),
            bundleProperty = "%resource.status"
        )
        val errors = element.validate()
        assertThat(errors).isEmpty()

        element = FhirTransformSchemaElement(
            "name", value = listOf("someValue"),
            constants = sortedMapOf("const1" to "value"),
            bundleProperty = "%resource.status"
        )
        assertThat(element.validate()).isEmpty()
    }

    @Test
    fun `test validate schema with schemas`() {
        val goodElement = FhirTransformSchemaElement(
            "name", value = listOf("final"),
            bundleProperty = "%resource.status"
        )
        var childSchema = FhirTransformSchema(elements = mutableListOf(goodElement))
        var elementWithSchema = FhirTransformSchemaElement("name", schema = "schemaName", schemaRef = childSchema)
        var topSchema = FhirTransformSchema(mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isTrue()
        assertThat(topSchema.errors).isEmpty()

        val badElement = FhirTransformSchemaElement("name", value = listOf("final")) // No bundleProperty = error
        childSchema = FhirTransformSchema(elements = mutableListOf(badElement))
        elementWithSchema = FhirTransformSchemaElement("name", schema = "schemaName", schemaRef = childSchema)
        topSchema = FhirTransformSchema(mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()
    }

    @Test
    fun `test extension of element`() {
        fun newParent(): FhirTransformSchemaElement {
            return FhirTransformSchemaElement(
                "name", condition = "condition1",
                schema = "schema1", schemaRef = FhirTransformSchema(), resource = "resource1", resourceIndex = "index1",
                value = listOf("value1"), constants = sortedMapOf("k1" to "v1"), bundleProperty = "%resource.status"
            )
        }

        val originalElement = newParent()
        val elementA = FhirTransformSchemaElement("name")
        val mergedElement = elementA.extend(newParent())
        assertThat(mergedElement is FhirTransformSchemaElement)
        if (mergedElement is FhirTransformSchemaElement) {
            assertAll {
                assertThat(mergedElement.condition).isEqualTo(originalElement.condition)
                assertThat(mergedElement.schema).isEqualTo(originalElement.schema)
                assertThat(mergedElement.schemaRef?.name).isEqualTo(originalElement.schemaRef?.name)
                assertThat(mergedElement.resource).isEqualTo(originalElement.resource)
                assertThat(mergedElement.resourceIndex).isEqualTo(originalElement.resourceIndex)
                assertThat(mergedElement.value.size).isEqualTo(originalElement.value.size)
                assertThat(mergedElement.constants.size).isEqualTo(originalElement.constants.size)
                assertThat(mergedElement.bundleProperty).isEqualTo(originalElement.bundleProperty)
                mergedElement.value
                    .forEachIndexed { index, value -> assertThat(originalElement.value[index]).isEqualTo(value) }
                mergedElement.constants
                    .forEach { (key, value) -> assertThat(originalElement.constants[key]).isEqualTo(value) }
            }
        }

        val elementB = FhirTransformSchemaElement(
            "name",
            condition = "condition2",
            schema = "schema2",
            schemaRef = FhirTransformSchema(),
            resource = "resource2",
            resourceIndex = "index2",
            bundleProperty = "%resource.status"
        )
        val mergedElementB = elementB.extend(newParent())
        assertThat(mergedElementB is FhirTransformSchemaElement)
        if (mergedElementB is FhirTransformSchemaElement) {
            assertAll {
                assertThat(mergedElementB.condition).isEqualTo(elementB.condition)
                assertThat(mergedElementB.schema).isEqualTo(elementB.schema)
                assertThat(mergedElementB.schemaRef).isEqualTo(elementB.schemaRef)
                assertThat(mergedElementB.resource).isEqualTo(elementB.resource)
                assertThat(mergedElementB.resourceIndex).isEqualTo(elementB.resourceIndex)
                assertThat(mergedElementB.value.size).isEqualTo(originalElement.value.size)
                assertThat(mergedElementB.constants.size).isEqualTo(originalElement.constants.size)
                assertThat(mergedElementB.bundleProperty).isEqualTo(originalElement.bundleProperty)
                mergedElementB.value
                    .forEachIndexed { index, value -> assertThat(originalElement.value[index]).isEqualTo(value) }
                mergedElementB.constants
                    .forEach { (key, value) -> assertThat(originalElement.constants[key]).isEqualTo(value) }
            }
        }

        val elementC = FhirTransformSchemaElement(
            "name", condition = "condition3",
            schema = "schema3", schemaRef = FhirTransformSchema(), resource = "resource3", resourceIndex = "index3",
            value = listOf("value3"), constants = sortedMapOf("k3" to "v3"), bundleProperty = "%resource.status"
        )
        val mergedElementC = elementC.extend(newParent())

        assertThat(mergedElementC is FhirTransformSchemaElement)
        if (mergedElementC is FhirTransformSchemaElement) {
            assertAll {
                assertThat(mergedElementC.condition).isEqualTo(elementC.condition)
                assertThat(mergedElementC.schema).isEqualTo(elementC.schema)
                assertThat(mergedElementC.schemaRef).isEqualTo(elementC.schemaRef)
                assertThat(mergedElementC.resource).isEqualTo(elementC.resource)
                assertThat(mergedElementC.resourceIndex).isEqualTo(elementC.resourceIndex)
                assertThat(mergedElementC.value.size).isEqualTo(elementC.value.size)
                assertThat(mergedElementC.constants.size).isEqualTo(elementC.constants.size)
                assertThat(mergedElementC.bundleProperty).isEqualTo(originalElement.bundleProperty)
                mergedElementC.value
                    .forEachIndexed { index, value -> assertThat(elementC.value[index]).isEqualTo(value) }
                mergedElementC.constants
                    .forEach { (key, value) -> assertThat(elementC.constants[key]).isEqualTo(value) }
            }
        }
    }

    @Test
    fun `test invalid extension of element`() {
        val elementA = FhirTransformSchemaElement("name")
        val elementB = ConverterSchemaElement("name")
        assertThat { elementA.extend(elementB) }.isFailure()
    }

    @Test
    fun `test extension of schema with unnamed element`() {
        val schemaA = FhirTransformSchema(elements = mutableListOf((FhirTransformSchemaElement())))
        val schemaB = FhirTransformSchema(elements = mutableListOf((FhirTransformSchemaElement())))
        assertThat { schemaA.extend(schemaB) }.isFailure()
    }

    @Test
    fun `test find element`() {
        val childSchema = FhirTransformSchema(
            elements = mutableListOf(
                FhirTransformSchemaElement("child1"),
                FhirTransformSchemaElement("child2"),
                FhirTransformSchemaElement("child3")
            )
        )
        val schema = FhirTransformSchema(
            elements = mutableListOf(
                FhirTransformSchemaElement("parent1"),
                FhirTransformSchemaElement("parent2"),
                FhirTransformSchemaElement("parent3"),
                FhirTransformSchemaElement("schemaElement", schema = "childSchema", schemaRef = childSchema)
            )
        )

        assertThat(schema.findElement("parent2")).isEqualTo(schema.elements[1])
        assertThat(schema.findElement("child2")).isEqualTo(childSchema.elements[1])
    }

    @Test
    fun `test extension of schemas`() {
        val baseSchema = FhirTransformSchema(
            elements = mutableListOf(
                FhirTransformSchemaElement("parent1", required = true),
                FhirTransformSchemaElement("child2", condition = "condition1"),
                FhirTransformSchemaElement("newElement1"),
            ),
            constants = sortedMapOf(Pair("K1", "baseV1"), Pair("K2", "baseV2"), Pair("K3", "baseV3")),
        )
        baseSchema.name = "baseSchema"

        val parentSchema = FhirTransformSchema(constants = sortedMapOf(Pair("K2", "parentV2")))
        parentSchema.name = "parentSchema"
        parentSchema.extend(baseSchema)

        val referencedSchema = FhirTransformSchema(
            elements = mutableListOf(
                FhirTransformSchemaElement("child1"),
                FhirTransformSchemaElement("child2"),
                FhirTransformSchemaElement("child3")
            ),
            constants = sortedMapOf(Pair("K3", "refV3"), Pair("K5", "refV5")),
        )
        val schema = FhirTransformSchema(
            elements = mutableListOf(
                FhirTransformSchemaElement("parent1"),
                FhirTransformSchemaElement("parent2"),
                FhirTransformSchemaElement("parent3"),
                FhirTransformSchemaElement("schemaElement", schema = "childSchema", schemaRef = referencedSchema)
            ),
            constants = sortedMapOf(Pair("K1", "testV1"), Pair("K4", "testV4")),
        )
        schema.name = "testSchema"
        schema.extend(parentSchema)

        assertThat((schema.elements[0]).required).isEqualTo((baseSchema.elements[0]).required)
        assertThat(referencedSchema.elements[1].condition).isEqualTo(baseSchema.elements[1].condition)
        assertThat(schema.elements.last().name).isEqualTo(baseSchema.elements[2].name)
        assertThat(schema.name).isEqualTo("testSchema")
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