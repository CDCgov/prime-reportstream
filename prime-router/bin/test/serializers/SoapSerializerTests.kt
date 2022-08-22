package gov.cdc.prime.router.serializers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.test.Test

@JacksonXmlRootElement(localName = "elr:TestPayload")
data class TestSoapPayload(
    @field:JacksonXmlProperty(localName = "elr:UserName")
    val userName: String,
    @field:JacksonXmlProperty(localName = "elr:Password")
    val password: String,
    @field:JacksonXmlProperty(localName = "elr:Timestamp")
    val timestamp: String,
) : XmlObject

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoapSerializerTests {
    private val defaultNamespace = "http://reportstream.cdc.gov"
    private val namespaces = mapOf("xmlns:elr" to defaultNamespace)

    @Test
    fun `test serializing object to xml`() {
        val userName = UUID.randomUUID().toString()
        val password = UUID.randomUUID().toString()
        val timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
        val testPayload = TestSoapPayload(userName = userName, password = password, timestamp = timestamp)
        val actual = testPayload.toXml()
        // spacing matters. Jackson uses two spaces, not tabs
        val expected = """
            <?xml version='1.0' encoding='UTF-8'?>
            <elr:TestPayload>
              <elr:UserName>$userName</elr:UserName>
              <elr:Password>$password</elr:Password>
              <elr:Timestamp>$timestamp</elr:Timestamp>
            </elr:TestPayload>
        """.trimIndent()
        assertThat(
            actual.trim().replace("\r|\n".toRegex(), "")
        ).isEqualTo(
            expected.trim().replace("\r|\n".toRegex(), "")
        )
    }

    @Test
    fun `test serializing object in a soap envelope to xml`() {
        val userName = UUID.randomUUID().toString()
        val password = UUID.randomUUID().toString()
        val timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
        val testPayload = TestSoapPayload(userName = userName, password = password, timestamp = timestamp)
        val envelope = SoapEnvelope(testPayload, namespaces)
        val actual = envelope.toXml()
        val expected = """
            <?xml version='1.0' encoding='UTF-8'?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:elr="$defaultNamespace">
              <soapenv:Header/>
              <soapenv:Body>
                <elr:TestPayload>
                  <elr:UserName>$userName</elr:UserName>
                  <elr:Password>$password</elr:Password>
                  <elr:Timestamp>$timestamp</elr:Timestamp>
                </elr:TestPayload>
              </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()
        assertThat(
            actual.trim().replace("\r|\n".toRegex(), "")
        ).isEqualTo(
            expected.trim().replace("\r|\n".toRegex(), "")
        )
    }
}