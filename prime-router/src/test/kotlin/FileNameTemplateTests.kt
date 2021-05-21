package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockkClass
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class FileNameTemplateTests {
    private val literal = """
        ---
        elements:
          - literal(cdcprime)
    """.trimIndent()
    private val config = Hl7Configuration(
        receivingApplicationName = "receiving application",
        receivingApplicationOID = "",
        receivingFacilityName = "receiving facility",
        receivingFacilityOID = "",
        receivingOrganization = "yoyodyne",
        messageProfileId = null
    )
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    private fun createFileName(yaml: String): FileNameTemplate {
        val inputStream = yaml.byteInputStream()
        return mapper.readValue(inputStream)
    }

    @Test
    fun `test literal name element`() {
        val literalElement = Literal()
        val expected = "cdcprime"
        val actual = literalElement.getElementValue(listOf(expected))
        assertEquals(expected, actual)
    }

    @Test
    fun `test reading literal name element from yaml`() {
        val fileName = mapper.readValue<FileNameTemplate>(literal)
        val actual = fileName.getFileName()
        assertEquals("cdcprime", actual)
    }

    @Test
    fun `test receiving organization element`() {
        val receivingOrg = ReceivingOrganization()
        val expected = "yoyodyne"
        val actual = receivingOrg.getElementValue(translatorConfig = config)
        assertEquals(expected, actual)
    }

    @Test
    fun `test concatenating multiple elements`() {
        // arrange
        val nameElementSerialized = """
            ---
                elements:
                    - literal(cdcprime_)
                    - receivingOrganization()
        """.trimIndent()
        val expected = "cdcprime_yoyodyne"
        val fileName = createFileName(nameElementSerialized)
        // act
        val actual = fileName.getFileName(translatorConfig = config)
        // assert
        assertEquals(expected, actual)
    }

    @Test
    fun `test alternate literal syntax`() {
        // arrange
        val nameElementSerialized = """
            ---
                elements:
                    - cdcprime_
                    - receivingOrganization()
        """.trimIndent()
        val expected = "cdcprime_yoyodyne"
        val fileName = createFileName(nameElementSerialized)
        // act
        val actual = fileName.getFileName(translatorConfig = config)
        // assert
        assertEquals(expected, actual)
    }

    @Test
    fun `test createdDate file name element with defaultFormat`() {
        val element = CreatedDate()
        val expectedStartsWithFormat = "yyyyMMddHH"
        val offsetDt = OffsetDateTime.now()
        val expectedStartsWith = DateTimeFormatter.ofPattern(expectedStartsWithFormat)
        val expected = expectedStartsWith.format(offsetDt)
        val actual = element.getElementValue()
        assertTrue(actual.startsWith(expected))
    }

    @Test
    fun `test createdDate file name element with bogus format`() {
        val element = CreatedDate()
        val expected = ""
        val actual = element.getElementValue(listOf("As formats go, I'm bogus"))
        assertEquals(expected, actual)
    }

    @Test
    fun `test createdDate file name element with supplied format`() {
        // arrange
        val expectedStartsWithFormat = "yyyyMMddHH"
        val nameElementSerialized = """
            ---
                elements:
                    - createdDate($expectedStartsWithFormat)
        """.trimIndent()
        val offsetDt = OffsetDateTime.now()
        val expectedStartsWith = DateTimeFormatter.ofPattern(expectedStartsWithFormat)
        val expected = expectedStartsWith.format(offsetDt)
        val fileName = createFileName(nameElementSerialized)
        // act
        val actual = fileName.getFileName(translatorConfig = config)
        // assert
        assertEquals(expected, actual)
    }

    @Test
    fun `test createdDate file name element with no format`() {
        // arrange
        val expectedStartsWithFormat = "yyyyMMddHH"
        val nameElementSerialized = """
            ---
                elements:
                    - createdDate()
        """.trimIndent()
        val offsetDt = OffsetDateTime.now()
        val expectedStartsWith = DateTimeFormatter.ofPattern(expectedStartsWithFormat)
        val expected = expectedStartsWith.format(offsetDt)
        val fileName = createFileName(nameElementSerialized)
        // act
        val actual = fileName.getFileName(translatorConfig = config)
        // assert
        assertTrue(actual.startsWith(expected), "Actual was: $actual")
    }

    @Test
    fun `test regexReplace file name element`() {
        val element = RegexReplace()
        val expected = "AcmeLabs"
        val actual = element.getElementValue(listOf(" Acme_Labs-", "[ _-]+", ""))
        assertEquals(expected, actual)
    }

    @Test
    fun `test lower case file name`() {
        // arrange
        val nameElementSerialized = """
            ---
                elements:
                    - cdcprime_
                    - receivingOrganization()
                lowerCase: true
        """.trimIndent()
        val expected = "cdcprime_yoyodyne"
        val fileName = createFileName(nameElementSerialized)
        // act
        val actual = fileName.getFileName(translatorConfig = config)
        // assert
        assertEquals(expected, actual)
    }

    @Test
    fun `test upper case file name`() {
        // arrange
        val nameElementSerialized = """
            ---
                elements:
                    - cdcprime_
                    - receivingOrganization()
                upperCase: true
        """.trimIndent()
        val expected = "CDCPRIME_YOYODYNE"
        val fileName = createFileName(nameElementSerialized)
        // act
        val actual = fileName.getFileName(translatorConfig = config)
        // assert
        assertEquals(expected, actual)
    }

    @Test
    fun `test file name with no case change`() {
        // arrange
        val nameElementSerialized = """
            ---
                elements:
                    - CdcPrime_
                    - receivingOrganization()
        """.trimIndent()
        val expected = "CdcPrime_yoyodyne"
        val fileName = createFileName(nameElementSerialized)
        // act
        val actual = fileName.getFileName(translatorConfig = config)
        // assert
        assertEquals(expected, actual)
    }

    @Test
    fun `test schema base name simple case`() {
        val config = mockkClass(TranslatorConfiguration::class)
        every { config.schemaName }.returns("covid-19")
        SchemaBaseName().run {
            assertEquals("covid-19", this.getElementValue(emptyList(), config))
        }
    }

    @Test
    fun `test schema base name complex case`() {
        val config = mockkClass(TranslatorConfiguration::class)
        every { config.schemaName }.returns("/dir1/sub-dir/complex-covid-19")
        SchemaBaseName().run {
            assertEquals("complex-covid-19", this.getElementValue(emptyList(), config))
        }
    }

    @Test
    fun `test schema base name error case`() {
        val config: TranslatorConfiguration? = null
        SchemaBaseName().run {
            assertFails { this.getElementValue(emptyList(), config) }
        }
    }

    @Test
    fun `load file name templates from metadata`() {
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        assertTrue(metadata.fileNameTemplates.isNotEmpty())
        assertTrue(metadata.fileNameTemplates.containsKey("standard"))
    }

    @Test
    fun `get processing mode from translation config`() {
        ProcessingModeCode().run {
            val hl7Config1 = mockkClass(Hl7Configuration::class)
            every { hl7Config1.processingModeCode }.returns("t")
            assertEquals("testing", this.getElementValue(emptyList(), hl7Config1))

            val hl7Config2 = mockkClass(Hl7Configuration::class)
            every { hl7Config2.processingModeCode }.returns("p")
            assertEquals("production", this.getElementValue(emptyList(), hl7Config2))

            val hl7Config3 = mockkClass(Hl7Configuration::class)
            every { hl7Config3.processingModeCode }.returns("d")
            assertEquals("development", this.getElementValue(emptyList(), hl7Config3))

            val hl7Config4 = mockkClass(Hl7Configuration::class)
            every { hl7Config4.processingModeCode }.returns("junk data")
            assertEquals("testing", this.getElementValue(emptyList(), hl7Config4))
        }
    }
}