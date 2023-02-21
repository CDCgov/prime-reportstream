package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FHIRTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import org.hl7.fhir.r4.model.Bundle
import kotlin.test.Test

class FhirTransformerTests {

    @Test
    fun `test transform with nested schemas`() {
        val bundle = Bundle()
        bundle.id = "abc123"

        // check for dupes in various scenarios:
        // root -> A -> C
        //      -> B
        val elemB =
            FHIRTransformSchemaElement("elementB", value = listOf("'654321'"), bundleProperty = "%resource.id")
        val elemC =
            FHIRTransformSchemaElement(
                "elementC",
                value = listOf("'654321'"),
                bundleProperty = "%resource.id"
            )

        val childSchema = FhirTransformSchema(elements = mutableListOf(elemC))
        val elemA = FHIRTransformSchemaElement("elementA", schema = "elementC", schemaRef = childSchema)

        val rootSchema =
            FhirTransformSchema(elements = mutableListOf(elemA, elemB))

        // nobody sharing the same name
        assertThat(FhirTransformer(rootSchema).transform(bundle).isEmpty).isFalse()

        var newBundle = FhirTransformer(rootSchema).transform(bundle)
        assertThat(newBundle.id).isEqualTo("654321")

        // B/C sharing the same name
        elemC.name = "elementB"
        assertThat { FhirTransformer(rootSchema).transform(bundle) }.isFailure()
            .hasClass(SchemaException::class.java)

        // A/B sharing the same name
        elemC.name = "elementC"
        elemA.name = "elementB"
        assertThat { FhirTransformer(rootSchema).transform(bundle) }.isFailure()
            .hasClass(SchemaException::class.java)

        // A/C sharing the same name
        elemA.name = "elementC"
        assertThat { FhirTransformer(rootSchema).transform(bundle) }.isFailure()
            .hasClass(SchemaException::class.java)
    }

//    @Test
//    fun `test transform deeper property`() {
//        val bundle = Bundle()
//        bundle.id = "abc123"
//
//        val elemA = FHIRTransformSchemaElement(
//            "elementA",
//            value = listOf("'Somefirstname Somelastname'"),
//            resource = "Bundle.entry.resource.ofType(Patient).contact",
//            bundleProperty = "%resource.name.text"
//        )
//
//        val schema = FhirTransformSchema(elements = mutableListOf(elemA))
//
//        // nobody sharing the same name
//        assertThat(FhirTransformer(schema).transform(bundle).isEmpty).isFalse()
//
//        var newBundle = FhirTransformer(schema).transform(bundle)
//        assertThat(newBundle).isEqualTo("654321")
//    }
}