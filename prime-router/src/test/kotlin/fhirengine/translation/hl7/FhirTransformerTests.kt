package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.fhirconverter.translation.hl7.InlineValueSet
import gov.cdc.prime.fhirconverter.translation.hl7.schema.ConfigSchemaElementProcessingException
import gov.cdc.prime.fhirconverter.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.fhirconverter.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.fhirconverter.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.fhirconverter.translation.hl7.schema.fhirTransform.FhirTransformSchemaElementAction
import gov.cdc.prime.fhirconverter.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.helpers.RouterSchemaReferenceResolverHelper
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.apache.logging.log4j.kotlin.KotlinLogger
import org.hl7.fhir.r4.model.Annotation
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.MarkdownType
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FhirTransformerTests {

    /**
     * Return a FhirTransformer and a mocked KotlinLogger using the given [schema].
     */
    private fun setupFhirTransformer(schema: FhirTransformSchema): Pair<FhirTransformer, KotlinLogger> {
        val logger = mockkClass(KotlinLogger::class)
        val transformer = spyk(FhirTransformer(schema))
        every { transformer.logger }.returns(logger)
        every { logger.warn(any<String>()) }.returns(Unit)
        every { logger.error(any<String>()) }.returns(Unit)
        every { logger.trace(any<String>()) }.returns(Unit)
        return Pair(transformer, logger)
    }

    private fun verifyErrorAndResetLogger(logger: KotlinLogger) {
        verify(exactly = 1) { logger.error(any<String>()) }
        clearMocks(logger, answers = false)
    }

    private fun verifyDebugAndResetLogger(logger: KotlinLogger) {
        verify { logger.debug(any<String>()) }
        clearMocks(logger, answers = false)
    }

    @Test
    fun `test get value`() {
        val mockSchema = mockk<FhirTransformSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "abc123"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirTransformer(mockSchema)

        var element = FhirTransformSchemaElement("name", value = listOf("Bundle.id", "Bundle.timestamp"))
        assertThat(converter.getValue(element, bundle, bundle, customContext)).isEqualTo(IdType(bundle.id))

        element = FhirTransformSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.id"))
        assertThat(converter.getValue(element, bundle, bundle, customContext)).isEqualTo(IdType(bundle.id))

        element = FhirTransformSchemaElement("name", value = listOf("Bundle.timestamp", "Bundle.timestamp"))
        assertThat(converter.getValue(element, bundle, bundle, customContext)).isNull()
    }

    @Test
    fun `test get value from primitive type`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val diagnosticReport = DiagnosticReport()
        diagnosticReport.issued = Date()
        bundle.addEntry().resource = diagnosticReport
        val customContext = CustomContext(bundle, bundle)
        val element = FhirTransformSchemaElement(
            "name",
            value = listOf("Bundle.entry.resource.ofType(DiagnosticReport).issued")
        )
        val transformer = FhirTransformer(FhirTransformSchema())

        val value = transformer.getValue(element, bundle, bundle, customContext)
        assertThat(value).isNotSameInstanceAs(diagnosticReport.issued)
    }

    @Test
    fun `test get value from value set`() {
        val mockSchema = mockk<FhirTransformSchema>() // Just a dummy schema to pass around
        val bundle = Bundle()
        bundle.id = "stagnatious"
        val customContext = CustomContext(bundle, bundle)
        val converter = FhirTransformer(mockSchema)

        val valueSet = InlineValueSet(
            sortedMapOf(
                Pair("Stagnatious", "S"), // casing should not matter
                Pair("grompfle", "G")
            )
        )

        var element = FhirTransformSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        var value = converter.getValue(element, bundle, bundle, customContext)
        assertThat(value).isNotNull().isInstanceOf(StringType::class.java)
        assertThat(value?.primitiveValue()).isEqualTo("S")

        bundle.id = "grompfle"
        element = FhirTransformSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        value = converter.getValue(element, bundle, bundle, customContext)
        assertThat(value).isNotNull().isInstanceOf(StringType::class.java)
        assertThat(value?.primitiveValue()).isEqualTo("G")

        bundle.id = "GRompfle" // verify case insensitivity
        element = FhirTransformSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        value = converter.getValue(element, bundle, bundle, customContext)
        assertThat(value).isNotNull().isInstanceOf(StringType::class.java)
        assertThat(value?.primitiveValue()).isEqualTo("G")

        bundle.id = "unmapped"
        element = FhirTransformSchemaElement("name", value = listOf("Bundle.id"), valueSet = valueSet)
        value = converter.getValue(element, bundle, bundle, customContext)
        assertThat(value).isNull()

        element = FhirTransformSchemaElement("name", value = listOf("unmapped"), valueSet = valueSet)
        assertThat(converter.getValue(element, bundle, bundle, customContext)).isNull()
    }

    @Test
    fun `test transform with nested schemas`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        bundle.addEntry().resource = resource

        val elemB =
            FhirTransformSchemaElement("elementB", value = listOf("'654321'"), bundleProperty = "%resource.id")
        val elemC =
            FhirTransformSchemaElement(
                "elementC",
                value = listOf("'fedcba'"),
                bundleProperty = "Bundle.entry.resource.ofType(Patient).id"
            )

        val childSchema = FhirTransformSchema(elements = mutableListOf(elemB, elemC))
        val elemA = FhirTransformSchemaElement("elementA", schema = "schema", schemaRef = childSchema)

        val rootSchema = FhirTransformSchema(elements = mutableListOf(elemA))

        val newBundle = FhirTransformer(rootSchema).process(bundle)
        assertThat(newBundle.id).isEqualTo("654321")
        assertThat(newBundle.entry[0].resource.id).isEqualTo("fedcba")
    }

    @Test
    fun `test transform with nested schemas and override duplicate elements`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        bundle.addEntry().resource = resource

        // root: A -> child: B
        //       B           B
        val elemB1 =
            FhirTransformSchemaElement("elementB", value = listOf("'654321'"), bundleProperty = "%resource.id")
        val elemB2 =
            FhirTransformSchemaElement(
                "elementB",
                value = listOf("'fedcba'"),
                bundleProperty = "Bundle.entry.resource.ofType(Patient).id"
            )

        val childSchema = FhirTransformSchema(elements = mutableListOf(elemB1, elemB2))
        val elemA = FhirTransformSchemaElement("elementA", schema = "schemaB2", schemaRef = childSchema)

        val rootSchema = FhirTransformSchema(elements = mutableListOf(elemA))

        val elemBOverride = FhirTransformSchemaElement("elementB", value = listOf("'overrideVal'"))
        val overrideSchema = FhirTransformSchema(elements = mutableListOf(elemBOverride))
        rootSchema.override(overrideSchema)

        val newBundle = FhirTransformer(rootSchema).process(bundle)
        assertThat(newBundle.id).isEqualTo("overrideVal")
        assertThat(newBundle.entry[0].resource.id).isEqualTo("overrideVal")
    }

    @Test
    fun `test transform deeper property`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        bundle.addEntry().resource = resource

        val elemA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("'First Last'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.contact.name.text"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        val newBundle = FhirTransformer(schema).process(bundle)
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

        val elemA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("'First Last'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            condition = "%resource.id = '654fed'",
            bundleProperty = "%resource.contact.name.text"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).process(bundle)
        var newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue).isEmpty()

        resource.id = "654fed"
        FhirTransformer(schema).process(bundle)
        newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("First Last")
    }

    @Test
    fun `test transform schema error`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        bundle.addEntry().resource = resource

        val elemA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("'2.9'"),
            bundleProperty = "Bundle.entry.resource.ofType(Patient)" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/ethnic-group\")" +
                ".value.coding.version"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))
        schema.name = "ErrorSchema"

        val ex = assertFailsWith<ConfigSchemaElementProcessingException> {
            FhirTransformer(schema).process(bundle)
        }
        assertThat(ex.message).isEqualTo(
            "Error encountered while applying: elementA in ErrorSchema to FHIR bundle. " +
                "\nError was: Attempt to add child with unknown name value"
        )
    }

    @Test
    fun `test multi-step transform with condition`() {
        val origBundle = Bundle()
        origBundle.id = "abc123"
        val resource = Patient()
        resource.id = "def456"
        origBundle.addEntry().resource = resource

        // Resource doesn't exist, so make sure bundle isn't updated
        val elemA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("'First Last'"),
            resource = "Bundle.entry.resource.ofType(Patient).contact",
            bundleProperty = "%resource.name.text"
        )
        val schemaA = FhirTransformSchema(elements = mutableListOf(elemA))
        var bundle = origBundle.copy()
        FhirTransformer(schemaA).process(bundle)
        var newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.name.text"
            )
        assertThat(newValue).isEmpty()

        // Resource does exist, make sure bundle is updated with Patient.contact
        val elemB = FhirTransformSchemaElement(
            "elementB",
            value = listOf("'other'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.contact.gender"
        )
        val schemaB = FhirTransformSchema(elements = mutableListOf(elemB))
        bundle = origBundle.copy()
        FhirTransformer(schemaB).process(bundle)
        newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle), bundle, bundle, "Bundle.entry.resource.ofType(Patient).contact.gender"
            )
        assertThat(newValue[0].primitiveValue()).isEqualTo("other")

        // In original order, same result
        val schemaC = FhirTransformSchema(elements = mutableListOf(elemA, elemB))
        bundle = origBundle.copy()
        FhirTransformer(schemaC).process(bundle)
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
        FhirTransformer(schemaD).process(bundle)
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

        val elemA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("true"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.active"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).process(bundle)
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
        val extension = "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority-universal-id"
        resource.id = "def456"
        bundle.addEntry().resource = resource

        val elemA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("'someValue'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.extension('$extension').value[x]"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).process(bundle)
        val newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle),
                bundle,
                bundle,
                "Bundle.entry.resource.ofType(Patient).extension('$extension').value[0]"
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

        val elemA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("Bundle.entry.resource.ofType(Patient).contact.name", "%resource.contact.name", "Bundle.id"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.contact.name.text"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).process(bundle)
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
    fun `test transform with value set`() {
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

        val patientElement = FhirTransformSchemaElement(
            "patientElement",
            value = listOf("%resource.name.text"),
            resource = "%resource",
            bundleProperty = "%resource.name.text",
            valueSet = InlineValueSet(
                sortedMapOf(
                    Pair("abc123", "ghi789"),
                    Pair("def456", "")
                )
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

        FhirTransformer(schema).process(bundle)

        assertThat(resource.name[0].text).isEqualTo("ghi789")
        assertThat(resource2.name[0].text).isEqualTo("")
        assertThat(resource3.name[0].text).isEqualTo("jkl369")
    }

    @Test
    fun `test set bundle property in nested extension`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val serviceRequest = ServiceRequest()
        bundle.addEntry().resource = serviceRequest
        val transformer = FhirTransformer(FhirTransformSchema())
        transformer.updateBundle(
            "Bundle.entry.resource.ofType(ServiceRequest).requester.extension('callback-number')" +
                ".valueString.extension('hl7v2Name').value[x]",
            StringType("hl7v2 use"),
            CustomContext(bundle, bundle), bundle, bundle
        )

        assertThat(
            serviceRequest
                .requester
                .getExtensionByUrl("callback-number").value
                .getExtensionByUrl("hl7v2Name")
                .value.primitiveValue()
        ).isEqualTo("hl7v2 use")
    }

    @Test
    fun `test set bundle property`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        patient.id = "def456"
        bundle.addEntry().resource = patient
        val transformer = FhirTransformer(FhirTransformSchema())

        transformer.updateBundle(
            "Bundle.entry.resource.ofType(Patient).name.text", StringType("name"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        assertThat(patient.name[0].text).isEqualTo("name")

        transformer.updateBundle(
            "Bundle.entry.resource.ofType(Patient).active", BooleanType("true"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        assertThat(patient.active).isTrue()

        transformer.updateBundle(
            "Bundle.entry.resource.ofType(Patient).id", IdType("newId"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        assertThat(patient.id).isEqualTo("newId")

        transformer.updateBundle(
            "Bundle.entry.resource.ofType(Patient).extension('someExtension').value[x]", IdType("newId"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        assertThat(patient.extension[0].value).isEqualTo(IdType("newId"))
    }

    @Test
    fun `test set bundle property failures`() {
        val (transformer, logger) = setupFhirTransformer(FhirTransformSchema())

        val bundle = Bundle()
        bundle.id = "abc123"

        // Can't currently create entry on the fly
        assertFailure {
            transformer.updateBundle(
                "Bundle.entry.resource.ofType(DiagnosticReport).status", CodeType("final"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }

        val patient = Patient()
        patient.id = "def456"
        bundle.addEntry().resource = patient

        // Can't currently create new resources on the fly
        assertFailure {
            transformer.updateBundle(
                "Bundle.entry.resource.ofType(DiagnosticReport).status", CodeType("final"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }

        // Extension matcher is provided with a non-string value
        assertFailure {
            transformer.updateBundle(
                "Bundle.entry.resource.ofType(Patient).extension(regexNonMatch).value[x]", IdType("newId"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }

        // Invalid bundleProperties
        assertFailure {
            transformer.updateBundle(
                "", IdType("newId"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }

        verifyErrorAndResetLogger(logger)

            transformer.updateBundle(
                "%key.text", StringType("SomeName"),
                CustomContext(
                    bundle,
                    bundle,
                    constants = mutableMapOf(Pair("key", "Bundle.entry.resource.ofType(Patient).name"))
                ),
                bundle, bundle
            )

        // Incompatible value types
        assertFailure {
            transformer.updateBundle(
                "Bundle.entry.resource.ofType(Patient).name.text", CodeableConcept(),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }

        assertFailure {
            transformer.updateBundle(
                "Bundle.entry.resource.ofType(Patient).active", StringType("nonBoolean"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }
    }

    @Test
    fun `test validate and split bundleProperty`() {
        val (transformer, logger) = setupFhirTransformer(FhirTransformSchema())

        assertFailure {
            transformer.validateAndSplitBundleProperty("")
        }
        verifyErrorAndResetLogger(logger)

        assertFailure {
            transformer.validateAndSplitBundleProperty("Bundle.entry.resource.ofType(Patient).name.%key")
        }
        verifyErrorAndResetLogger(logger)
    }

    @Test
    fun `test split bundleProperty`() {
        val transformer = FhirTransformer(FhirTransformSchema())

        assertThat(transformer.splitBundlePropertyPath("")).isEmpty()

        assertThat(transformer.splitBundlePropertyPath("id").count()).isEqualTo(1)

        assertThat(
            transformer.splitBundlePropertyPath("Bundle.entry.resource.ofType(Patient).name.%key").count()
        ).isEqualTo(6)

        val extension = "https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority-universal-id"
        assertThat(
            transformer.splitBundlePropertyPath("%resource.extension('$extension').value[x]").count()
        ).isEqualTo(3)
    }

    @Test
    fun `test schema level constants`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        patient.id = "def456"
        patient.name = listOf(HumanName())
        bundle.addEntry().resource = patient

        val elementA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("%testId"),
            resource = "%testRes",
            bundleProperty = "%testRes.id"
        )
        val elementB = FhirTransformSchemaElement(
            "elementB",
            value = listOf("%testName"),
            resource = "%testRes",
            bundleProperty = "%testPath.text"
        )
        val elementC = FhirTransformSchemaElement(
            "elementC",
            value = listOf("%testActive"),
            resource = "%testRes",
            bundleProperty = "%resource.active"
        )
        val schema =
            FhirTransformSchema(
                elements = mutableListOf(elementA, elementB, elementC),
                constants = sortedMapOf(
                    Pair("testId", "'12345'"),
                    Pair("testName", "'SomeName'"),
                    Pair("testActive", "true"),
                    Pair("testRes", "Bundle.entry.resource.ofType(Patient)"),
                    Pair("testPath", "Bundle.entry.resource.ofType(Patient).name[0]"),
                )
            )
        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(patient.id).isEqualTo("12345")
        assertThat(patient.name[0].text).isEqualTo("SomeName")
        assertThat(patient.active).isTrue()
    }

    @Test
    fun `test element level constants`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        patient.id = "def456"
        bundle.addEntry().resource = patient

        val elementA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("%testId"),
            resource = "%testRes",
            bundleProperty = "%resource.id",
            constants = sortedMapOf(
                Pair("testId", "'12345'"),
                Pair("testRes", "Bundle.entry.resource.ofType(Patient)"),
            ),
        )
        val elementB = FhirTransformSchemaElement(
            "elementB",
            value = listOf("%testId", "'backupValue'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.name.text",
        )
        val schema =
            FhirTransformSchema(
                elements = mutableListOf(elementA, elementB),
            )
        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(patient.id).isEqualTo("12345")
        assertThat(patient.name[0].text).isEqualTo("backupValue")
    }

    @Test
    fun `test constant inheritance`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        patient.id = "def456"
        bundle.addEntry().resource = patient

        val elementA = FhirTransformSchemaElement(
            "elementA",
            value = listOf("%testActive"),
            resource = "%testRes",
            bundleProperty = "%resource.active",
            constants = sortedMapOf(
                Pair("testId", "'12345'"),
                Pair("testRes", "Bundle.entry.resource.ofType(Patient)"),
                Pair("testActive", "true"),
            ),
        )
        val elementB = FhirTransformSchemaElement(
            "elementB",
            value = listOf("%testId", "%otherTestId", "%defaultName", "'none'"),
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.name.text",
        )

        val childElement = FhirTransformSchemaElement(
            "childElement",
            value = listOf("%testId", "%otherTestId", "%defaultId"),
            resource = "Bundle.entry",
            bundleProperty = "Bundle.entry[%myIndexVar].resource.id"
        )
        val childSchema = FhirTransformSchema(elements = mutableListOf(childElement))
        val elementWithChild = FhirTransformSchemaElement(
            "elementWithChild",
            resource = "Bundle.entry",
            resourceIndex = "myIndexVar",
            constants = sortedMapOf(Pair("otherTestId", "'abc-def'")),
            schemaRef = childSchema
        )

        val schema =
            FhirTransformSchema(
                elements = mutableListOf(elementA, elementWithChild, elementB),
                constants = sortedMapOf(Pair("defaultName", "'backupName'"), Pair("defaultId", "'backupId'"))
            )
        val transformer = FhirTransformer(schema)

        transformer.process(bundle)
        // Element A used local constant
        assertThat(patient.active).isTrue()
        // Element w/ child used element constant in child element, didn't access constants from Element A
        assertThat(patient.id).isEqualTo("abc-def")
        // Element B used default value from parent schema since no element constants were accessible
        assertThat(patient.name[0].text).isEqualTo("backupName")
    }

    @Test
    fun `test resource index`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val servRequest1 = ServiceRequest()
        servRequest1.id = "def456"
        val servRequest2 = ServiceRequest()
        servRequest2.id = "ghi789"
        bundle.addEntry().resource = servRequest1
        bundle.addEntry().resource = servRequest2

        val transformer = FhirTransformer(FhirTransformSchema())

        val childElement = FhirTransformSchemaElement(
            "childElement",
            value = listOf("%myIndexVar"),
            resource = "Bundle.entry",
            bundleProperty = "Bundle.entry[%myIndexVar].resource.id"
        )
        val childSchema = FhirTransformSchema(elements = mutableListOf(childElement))
        val element = FhirTransformSchemaElement(
            "name",
            resource = "Bundle.entry",
            resourceIndex = "myIndexVar",
            schemaRef = childSchema
        )
        transformer.transformBasedOnElement(element, bundle, bundle, CustomContext(bundle, bundle))
        assertThat(servRequest1.id).isEqualTo("0")
        assertThat(servRequest2.id).isEqualTo("1")
    }

    @Test
    fun `test transform dateTime property with changeTimezone`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        bundle.timestamp = isoFormat.parse("2021-08-09T08:52:00-04:00")
        val resource = Patient()
        resource.id = "def456"
        resource.meta = Meta()
        bundle.addEntry().resource = resource

        val elemA = FhirTransformSchemaElement(
            "elementA",
            constants = sortedMapOf(Pair("timezone", "'America/Phoenix'")),
            value = listOf("Bundle.timestamp.changeTimezone(%timezone)"),
            resource = "Bundle.entry.resource.ofType(Patient).meta",
            bundleProperty = "Bundle.entry.resource.ofType(Patient).meta.lastUpdated"
        )

        val schema = FhirTransformSchema(elements = mutableListOf(elemA))

        FhirTransformer(schema).process(bundle)
        val newValue =
            FhirPathUtils.evaluate(
                CustomContext(bundle, bundle),
                bundle,
                bundle,
                "Bundle.entry.resource.ofType(Patient).meta.lastUpdated"
            )
        assertThat(resource.meta.lastUpdated).isEqualTo(bundle.timestamp)
        assertThat(newValue[0].primitiveValue()).isEqualTo("2021-08-09T05:52:00.000-07:00")
    }

    @Test
    fun `test extending schema overwrite element`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        val bundle = messages[0]

        val childSchema = ConfigSchemaReader.fromFile(
            "classpath:/fhir_sender_transforms/test_extension_schema.yml",
            schemaClass = FhirTransformSchema::class.java,
            RouterSchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
        )

        val transformer = FhirTransformer(childSchema)
        val transformedBundle = transformer.process(bundle)

        val transformedDiagnosticReports = mutableListOf<DiagnosticReport>()
        var transformedPatient = Patient()
        transformedBundle.entry.forEach {
            when (val resource = it.resource) {
                is Patient -> transformedPatient = resource
                is DiagnosticReport -> transformedDiagnosticReports.add(resource)
            }
        }

        val transformedObservation = transformedDiagnosticReports[0].result[0].resource as Observation

        assertThat(transformedDiagnosticReports[0].id).isEqualTo("extensionId")
        assertThat(transformedPatient.id).isEqualTo("123456")
        assertThat(transformedObservation.status).isEqualTo(Observation.ObservationStatus.FINAL)
        assertThat(transformedPatient.name[0].text).isEqualTo("placeholder value")
    }

    @Test
    fun `test action DELETE`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        patient.id = "def456"
        patient.name = listOf(HumanName())
        val entry = bundle.addEntry()
        entry.fullUrl = patient.id
        entry.resource = patient

        val elementA = FhirTransformSchemaElement(
            "elementA",
            resource = "Bundle.entry.resource.ofType(Patient)",
            action = FhirTransformSchemaElementAction.DELETE
        )
        val schema =
            FhirTransformSchema(
                elements = mutableListOf(elementA)
            )

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(bundle.entry).hasSize(0)
    }

    @Test
    fun `test action APPEND`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val serviceRequest = ServiceRequest()
        serviceRequest.note = listOf(Annotation(MarkdownType("Starr")), Annotation(MarkdownType("Lennon")))
        serviceRequest.id = "sr123"
        val patient = Patient()
        patient.id = "def456"
        val patientEntry = bundle.addEntry()
        patientEntry.fullUrl = patient.id
        patientEntry.resource = patient
        val serviceRequestEntry = bundle.addEntry()
        serviceRequestEntry.fullUrl = serviceRequest.id
        serviceRequestEntry.resource = serviceRequest

        val elementA = FhirTransformSchemaElement(
            "elementA",
            resource = "Bundle.entry.resource.ofType(ServiceRequest).note",
            bundleProperty = "family",
            value = listOf("%resource.text"),
            action = FhirTransformSchemaElementAction.APPEND,
            appendToProperty = "Bundle.entry.resource.ofType(Patient).name"
        )
        val schema =
            FhirTransformSchema(
                elements = mutableListOf(elementA)
            )

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(patient.name).hasSize(2)
        assertThat(patient.name).transform { it.map { name -> name.family } }.containsOnly("Starr", "Lennon")
    }

    @Test
    fun `test convert observations to notes`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val serviceRequest = ServiceRequest()
        serviceRequest.note = mutableListOf(Annotation(MarkdownType("existing")))
        val serviceRequestEntry = bundle.addEntry()
        serviceRequestEntry.fullUrl = serviceRequest.id
        serviceRequestEntry.resource = serviceRequest
        val observation1 = Observation()
        observation1.id = "obs123"
        observation1.code.coding.add(Coding("system", "123", "display"))
        val observationEntry1 = bundle.addEntry()
        observationEntry1.fullUrl = observation1.id
        observationEntry1.resource = observation1
        val observation2 = Observation()
        observation2.id = "obs123"
        observation2.code.coding.add(Coding("system", "456", "display"))
        val observationEntry2 = bundle.addEntry()
        observationEntry2.fullUrl = observation2.id
        observationEntry2.resource = observation2

        @Suppress("ktlint:standard:max-line-length")
        val noteSource = FhirTransformSchemaElement(
            "note-source",
            bundleProperty =
                "extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/nte-annotation\").extension(\"NTE.2\").value[x]",
            value = listOf("\"O\""),
            action = FhirTransformSchemaElementAction.APPEND,
            appendToProperty = "Bundle.entry.resource.ofType(ServiceRequest).note"
        )
        // Existing elements = 2
        // AppendIndex = 0
        val noteText = FhirTransformSchemaElement(
            "note-text",
            bundleProperty = "text",
            value = listOf("%resource.code.coding.code"),
            action = FhirTransformSchemaElementAction.APPEND,
            appendToProperty = "Bundle.entry.resource.ofType(ServiceRequest).note"
        )
        val noteToObservationSchema = FhirTransformSchema(
            elements = mutableListOf(noteSource, noteText)
        )

        val schema =
            FhirTransformSchema(
                elements = mutableListOf(
                    FhirTransformSchemaElement(
                        schemaRef = noteToObservationSchema,
                        resource = "Bundle.entry.resource.ofType(Observation)",
                        action = FhirTransformSchemaElementAction.APPEND,
                        appendToProperty = "Bundle.entry.resource.ofType(ServiceRequest).note"
                    )
                )
            )

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(serviceRequest.note).hasSize(3)
        assertThat(serviceRequest.note)
            .transform { it.map { note -> note.text } }.containsOnly("existing", "123", "456")
        assertThat(serviceRequest.note[1].extension)
            .transform {
                it.find { ext -> ext.url == "https://reportstream.cdc.gov/fhir/StructureDefinition/nte-annotation" }
            }.isNotNull()
    }

    @Test
    fun `test creating at a specific index for SET action`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val observation1 = Observation()
        observation1.id = "obs123"
        observation1.identifier = mutableListOf(Identifier())
        val observationEntry1 = bundle.addEntry()
        observationEntry1.fullUrl = observation1.id
        observationEntry1.resource = observation1

        val addIdentifierName = FhirTransformSchemaElement(
            "note-source",
            resource = "Bundle.entry.resource.ofType(Observation)",
            bundleProperty =
            "%resource.identifier[1].value",
            value = listOf("\"Text\""),
            action = FhirTransformSchemaElementAction.SET,
        )

        val schema =
            FhirTransformSchema(
                elements = mutableListOf(addIdentifierName)
            )

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(observation1.identifier).hasSize(2)
        assertThat(observation1.identifier[0].value).isNull()
        assertThat(observation1.identifier[1].value).isEqualTo("Text")
    }

    @Test
    fun `test creating a valuex`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val observation1 = Observation()
        observation1.id = "obs123"
        val observationEntry1 = bundle.addEntry()
        observationEntry1.fullUrl = observation1.id
        observationEntry1.resource = observation1

        val observationValue = FhirTransformSchemaElement(
            "note-source",
            bundleProperty =
            "Bundle.entry.resource.ofType(Observation).value[x]",
            value = listOf("\"Text\""),
            action = FhirTransformSchemaElementAction.SET,
        )

        val schema =
            FhirTransformSchema(
                elements = mutableListOf(observationValue)
            )

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)

        assertThat(observation1.value.fhirType()).isEqualTo("string")
        assertThat(observation1.valueStringType.value).isEqualTo("Text")
    }

    @Test
    fun `test targeting a bundle property that targets multiple elements`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val observation1 = Observation()
        observation1.id = "obs123"
        val observationEntry1 = bundle.addEntry()
        observationEntry1.fullUrl = observation1.id
        observationEntry1.resource = observation1
        val observation2 = Observation()
        observation2.id = "obs123"
        val observationEntry2 = bundle.addEntry()
        observationEntry2.fullUrl = observation2.id
        observationEntry2.resource = observation2
        val noteText = FhirTransformSchemaElement(
            "note-source",
            bundleProperty =
            "Bundle.entry.resource.ofType(Observation).note.text",
            value = listOf("\"Text\""),
            action = FhirTransformSchemaElementAction.SET,
        )
        val schema =
            FhirTransformSchema(
                elements = mutableListOf(noteText)
            )

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)

        assertThat(observation1.note).hasSize(1)
        assertThat(observation1.note[0].text).isEqualTo("Text")
        assertThat(observation2.note).hasSize(1)
        assertThat(observation2.note[0].text).isEqualTo("Text")
    }

    @Test
    fun `test it should handle multiple append elements`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val serviceRequest = ServiceRequest()
        serviceRequest.note = mutableListOf(Annotation(MarkdownType("existing")))
        serviceRequest.orderDetail = mutableListOf(CodeableConcept(), CodeableConcept())
        val serviceRequestEntry = bundle.addEntry()
        serviceRequestEntry.fullUrl = serviceRequest.id
        serviceRequestEntry.resource = serviceRequest
        val observation1 = Observation()
        observation1.id = "obs123"
        observation1.code.coding.add(Coding("system", "123", "display"))
        val observationEntry1 = bundle.addEntry()
        observationEntry1.fullUrl = observation1.id
        observationEntry1.resource = observation1
        val observation2 = Observation()
        observation2.id = "obs123"
        observation2.code.coding.add(Coding("system", "456", "display"))
        val observationEntry2 = bundle.addEntry()
        observationEntry2.fullUrl = observation2.id
        observationEntry2.resource = observation2
        val patient = Patient()
        patient.id = "def456"
        val patientEntry = bundle.addEntry()
        patientEntry.fullUrl = patient.id
        patientEntry.resource = patient

        @Suppress("ktlint:standard:max-line-length")
        val noteSource = FhirTransformSchemaElement(
            "note-source",
            bundleProperty =
            "extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/nte-annotation\").extension(\"NTE.2\").value[x]",
            value = listOf("\"O\""),
            action = FhirTransformSchemaElementAction.APPEND,
            appendToProperty = "Bundle.entry.resource.ofType(ServiceRequest).note"
        )
        // Existing elements = 2
        // AppendIndex = 0
        val noteText = FhirTransformSchemaElement(
            "note-text",
            bundleProperty = "text",
            value = listOf("%resource.code.coding.code"),
            action = FhirTransformSchemaElementAction.APPEND,
            appendToProperty = "Bundle.entry.resource.ofType(ServiceRequest).note"
        )
        val noteToObservationSchema = FhirTransformSchema(
            elements = mutableListOf(noteSource, noteText)
        )

        val orderDetailCode = FhirTransformSchemaElement(
            "order-detail-code",
            bundleProperty = "coding.code",
            value = listOf("%resource.code.coding.code"),
            action = FhirTransformSchemaElementAction.APPEND,
            appendToProperty = "Bundle.entry.resource.ofType(ServiceRequest).orderDetail"
        )

        val orderDetailDisplay = FhirTransformSchemaElement(
            "order-detail-code",
            bundleProperty = "coding.display",
            value = listOf("'Display'"),
            action = FhirTransformSchemaElementAction.APPEND,
            appendToProperty = "Bundle.entry.resource.ofType(ServiceRequest).orderDetail"
        )

        val observationToOrderDetailSchema = FhirTransformSchema(
            elements = mutableListOf(orderDetailCode, orderDetailDisplay)
        )

        val schema =
            FhirTransformSchema(
                elements = mutableListOf(
                    FhirTransformSchemaElement(
                        "convert-observation-to-note",
                        schemaRef = noteToObservationSchema,
                        resource = "Bundle.entry.resource.ofType(Observation)",
                        action = FhirTransformSchemaElementAction.APPEND,
                        appendToProperty = "Bundle.entry.resource.ofType(ServiceRequest).note"
                    ),
                    FhirTransformSchemaElement(
                        "convert-observation-to-order-detail",
                        schemaRef = observationToOrderDetailSchema,
                        resource = "Bundle.entry.resource.ofType(Observation)",
                        action = FhirTransformSchemaElementAction.APPEND,
                        appendToProperty = "Bundle.entry.resource.ofType(ServiceRequest).orderDetail"
                    ),
                    FhirTransformSchemaElement(
                        "convert-observation-to-patient-name",
                        resource = "Bundle.entry.resource.ofType(Observation)",
                        bundleProperty = "family",
                        value = listOf("%resource.code.coding.code"),
                        action = FhirTransformSchemaElementAction.APPEND,
                        appendToProperty = "Bundle.entry.resource.ofType(Patient).name"
                    )
                )
            )

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(serviceRequest.note).hasSize(3)
        assertThat(serviceRequest.orderDetail).hasSize(4)
        assertThat(patient.name).hasSize(2)
    }

    @Test
    fun `test accessing by index while setting the actual bundle property`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        val name = HumanName()
        name.given = mutableListOf(StringType("foo"), StringType("bar"))
        patient.name = mutableListOf(name)
        patient.id = "def456"
        val patientEntry = bundle.addEntry()
        patientEntry.fullUrl = patient.id
        patientEntry.resource = patient

        val updateFirstGivenNameSchemaElement = FhirTransformSchemaElement(
            "update-first-given-name",
            resource = "Bundle.entry.resource.ofType(Patient).name",
            bundleProperty = "%resource.given[0]",
            value = listOf("''")
        )
        val schema = FhirTransformSchema(elements = mutableListOf(updateFirstGivenNameSchemaElement))

        val transformer = FhirTransformer(schema)
        val exception = assertThrows<ConfigSchemaElementProcessingException> {
            transformer.process(bundle)
        }
        assertThat(exception.message)
            .contains("Schema is attempting to set a value for a particular index which is not allowed")
    }

    @Test
    fun `test deidentify human name`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        val name = HumanName()
        name.given = mutableListOf(StringType("foo"), StringType("bar"))
        name.family = "family"
        patient.name = mutableListOf(name)
        patient.id = "def456"
        val patientEntry = bundle.addEntry()
        patientEntry.fullUrl = patient.id
        patientEntry.resource = patient

        val updateFirstGivenNameSchemaElement = FhirTransformSchemaElement(
            "update-first-given-name",
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.name",
            function = "deidentifyHumanName()",
        )
        val schema = FhirTransformSchema(elements = mutableListOf(updateFirstGivenNameSchemaElement))

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(name.given).isEmpty()
        assertThat(name.family).isNull()
    }

    @Test
    fun `test deidentify human name with a value`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val patient = Patient()
        val name = HumanName()
        name.given = mutableListOf(StringType("foo"), StringType("bar"))
        name.family = "family"
        patient.name = mutableListOf(name)
        patient.id = "def456"
        val patientEntry = bundle.addEntry()
        patientEntry.fullUrl = patient.id
        patientEntry.resource = patient

        val updateFirstGivenNameSchemaElement = FhirTransformSchemaElement(
            "update-first-given-name",
            resource = "Bundle.entry.resource.ofType(Patient)",
            bundleProperty = "%resource.name",
            function = "deidentifyHumanName('deidentified')",
        )
        val schema = FhirTransformSchema(elements = mutableListOf(updateFirstGivenNameSchemaElement))

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(name.given).transform { it -> it.map { st -> st.value } }
            .containsOnly("deidentified", "deidentified")
        assertThat(name.family).isEqualTo("deidentified")
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `test move Observation to ServiceRequest note`() {
        val bundle = Bundle()
        bundle.id = "abc123"
        val serviceRequest = ServiceRequest()
        val serviceRequestEntry = bundle.addEntry()
        serviceRequestEntry.fullUrl = serviceRequest.id
        serviceRequestEntry.resource = serviceRequest
        val observation1 = Observation()
        observation1.id = "obs123"
        observation1.code.coding.add(
            Coding(
                "SNOMED",
                "92142-9",
                "Influenza virus A RNA [Presence] in Respiratory specimen by NAA with probe detection"
            )
        )
        observation1.interpretation = mutableListOf(CodeableConcept(Coding("loinc", "260385009", "Negative")))
        val observationEntry1 = bundle.addEntry()
        observationEntry1.fullUrl = observation1.id
        observationEntry1.resource = observation1
        val observation2 = Observation()
        observation2.id = "obs123"
        observation2.code.coding.add(
            Coding(
                "SNOMED",
                "92141-1",
                " Influenza virus B RNA [Presence] in Respiratory specimen by NAA with probe detection"
            )
        )
        observation2.interpretation = mutableListOf(CodeableConcept(Coding("loinc", "260385009", "Negative")))
        val observationEntry2 = bundle.addEntry()
        observationEntry2.fullUrl = observation2.id
        observationEntry2.resource = observation2

        val schema = ConfigSchemaReader.fromFile(
            "classpath:/fhir_sender_transforms/convert-all-obs-to-note.yml",
            schemaClass = FhirTransformSchema::class.java,
            RouterSchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
        )

        val transformer = FhirTransformer(schema)
        transformer.process(bundle)
        assertThat(serviceRequest.note).hasSize(2)
        val notes = serviceRequest.note
        notes.forEach { note ->
            assertThat(
                note
                    .getExtensionByUrl("https://reportstream.cdc.gov/fhir/StructureDefinition/nte-annotation")
                    .getExtensionByUrl("NTE.2")
                    .value
                    .toString()
            ).isEqualTo("L")
        }
        assertThat(serviceRequest.note)
            .transform { it.map { note -> note.text } }
            .containsOnly(
                "OBX filtered for identifier=92142-9 - Influenza virus A RNA [Presence] in Respiratory specimen by NAA with probe detection;value = 260385009 - Negative OBX was removed due to your jurisdictional reporting rules indicating this result is not reportable.",
                "OBX filtered for identifier=92141-1 -  Influenza virus B RNA [Presence] in Respiratory specimen by NAA with probe detection;value = 260385009 - Negative OBX was removed due to your jurisdictional reporting rules indicating this result is not reportable."
            )
    }
}