package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileNameTests {
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

    private fun createFileName(yaml: String): FileName {
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
        val fileName = mapper.readValue<FileName>(literal)
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
}