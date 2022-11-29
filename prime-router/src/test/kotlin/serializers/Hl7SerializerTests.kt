package gov.cdc.prime.router.serializers

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.model.Varies
import ca.uhn.hl7v2.model.v251.datatype.CE
import ca.uhn.hl7v2.model.v251.datatype.CWE
import ca.uhn.hl7v2.model.v251.datatype.DLN
import ca.uhn.hl7v2.model.v251.datatype.DR
import ca.uhn.hl7v2.model.v251.datatype.DT
import ca.uhn.hl7v2.model.v251.datatype.DTM
import ca.uhn.hl7v2.model.v251.datatype.NM
import ca.uhn.hl7v2.model.v251.datatype.SN
import ca.uhn.hl7v2.model.v251.datatype.TS
import ca.uhn.hl7v2.model.v251.datatype.XTN
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.parser.ParserConfiguration
import ca.uhn.hl7v2.util.Terser
import ca.uhn.hl7v2.validation.ValidationContext
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.common.Hl7Utilities
import gov.cdc.prime.router.unittest.UnitTestUtils
import gov.cdc.prime.router.unittest.UnitTestUtils.createConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Hl7SerializerTests {
    private val context = DefaultHapiContext()
    private val emptyTerser = Terser(ORU_R01())

    @Test
    fun `test XTN phone decoding`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val mockTerser = mockk<Terser>()
        val mockSegment = mockk<Segment>()
        val emptyPhoneField = mockk<XTN>()
        val emailField = mockk<XTN>()
        val phoneField = mockk<XTN>()
        val deprecatedPhoneField = mockk<XTN>()
        val element = Element("phone", Element.Type.TELEPHONE, hl7Field = "PID-13")

        // Bad field value
        every { mockTerser.getSegment(any()) } returns null
        var phoneNumber = serializer.decodeHl7TelecomData(
            mockTerser, Element("phone", Element.Type.TELEPHONE),
            "PID-BLAH"
        )
        assertThat(phoneNumber).isEqualTo("")

        // Segment not found
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(phoneNumber).isEqualTo("")

        // No phone number due to zero repetitions
        every { mockTerser.getSegment(any()) } returns mockSegment
        every { mockSegment.getField(any()) } returns emptyArray()
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(phoneNumber).isEqualTo("")

        // No phone number
        every { mockSegment.getField(any()) } returns arrayOf(emptyPhoneField) // This is only to get the number of reps
        every { mockTerser.get(any()) } returns ""
        every { emptyPhoneField.areaCityCode.isEmpty } returns true
        every { emptyPhoneField.localNumber.isEmpty } returns true
        every { emptyPhoneField.telephoneNumber.isEmpty } returns true
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(phoneNumber).isEqualTo("")

        // Multiple repetitions with no phone number
        every { mockSegment.getField(any()) } returns arrayOf(emptyPhoneField, emptyPhoneField, emptyPhoneField)
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(phoneNumber).isEqualTo("")

        // Phone number in deprecated component
        every { deprecatedPhoneField.areaCityCode.isEmpty } returns true
        every { deprecatedPhoneField.localNumber.isEmpty } returns true
        every { deprecatedPhoneField.telephoneNumber.isEmpty } returns false
        every { deprecatedPhoneField.telephoneNumber.valueOrEmpty } returns "(555)555-5555"
        every { mockSegment.getField(any()) } returns arrayOf(deprecatedPhoneField)
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(phoneNumber).isEqualTo("5555555555:1:")

        // Phone number in newer components.  Will ignore phone number in deprecated component
        every { mockSegment.getField(any()) } returns arrayOf(phoneField)
        every { phoneField.areaCityCode.isEmpty } returns false
        every { phoneField.localNumber.isEmpty } returns false
        every { phoneField.telephoneNumber.value } returns "(555)555-5555"
        every { phoneField.telecommunicationEquipmentType.isEmpty } returns false
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns "PH"
        every { phoneField.countryCode.value } returns "1"
        every { phoneField.areaCityCode.value } returns "666"
        every { phoneField.localNumber.value } returns "7777777"
        every { phoneField.extension.value } returns "9999"
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(phoneNumber).isEqualTo("6667777777:1:9999")

        // No type assumed to be a phone number
        every { phoneField.telecommunicationEquipmentType.isEmpty } returns true
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns null
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(phoneNumber).isEqualTo("6667777777:1:9999")

        // A Fax number is not used
        every { phoneField.telecommunicationEquipmentType.isEmpty } returns false
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns "FX"
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(phoneNumber).isEqualTo("")

        // Test repetitions.  The first repetition for the XTN type can be empty when there is no primary phone number
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns "PH"
        every { emailField.areaCityCode.isEmpty } returns true
        every { emailField.localNumber.isEmpty } returns true
        every { emailField.telephoneNumber.isEmpty } returns true
        every { emailField.emailAddress.valueOrEmpty } returns "dummyemail@cdc.local"
        every { emailField.telecommunicationEquipmentType.isEmpty } returns false
        every { emailField.telecommunicationEquipmentType.valueOrEmpty } returns "Internet"
        every { emailField.telecommunicationUseCode.valueOrEmpty } returns "NET"
        every { mockSegment.getField(any()) } returns arrayOf(emptyPhoneField, emailField, phoneField)
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(phoneNumber).isEqualTo("6667777777:1:9999")
    }

    @Test
    fun `test XTN email decoding`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val mockTerser = mockk<Terser>()
        val mockSegment = mockk<Segment>()
        val emailField = mockk<XTN>()
        val phoneField = mockk<XTN>()
        val element = Element("email", Element.Type.EMAIL, hl7Field = "PID-13")

        // Bad field value
        every { mockTerser.getSegment(any()) } returns null
        var email = serializer.decodeHl7TelecomData(
            mockTerser, Element("email", Element.Type.EMAIL),
            "PID-BLAH"
        )
        assertThat(email).isEqualTo("")

        // Segment not found
        email = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(email).isEqualTo("")

        // No email number due to zero repetitions
        every { mockTerser.getSegment(any()) } returns mockSegment
        every { mockSegment.getField(any()) } returns emptyArray()
        email = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(email).isEqualTo("")

        // No email
        every { mockSegment.getField(any()) } returns arrayOf(phoneField)
        every { phoneField.telecommunicationEquipmentType.isEmpty } returns false
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns "PH"
        email = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(email).isEqualTo("")

        // Test repetitions.
        every { emailField.emailAddress.valueOrEmpty } returns "dummyemail@cdc.local"
        every { emailField.telecommunicationEquipmentType.isEmpty } returns false
        every { emailField.telecommunicationEquipmentType.valueOrEmpty } returns "Internet"
        every { mockSegment.getField(any()) } returns arrayOf(emailField, phoneField)
        email = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertThat(email).isEqualTo("dummyemail@cdc.local")
    }

    @Test
    fun `test date time decoding`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val mockTerser = mockk<Terser>()
        val mockSegment = mockk<Segment>()
        val mockTS = mockk<TS>()
        val mockDR = mockk<DR>()
        val mockDTM = mockk<DTM>()
        val now = OffsetDateTime.now()
        val nowAsDate = Date.from(now.toInstant())
        val dateTimeElement = Element("field", hl7Field = "OBX-14", type = Element.Type.DATETIME)
        val warnings = mutableListOf<ActionLogDetail>()
        val dateFormatterWithTimeZone = DateTimeFormatter.ofPattern(DateUtilities.datetimePattern)
        val dateFormatterNoTimeZone = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

        // Segment not found
        every { mockTerser.getSegment(any()) } returns null
        var dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertThat(dateTime).isEqualTo("")

        // Bad field value
        every { mockTerser.getSegment(any()) } returns mockSegment
        dateTime = serializer.decodeHl7DateTime(
            mockTerser, Element("field", hl7Field = "OBX-Blah"),
            "OBX-Blah", warnings
        )
        assertThat(dateTime).isEqualTo("")

        // No field value
        every { mockSegment.getField(any(), any()) } returns null
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertThat(dateTime).isEqualTo("")

        // Field value is TS, but no time
        every { mockSegment.getField(any(), any()) } returns mockTS
        every { mockTS.time } returns null
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertThat(dateTime).isEqualTo("")

        // Field value is TS has a time
        every { mockTS.time } returns mockDTM
        every { mockTS.time.valueAsDate } returns nowAsDate
        every { mockTS.time.value } returns dateFormatterWithTimeZone.format(now)
        every { mockTS.time.gmtOffset } returns 0
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertThat(dateTime).isEqualTo(dateFormatterWithTimeZone.format(now.withOffsetSameInstant(ZoneOffset.UTC)))

        // Field value is TS has a time, but no GMT offset
        every { mockTS.time } returns mockDTM
        val cal = Calendar.getInstance()
        cal.time = nowAsDate
        every { mockTS.time.valueAsCalendar } returns cal
        every { mockTS.time.value } returns dateFormatterWithTimeZone.format(now)
        every { mockTS.time.gmtOffset } returns -99
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertThat(dateTime).isEqualTo(dateFormatterWithTimeZone.format(now.withOffsetSameInstant(ZoneOffset.UTC)))

        // Field value is DR, but no range
        every { mockSegment.getField(any(), any()) } returns mockDR
        every { mockDR.rangeStartDateTime } returns null
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertThat(dateTime).isEqualTo("")

        // Field value is DR has a range, but with no time
        every { mockDR.rangeStartDateTime } returns mockTS
        every { mockDR.rangeStartDateTime.time } returns null
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertThat(dateTime).isEqualTo("")

        // Field value is DR and has a time
        every { mockDR.rangeStartDateTime } returns mockTS
        every { mockDR.rangeStartDateTime.time } returns mockDTM
        every { mockDR.rangeStartDateTime.time.valueAsDate } returns nowAsDate
        every { mockDR.rangeStartDateTime.time.gmtOffset } returns -99
        every { mockDR.rangeStartDateTime.time.valueAsCalendar } returns cal
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertThat(dateTime).isEqualTo(dateFormatterWithTimeZone.format(now.withOffsetSameInstant(ZoneOffset.UTC)))

        // Generate a warning for not having the timezone offsets
        every { mockDR.rangeStartDateTime } returns mockTS
        every { mockDR.rangeStartDateTime.time } returns mockDTM
        every { mockDR.rangeStartDateTime.time.valueAsDate } returns nowAsDate
        every { mockDR.rangeStartDateTime.time.value } returns dateFormatterNoTimeZone.format(now)
        warnings.clear()
        serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertThat(warnings.size == 1).isTrue()

        // Test a bit more the regex for the warning
        fun testForTimestampWarning(dateString: String, numExpectedWarnings: Int) {
            // Note the data type here does not affect how the raw string gets check for a warning
            every { mockDR.rangeStartDateTime.time.valueAsDate.toInstant() } returns Date().toInstant()
            every { mockDR.toString() } returns dateString
            every { mockSegment.getField(any(), any()) } returns mockDR
            warnings.clear()
            serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
            assertThat(warnings.size).isEqualTo(numExpectedWarnings)
        }

        testForTimestampWarning("TS[202101011200]", 1)
        testForTimestampWarning("TS[202101011200.0000]", 1)
        testForTimestampWarning("TS[2021010112-0400]", 1)
        testForTimestampWarning("DR[202101011200.0000-4000]", 0)
        testForTimestampWarning("TS[202101011200.0000+4000]", 0)
        testForTimestampWarning("DR[202101011259+4000]", 0)
    }

    @Test
    fun `test date decoding`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val mockTerser = mockk<Terser>()
        val mockSegment = mockk<Segment>()
        val mockDT = mockk<DT>()
        val dateElement = Element("field", hl7Field = "OBX-14", type = Element.Type.DATE)
        val dateFormatterDate = DateTimeFormatter.ofPattern(DateUtilities.datePattern)
        val warnings = mutableListOf<ActionLogDetail>()

        every { mockTerser.getSegment(any()) } returns mockSegment

        // Field value is DT with a date
        val date = LocalDate.of(1995, 1, 1)
        val formattedDate = dateFormatterDate.format(date)
        every { mockDT.toString() } returns formattedDate
        every { mockDT.year } returns date.year
        every { mockDT.month } returns date.monthValue
        every { mockDT.day } returns date.dayOfMonth
        every { mockSegment.getField(any(), any()) } returns mockDT
        val dateTime = serializer.decodeHl7DateTime(mockTerser, dateElement, dateElement.hl7Field!!, warnings)
        assertThat(dateTime).isEqualTo(formattedDate)

        // Test a bit more the regex for the warning
        fun testForDateWarning(dateString: String, numExpectedWarnings: Int) {
            every { mockDT.toString() } returns dateString
            every { mockDT.year } returns 1995
            every { mockDT.month } returns 1
            every { mockDT.day } returns 1
            every { mockSegment.getField(any(), any()) } returns mockDT
            warnings.clear()
            serializer.decodeHl7DateTime(mockTerser, dateElement, dateElement.hl7Field!!, warnings)
            assertThat(warnings.size).isEqualTo(numExpectedWarnings)
        }

        testForDateWarning("DT[19950101]", 0)
        testForDateWarning("TS[19950101120101.0001+4000]", 0)
        testForDateWarning("DT[199501]", 1)
        testForDateWarning("DT[1995]", 1)
    }

    @Test
    fun `test reading message with international characters from serializer`() {
        // Sample UTF-8 taken from https://www.kermitproject.org/utf8.html as a byte array, so we are not
        // restricted by the encoding of this code file
        val greekString = String(
            byteArrayOf(-50, -100, -49, -128, -50, -65, -49, -127, -49, -114),
            Charsets.UTF_8
        )

        // Java strings are stored as UTF-16
        val intMessage =
            """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||$greekString^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85||94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
NTE|1|L|This is a comment|RE
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600^202102090000-0600
NTE|1|L|This is a final comment|RE"""

        // arrange
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(intMessage, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        // assert
        assertThat(terser.get("/.PID-5-1")).isEqualTo(greekString)
    }

    @Test
    fun `test reading NTE segments into a single string value`() {
        // a simple message with a single comment
        val sampleMessage =
            """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Test^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85||94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
NTE|1|L|This is a comment|RE
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600^202102090000-0600"""

        arrangeTest(sampleMessage).run {
            // assert
            Hl7Serializer.decodeNTESegments(this).run {
                assertThat(this).isEqualTo("This is a comment")
            }
        }
        // a message with many comments
        val complexMessage =
            """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Test^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
NTE|1|L|This is patient comment 1|RE
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85||94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
NTE|1|L|This is order observation comment 1|RE
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
NTE|1|L|This is observation comment 1|RE
NTE|2|L|This is observation comment 2|RE
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600^202102090000-0600"""
        arrangeTest(complexMessage).run {
            // assert
            Hl7Serializer.decodeNTESegments(this).run {
                assertThat(this).isEqualTo(
                    "This is patient comment 1 This is order observation comment 1 " +
                        "This is observation comment 1 This is observation comment 2"
                )
            }
        }
    }

    @Test
    fun `test terser spec generator`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        assertThat(serializer.getTerserSpec("MSH-1-1")).isEqualTo("/MSH-1-1")
        assertThat(serializer.getTerserSpec("PID-1")).isEqualTo("/.PID-1")
        assertThat(serializer.getTerserSpec("")).isEqualTo("/.")
    }

    @Test
    fun `test setTelephoneComponents for patient`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val mockTerser = mockk<Terser>()
        every { mockTerser.set(any(), any()) } returns Unit
        every { mockTerser.get("/PATIENT_RESULT/PATIENT/PID-13(0)-2") } returns ""

        val patientPathSpec = serializer.formPathSpec("PID-13")
        val patientElement = Element("patient_phone_number", hl7Field = "PID-13", type = Element.Type.TELEPHONE)
        serializer.setTelephoneComponent(
            mockTerser,
            "5555555555:1:",
            patientPathSpec,
            patientElement,
            Hl7Configuration.PhoneNumberFormatting.ONLY_DIGITS_IN_COMPONENT_ONE
        )

        verify {
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-1", "5555555555")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-2", "PRN")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-3", "PH")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-5", "1")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-6", "555")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-7", "5555555")
        }
    }

    @Test
    fun `test setTelephoneComponents for facility`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val mockTerser = mockk<Terser>()
        every { mockTerser.set(any(), any()) } returns Unit

        val facilityPathSpec = serializer.formPathSpec("ORC-23")
        val facilityElement = Element(
            "ordering_facility_phone_number",
            hl7Field = "ORC-23",
            type = Element.Type.TELEPHONE
        )

        // InValid empty or blank telephone
        serializer.setTelephoneComponent(
            mockTerser,
            "",
            facilityPathSpec,
            facilityElement,
            Hl7Configuration.PhoneNumberFormatting.STANDARD
        )

        // Verify following methods never been called
        verify(exactly = 0) {
            mockTerser.set(any(), any())
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-2", "WPN")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-3", "PH")
        }

        // Invalid telephone
        serializer.setTelephoneComponent(
            mockTerser,
            "5555555555:1:3333",
            facilityPathSpec,
            facilityElement,
            Hl7Configuration.PhoneNumberFormatting.STANDARD
        )

        verify {
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-1", "(555)555-5555X3333")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-2", "WPN")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-3", "PH")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-5", "1")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-6", "555")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-7", "5555555")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-8", "3333")
        }

        // Valid telephone
        serializer.setTelephoneComponent(
            mockTerser,
            "8002324636:1:3333",
            facilityPathSpec,
            facilityElement,
            Hl7Configuration.PhoneNumberFormatting.STANDARD
        )

        verify {
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-1", "(800)232-4636X3333")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-2", "WPN")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-3", "PH")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-5", "1")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-6", "800")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-7", "2324636")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-8", "3333")
        }
    }

    @Test
    fun `testSetTelephoneComponentsValidatePhoneNumbers`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val mockTerser = mockk<Terser>()
        every { mockTerser.set(any(), any()) } returns Unit

        val facilityPathSpec = serializer.formPathSpec("ORC-23")
        val facilityElement = Element(
            "ordering_facility_phone_number",
            hl7Field = "ORC-23",
            type = Element.Type.TELEPHONE
        )

        // Valid (MX) telephone
        serializer.setTelephoneComponent(
            mockTerser,
            "5555555555:1:3333",
            facilityPathSpec,
            facilityElement,
            Hl7Configuration.PhoneNumberFormatting.STANDARD
        )

        verify {
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-1", "(555)555-5555X3333")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-2", "WPN")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-3", "PH")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-5", "1")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-6", "555")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-7", "5555555")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-8", "3333")
        }
    }

    @Test
    fun `test setCliaComponents`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val terser = spyk(emptyTerser)
        every { terser.set(any(), any()) } returns Unit

        serializer.setCliaComponent(
            terser,
            "XYZ",
            "OBX-23-10"
        )

        verify {
            terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-23-10", "XYZ")
        }
    }

    @Test
    fun `test setCliaComponents truncation`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)

        val terser = spyk(emptyTerser)
        every { terser.set(any(), any()) } returns Unit
        val configuration = createConfig(
            truncateHDNamespaceIds = false,
            truncateHl7Fields = "OBX-23-10, MSH-3-1" // Enables truncation on these fields
        )

        // The OBX-23-10 length is 20
        serializer.setCliaComponent(
            terser,
            "012345678901234567890123456789",
            "OBX-23-10",
            configuration
        )

        verify {
            terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-23-10", "01234567890123456789")
        }
    }

    @Test
    fun `test setCliaComponents in HD`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val terser = spyk(emptyTerser)
        every { terser.set(any(), any()) } returns Unit
        val hl7Field = "ORC-3-3"
        val value = "dummy"

        serializer.setCliaComponent(
            terser,
            value,
            hl7Field
        )

        verify {
            terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-3-3", value)
            terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-3-4", "CLIA")
        }
    }

    @Test
    fun `test setPlainOrderingFacility`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val mockTerser = mockk<Terser>()
        every { mockTerser.set(any(), any()) } returns Unit
        val facilityName = "Very Long Facility Name That Should Truncate After Here"
        serializer.setPlainOrderingFacility(mockTerser, facilityName)
        verify {
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-21-1", facilityName.take(50))
        }
    }

    @Test
    fun `test setNCESOrderingFacility`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val mockTerser = mockk<Terser>()
        every { mockTerser.set(any(), any()) } returns Unit
        val facilityName = "Very Long Facility Name That Should Truncate After Here"
        val ncesId = "A00000009"
        serializer.setNCESOrderingFacility(mockTerser, facilityName, ncesId)
        verify {
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-21-1", "${facilityName.take(32)}_NCES_$ncesId")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-21-6-1", "NCES.IES")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-21-6-2", "2.16.840.1.113883.3.8589.4.1.119")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-21-6-3", "ISO")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-21-7", "XX")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-21-10", ncesId)
        }
    }

    @Test
    fun `test canonicalSchoolName`() {
        val serializer = Hl7Utilities

        // Use NCES actual table values to test
        val senior = serializer.canonicalizeSchoolName("SHREWSBURY SR HIGH")
        assertThat(senior).isEqualTo("SHREWSBURY SENIOR HIGH")

        val stJohns = serializer.canonicalizeSchoolName("ST JOHN'S HIGH SCHOOL")
        assertThat(stJohns).isEqualTo("ST JOHNS HIGH")

        val sizer = serializer.canonicalizeSchoolName("SIZER SCHOOL: A NORTH CENTRAL CHARTER ESSENTIAL SCHOOL")
        assertThat(sizer).isEqualTo("SIZER NORTH CENTRAL CHARTER ESSENTIAL")

        val elem = serializer.canonicalizeSchoolName("NORTHERN LINCOLN ELEM.")
        assertThat(elem).isEqualTo("NORTHERN LINCOLN ELEMENTARY")

        val elem2 = serializer.canonicalizeSchoolName("WAKEFIELD HILLS EL. SCHOOL")
        assertThat(elem2).isEqualTo("WAKEFIELD HILLS ELEMENTARY")

        val jr1 = serializer.canonicalizeSchoolName("M. L. KING JR. MIDDLE SCHOOL")
        assertThat(jr1).isEqualTo("KING JR MIDDLE")

        val jr2 = serializer.canonicalizeSchoolName("CHURCHILL JR HIGH SCHOOL")
        assertThat(jr2).isEqualTo("CHURCHILL JUNIOR HIGH")

        val tahono = serializer.canonicalizeSchoolName("TOHONO O`ODHAM HIGH SCHOOL")
        assertThat(tahono).isEqualTo("TOHONO ODHAM HIGH")

        val possesive = serializer.canonicalizeSchoolName("ST TIMOTHY'S EPISCOPAL DAY SCHOOL")
        assertThat(possesive).isEqualTo("ST TIMOTHYS EPISCOPAL DAY")

        val tse = serializer.canonicalizeSchoolName("TSE'II'AHI' COMMUNITY SCHOOL")
        assertThat(tse).isEqualTo("TSE II AHI COMMUNITY")
    }

    @Test
    fun `test setTruncationLimitWithEncoding`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val testValueWithSpecialChars = "Test & Value ~ Text ^ String"
        val testValueNoSpecialChars = "Test Value Text String"
        val testLimit = 20
        val newLimitWithSpecialChars = serializer.getTruncationLimitWithEncoding(testValueWithSpecialChars, testLimit)
        val newLimitNoSpecialChars = serializer.getTruncationLimitWithEncoding(testValueNoSpecialChars, testLimit)

        assertEquals(newLimitWithSpecialChars, 16)
        assertEquals(newLimitNoSpecialChars, testLimit)
    }

    @Test
    fun `test truncateValue with truncated HD`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val hl7Config = createConfig(truncateHDNamespaceIds = true)
        val inputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & Value ~ T",
            "Test Value Text String" to "Test Value Text Stri"
        )
        for ((input, expected) in inputAndExpected) {
            val actual = serializer.trimAndTruncateValue(input, "MSH-4-1", hl7Config, emptyTerser)
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test truncateValue with HD`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val hl7Config = createConfig(
            truncateHDNamespaceIds = false,
            truncateHl7Fields = "MSH-4-1, MSH-3-1" // Enables truncation on these fields
        )
        // The truncation with encoding will subtract 2 from the length for every occurrence of a
        // special characters [&^~]. This is done because the HL7 parser escapes them by replacing them with a three
        // character string. For example, & will get replaced with \T\. This adds 2 to the length of the value.
        // Because of this, after getting the length truncated to 20 (HD truncation), the string value gets checked
        // one more time to accommodate for any especial characters.
        // "Test & Value ~ Text" truncates to "Test & Value ~ T" because the final string value will be
        // converted to "Test \T\ Value \R\ Test",
        // so the final string value with 20 characters will be equals to "Test \T\ Value \R\ T"
        val inputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & Value ~ T",
        )
        for ((input, expected) in inputAndExpected) {
            val actual = serializer.trimAndTruncateValue(input, "MSH-4-1", hl7Config, emptyTerser)
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test trimAndTruncate with NPI`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val hl7Config = createConfig(
            truncateHDNamespaceIds = false,
            suppressNonNPI = false,
            truncateHl7Fields = "ORC-12-1, OBR-16-1" // Enables truncation on this field
        )
        val inputAndExpected = mapOf(
            "1234567890" to "1234567890",
            "12345678901234567890" to "123456789012345",
        )
        for ((input, expected) in inputAndExpected) {
            val actual = serializer.trimAndTruncateValue(input, "ORC-12-1", hl7Config, emptyTerser)
            assertThat(actual).isEqualTo(expected)
            val actual2 = serializer.trimAndTruncateValue(input, "OBR-16-1", hl7Config, emptyTerser)
            assertThat(actual2).isEqualTo(expected)
        }
    }

    @Test
    fun `test getHl7MaxLength`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        // Test the ordering provider id has the right length
        assertThat(serializer.getHl7MaxLength("ORC-12-1", emptyTerser)).isEqualTo(15)
        assertThat(serializer.getHl7MaxLength("OBR-16-1", emptyTerser)).isEqualTo(15)
        // Test that MSH returns reasonable values
        assertThat(serializer.getHl7MaxLength("MSH-7", emptyTerser)).isEqualTo(26)
        assertThat(serializer.getHl7MaxLength("MSH-4-1", emptyTerser)).isEqualTo(20)
        assertThat(serializer.getHl7MaxLength("MSH-3-1", emptyTerser)).isEqualTo(20)
        assertThat(serializer.getHl7MaxLength("MSH-4-2", emptyTerser)).isEqualTo(199)
        assertThat(serializer.getHl7MaxLength("MSH-1", emptyTerser)).isEqualTo(1)
        // Test that OBX returns reasonable values
        assertThat(serializer.getHl7MaxLength("OBX-2", emptyTerser)).isEqualTo(2)
        assertThat(serializer.getHl7MaxLength("OBX-5", emptyTerser)).isEqualTo(99999)
        assertThat(serializer.getHl7MaxLength("OBX-11", emptyTerser)).isEqualTo(1)
        // This component limit is smaller than the enclosing field. This inconsistency was fixed by v2.9
        assertThat(serializer.getHl7MaxLength("OBX-18", emptyTerser)).isEqualTo(22)
        assertThat(serializer.getHl7MaxLength("OBX-18-1", emptyTerser)).isEqualTo(199)
        assertThat(serializer.getHl7MaxLength("OBX-19", emptyTerser)).isEqualTo(26)
        assertThat(serializer.getHl7MaxLength("OBX-23-1", emptyTerser)).isEqualTo(50)
        // Test that a subcomponent returns null
        assertThat(serializer.getHl7MaxLength("OBR-16-1-2", emptyTerser)).isNull()
    }

    @Test
    fun `test unicodeToAscii`() {
        // arrange
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(UnitTestUtils.simpleMetadata, settings)
        val unicodeInput: String = "ÀÁÂÃÄÅ, ÈÉÊË, Î, Ô, Ù, Ç"
        // act
        val expectedValue: String = "AAAAAA, EEEE, I, O, U, C"
        val actualValue: String = serializer.unicodeToAscii(unicodeInput)
        // assert
        assertThat(actualValue).isEqualTo(expectedValue)
    }

    @Ignore // Test case works locally but not in github. Build issue seems to be the one affecting it in remote branch.
    @Test
    fun `test write a message with Receiver for VT with HD truncation and OBX-23-1 with 50 chars`() {
        val inputStream = File("./src/test/unit_test_files/vt_test_file.csv").inputStream()
        val settings = FileSettings("./settings")
        val schema = "primedatainput/pdi-covid-19"
        val metadata = Metadata.getInstance()
        val serializer = Hl7Serializer(metadata, settings)
        val csvSerializer = CsvSerializer(metadata)

        val hl7Config = mockkClass(Hl7Configuration::class).also {
            every { it.replaceValue }.returns(mapOf("MSH-3-1" to "CDC PRIME - Atlanta,"))
            every { it.format }.returns(Report.Format.HL7)
            every { it.useTestProcessingMode }.returns(false)
            every { it.suppressQstForAoe }.returns(false)
            every { it.suppressAoe }.returns(false)
            every { it.defaultAoeToUnknown }.returns(false)
            every { it.suppressHl7Fields }.returns(null)
            every { it.useBlankInsteadOfUnknown }.returns(null)
            every { it.convertTimestampToDateTime }.returns(null)
            every { it.truncateHDNamespaceIds }.returns(true)
            every { it.truncateHl7Fields }.returns(null)
            every { it.suppressNonNPI }.returns(false)
            every { it.phoneNumberFormatting }.returns(Hl7Configuration.PhoneNumberFormatting.STANDARD)
            every { it.usePid14ForPatientEmail }.returns(false)
            every { it.reportingFacilityName }.returns(null)
            every { it.reportingFacilityId }.returns(null)
            every { it.reportingFacilityIdType }.returns(null)
            every { it.cliaForOutOfStateTesting }.returns(null)
            every { it.valueSetOverrides }.returns((mapOf()))
            every { it.useOrderingFacilityName }.returns(Hl7Configuration.OrderingFacilityName.STANDARD)
            every { it.cliaForSender }.returns(mapOf())
        }
        val receiver = mockkClass(Receiver::class).also {
            every { it.translation }.returns(hl7Config)
            every { it.format }.returns(Report.Format.HL7)
            every { it.organizationName }.returns("vt-dph")
        }

        val testReport = csvSerializer.readExternal(schema, inputStream, listOf(TestSource), receiver).report
        val output = serializer.createMessage(testReport, 0)
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(output, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)

        // assert
        assertThat(terser.get("/MSH-4-1")).isEqualTo("High Meadow")
        assertThat(terser.get("/MSH-4-1").length).isLessThanOrEqualTo(20)
        println("Value of MSH-3-1: " + terser.get("/MSH-3-1"))
        assertThat(terser.get("/MSH-3-1").length).isLessThanOrEqualTo(20)
        assertThat(
            terser.get(
                "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-23-1"
            ).length
        ).isLessThanOrEqualTo(50)
        assertThat(
            terser.get(
                "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-23-1"
            )
        ).isEqualTo("High Meadow")
        assertThat(output).isNotNull()
    }

    @Test
    fun `generate empty HL7 batch`() {
        // Setup
        val oneOrganization = DeepOrganization(
            "phd", "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(Receiver("elr", "phd", Topic.TEST, CustomerStatus.INACTIVE, "one"))
        )
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val serializer = Hl7Serializer(metadata, settings)
        val report1 = Report(one, emptyList(), source = TestSource, metadata = metadata)
        val outputStream = ByteArrayOutputStream()

        // Act
        serializer.writeBatch(
            report1,
            outputStream,
            "sending_app",
            "receiving_app",
            "receiving_facility"
        )
        val result = String(outputStream.toByteArray()).split('\r')

        // Assert

        // should be 4 lines and a newline at the end
        assertEquals(5, result.size)
        // should have FHS, BHS, BTS, FTS
        assertEquals(true, result[0].startsWith("FHS"))
        assertEquals(true, result[1].startsWith("BHS"))
        assertEquals(true, result[2].startsWith("BTS"))
        assertEquals(true, result[3].startsWith("FTS"))
        assertEquals(0, result[4].length)
        // should have passed in sending app, receiving app, receiving facility
        val parts = result[0].split('|')
        assertEquals("sending_app", parts[2])
        assertEquals("receiving_app", parts[4])
        assertEquals("receiving_facility", parts[5])
    }

    @Test
    fun `testOrganizationYmlReplaceValueAwithBUsingTerserSettingField`() {
        val oneOrganization = DeepOrganization(
            "phd", "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(
                Receiver(
                    "elr", "phd", Topic.TEST,
                    CustomerStatus.INACTIVE, "one"
                )
            )
        )
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val serializer = Hl7Serializer(metadata, settings)

        val message = ORU_R01()
        message.initQuickstart(Hl7Serializer.MESSAGE_CODE, Hl7Serializer.MESSAGE_TRIGGER_EVENT, "T")
        val terser = Terser(message)

        // SEG: ORC, FIELD: 12, ELEMENT: 2
        val pathORCf12e2 = "/PATIENT_RESULT/ORDER_OBSERVATION/ORC-12-2"
        val pathORC = "/PATIENT_RESULT/ORDER_OBSERVATION/ORC"
        val pathOBR = "/PATIENT_RESULT/ORDER_OBSERVATION/OBR"
        // SEG: OBX, FIELD: 3, ELEMENT: 1
        val pathOBXf3e1 = "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-3-1"
        val pathOBXf3e2 = "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-3-2"
        val pathOBXf3e3 = "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-3-3"
        val pathOBXf17e1 = "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-17-1"
        val pathOBXf17e2 = "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-17-2"
        // SEG: OBX, FIELD: 17, REP: 0, ELEMENT: 1
        val pathOBXf17r0e1 = "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-17(0)-1"
        val pathOBXf17r0e3 = "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-17(0)-3"
        val pathOBXf17r1e1 = "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-17(1)-1"
        val pathOBXf17r1e3 = "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-17(1)-3"
        val pathSPM = "/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM"

        // Set known values
        terser.set("MSH-3", "PHX.ProviderReportingService")
        terser.set("MSH-10", "0987654321") // Message ID
        terser.set("MSH-11-1", "P")
        terser.set(pathOBXf3e1, "94534-5")
        terser.set(pathOBXf3e2, "SARS Old String")
        terser.set(pathOBXf3e3, "LN")
        terser.set(pathOBXf17e1, "PhoenixDx SARS-CoV-2 Multiplex_Trax Management Services Inc.")
        terser.set(pathOBXf17e2, "SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by NAA with probe")

        terser.set("$pathSPM-2-1-1", "1234567")
        terser.set("$pathSPM-2-1-2", "WASHINGTON TEST SITE")
        terser.set("$pathSPM-2-1-3", "123456789")
        terser.set("$pathSPM-2-1-4", "NPI")

        terser.set("$pathSPM-2-2-1", "2345678")
        terser.set("$pathSPM-2-2-2", "NEW YORK TEST SITE")
        terser.set("$pathSPM-2-2-3", "234567890")
        terser.set("$pathSPM-2-2-4", "NPI")

        val msh3ValuePair = arrayListOf(mapOf("*" to "CDC PRIME - Atlanta^2.16.840.1.114222.4.1.237821^ISO"))
        val mshf11e1Values = arrayListOf(mapOf("*" to "D"))
        val unKnownValuePair = arrayListOf(mapOf("*" to "Unknown"))
        val obxValuePair = arrayListOf(mapOf("*" to "OBX31^OBX32^OBX33"))
        // Note ths OBX-17 contains field repeator (~)
        val obxf17ValuePair = arrayListOf(mapOf("*" to "OBX-17(0)-1^^OBX-17(0)-3~OBX-17(1)-1^^OBX-17(1)-3"))
        val spmValuePair = arrayListOf(mapOf("*" to "646&Wichita TEST SITE&123&NPI"))

        // Unit test for replace "" (Blank) and Not replace if something is there (ORC-4-1)
        // terser.set("$pathORC-2-2", "SA.OTCSelfReport")
        terser.set("$pathORC-2-3", "1234567890")
        terser.set("$pathORC-2-4", "CLIA")
        terser.set("$pathORC-3-1", "")
        terser.set("$pathORC-4-1", "NOT REPLACE")
        val replaceBlankWithValueRef = arrayListOf(mapOf("" to "*MSH-10"))
        val orcValuePairReplaceBlank = arrayListOf(mapOf("" to "REPLACED BLANK"))
        val orcValuePairNotReplaceBlank = arrayListOf(mapOf("" to "XYZ"))

        val replaceValueAwithB: Map<String, Any>? = mapOf(
            "ORC-2-1" to replaceBlankWithValueRef, // We didn't set this field. Therefore, it is empty.
            "ORC-2-2" to orcValuePairReplaceBlank, // We didn't set this field. Therefore, it is empty.
            "ORC-3" to orcValuePairReplaceBlank,
            "ORC-4" to orcValuePairNotReplaceBlank, // Don't replace bcz something is in there
            "OBR-2-1" to replaceBlankWithValueRef, // We didn't set this field. Therefore, it is empty.
            "MSH-3" to msh3ValuePair,
            "MSH-11-1" to mshf11e1Values,
            // Note for the value=""/blank/null/empty is same as the value is not in HL7 file.
            // .. Hl7Serializer.kt will not set to any value.  Therefore,
            // .. if replaceValueAwithB contains:
            // ..   ORC-12-2: ["*":"unKnow"], it will add "unKnown" value to the component
            "ORC-12-2" to unKnownValuePair,
            "OBX-3" to obxValuePair,
            "OBX-17" to obxf17ValuePair,
            "SPM-2" to spmValuePair
        )

        replaceValueAwithB?.let {
            serializer.replaceValueAwithBUsingTerser(
                replaceValueAwithB, terser,
                message.patienT_RESULT.ordeR_OBSERVATION.observationReps
            )

            // Check ORC-2-1 (was blank) replaced with content of MSH-10 which is 0987654321
            assertThat(terser.get("$pathORC-2-1")).isEqualTo("0987654321")
            assertThat(terser.get("$pathORC-2-2")).isEqualTo("REPLACED BLANK")
            assertThat(terser.get("$pathORC-3-1")).isEqualTo("REPLACED BLANK")
            assertThat(terser.get("$pathORC-4-1")).isEqualTo("NOT REPLACE")

            // Check OBR-2-1 (was blank) replaced with content of MSH-10 which is 0987654321
            assertThat(terser.get("$pathOBR-2-1")).isEqualTo("0987654321")

            assertThat(terser.get("MSH-3-1")).isEqualTo("CDC PRIME - Atlanta")
            assertThat(terser.get("MSH-3-2")).isEqualTo("2.16.840.1.114222.4.1.237821")
            assertThat(terser.get("MSH-3-3")).isEqualTo("ISO")
            assertThat(terser.get("MSH-11-1")).isEqualTo("D")

            assertThat(terser.get(pathORCf12e2)).isEqualTo("Unknown")

            assertThat(terser.get(pathOBXf3e1)).isEqualTo("OBX31")
            assertThat(terser.get(pathOBXf3e2)).isEqualTo("OBX32")
            assertThat(terser.get(pathOBXf3e3)).isEqualTo("OBX33")

            assertThat(terser.get(pathOBXf17r0e1)).isEqualTo("OBX-17(0)-1")
            assertThat(terser.get(pathOBXf17r0e3)).isEqualTo("OBX-17(0)-3")
            assertThat(terser.get(pathOBXf17r1e1)).isEqualTo("OBX-17(1)-1")
            assertThat(terser.get(pathOBXf17r1e3)).isEqualTo("OBX-17(1)-3")

            assertThat(terser.get("$pathSPM-2-1-1")).isEqualTo("646")
            assertThat(terser.get("$pathSPM-2-1-2")).isEqualTo("Wichita TEST SITE")
            assertThat(terser.get("$pathSPM-2-1-3")).isEqualTo("123")
            assertThat(terser.get("$pathSPM-2-1-4")).isEqualTo("NPI")

            assertThat(terser.get("$pathSPM-2-2-1")).isEqualTo("2345678")
            assertThat(terser.get("$pathSPM-2-2-2")).isEqualTo("NEW YORK TEST SITE")
            assertThat(terser.get("$pathSPM-2-2-3")).isEqualTo("234567890")
            assertThat(terser.get("$pathSPM-2-2-4")).isEqualTo("NPI")
        }

        // Test case for exact match
        val arrayistValuesReplace = arrayListOf(
            mapOf(
                "PHX.ProviderReportingService" to
                    "CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO"
            )
        )

        // Reset
        terser.set("MSH-3", "PHX.ProviderReportingService")
        terser.set("MSH-11-1", "P")
        val replaceValueAwithBReplace = mapOf("MSH-3" to arrayistValuesReplace)
        replaceValueAwithB?.let {
            serializer.replaceValueAwithBUsingTerser(
                replaceValueAwithBReplace, terser,
                message.patienT_RESULT.ordeR_OBSERVATION.observationReps
            )
            assertThat(terser.get("MSH-3-1")).isEqualTo("CDC PRIME - Atlanta, Georgia (Dekalb)")
            assertThat(terser.get("MSH-3-2")).isEqualTo("2.16.840.1.114222.4.1.237821")
            assertThat(terser.get("MSH-3-3")).isEqualTo("ISO")
            assertThat(terser.get("MSH-11-1")).isEqualTo("P") // Still since we did have it in the replace list
            assertThat(terser.get(pathORCf12e2)).isEqualTo("Unknown") // Left over from above terser modification.
        }

        // Test case exact match But not replace since the value in terser and new is NOT match.
        val arrayistValues = arrayListOf(
            mapOf(
                "PHX.ProviderReportingService" to
                    "CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO"
            )
        )

        // Reset
        terser.set("MSH-3", "Don't replace me")
        val arrayistValuesNotReplace = mapOf("MSH-3" to arrayistValues)
        replaceValueAwithB?.let {
            serializer.replaceValueAwithBUsingTerser(
                arrayistValuesNotReplace, terser,
                message.patienT_RESULT.ordeR_OBSERVATION.observationReps
            )
            assertThat(terser.get("MSH-3-1")).isEqualTo("Don't replace me")
        }
    }

    // test the extraction of the observation value from different types of data
    @Test
    fun `test decoding obx observation value`() {
        // arrange
        val validationContext = mockk<ValidationContext>()
        every { validationContext.getPrimitiveRules(any(), any(), any()) } returns mutableListOf()
        val parserConfiguration = spyk<ParserConfiguration>()
        val parser = mockk<ca.uhn.hl7v2.parser.Parser>()
        every { parser.validationContext } returns validationContext
        every { parser.parserConfiguration } returns parserConfiguration
        val message = mockk<Message>()
        every { message.parser } returns parser
        every { message.version } returns "2.5.1"
        val varies = Varies(message)
        // arrange our special checks, first SN
        val sn = SN(message)
        varies.data = sn
        sn.num1.value = "100"
        sn.comparator.value = ">"
        assertThat(Hl7Serializer.decodeObxIdentifierValue(varies)).isEqualTo("100")
        // check NM
        val nm = NM(message)
        varies.data = nm
        nm.value = "47"
        assertThat(Hl7Serializer.decodeObxIdentifierValue(varies)).isEqualTo("47")
        // check DT
        val dt = DT(message)
        varies.data = dt
        dt.value = "19000101"
        assertThat(Hl7Serializer.decodeObxIdentifierValue(varies)).isEqualTo("19000101")
        // check CWE
        val cwe = CWE(message)
        varies.data = cwe
        cwe.cwe1_Identifier.value = "N"
        cwe.cwe2_Text.value = "No"
        cwe.cwe3_NameOfCodingSystem.value = "HL70136"
        assertThat(Hl7Serializer.decodeObxIdentifierValue(varies)).isEqualTo("N")
        // check CE
        val ce = CE(message)
        varies.data = ce
        ce.ce1_Identifier.value = "N"
        ce.ce2_Text.value = "No"
        ce.ce3_NameOfCodingSystem.value = "HL70136"
        assertThat(Hl7Serializer.decodeObxIdentifierValue(varies)).isEqualTo("N")
        // check DLN, which is a driver's license number, and I didn't encode for this.
        // this checks the logic that manually parses and teases out the data
        val dlNumber = "9999999"
        val dln = DLN(message)
        varies.data = dln
        dln.dln1_LicenseNumber.value = dlNumber
        dln.dln2_IssuingStateProvinceCountry.value = "NJ"
        assertThat(Hl7Serializer.decodeObxIdentifierValue(varies)).isEqualTo(dlNumber)
    }

    @Test
    fun `parse a complex message with many AOEs`() {
        // a message with many AOEs
        val complexMessage =
            """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Test^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
NTE|1|L|This is patient comment 1|RE
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85||94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
NTE|1|L|This is order observation comment 1|RE
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
NTE|1|L|This is observation comment 1|RE
NTE|2|L|This is observation comment 2|RE
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600^202102090000-0600
OBX|2|NM|30525-0^Age^LN||14|a^year^UCUM
OBX|3|DLN|53245-7^Driver license^LN||99999999^NJ|a^year^UCUM
"""
        arrangeTest(complexMessage).run {
            // assert
            Hl7Serializer.decodeAOEQuestion(
                Element(name = "symptomatic", hl7AOEQuestion = "95419-8"),
                this,
                0
            ).run {
                assertThat(this).isEqualTo("N")
            }
            Hl7Serializer.decodeAOEQuestion(
                Element(name = "congregate_care", hl7AOEQuestion = "95421-4"),
                this,
                0
            ).run {
                assertThat(this).isEqualTo("N")
            }
            Hl7Serializer.decodeAOEQuestion(
                Element(name = "first_test", hl7AOEQuestion = "95417-2"),
                this,
                0
            ).run {
                assertThat(this).isEqualTo("Y")
            }
            Hl7Serializer.decodeAOEQuestion(
                Element(name = "patient_employed_in_healthcare", hl7AOEQuestion = "30525-0"),
                this,
                0
            ).run {
                assertThat(this).isEqualTo("14")
            }
            Hl7Serializer.decodeAOEQuestion(
                Element(name = "patient_employed_in_healthcare", hl7AOEQuestion = "95418-0"),
                this,
                0
            ).run {
                assertThat(this).isEqualTo("Y")
            }
            // one we don't have encoded as a datatype
            Hl7Serializer.decodeAOEQuestion(
                Element(name = "patient_drivers_license", hl7AOEQuestion = "53245-7"),
                this,
                0
            ).run {
                assertThat(this).isEqualTo("99999999")
            }
            // one we don't have in the message
            Hl7Serializer.decodeAOEQuestion(
                Element(name = "pregnant", hl7AOEQuestion = "82810-3"),
                this,
                0
            ).run {
                assertThat(this).isEqualTo("")
            }
        }
    }

    /**
     * Given a string message, parse it and turn it into a message that can be interacted with
     * @param rawMessage - the raw string message to parse
     */
    private fun arrangeTest(rawMessage: String): Message {
        // arrange
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(rawMessage, "\r")
        return parser.parse(cleanedMessage)
    }
}