package gov.cdc.prime.router

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import assertk.assertions.startsWith
import assertk.assertions.support.expected
import assertk.assertions.support.show
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockkClass
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class FileNameTemplateIntegrationTests {
    private val literal = """
        ---
        elements:
          - literal(cdcprime)
    """.trimIndent()
    private val fileIdTemplate = """
        ---
        elements:
            - uuid()
    """.trimIndent()
    private val config = mockkClass(Hl7Configuration::class).also {
        every { it.receivingApplicationName }.returns("receiving application")
        every { it.receivingFacilityName }.returns("receiving facility")
        every { it.receivingOrganization }.returns("yoyodyne")
    }
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )
    private val metadata = Metadata.getInstance()
    private val dateFormat = "yyyyMMdd"
    private val formatter = DateTimeFormatter.ofPattern(dateFormat)
    private val reportId = UUID.randomUUID()
    private var formattedDate: String? = null

    private fun createFileName(yaml: String): FileNameTemplate {
        val inputStream = yaml.byteInputStream()
        return mapper.readValue(inputStream)
    }

    @BeforeTest
    fun setFormattedDate() {
        formattedDate = formatter.format(OffsetDateTime.now())
    }

    @Test
    fun `test literal name element`() {
        val literalElement = Literal()
        val expected = "cdcprime"
        val actual = literalElement.getElementValue(listOf(expected))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test reading literal name element from yaml`() {
        val fileName = mapper.readValue<FileNameTemplate>(literal)
        val actual = fileName.getFileName(reportId = reportId)
        assertThat(actual).isEqualTo("cdcprime")
    }

    @Test
    fun `test getting file name UUID`() {
        val fileName = mapper.readValue<FileNameTemplate>(fileIdTemplate)
        val actual = fileName.getFileName(reportId = reportId)
        assertThat(actual).isEqualTo(reportId.toString())
    }

    @Test
    fun `test receiving organization element`() {
        val receivingOrg = ReceivingOrganization()
        val expected = "yoyodyne"
        val actual = receivingOrg.getElementValue(translatorConfig = config)
        assertThat(actual).isEqualTo(expected)
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
        val actual = fileName.getFileName(translatorConfig = config, reportId = reportId)
        // assert
        assertThat(actual).isEqualTo(expected)
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
        val actual = fileName.getFileName(translatorConfig = config, reportId = reportId)
        // assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test rand6`() {
        // arrange
        val nameElementSerialized = """
            ---
                elements:
                    - cdcprime_
                    - rand6()
        """.trimIndent()
        val fileName = createFileName(nameElementSerialized)
        // act
        val actual = fileName.getFileName(translatorConfig = config, reportId = reportId)
        val actualLast6 = actual.takeLast(6)
        // assert
        assertThat(actualLast6.length).isEqualTo(6)
        assertThat { actualLast6.toInt() }.isSuccess()
    }

    @Test
    fun `test createdDate file name element with defaultFormat`() {
        val element = CreatedDate()
        val expectedStartsWithFormat = "yyyyMMddHH"
        val offsetDt = OffsetDateTime.now()
        val expectedStartsWith = DateTimeFormatter.ofPattern(expectedStartsWithFormat)
        val expected = expectedStartsWith.format(offsetDt)
        val actual = element.getElementValue()
        assertThat(actual).startsWith(expected)
    }

    @Test
    fun `test createdDate file name element with bogus format`() {
        val element = CreatedDate()
        val expected = ""
        val actual = element.getElementValue(listOf("As formats go, I'm bogus"))
        assertThat(actual).isEqualTo(expected)
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
        val actual = fileName.getFileName(translatorConfig = config, reportId = reportId)
        // assert
        assertThat(actual).isEqualTo(expected)
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
        val actual = fileName.getFileName(translatorConfig = config, reportId = reportId)
        // assert
        assertThat(actual).startsWith(expected)
    }

    @Test
    fun `test regexReplace file name element`() {
        val element = RegexReplace()
        val expected = "AcmeLabs"
        val actual = element.getElementValue(listOf(" Acme_Labs-", "[ _-]+", ""))
        assertThat(actual).isEqualTo(expected)
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
        val actual = fileName.getFileName(translatorConfig = config, reportId = reportId)
        // assert
        assertThat(actual).isEqualTo(expected)
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
        val actual = fileName.getFileName(translatorConfig = config, reportId = reportId)
        // assert
        assertThat(actual).isEqualTo(expected)
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
        val actual = fileName.getFileName(translatorConfig = config, reportId = reportId)
        // assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test schema base name simple case`() {
        val config = mockkClass(TranslatorConfiguration::class)
        every { config.schemaName }.returns("covid-19")
        SchemaBaseName().run {
            assertThat(this.getElementValue(emptyList(), config)).isEqualTo("covid-19")
        }
    }

    @Test
    fun `test schema base name complex case`() {
        val config = mockkClass(TranslatorConfiguration::class)
        every { config.schemaName }.returns("/dir1/sub-dir/complex-covid-19")
        SchemaBaseName().run {
            assertThat(this.getElementValue(emptyList(), config)).isEqualTo("complex-covid-19")
        }
    }

    @Test
    fun `load file name templates from metadata`() {
        assertThat(metadata.fileNameTemplates).isNotEmpty()
        assertThat(metadata.fileNameTemplates).containsKey("standard")
    }

    @Test
    fun `get processing mode from translation config`() {
        ProcessingModeCode().run {
            val hl7Config1 = mockkClass(Hl7Configuration::class)
            every { hl7Config1.processingModeCode }.returns("t")
            assertThat(this.getElementValue(emptyList(), hl7Config1)).isEqualTo("testing")

            val hl7Config2 = mockkClass(Hl7Configuration::class)
            every { hl7Config2.processingModeCode }.returns("p")
            assertThat(this.getElementValue(emptyList(), hl7Config2)).isEqualTo("production")

            val hl7Config3 = mockkClass(Hl7Configuration::class)
            every { hl7Config3.processingModeCode }.returns("d")
            assertThat(this.getElementValue(emptyList(), hl7Config3)).isEqualTo("development")

            val hl7Config4 = mockkClass(Hl7Configuration::class)
            every { hl7Config4.processingModeCode }.returns("junk data")
            assertThat(this.getElementValue(emptyList(), hl7Config4)).isEqualTo("testing")
        }
    }

    @Test
    fun `test APHL name format`() {
        val key = "aphl"
        assertThat(metadata.fileNameTemplates).containsKey(key)
        val aphlNameFormat = metadata.fileNameTemplates[key]
        assertThat(aphlNameFormat).isNotNull()
        val translationConfig = mockkClass(Hl7Configuration::class).also {
            every { it.receivingOrganization }.returns("laoph")
            every { it.processingModeCode }.returns("P")
        }
        val fileName = aphlNameFormat?.getFileName(translationConfig, reportId = reportId)
            ?: assertk.fail("error getting file name")
        assertThat(fileName).startsWith("cdcprime_cdcprime_laoph_production_production_$formattedDate")
    }

    @Test
    fun `test Ohio name format`() {
        val key = "ohio"
        assertThat(metadata.fileNameTemplates).containsKey(key)
        val ohioNameFormat = metadata.fileNameTemplates[key]
        val fileName = ohioNameFormat?.getFileName(null, reportId = reportId)
            ?: assertk.fail("error getting Ohio name")
        assertThat(fileName).startsWith("CDCPRIME_$formattedDate")
    }

    @Test
    fun `test aphl light name format`() {
        // arrange
        val key = "aphl_light"
        val config = mockkClass(Hl7Configuration::class).also {
            every { it.processingModeCode }.returns("P")
            every { it.receivingOrganization }.returns("laoph")
        }
        assertThat(metadata.fileNameTemplates).containsKey(key)
        // act
        val aphlNameFormat = metadata.fileNameTemplates[key]
        val fileName = aphlNameFormat?.getFileName(config, reportId = reportId)
            ?: assertk.fail("error getting aphl light file name")
        // assert
        assertThat(fileName).startsWith("cdcprime_laoph_production_$formattedDate")
    }

    companion object {
        private fun Assert<Map<String, *>>.containsKey(expected: String) = given { actual ->
            if (actual.containsKey(expected)) return@given
            expected("containsKey:${show(expected)} but key doesn't exist in map")
        }
    }
}