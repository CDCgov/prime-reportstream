package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.HL7ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.helpers.SchemaReferenceResolverHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.verify
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
        val schema = ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream(), HL7ConverterSchema::class.java)
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
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream(), HL7ConverterSchema::class.java) }

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
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream(), HL7ConverterSchema::class.java) }
        yaml = """
            name ORU-R01-Base
        """.trimIndent()
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream(), HL7ConverterSchema::class.java) }
        yaml = """
            name: [ORU-R01-Base,other]
        """.trimIndent()
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream(), HL7ConverterSchema::class.java) }

        // Empty file
        yaml = ""
        assertFailure { ConfigSchemaReader.readOneYamlSchema(yaml.byteInputStream(), HL7ConverterSchema::class.java) }
    }

    @Test
    fun `test read converter vs fhir transform`() {
        // This is a valid fhir transform schema
        assertThat(
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transforms/sample_schema.yml",

                schemaClass = FhirTransformSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        )

        // This is an invalid hl7v2 schema
        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transforms/sample_schema.yml",
                schemaClass = HL7ConverterSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        }

        // This is a valid hl7v2 schema
        assertThat(
            ConfigSchemaReader.fromFile(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-01/ORU_R01.yml",
                schemaClass = HL7ConverterSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        )

        // This is an invalid fhir transform schema
        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-01/ORU_R01.yml",
                schemaClass = FhirTransformSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        }
    }

    @Test
    fun `test read from file`() {
        assertThat(
            ConfigSchemaReader.fromFile(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-01/ORU_R01.yml",
                schemaClass = HL7ConverterSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            ).isValid()
        ).isTrue()

        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-02/ORU_R01_incomplete.yml",
                schemaClass = HL7ConverterSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        }

        assertThat(
            ConfigSchemaReader.fromFile(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-01/ORU_R01.yml",
                schemaClass = HL7ConverterSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            ).isValid()
        ).isTrue()

        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-02/ORU_R01_incomplete.yml",
                schemaClass = HL7ConverterSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        }
    }

    @Test
    fun `test read from file with extends`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-06/ORU_R01_circular.yml",
                schemaClass = HL7ConverterSchema::class.java,
                schemaServiceProviders =
                    SchemaReferenceResolverHelper.getSchemaServiceProviders(
                        mockk<BlobAccess.BlobContainerMetadata>()
                    )
            )
        }.messageContains("Schema circular dependency")

        val schema = ConfigSchemaReader.fromFile(
            "classpath:/fhirengine/translation/hl7/schema/schema-read-test-06/ORU_R01_extends.yml",

            schemaClass = HL7ConverterSchema::class.java,
            schemaServiceProviders =
                SchemaReferenceResolverHelper.getSchemaServiceProviders(
                    mockk<BlobAccess.BlobContainerMetadata>()
                )
        )
        assertThat(schema.isValid()).isTrue()
        assertThat(schema.constants["baseConstant"]).isEqualTo("baseValue")
        assertThat(schema.constants["lowLevelConstant"]).isEqualTo("lowLevelValue")
        assertThat(schema.constants["overriddenConstant"]).isEqualTo("overriddenValue")
        assertThat(schema.name).isEqualTo("/fhirengine/translation/hl7/schema/schema-read-test-06/ORU_R01_extends.yml")
    }

    @Test
    fun `test read FHIR Transform from file`() {
        assertThat(
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transforms/sample_schema.yml",
                schemaClass = FhirTransformSchema::class.java,
                schemaServiceProviders =
                    SchemaReferenceResolverHelper.getSchemaServiceProviders(
                        mockk<BlobAccess.BlobContainerMetadata>()
                    )
            ).isValid()
        ).isTrue()

        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transforms/incomplete_schema.yml",

                schemaClass = FhirTransformSchema::class.java,
                schemaServiceProviders =
                    SchemaReferenceResolverHelper.getSchemaServiceProviders(
                        mockk<BlobAccess.BlobContainerMetadata>()
                    )
            )
        }

        assertThat(
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transforms/sample_schema.yml",
                schemaClass = FhirTransformSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            ).isValid()
        ).isTrue()

        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transforms/invalid_value_set.yml",
                schemaClass = FhirTransformSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        }

        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transformsincomplete_schema.yml",
                schemaClass = FhirTransformSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        }

        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transforms/no_schema_nor_value.yml",
                schemaClass = FhirTransformSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        }
    }

    @Test
    fun `test read FHIR Transform from file with extends`() {
        assertFailure {
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transforms/circular_schema.yml",
                schemaClass = FhirTransformSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        }.messageContains("Schema circular dependency")

        assertThat(
            ConfigSchemaReader.fromFile(
                "classpath:/fhir_sender_transforms/extends_schema.yml",
                schemaClass = FhirTransformSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            ).isValid()
        ).isTrue()
    }

    @Test
    fun `test extends schema with URI`() {
        assertThat(
            ConfigSchemaReader.fromFile(
                "classpath:/fhirengine/translation/hl7/schema/schema-read-test-07/ORU_R01.yml",

                schemaClass = HL7ConverterSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        )
    }

    @Test
    fun `test read extended schema from file`() {
        // This is a good schema
        val schema = ConfigSchemaReader.readSchemaTreeRelative(
            "ORU_R01-extended",
            "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-01",
            schemaClass = HL7ConverterSchema::class.java
        )

        assertThat(schema.errors).isEmpty()
        assertThat(schema.name).isEqualTo("ORU_R01-extended") // match filename
        assertThat(schema.hl7Class).isEqualTo("ca.uhn.hl7v2.model.v251.message.ORU_R01")
        assertThat(schema.elements).isNotEmpty()

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
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-04",
                schemaClass = HL7ConverterSchema::class.java
            )
        }
    }

    @Test
    fun `test deep circular reference exception when loading schema`() {
        assertFailure {
            ConfigSchemaReader.readSchemaTreeRelative(
                "ORU_R01",
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-05",
                schemaClass = HL7ConverterSchema::class.java
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
            ConfigSchemaReader.readSchemaTreeUri(
                file.toURI(),
                schemaClass = HL7ConverterSchema::class.java,
                schemaServiceProviders =
                    SchemaReferenceResolverHelper.getSchemaServiceProviders(
                        mockk<BlobAccess.BlobContainerMetadata>()
                    )
            )
        )
    }

    @Test
    fun `reads a file with an azure protocol`() {
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val blobConnectionInfo = mockk<BlobAccess.BlobContainerMetadata>()
        every { blobConnectionInfo.getBlobEndpoint() } returns "http://endpoint/metadata"
        every { BlobAccess.downloadBlobAsByteArray(any<String>(), any()) } returns
            File(
                "src/test/resources/fhirengine/translation/hl7/schema/schema-read-test-07",
                "ORU_R01.yml"
            ).readBytes()
        assertThat(
            ConfigSchemaReader.readSchemaTreeUri(
                URI(
                    """
                   azure:/hl7_mapping/schema/schema-read-test-07/ORU_R01.yml
                    """.trimIndent()
                ),
                schemaClass = HL7ConverterSchema::class.java,
                schemaServiceProviders = SchemaReferenceResolverHelper.getSchemaServiceProviders(blobConnectionInfo)
            )
        )
        verify(exactly = 1) {
            BlobAccess.downloadBlobAsByteArray(
                "http://endpoint/metadata/hl7_mapping/schema/schema-read-test-07/ORU_R01.yml",
                blobConnectionInfo
            )
        }
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
                schemaClass = HL7ConverterSchema::class.java,
                SchemaReferenceResolverHelper.getSchemaServiceProviders(mockk<BlobAccess.BlobContainerMetadata>())
            )
        }.messageContains("Schema circular dependency")
    }
}