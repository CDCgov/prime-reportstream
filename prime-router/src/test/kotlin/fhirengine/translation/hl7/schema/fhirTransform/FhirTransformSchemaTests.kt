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

        val goodElement = FHIRTransformSchemaElement(value = listOf("final"), bundleProperty = "%resource.status")
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
        var element = FHIRTransformSchemaElement()
        assertThat(element.validate()).isNotEmpty()

        element = FHIRTransformSchemaElement("name")
        assertThat(element.validate()).isNotEmpty()

        element = FHIRTransformSchemaElement("name", value = listOf("final"))
        assertThat(element.validate()).isNotEmpty()

        element = FHIRTransformSchemaElement("name", value = listOf("final"), bundleProperty = "%resource.status")
        assertThat(element.validate()).isEmpty()

        element =
            FHIRTransformSchemaElement("name", value = listOf("final", "partial"), bundleProperty = "%resource.status")
        assertThat(element.validate()).isEmpty()

        element = FHIRTransformSchemaElement(
            "name", value = listOf("final"), schema = "schema", bundleProperty = "%resource.status"
        )
        assertThat(element.validate()).isNotEmpty()

        element = FHIRTransformSchemaElement("name", schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = FHIRTransformSchemaElement("name", value = listOf("final"), schema = "schema")
        assertThat(element.validate()).isNotEmpty()

        element = FHIRTransformSchemaElement(
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
                FHIRTransformSchemaElement(
                    "name",
                    value = listOf("final"),
                    bundleProperty = "%resource.status"
                )
            )
        )
        element = FHIRTransformSchemaElement(
            "name", schema = "someSchema", schemaRef = aSchema, resourceIndex = "someIndex"
        )
        assertThat(element.validate()).isNotEmpty()
        element = FHIRTransformSchemaElement(
            "name", schema = "someSchema", schemaRef = aSchema, resourceIndex = "someIndex",
            resource = "someResource"
        )
        assertThat(element.validate()).isEmpty()
        element = FHIRTransformSchemaElement(
            "name", value = listOf("someValue"), resourceIndex = "someIndex",
            resource = "someResource"
        )
        assertThat(element.validate()).isNotEmpty()

        // Check on constants
        element = FHIRTransformSchemaElement(
            "name", value = listOf("someValue"), constants = sortedMapOf("const1" to ""),
            bundleProperty = "%resource.status"
        )
        val errors = element.validate()
        assertThat(errors).isEmpty()

        element = FHIRTransformSchemaElement(
            "name", value = listOf("someValue"),
            constants = sortedMapOf("const1" to "value"),
            bundleProperty = "%resource.status"
        )
        assertThat(element.validate()).isEmpty()
    }

    @Test
    fun `test validate schema with schemas`() {
        val goodElement = FHIRTransformSchemaElement(
            "name", value = listOf("final"),
            bundleProperty = "%resource.status"
        )
        var childSchema = FhirTransformSchema(elements = mutableListOf(goodElement))
        var elementWithSchema = FHIRTransformSchemaElement("name", schema = "schemaName", schemaRef = childSchema)
        var topSchema = FhirTransformSchema(mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isTrue()
        assertThat(topSchema.errors).isEmpty()

        var badElement = FHIRTransformSchemaElement("name", value = listOf("final")) // No bundleProperty = error
        childSchema = FhirTransformSchema(elements = mutableListOf(badElement))
        elementWithSchema = FHIRTransformSchemaElement("name", schema = "schemaName", schemaRef = childSchema)
        topSchema = FhirTransformSchema(mutableListOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()
    }

    @Test
    fun `test merge of element`() {
        fun newParent(): FHIRTransformSchemaElement {
            return FHIRTransformSchemaElement(
                "name", condition = "condition1",
                schema = "schema1", schemaRef = FhirTransformSchema(), resource = "resource1", resourceIndex = "index1",
                value = listOf("value1"), constants = sortedMapOf("k1" to "v1"), bundleProperty = "%resource.status"
            )
        }

        val originalElement = newParent()
        val elementA = FHIRTransformSchemaElement("name")
        val parentElement = newParent().merge(elementA)
        assertThat(parentElement is FHIRTransformSchemaElement)
        if (parentElement is FHIRTransformSchemaElement) {
            assertAll {
                assertThat(parentElement.condition).isEqualTo(originalElement.condition)
                assertThat(parentElement.schema).isEqualTo(originalElement.schema)
                assertThat(parentElement.schemaRef?.name).isEqualTo(originalElement.schemaRef?.name)
                assertThat(parentElement.resource).isEqualTo(originalElement.resource)
                assertThat(parentElement.resourceIndex).isEqualTo(originalElement.resourceIndex)
                assertThat(parentElement.value.size).isEqualTo(originalElement.value.size)
                assertThat(parentElement.constants.size).isEqualTo(originalElement.constants.size)
                assertThat(parentElement.bundleProperty).isEqualTo(originalElement.bundleProperty)
                parentElement.value
                    .forEachIndexed { index, value -> assertThat(originalElement.value[index]).isEqualTo(value) }
                parentElement.constants
                    .forEach { (key, value) -> assertThat(originalElement.constants[key]).isEqualTo(value) }
            }
        }

        val elementB = FHIRTransformSchemaElement(
            "name",
            condition = "condition2",
            schema = "schema2",
            schemaRef = FhirTransformSchema(),
            resource = "resource2",
            resourceIndex = "index2",
            bundleProperty = "%resource.status"
        )
        val parentElementB = newParent().merge(elementB)
        assertThat(parentElementB is FHIRTransformSchemaElement)
        if (parentElementB is FHIRTransformSchemaElement) {
            assertAll {
                assertThat(parentElementB.condition).isEqualTo(elementB.condition)
                assertThat(parentElementB.schema).isEqualTo(elementB.schema)
                assertThat(parentElementB.schemaRef).isEqualTo(elementB.schemaRef)
                assertThat(parentElementB.resource).isEqualTo(elementB.resource)
                assertThat(parentElementB.resourceIndex).isEqualTo(elementB.resourceIndex)
                assertThat(parentElementB.value.size).isEqualTo(originalElement.value.size)
                assertThat(parentElementB.constants.size).isEqualTo(originalElement.constants.size)
                assertThat(parentElementB.bundleProperty).isEqualTo(originalElement.bundleProperty)
                parentElementB.value
                    .forEachIndexed { index, value -> assertThat(originalElement.value[index]).isEqualTo(value) }
                parentElementB.constants
                    .forEach { (key, value) -> assertThat(originalElement.constants[key]).isEqualTo(value) }
            }
        }

        val elementC = FHIRTransformSchemaElement(
            "name", condition = "condition3",
            schema = "schema3", schemaRef = FhirTransformSchema(), resource = "resource3", resourceIndex = "index3",
            value = listOf("value3"), constants = sortedMapOf("k3" to "v3"), bundleProperty = "%resource.status"
        )
        val parentElementC = newParent().merge(elementC)

        assertThat(parentElementC is FHIRTransformSchemaElement)
        if (parentElementC is FHIRTransformSchemaElement) {
            assertAll {
                assertThat(parentElementC.condition).isEqualTo(elementC.condition)
                assertThat(parentElementC.schema).isEqualTo(elementC.schema)
                assertThat(parentElementC.schemaRef).isEqualTo(elementC.schemaRef)
                assertThat(parentElementC.resource).isEqualTo(elementC.resource)
                assertThat(parentElementC.resourceIndex).isEqualTo(elementC.resourceIndex)
                assertThat(parentElementC.value.size).isEqualTo(elementC.value.size)
                assertThat(parentElementC.constants.size).isEqualTo(elementC.constants.size)
                assertThat(parentElementC.bundleProperty).isEqualTo(originalElement.bundleProperty)
                parentElementC.value
                    .forEachIndexed { index, value -> assertThat(elementC.value[index]).isEqualTo(value) }
                parentElementC.constants
                    .forEach { (key, value) -> assertThat(elementC.constants[key]).isEqualTo(value) }
            }
        }
    }

    @Test
    fun `test invalid merge of element`() {
        val elementA = FHIRTransformSchemaElement("name")
        val elementB = ConverterSchemaElement("name")
        assertThat { elementA.merge(elementB) }.isFailure()
    }

    @Test
    fun `test find element`() {
        val childSchema = FhirTransformSchema(
            elements = mutableListOf(
                FHIRTransformSchemaElement("child1"),
                FHIRTransformSchemaElement("child2"),
                FHIRTransformSchemaElement("child3")
            )
        )
        val schema = FhirTransformSchema(
            elements = mutableListOf(
                FHIRTransformSchemaElement("parent1"),
                FHIRTransformSchemaElement("parent2"),
                FHIRTransformSchemaElement("parent3"),
                FHIRTransformSchemaElement("schemaElement", schema = "childSchema", schemaRef = childSchema)
            )
        )

        assertThat(schema.findElement("parent2")).isEqualTo(schema.elements[1])
        assertThat(schema.findElement("child2")).isEqualTo(childSchema.elements[1])
    }

    @Test
    fun `test merge of schemas`() {
        val childSchema = FhirTransformSchema(
            elements = mutableListOf(
                FHIRTransformSchemaElement("child1"),
                FHIRTransformSchemaElement("child2"),
                FHIRTransformSchemaElement("child3")
            )
        )
        val schema = FhirTransformSchema(
            elements = mutableListOf(
                FHIRTransformSchemaElement("parent1"),
                FHIRTransformSchemaElement("parent2"),
                FHIRTransformSchemaElement("parent3"),
                FHIRTransformSchemaElement("schemaElement", schema = "childSchema", schemaRef = childSchema)
            )
        )

        val extendedSchema = FhirTransformSchema(
            elements = mutableListOf(
                FHIRTransformSchemaElement("parent1"),
                FHIRTransformSchemaElement("child2", condition = "condition1"),
                FHIRTransformSchemaElement("newElement1"),
            )
        )

        schema.merge(extendedSchema)
        assertThat(childSchema.elements[1].condition).isEqualTo(extendedSchema.elements[1].condition)
        assertThat(schema.elements.last().name).isEqualTo(extendedSchema.elements[2].name)
    }
}