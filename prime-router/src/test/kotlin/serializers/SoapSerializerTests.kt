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

@JacksonXmlRootElement(localName = "stag:TestSoap12Message")
data class TestSoap12Payload(
    @field:JacksonXmlProperty(localName = "stag:TestSoap12Payload")
    val textFileContents: String
) : XmlObject

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoapSerializerTests {
    private val defaultNamespace = "http://reportstream.cdc.gov"
    private val namespaces = mapOf("xmlns:elr" to defaultNamespace)
    private val soap12namespaces = mapOf("xmlns:stag" to defaultNamespace)
    private val soapVersion = null

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
    fun `test serializing SOAP12 object to xml`() {
        val textFileContents = UUID.randomUUID().toString()
        val testSoap12Message = TestSoap12Payload(textFileContents = textFileContents)
        val actual = testSoap12Message.toXml()
        // spacing matters. Jackson uses two spaces, not tabs
        val expected = """
            <?xml version='1.0' encoding='UTF-8'?>
            <stag:TestSoap12Message>
              <stag:TestSoap12Payload>$textFileContents</stag:TestSoap12Payload>
            </stag:TestSoap12Message>
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
        val envelope = SoapEnvelope(testPayload, namespaces, soapVersion)
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

    @Test
    fun `test serializing a SOAP12 object in a soap envelope to xml`() {
        val textFileContents = UUID.randomUUID().toString()
        val testSoap12Message = TestSoap12Payload(textFileContents = textFileContents)
        val soapVersion = "SOAP12"
        val timestampId = "TS-" + (java.util.UUID.randomUUID())
        val timeCreated = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
        val timeExpired = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now().plusSeconds(180))
        val envelope = SoapEnvelope(testSoap12Message, soap12namespaces, soapVersion)
        val actual = envelope.toXml()
        val expected = """
            <?xml version='1.0' encoding='UTF-8'?>
            <soapenv:Envelope xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope" xmlns:stag="$defaultNamespace">
              <soapenv:Header>
                <wsse:Security soapenv:mustUnderstand="1" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
                  <wsu:Timestamp wsu:Id="$timestampId" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
                    <wsu:Created>$timeCreated</wsu:Created>
                    <wsu:Expires>$timeExpired</wsu:Expires>
                  </wsu:Timestamp>
                </wsse:Security>
              </soapenv:Header>
              <soapenv:Body>
                <stag:TestSoap12Message>
                  <stag:TestSoap12Payload>$textFileContents</stag:TestSoap12Payload>
                </stag:TestSoap12Message>
              </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()
        assertThat(
            actual.trim().replace("\r|\n".toRegex(), "")
                .replace(
                    "<wsu:Created>.*?</wsu:Created>".toRegex(),
                    "<wsu:Created>2023-11-20T17:08:48.229Z</wsu:Created>"
                )
                .replace(
                    "<wsu:Expires>.*?</wsu:Expires>".toRegex(),
                    "<wsu:Expires>2023-11-20T17:09:48.229Z</wsu:Expires>"
                )
                .replace(
                    "wsu:Id=\".*?\"".toRegex(),
                    "wsu:Id=\"TS-A766CDE2A9C666F2B317005001282431\""
                )
        ).isEqualTo(
            expected.trim().replace("\r|\n".toRegex(), "")
                .replace(
                    "<wsu:Created>.*?</wsu:Created>".toRegex(),
                    "<wsu:Created>2023-11-20T17:08:48.229Z</wsu:Created>"
                )
                .replace(
                    "<wsu:Expires>.*?</wsu:Expires>".toRegex(),
                    "<wsu:Expires>2023-11-20T17:09:48.229Z</wsu:Expires>"
                )
                .replace(
                    "wsu:Id=\".*?\"".toRegex(),
                    "wsu:Id=\"TS-A766CDE2A9C666F2B317005001282431\""
                )
        )
    }
}