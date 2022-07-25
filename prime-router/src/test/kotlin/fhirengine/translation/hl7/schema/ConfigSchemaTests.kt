package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlin.test.Test

class ConfigSchemaTests {
    @Test
    fun `test validate schema`() {
        var schema = ConfigSchema()
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema("name")
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema("name", "ORU_R01")
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema("name", "ORU_R01", "2.5.1")
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        val goodElement = ConfigSchemaElement("name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7"))
        schema = ConfigSchema("name", "ORU_R01", "2.5.1", listOf(goodElement))
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.errors).isEmpty()

        // A child schema
        schema = ConfigSchema("name", "ORU_R01", "2.5.1", listOf(goodElement))
        assertThat(schema.validate(true)).isNotEmpty()

        // A bad type
        schema = ConfigSchema("name", "VAT", "2.5.1", listOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema(null, "ORU_R01", "2.5.1", listOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema("name", null, "2.5.1", listOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.errors).isNotEmpty()

        schema = ConfigSchema("name", "ORU_R01", null, listOf(goodElement))
        assertThat(schema.isValid()).isFalse()
        assertThat(schema.isValid()).isFalse() // We check again to make sure we get the same value
        assertThat(schema.errors).isNotEmpty()
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
        assertThat(element.valueExpressions.size).isEqualTo(2)

        element = ConfigSchemaElement("name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7"), schema = "schema")
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
        assertThat(element.valueExpressions).isNotNull()
        assertThat(element.resourceExpression).isNotNull()
        assertThat(element.conditionExpression).isNotNull()

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
    }

    @Test
    fun `test validate schema with schemas`() {
        var goodElement = ConfigSchemaElement("name", value = listOf("Bundle"), hl7Spec = listOf("MSH-7"))
        var childSchema = ConfigSchema("name", elements = listOf(goodElement))
        var elementWithSchema = ConfigSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        var topSchema = ConfigSchema("name", "ORU_R01", "2.5.1", listOf(elementWithSchema))
        assertThat(topSchema.isValid()).isTrue()
        assertThat(topSchema.errors).isEmpty()

        goodElement = ConfigSchemaElement("name", value = listOf("Bundle")) // No HL7Spec = error
        childSchema = ConfigSchema("name", elements = listOf(goodElement))
        elementWithSchema = ConfigSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConfigSchema("name", "ORU_R01", "2.5.1", listOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()

        childSchema = ConfigSchema("name", hl7Version = "2.5.1", elements = listOf(goodElement))
        elementWithSchema = ConfigSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConfigSchema("name", "ORU_R01", "2.5.1", listOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()

        childSchema = ConfigSchema("name", hl7Type = "ORU_R01", elements = listOf(goodElement))
        elementWithSchema = ConfigSchemaElement("name", schema = "schemaname", schemaRef = childSchema)
        topSchema = ConfigSchema("name", "ORU_R01", "2.5.1", listOf(elementWithSchema))
        assertThat(topSchema.isValid()).isFalse()
        assertThat(topSchema.errors).isNotEmpty()
    }
}