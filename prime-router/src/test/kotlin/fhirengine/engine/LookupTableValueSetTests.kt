package gov.cdc.prime.router.fhirengine.engine

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.fhirTransformSchemaFromFile
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Test
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table

class LookupTableValueSetTests {
    @Test
    fun `test read extended FHIR Transform from file`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

        val testTable = Table.create(
            "lookuptable",
            StringColumn.create("key", "abc123", "def456"),
            StringColumn.create("value", "ghi789", "")
        )
        val testLookupTable = LookupTable(name = "lookuptable", table = testTable)

        mockkConstructor(Metadata::class)
        every { anyConstructed<Metadata>().findLookupTable("lookuptable") } returns testLookupTable
        mockkObject(Metadata)
        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata

        assertThat(
            fhirTransformSchemaFromFile(
                "classpath:/fhir_sender_transforms/lookup_value_set.yml",
                blobConnectionInfo = mockk<BlobAccess.BlobContainerMetadata>()
            ).isValid()
        ).isTrue()

        assertFailure {
            fhirTransformSchemaFromFile(
                "classpath:/fhir_sender_transforms/invalid_lookup_value_set.yml",

                blobConnectionInfo = mockk<BlobAccess.BlobContainerMetadata>()
            )
        }
        unmockkAll()
    }

    @Test
    fun `test transform with lookup value set`() {
        val testTable = Table.create(
            "table",
            StringColumn.create("key", "abc123", "def456"),
            StringColumn.create("value", "ghi789", ""),
            StringColumn.create("secondValue", "ijk012", "lmn345")
        )
        val testLookupTable = LookupTable(name = "table", table = testTable)

        mockkConstructor(Metadata::class)
        every { anyConstructed<Metadata>().findLookupTable("table") } returns testLookupTable
        mockkObject(Metadata)
        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata

        var bundle = Bundle()
        bundle.id = "bundle1"
        var resource = Patient()
        resource.addName(HumanName().setTextElement(StringType("abc123")))
        bundle.addEntry().resource = resource
        var resource2 = Patient()
        resource2.addName(HumanName().setTextElement(StringType("def456")))
        bundle.addEntry().resource = resource2
        var resource3 = Patient()
        resource3.addName(HumanName().setTextElement(StringType("jkl369")))
        bundle.addEntry().resource = resource3

        var valueConfig = LookupTableValueSetConfig(tableName = "table", keyColumn = "key", valueColumn = "value")

        var patientElement = FhirTransformSchemaElement(
            "patientElement",
            value = listOf("%resource.name.text"),
            resource = "%resource",
            bundleProperty = "%resource.name.text",
            valueSet = LookupTableValueSet(
                valueConfig
            )
        )
        var childSchema = FhirTransformSchema(elements = mutableListOf(patientElement))

        val elemA = FhirTransformSchemaElement(
            "elementA",
            resource = "Bundle.entry.resource.ofType(Patient)",
            schemaRef = childSchema,
            resourceIndex = "patientIndex",
        )

        var schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).process(bundle)

        assertThat(resource.name[0].text).isEqualTo("ghi789")
        assertThat(resource2.name[0].text).isEqualTo("")
        assertThat(resource3.name[0].text).isEqualTo("jkl369")

        // Test getting the second column from the same table.

        bundle = Bundle()
        bundle.id = "bundle2"
        resource = Patient()
        resource.addName(HumanName().setTextElement(StringType("def456")))
        bundle.addEntry().resource = resource
        resource2 = Patient()
        resource2.addName(HumanName().setTextElement(StringType("abc123")))
        bundle.addEntry().resource = resource2
        resource3 = Patient()
        resource3.addName(HumanName().setTextElement(StringType("jkl369")))
        bundle.addEntry().resource = resource3

        valueConfig = LookupTableValueSetConfig(tableName = "table", keyColumn = "key", valueColumn = "secondValue")

        patientElement = FhirTransformSchemaElement(
            "patientElement",
            value = listOf("%resource.name.text"),
            resource = "%resource",
            bundleProperty = "%resource.name.text",
            valueSet = LookupTableValueSet(
                valueConfig
            )
        )

        childSchema = FhirTransformSchema(elements = mutableListOf(patientElement))

        val elemB = FhirTransformSchemaElement(
            "elementB",
            resource = "Bundle.entry.resource.ofType(Patient)",
            schemaRef = childSchema,
            resourceIndex = "patientIndex",
        )

        schema = FhirTransformSchema(elements = mutableListOf(elemB))

        FhirTransformer(schema).process(bundle)

        assertThat(resource.name[0].text).isEqualTo("lmn345")
        assertThat(resource2.name[0].text).isEqualTo("ijk012")
        assertThat(resource3.name[0].text).isEqualTo("jkl369")

        unmockkAll()
    }
}