package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isTrue
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.fhirTransformSchemaFromFile
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
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
                "lookup_value_set",
                "src/test/resources/fhir_sender_transforms",
            ).isValid()
        ).isTrue()

        assertThat {
            fhirTransformSchemaFromFile(
                "invalid_lookup_value_set",
                "src/test/resources/fhir_sender_transforms",
            )
        }.isFailure()
        unmockkAll()
    }

    @Test
    fun `test transform with lookup value set`() {
        val testTable = Table.create(
            "table",
            StringColumn.create("key", "abc123", "def456"),
            StringColumn.create("value", "ghi789", "")
        )
        val testLookupTable = LookupTable(name = "table", table = testTable)

        mockkConstructor(Metadata::class)
        every { anyConstructed<Metadata>().findLookupTable("table") } returns testLookupTable
        mockkObject(Metadata)
        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata

        val bundle = Bundle()
        bundle.id = "bundle1"
        val resource = Patient()
        resource.addName(HumanName().setTextElement(StringType("abc123")))
        bundle.addEntry().resource = resource
        val resource2 = Patient()
        resource2.addName(HumanName().setTextElement(StringType("def456")))
        bundle.addEntry().resource = resource2
        val resource3 = Patient()
        resource3.addName(HumanName().setTextElement(StringType("jkl369")))
        bundle.addEntry().resource = resource3

        val valueConfig = LookupTableValueSetConfig(tableName = "table", keyColumn = "key", valueColumn = "value")

        val patientElement = FhirTransformSchemaElement(
            "patientElement",
            value = listOf("%resource.name.text"),
            resource = "%resource",
            bundleProperty = "%resource.name.text",
            valueSet = LookupTableValueSet(
                valueConfig
            )
        )
        val childSchema = FhirTransformSchema(elements = mutableListOf(patientElement))

        val elemA = FhirTransformSchemaElement(
            "elementA",
            resource = "Bundle.entry.resource.ofType(Patient)",
            schemaRef = childSchema,
            resourceIndex = "patientIndex",
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).transform(bundle)

        assertThat(resource.name[0].text).isEqualTo("ghi789")
        assertThat(resource2.name[0].text).isEqualTo("")
        assertThat(resource3.name[0].text).isEqualTo("jkl369")

        unmockkAll()
    }
}