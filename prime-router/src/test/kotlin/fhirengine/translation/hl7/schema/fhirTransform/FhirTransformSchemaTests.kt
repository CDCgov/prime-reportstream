package gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
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

        var badElement = FhirTransformSchemaElement("name", value = listOf("final")) // No bundleProperty = error
        childSchema = FhirTransformSchema(elements = mutableListOf(badElement))
        elementWithSchema = FhirTransformSchemaElement("name", schema = "schemaName", schemaRef = childSchema)
        topSchema = FhirTransformSchema(mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()
    }

    @Test
    fun `test merge of element`() {
        fun newParent(): FhirTransformSchemaElement {
            return FhirTransformSchemaElement(
                "name", condition = "condition1",
                schema = "schema1", schemaRef = FhirTransformSchema(), resource = "resource1", resourceIndex = "index1",
                value = listOf("value1"), constants = sortedMapOf("k1" to "v1"), bundleProperty = "%resource.status"
            )
        }

        val originalElement = newParent()
        val elementA = FhirTransformSchemaElement("name")
        val parentElement = newParent().merge(elementA)
        assertThat(parentElement is FhirTransformSchemaElement)
        if (parentElement is FhirTransformSchemaElement) {
            assertAll {
                assertThat(parentElement.condition).isEqualTo(originalElement.condition)
                assertThat(parentElement.schema).isEqualTo(originalElement.schema)
                assertThat(parentElement.schemaRef?.name).isEqualTo(originalElement.schemaRef?.name)
                assertThat(parentElement.resource).isEqualTo(originalElement.resource)
                assertThat(parentElement.resourceIndex).isEqualTo(originalElement.resourceIndex)
                assertThat(parentElement.value).isEqualTo(originalElement.value)
                assertThat(parentElement.valueSet).isEqualTo(originalElement.valueSet)
                assertThat(parentElement.constants).isEqualTo(originalElement.constants)
                assertThat(parentElement.bundleProperty).isEqualTo(originalElement.bundleProperty)
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
        val parentElementB = newParent().merge(elementB)
        assertThat(parentElementB is FhirTransformSchemaElement)
        if (parentElementB is FhirTransformSchemaElement) {
            assertAll {
                assertThat(parentElementB.condition).isEqualTo(elementB.condition)
                assertThat(parentElementB.schema).isEqualTo(elementB.schema)
                assertThat(parentElementB.schemaRef).isEqualTo(elementB.schemaRef)
                assertThat(parentElementB.resource).isEqualTo(elementB.resource)
                assertThat(parentElementB.resourceIndex).isEqualTo(elementB.resourceIndex)
                assertThat(parentElementB.value).isEqualTo(originalElement.value)
                assertThat(parentElementB.valueSet).isEqualTo(originalElement.valueSet)
                assertThat(parentElementB.constants).isEqualTo(originalElement.constants)
                assertThat(parentElementB.bundleProperty).isEqualTo(originalElement.bundleProperty)
            }
        }

        val elementC = FhirTransformSchemaElement(
            "name", condition = "condition3",
            schema = "schema3", schemaRef = FhirTransformSchema(), resource = "resource3", resourceIndex = "index3",
            value = listOf("value3"), constants = sortedMapOf("k3" to "v3"), bundleProperty = "%resource.status"
        )
        val parentElementC = newParent().merge(elementC)

        assertThat(parentElementC is FhirTransformSchemaElement)
        if (parentElementC is FhirTransformSchemaElement) {
            assertAll {
                assertThat(parentElementC.condition).isEqualTo(elementC.condition)
                assertThat(parentElementC.schema).isEqualTo(elementC.schema)
                assertThat(parentElementC.schemaRef).isEqualTo(elementC.schemaRef)
                assertThat(parentElementC.resource).isEqualTo(elementC.resource)
                assertThat(parentElementC.resourceIndex).isEqualTo(elementC.resourceIndex)
                assertThat(parentElementC.value).isEqualTo(elementC.value)
                assertThat(parentElementC.valueSet).isEqualTo(elementC.valueSet)
                assertThat(parentElementC.constants).isEqualTo(elementC.constants)
                assertThat(parentElementC.bundleProperty).isEqualTo(originalElement.bundleProperty)
            }
        }
    }

    @Test
    fun `test invalid merge of element`() {
        val elementA = FhirTransformSchemaElement("name")
        val elementB = ConverterSchemaElement("name")
        assertThat { elementA.merge(elementB) }.isFailure()
    }

    @Test
    fun `test merge of schema with unnamed element`() {
        val schemaA = FhirTransformSchema(elements = mutableListOf((FhirTransformSchemaElement())))
        val schemaB = FhirTransformSchema(elements = mutableListOf((FhirTransformSchemaElement())))
        assertThat { schemaA.merge(schemaB) }.isFailure()
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
    fun `test merge of schemas`() {
        val referencedSchema = FhirTransformSchema(
            elements = mutableListOf(
                FhirTransformSchemaElement("child1"),
                FhirTransformSchemaElement("child2"),
                FhirTransformSchemaElement("child3")
            ),
            constants = sortedMapOf(Pair("K3", "refV3"), Pair("K5", "refV5")),
        )
        referencedSchema.name = "referencedSchema"

        val baseSchema = FhirTransformSchema(
            elements = mutableListOf(
                FhirTransformSchemaElement("parent1"),
                FhirTransformSchemaElement("parent2"),
                FhirTransformSchemaElement("parent3"),
                FhirTransformSchemaElement("schemaElement", schema = "childSchema", schemaRef = referencedSchema)
            ),
            constants = sortedMapOf(Pair("K1", "baseV1"), Pair("K2", "baseV2"), Pair("K3", "baseV3")),
        )
        baseSchema.name = "baseSchema"

        val parentSchema = FhirTransformSchema(constants = sortedMapOf(Pair("K2", "parentV2")))
        parentSchema.name = "parentSchema"

        val schema = FhirTransformSchema(
            elements = mutableListOf(
                FhirTransformSchemaElement("parent1", required = true),
                FhirTransformSchemaElement("child2", condition = "condition1"),
                FhirTransformSchemaElement("newElement"),
            ),
            constants = sortedMapOf(Pair("K1", "testV1"), Pair("K4", "testV4")),
        )
        schema.name = "testSchema"

        baseSchema.merge(parentSchema).merge(schema)
        assertThat((baseSchema.elements[0]).required).isEqualTo((schema.elements[0]).required)
        assertThat(referencedSchema.elements[1].condition).isEqualTo(schema.elements[1].condition)
        assertThat(baseSchema.elements.last().name).isEqualTo(schema.elements[2].name)
        assertThat(baseSchema.name).isEqualTo("testSchema")
        assertThat(baseSchema.constants["K1"]).isEqualTo("testV1")
        assertThat(baseSchema.constants["K2"]).isEqualTo("parentV2")
        assertThat(baseSchema.constants["K3"]).isEqualTo("baseV3")
        assertThat(baseSchema.constants["K4"]).isEqualTo("testV4")
    }
}