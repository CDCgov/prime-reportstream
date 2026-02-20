package gov.cdc.prime.router.fhirengine.engine

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import gov.cdc.prime.fhirconverter.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.fhirconverter.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.helpers.RouterSchemaReferenceResolverHelper
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Test
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table

class LookupTableValueSetTests {

    @Test
    fun `test fails to read invalid lookup value set schema`() {
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

        assertFailure {
            RouterSchemaReferenceResolverHelper.retrieveFhirSchemaReference(
                "classpath:/fhir_sender_transforms/invalid_lookup_value_set.yml",
                mockk<BlobAccess.BlobContainerMetadata>()
            )
        }
        unmockkAll()
    }

    @Test
    fun `test read a FHIR Transform Schema with lookup value set from file`() {
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
            RouterSchemaReferenceResolverHelper.retrieveFhirSchemaReference(
                "classpath:/fhir_sender_transforms/lookup_value_set.yml",
                mockk<BlobAccess.BlobContainerMetadata>()
            ).isValid()
        ).isTrue()

        unmockkAll()
    }

    @Test
    fun `test transform a FHIR Transform Schema with lookup value set`() {
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

        val bundle = Bundle()
        bundle.id = "bundle1"
        val resource = Patient()
        resource.addName(HumanName().setTextElement(StringType("abc123")))
        resource.addTelecom(ContactPoint().setValueElement(StringType("abc123")))
        bundle.addEntry().resource = resource
        val resource2 = Patient()
        resource2.addName(HumanName().setTextElement(StringType("def456")))
        resource2.addTelecom(ContactPoint().setValueElement(StringType("def456")))
        bundle.addEntry().resource = resource2
        val resource3 = Patient()
        resource3.addName(HumanName().setTextElement(StringType("jkl369")))
        resource3.addTelecom(ContactPoint().setValueElement(StringType("jkl369")))
        bundle.addEntry().resource = resource3

        val valueConfig = LookupTableValueSetConfig(tableName = "table", keyColumn = "key", valueColumn = "value")
        val patientNameElement = FhirTransformSchemaElement(
            "patientNameElement",
            value = listOf("%resource.name.text"),
            resource = "%resource",
            bundleProperty = "%resource.name.text",
            valueSet = LookupTableValueSet(
                valueConfig
            )
        )
        val valueConfig2 =
            LookupTableValueSetConfig(tableName = "table", keyColumn = "key", valueColumn = "secondValue")
        val patientTelecomElement = FhirTransformSchemaElement(
            "patientTelecomElement",
            value = listOf("%resource.telecom.value"),
            resource = "%resource",
            bundleProperty = "%resource.telecom.value",
            valueSet = LookupTableValueSet(
                valueConfig2
            )
        )
        val childSchema = FhirTransformSchema(elements = mutableListOf(patientNameElement, patientTelecomElement))

        val elemA = FhirTransformSchemaElement(
            "elementA",
            resource = "Bundle.entry.resource.ofType(Patient)",
            schemaRef = childSchema,
            resourceIndex = "patientIndex",
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema, mutableListOf(), mutableListOf()).process(bundle)

        assertThat(resource.name[0].text).isEqualTo("ghi789")
        assertThat(resource.telecom[0].value).isEqualTo("ijk012")
        assertThat(resource2.name[0].text).isEqualTo("")
        assertThat(resource2.telecom[0].value).isEqualTo("lmn345")
        assertThat(resource3.name[0].text).isEqualTo("jkl369")
        assertThat(resource3.telecom[0].value).isEqualTo("jkl369")

        unmockkAll()
    }
}