package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.converterSchemaFromFile
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.fhirTransformSchemaFromFile
import io.mockk.every
import io.mockk.mockkObject
import java.io.File
import java.net.URI
import kotlin.test.Test

class ConfigSchemaReaderTests {
    @Test
    fun `test read one yaml schema`() {
        var yaml = """
            name: ORU-R01-Base
            hl7Class: ca.uhn.hl7v2.model.v251.message.ORU_R01
            elements:
            - name: message-headers
              condition: >
                Bundle.entry.resource.ofType(MessageHeader) and
                Bundle.entry.resource.ofType(Provenance) and
                Bundle.entry.resource.ofType(Provenance).activity.coding.code = 'R01'
              value: ['1']
              hl7Spec:
                - .PID.1
        """.trimIndent()
        val schema = ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream())
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.name).isEqualTo("ORU-R01-Base")

        // Schema with unknown property
        yaml = """
            name: ORU-R01-Base
            SOMEBADPROPERTY: ORU_R01
            hl7Version: 2.5.1
            elements:
            - name: message-headers
              condition: >
                Bundle.entry.resource.ofType(MessageHeader) and
                Bundle.entry.resource.ofType(Provenance) and
                Bundle.entry.resource.ofType(Provenance).activity.coding.code = 'R01'
              required: true
              schema: ORU_R01/header.yml
        """.trimIndent()
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }

        // Badly formatted YAML - First condition has incorrect identation
        yaml = """
            name: ORU-R01-Base
            hl7Class: ca.uhn.hl7v2.model.v251.message.ORU_R01
            elements:
            - name: message-headers
              condition: >
            Bundle.entry.resource.ofType(MessageHeader) and
                Bundle.entry.resource.ofType(Provenance) and
                Bundle.entry.resource.ofType(Provenance).activity.coding.code = 'R01'
              required: true
              schema: ORU_R01/header.yml
        """.trimIndent()
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }
        yaml = """
            name ORU-R01-Base
        """.trimIndent()
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }
        yaml = """
            name: [ORU-R01-Base,other]
        """.trimIndent()
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }

        // Empty file
        yaml = ""
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }
    }

    @Test
    fun `test read schema tree from file`() {
        // This is a good schema
        val schema = ConfigSchemaReader.readSchemaTreeRelative(
            "ORU_R01",
            "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01"
        )

        assertThat(schema is ConverterSchema).isTrue()
        if (schema is ConverterSchema) {
            assertThat(schema.name).isEqualTo("ORU_R01") // match filename
            assertThat(schema.hl7Class).isEqualTo("ca.uhn.hl7v2.model.v251.message.ORU_R01")
            assertThat(schema.elements).isNotEmpty()
        }

        val patientInfoElement = schema.elements.single { it.name == "patient-information" }
        assertThat(patientInfoElement is ConverterSchemaElement).isTrue()
        if (patientInfoElement is ConverterSchemaElement) {
            assertThat(patientInfoElement.schema).isNotNull()
            assertThat(patientInfoElement.schema!!).isNotEmpty()
            assertThat(patientInfoElement.schemaRef).isNotNull()
        }

        assertThat(patientInfoElement.schemaRef!!.name).isEqualTo("ORU_R01/patient") // match filename
        val patientNameElement = patientInfoElement.schemaRef!!.elements.single { it.name == "patient-last-name" }
        assertThat(patientNameElement is ConverterSchemaElement).isTrue()
        if (patientNameElement is ConverterSchemaElement) {
            assertThat(patientNameElement.hl7Spec).isNotEmpty()
        }

        // This is a bad schema.
        assertFailure {
            ConfigSchemaReader.readSchemaTreeRelative(
                "ORU_R01_incomplete",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-02"
            )
        }

        assertFailure {
            ConfigSchemaReader.readSchemaTreeRelative(
                "ORU_R01_bad",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-03"
            )
        }
    }

    @Test
    fun `test read converter vs fhir transform`() {
        // This is a valid fhir transform schema
        assertThat(
            ConfigSchemaReader.readSchemaTreeRelative(
                "sample_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        )

        // This is an invalid hl7v2 schema
        assertFailure {
            ConfigSchemaReader.readSchemaTreeRelative(
                "sample_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = ConverterSchema::class.java,
            )
        }

        // This is a valid hl7v2 schema
        assertThat(
            ConfigSchemaReader.readSchemaTreeRelative(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01",
                schemaClass = ConverterSchema::class.java,
            )
        )

        // This is an invalid fhir transform schema
        assertFailure {
            ConfigSchemaReader.readSchemaTreeRelative(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01",
                schemaClass = FhirTransformSchema::class.java,
            )
        }
    }

    @Test
    fun `test read from file`() {
        assertThat(
            ConfigSchemaReader.fromFile(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01",
                schemaClass = ConverterSchema::class.java,
            ).isValid()
        ).isTrue()

        assertFailure {
            ConfigSchemaReader.fromFile(
                "ORU_R01_incomplete",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-02",
                schemaClass = ConverterSchema::class.java,
            )
        }

        assertThat(
            converterSchemaFromFile(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01"
            ).isValid()
        ).isTrue()

        assertFailure {
            converterSchemaFromFile(
                "ORU_R01_incomplete",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-02"
            )
        }
    }

    @Test
    fun `test read from file with extends`() {
        assertFailure {
            ConfigSchemaReader.fromFile(
                "ORU_R01_circular",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-06",
                schemaClass = ConverterSchema::class.java,
            )
        }.messageContains("Schema circular dependency")

        val schema = ConfigSchemaReader.fromFile(
            "ORU_R01_extends",
            "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-06",
            schemaClass = ConverterSchema::class.java,
        )
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.constants["baseConstant"]).isEqualTo("baseValue")
        assertThat(schema.constants["lowLevelConstant"]).isEqualTo("lowLevelValue")
        assertThat(schema.constants["overriddenConstant"]).isEqualTo("overriddenValue")
        assertThat(schema.name).isEqualTo("ORU_R01_extends")
    }

    @Test
    fun `test read FHIR Transform schema tree from file`() {
        // This is a good schema
        val schema = ConfigSchemaReader.readSchemaTreeRelative(
            "sample_schema",
            "src/test/resources/fhir_sender_transforms",
            schemaClass = FhirTransformSchema::class.java,
        )

        assertThat(schema.errors).isEmpty()
        assertThat(schema.name).isEqualTo("sample_schema") // match filename
        assertThat(schema.elements).isNotEmpty()

        val statusElement = schema.elements.single { it.name == "status" }

        assertThat(statusElement is FhirTransformSchemaElement).isTrue()
        if (statusElement is FhirTransformSchemaElement) {
            assertThat(statusElement.schema).isNull()
            assertThat(statusElement.constants).isNotNull()
            assertThat(statusElement.condition).isNotNull()
            assertThat(statusElement.bundleProperty).isEqualTo("%resource.status")
        }

        // This is a bad schema.
        assertFailure {
            ConfigSchemaReader.readSchemaTreeRelative(
                "invalid_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        }

        assertFailure {
            ConfigSchemaReader.readSchemaTreeRelative(
                "incomplete_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        }
    }

    @Test
    fun `test read FHIR Transform from file`() {
        assertThat(
            ConfigSchemaReader.fromFile(
                "sample_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            ).isValid()
        ).isTrue()

        assertFailure {
            ConfigSchemaReader.fromFile(
                "incomplete_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        }

        assertThat(
            fhirTransformSchemaFromFile(
                "sample_schema",
                "src/test/resources/fhir_sender_transforms",
            ).isValid()
        ).isTrue()

        assertFailure {
            fhirTransformSchemaFromFile(
                "invalid_value_set",
                "src/test/resources/fhir_sender_transforms",
            )
        }

        assertFailure {
            fhirTransformSchemaFromFile(
                "incomplete_schema",
                "src/test/resources/fhir_sender_transforms",
            )
        }

        assertFailure {
            fhirTransformSchemaFromFile(
                "no_schema_nor_value",
                "src/test/resources/fhir_sender_transforms",
            )
        }
    }

    @Test
    fun `test read FHIR Transform from file with extends`() {
        assertFailure {
            ConfigSchemaReader.fromFile(
                "circular_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        }.messageContains("Schema circular dependency")

        assertThat(
            ConfigSchemaReader.fromFile(
                "extends_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            ).isValid()
        ).isTrue()
    }

    @Test
    fun `test extends schema with URI`() {
        assertThat(
            ConfigSchemaReader.fromFile(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-07/ORU_R01.yml",
                null,
                schemaClass = ConverterSchema::class.java,
            )
        )
    }

    @Test
    fun `test read extended schema from file`() {
        // This is a good schema
        val schema = ConfigSchemaReader.readSchemaTreeRelative(
            "ORU_R01-extended",
            "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01"
        )

        assertThat(schema is ConverterSchema).isTrue()
        if (schema is ConverterSchema) {
            assertThat(schema.errors).isEmpty()
            assertThat(schema.name).isEqualTo("ORU_R01-extended") // match filename
            assertThat(schema.hl7Class).isEqualTo("ca.uhn.hl7v2.model.v251.message.ORU_R01")
            assertThat(schema.elements).isNotEmpty()
        }

        val patientLastNameElement = schema.findElements("patient-last-name")
        assertThat(patientLastNameElement).isNotNull()
        assertThat(patientLastNameElement[0].condition).isEqualTo("true")
        assertThat(patientLastNameElement[0].value).isNotNull()
        assertThat(patientLastNameElement[0].value!![0]).isEqualTo("DUMMY")

        val orderElement = schema.findElements("order-observations")
        assertThat(orderElement).isNotNull()
        assertThat(orderElement[0].condition).isEqualTo("false")

        val newElement = schema.findElements("new-element")
        assertThat(newElement).isNotNull()
    }

    @Test
    fun `test simple circular reference exception when loading schema`() {
        assertFailure {
            ConfigSchemaReader.readSchemaTreeRelative(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-04"
            )
        }
    }

    @Test
    fun `test deep circular reference exception when loading schema`() {
        assertFailure {
            ConfigSchemaReader.readSchemaTreeRelative(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-05"
            )
        }
    }

    @Test
    fun `test reads a file with file protocol`() {
        val file = File(
            "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-07",
            "ORU_R01.yml"
        )
        assertThat(
            ConfigSchemaReader.readSchemaTreeUri(file.toURI())
        )
    }

    @Test
    fun `reads a file with an azure protocol`() {
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns File(
            "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-07",
            "ORU_R01.yml"
        ).readBytes()
        assertThat(
            ConfigSchemaReader.readSchemaTreeUri(
                URI(
                    """
                    azure://azure.container.com/src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-07/ORU_R01.yml
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    fun `correctly flags a circular dependency when using a URI`() {
        val file = File(
            "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-08",
            "ORU_R01_circular.yml"
        )
        assertFailure {
            ConfigSchemaReader.fromFile(
                file.toURI().toString(),
                schemaClass = ConverterSchema::class.java,
            )
        }.messageContains("Schema circular dependency")
    }
}