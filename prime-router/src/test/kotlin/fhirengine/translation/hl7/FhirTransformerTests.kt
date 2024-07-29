package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElementProcessingException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.apache.logging.log4j.kotlin.KotlinLogger
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.StringType
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
        transformer.setBundleProperty(
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
        val (transformer, logger) = setupFhirTransformer(FhirTransformSchema())

        val bundle = Bundle()
        bundle.id = "abc123"

        // Can't currently create entry on the fly
        assertFailure {
            transformer.setBundleProperty(
                "Bundle.entry.resource.ofType(DiagnosticReport).status", CodeType("final"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }

        val patient = Patient()
        patient.id = "def456"
        bundle.addEntry().resource = patient

        // Can't currently create new resources on the fly
        assertFailure {
            transformer.setBundleProperty(
                "Bundle.entry.resource.ofType(DiagnosticReport).status", CodeType("final"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }

        // Extension matcher is provided with a non-string value
        transformer.setBundleProperty(
            "Bundle.entry.resource.ofType(Patient).extension(regexNonMatch).value[x]", IdType("newId"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        verifyErrorAndResetLogger(logger)

        // Invalid bundleProperties
        transformer.setBundleProperty(
            "", IdType("newId"),
            CustomContext(bundle, bundle), bundle, bundle
        )
        verifyErrorAndResetLogger(logger)

        transformer.setBundleProperty(
            "%key.text", StringType("SomeName"),
            CustomContext(
                bundle,
                bundle,
                constants = mutableMapOf(Pair("key", "Bundle.entry.resource.ofType(Patient).name"))
            ),
            bundle, bundle
        )
        verifyErrorAndResetLogger(logger)

        // Incompatible value types
        assertFailure {
            transformer.setBundleProperty(
                "Bundle.entry.resource.ofType(Patient).name.text", CodeableConcept(),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }
        verifyDebugAndResetLogger(logger)

        assertFailure {
            transformer.setBundleProperty(
                "Bundle.entry.resource.ofType(Patient).active", StringType("nonBoolean"),
                CustomContext(bundle, bundle), bundle, bundle
            )
        }
        verifyDebugAndResetLogger(logger)
    }

    @Test
    fun `test validate and split bundleProperty`() {
        val (transformer, logger) = setupFhirTransformer(FhirTransformSchema())

        transformer.validateAndSplitBundleProperty("")
        verifyErrorAndResetLogger(logger)

        transformer.validateAndSplitBundleProperty("id")
        verifyErrorAndResetLogger(logger)

        transformer.validateAndSplitBundleProperty("Bundle.entry.resource.ofType(Patient).name.%key")
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
            blobConnectionInfo = mockk<BlobAccess.BlobContainerMetadata>()
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
}