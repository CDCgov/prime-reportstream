package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FHIRTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
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

        val newBundle = FhirTransformer(rootSchema).transform(bundle)
        assertThat(newBundle.id).isEqualTo("654321")
        assertThat(bundle.id).isEqualTo("654321")

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

    @Test
    fun `test transform deeper property`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        bundle.addEntry().resource = resource

        val elemA = FHIRTransformSchemaElement(
            "elementA",
            value = listOf("'First Last'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.contact.name.text"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        val newBundle = FhirTransformer(schema).transform(bundle)
        var newValue =
            FhirPathUtils.evaluate(
                CustomContext(newBundle, newBundle),
                newBundle,
                newBundle,
                "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("First Last")

        newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle),
                bundle,
                bundle,
                "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("First Last")
    }

    @Test
    fun `test transform with condition`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        bundle.addEntry().resource = resource

        val elemA = FHIRTransformSchemaElement(
            "elementA",
            value = listOf("'First Last'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            condition = "%resource.id = '654fed'",
            bundleProperty = "%resource.contact.name.text"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).transform(bundle)
        var newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue).isEmpty()

        resource.id = "654fed"
        FhirTransformer(schema).transform(bundle)
        newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("First Last")
    }

    @Test
    fun `test multi-step transform with condition`() {
        val origBundle = Bundle()
        origBundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        origBundle.addEntry().resource = resource

        // Resource doesn't exist, so make sure bundle isn't updated
        val elemA = FHIRTransformSchemaElement(
            "elementA",
            value = listOf("'First Last'"),
            resource = "Bundle.entry.resource.ofType(Patient).contact",
            bundleProperty = "%resource.name.text"
        )
        val schemaA = FhirTransformSchema(elements = mutableListOf(elemA))
        var bundle = origBundle.copy()
        FhirTransformer(schemaA).transform(bundle)
        var newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue).isEmpty()

        // Resource does exist, make sure bundle is updated with Patient.contact
        val elemB = FHIRTransformSchemaElement(
            "elementB",
            value = listOf("'other'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.contact.gender"
        )
        val schemaB = FhirTransformSchema(elements = mutableListOf(elemB))
        bundle = origBundle.copy()
        FhirTransformer(schemaB).transform(bundle)
        newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.gender"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("other")

        // In original order, same result
        val schemaC = FhirTransformSchema(elements = mutableListOf(elemA, elemB))
        bundle = origBundle.copy()
        FhirTransformer(schemaC).transform(bundle)
        newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue).isEmpty()
        newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.gender"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("other")

        // In backwards order, both updates occur
        val schemaD = FhirTransformSchema(elements = mutableListOf(elemB, elemA))
        bundle = origBundle.copy()
        FhirTransformer(schemaD).transform(bundle)
        newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("First Last")
        newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.gender"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("other")
    }

    @Test
    fun `test transform boolean property`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        bundle.addEntry().resource = resource

        val elemA = FHIRTransformSchemaElement(
            "elementA",
            value = listOf("true"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.active"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).transform(bundle)
        val newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle),
                bundle,
                bundle,
                "Bundle.entry.resource.ofType(Patient).active"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("true")
    }

    @Test
    fun `test transform extension property`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        bundle.addEntry().resource = resource

        val elemA = FHIRTransformSchemaElement(
            "elementA",
            value = listOf("'someValue'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.extension('someExtension').value[x]"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).transform(bundle)
        val newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle),
                bundle,
                bundle,
                "Bundle.entry.resource.ofType(Patient).extension('someExtension').value[0]"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("someValue")
    }

    @Test
    fun `test transform with multiple values`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        bundle.addEntry().resource = resource

        val elemA = FHIRTransformSchemaElement(
            "elementA",
            value = listOf("Bundle.entry.resource.ofType(Patient).contact.name", "%resource.contact.name", "Bundle.id"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.contact.name.text"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).transform(bundle)
        val newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle),
                bundle,
                bundle,
                "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("abc123")
    }

    @Test
    fun `test set bundle property`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        patient.id = "def456"
        bundle.addEntry().resource = patient
        val transformer = FhirTransformer(FhirTransformSchema())

        transformer.setBundleProperty(
            "Bundle.entry.resource.ofType(Patient).name.text", StringType("name"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        assertThat(patient.name[0].text).isEqualTo("name")

        transformer.setBundleProperty(
            "Bundle.entry.resource.ofType(Patient).active", BooleanType("true"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        assertThat(patient.active).isTrue()

        transformer.setBundleProperty(
            "Bundle.entry.resource.ofType(Patient).id", IdType("newId"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        assertThat(patient.id).isEqualTo("newId")

        transformer.setBundleProperty(
            "Bundle.entry.resource.ofType(Patient).extension('someExtension').value[x]", IdType("newId"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        assertThat(patient.extension[0].value).isEqualTo(IdType("newId"))
    }

    @Test
    fun `test set bundle property failures`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        patient.id = "def456"
        bundle.addEntry().resource = patient
        val transformer = FhirTransformer(FhirTransformSchema())

        // Incompatible value types
        assertThat {
            transformer.setBundleProperty(
                "Bundle.entry.resource.ofType(Patient).name.text", CodeableConcept(),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }.isFailure()
        assertThat {
            transformer.setBundleProperty(
                "Bundle.entry.resource.ofType(Patient).active", StringType("nonBoolean"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }.isFailure()

        // Can't currently create new resources on the fly
        assertThat {
            transformer.setBundleProperty(
                "Bundle.entry.resource.ofType(DiagnosticReport).status", CodeType("final"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }.isFailure()

        // Improper extension format
        assertThat {
            transformer.setBundleProperty(
                "Bundle.entry.resource.ofType(Patient).extension(regexNonMatch).value[x]", IdType("newId"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }.isFailure()
    }
}