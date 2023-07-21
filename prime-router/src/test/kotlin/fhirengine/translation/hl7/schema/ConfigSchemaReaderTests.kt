package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.converterSchemaFromFile
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.fhirTransformSchemaFromFile
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
        assertThat { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }.isFailure()

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
        assertThat { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }.isFailure()
        yaml = """
            name ORU-R01-Base
        """.trimIndent()
        assertThat { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }.isFailure()
        yaml = """
            name: [ORU-R01-Base,other]
        """.trimIndent()
        assertThat { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }.isFailure()

        // Empty file
        yaml = ""
        assertThat { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream()) }.isFailure()
    }

    @Test
    fun `test read schema tree from file`() {
        // This is a good schema
        val schema = ConfigSchemaReader.readSchemaTreeFromFile(
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
        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "ORU_R01_incomplete",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-02"
            )
        }.isFailure()

        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "ORU_R01_bad",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-03"
            )
        }.isFailure()
    }

    @Test
    fun `test read converter vs fhir transform`() {
        // This is a valid fhir transform schema
        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "sample_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        }.isSuccess()

        // This is an invalid hl7v2 schema
        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "sample_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = ConverterSchema::class.java,
            )
        }.isFailure()

        // This is a valid hl7v2 schema
        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01",
                schemaClass = ConverterSchema::class.java,
            )
        }.isSuccess()

        // This is an invalid fhir transform schema
        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01",
                schemaClass = FhirTransformSchema::class.java,
            )
        }.isFailure()
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

        assertThat {
            ConfigSchemaReader.fromFile(
                "ORU_R01_incomplete",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-02",
                schemaClass = ConverterSchema::class.java,
            )
        }.isFailure()

        assertThat(
            converterSchemaFromFile(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01"
            ).isValid()
        ).isTrue()

        assertThat {
            converterSchemaFromFile(
                "ORU_R01_incomplete",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-02"
            )
        }.isFailure()
    }

    @Test
    fun `test read from file with extends`() {
        assertThat {
            ConfigSchemaReader.fromFile(
                "ORU_R01_circular",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-06",
                schemaClass = ConverterSchema::class.java,
            )
        }.isFailure().messageContains("Schema circular dependency")

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
        val schema = ConfigSchemaReader.readSchemaTreeFromFile(
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
        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "invalid_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        }.isFailure()

        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "incomplete_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        }.isFailure()
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

        assertThat {
            ConfigSchemaReader.fromFile(
                "incomplete_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        }.isFailure()

        assertThat(
            fhirTransformSchemaFromFile(
                "sample_schema",
                "src/test/resources/fhir_sender_transforms",
            ).isValid()
        ).isTrue()

        assertThat {
            fhirTransformSchemaFromFile(
                "invalid_value_set",
                "src/test/resources/fhir_sender_transforms",
            )
        }.isFailure()

        assertThat {
            fhirTransformSchemaFromFile(
                "incomplete_schema",
                "src/test/resources/fhir_sender_transforms",
            )
        }.isFailure()

        assertThat {
            fhirTransformSchemaFromFile(
                "no_schema_nor_value",
                "src/test/resources/fhir_sender_transforms",
            )
        }.isFailure()
    }

    @Test
    fun `test read FHIR Transform from file with extends`() {
        assertThat {
            ConfigSchemaReader.fromFile(
                "circular_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            )
        }.isFailure().messageContains("Schema circular dependency")

        assertThat(
            ConfigSchemaReader.fromFile(
                "extends_schema",
                "src/test/resources/fhir_sender_transforms",
                schemaClass = FhirTransformSchema::class.java,
            ).isValid()
        ).isTrue()
    }

    @Test
    fun `test read extended schema from file`() {
        // This is a good schema
        val schema = ConfigSchemaReader.readSchemaTreeFromFile(
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

        val patientLastNameElement = schema.findElement("patient-last-name")
        assertThat(patientLastNameElement).isNotNull()
        assertThat(patientLastNameElement!!.condition).isEqualTo("true")
        assertThat(patientLastNameElement.value).isNotNull()
        assertThat(patientLastNameElement.value!![0]).isEqualTo("DUMMY")

        val orderElement = schema.findElement("order-observations")
        assertThat(orderElement).isNotNull()
        assertThat(orderElement!!.condition).isEqualTo("false")

        val newElement = schema.findElement("new-element")
        assertThat(newElement).isNotNull()
    }

    @Test
    fun `test simple circular reference exception when loading schema`() {
        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-04"
            )
        }.isFailure()
    }

    @Test
    fun `test deep circular reference exception when loading schema`() {
        assertThat {
            ConfigSchemaReader.readSchemaTreeFromFile(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-05"
            )
        }.isFailure()
    }
}