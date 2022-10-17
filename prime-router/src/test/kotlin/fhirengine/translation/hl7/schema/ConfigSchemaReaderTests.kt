package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlin.test.Test

class ConfigSchemaReaderTests {
    @Test
    fun `test read one yaml schema`() {
        var yaml = """
            name: ORU-R01-Base
            hl7Type: ORU_R01
            hl7Version: 2.5.1
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
            hl7Type: ORU_R01
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

        assertThat(schema.errors).isEmpty()
        assertThat(schema.name).isEqualTo("ORU_R01") // match filename
        assertThat(schema.hl7Type).isEqualTo("ORU_R01")
        assertThat(schema.hl7Version).isEqualTo("2.5.1")
        assertThat(schema.elements).isNotEmpty()

        val patientInfoElement = schema.elements.single { it.name == "patient-information" }
        assertThat(patientInfoElement.schema).isNotNull()
        assertThat(patientInfoElement.schema!!).isNotEmpty()
        assertThat(patientInfoElement.schemaRef).isNotNull()

        assertThat(patientInfoElement.schemaRef!!.name).isEqualTo("ORU_R01/patient") // match filename
        val patientNameElement = patientInfoElement.schemaRef!!.elements.single { it.name == "patient-last-name" }
        assertThat(patientNameElement.hl7Spec).isNotEmpty()

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
    fun `test read from file`() {
        assertThat(
            ConfigSchemaReader.fromFile(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01"
            ).isValid()
        ).isTrue()

        assertThat {
            ConfigSchemaReader.fromFile(
                "ORU_R01_incomplete",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-02"
            )
        }.isFailure()
    }

    @Test
    fun `test read extended schema from file`() {
        // This is a good schema
        val schema = ConfigSchemaReader.readSchemaTreeFromFile(
            "ORU_R01-extended",
            "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01"
        )

        assertThat(schema.errors).isEmpty()
        assertThat(schema.name).isEqualTo("ORU_R01-extended") // match filename
        assertThat(schema.hl7Type).isEqualTo("ORU_R01")
        assertThat(schema.hl7Version).isEqualTo("2.7")
        assertThat(schema.elements).isNotEmpty()

        val patientLastNameElement = schema.findElement("patient-last-name")
        assertThat(patientLastNameElement).isNotNull()
        assertThat(patientLastNameElement!!.condition).isEqualTo("true")
        assertThat(patientLastNameElement.value).isNotEmpty()
        assertThat(patientLastNameElement.value[0]).isEqualTo("DUMMY")

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