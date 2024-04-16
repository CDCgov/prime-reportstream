package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.model.Message
import gov.cdc.prime.router.ActionLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.apache.logging.log4j.kotlin.KotlinLogger
import java.text.SimpleDateFormat
import kotlin.test.Test

class HL7ReaderTests {
    @Test
    fun `test decoding of bad HL7 messages`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)

        // Empty data
        val badData1 = ""
        hl7Reader.getMessages(badData1)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Some CSV was sent
        val badData2 = """
            a,b,c
            1,2,3
        """.trimIndent()
        hl7Reader.getMessages(badData2)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()

        // Some truncated HL7
        val badData3 = "MSH|^~\\&#|MEDITECH^2.16.840.1.114222.4.3.2.2.1.321.111^ISO|COCAA^1.2."
        hl7Reader.getMessages(badData3)
        assertThat(actionLogger.hasErrors()).isTrue()
        actionLogger.logs.clear()
    }

    @Test
    fun `test decoding of good HL7 messages`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)

        val goodData1 = """
MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||195808101020304|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|6|CWE|82810-3^Pregnant^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
NTE|1|L|This is a note|RE
SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600
        """.trimIndent()
        var messages = hl7Reader.getMessages(goodData1)
        assertThat(actionLogger.hasErrors()).isFalse()
        assertThat(messages.size).isEqualTo(1)
        actionLogger.logs.clear()

        val goodData2 = """
FHS|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|||202102101707-0500
BHS|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|||202102101707-0500
MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|0cba76f5-35e0-4a28-803a-2f31308aae9b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|0cba76f5-35e0-4a28-803a-2f31308aae9b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600
MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|612092|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||a40ea680-51bc-4a05-bf23-786dd08e64f2^^^Avante at Ormond Beach^PI||Keeling^Tyson^Chuck^^^^L||19550206|M||2106-3^White^HL70005^^^^2.5.1|97065 Mohr Island^Street^North Taylor^TX^69622^^^^48077||7275555555:1:^PRN^^kenton.wilderman@email.com^1^281^2498561|||||||||U^Unknown^HL70189||||||||N
ORC|RE|4ba0f2c4-5f39-4716-9daa-d450573e7019|4ba0f2c4-5f39-4716-9daa-d450573e7019|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|4ba0f2c4-5f39-4716-9daa-d450573e7019|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600
BTS|2
FTS|1
        """.trimIndent()
        messages = hl7Reader.getMessages(goodData2)
        assertThat(actionLogger.hasErrors()).isFalse()
        assertThat(messages.size).isEqualTo(2)
        actionLogger.logs.clear()
    }

    @Test
    fun `test get message time stamp`() {
        val hl7Reader = HL7Reader(ActionLogger())

        fun getTestMessage(timestampStr: String): Message {
            val rawData = """
MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|$timestampStr||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
            """.trimIndent()
            val message = hl7Reader.getMessages(rawData)
            assertThat(message.size).isEqualTo(1)
            return message[0]
        }

        var timestampStr = ""
        var timestamp = HL7Reader.getMessageTimestamp(getTestMessage(timestampStr))
        assertThat(timestamp).isNull()

        timestampStr = "20210210170737"
        timestamp = HL7Reader.getMessageTimestamp(getTestMessage(timestampStr))
        assertThat(timestamp).isNotNull()
        assertThat(timestamp).isEqualTo(SimpleDateFormat("yyyyMMddHHmmss").parse(timestampStr))

        timestampStr = "20210210170737-0400"
        timestamp = HL7Reader.getMessageTimestamp(getTestMessage(timestampStr))
        assertThat(timestamp).isNotNull()
        assertThat(timestamp).isEqualTo(SimpleDateFormat("yyyyMMddHHmmssZ").parse(timestampStr))
    }

    @Test
    fun `test decoding of bad HL7 message - MSH Version ID missing`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)

        val goodData1 = """
MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P||||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
        """.trimIndent()
        hl7Reader.getMessages(goodData1)
        assertThat(actionLogger.hasErrors()).isTrue()
        assertThat(actionLogger.logs[0].detail.message).contains("Required field missing")
        actionLogger.logs.clear()
    }

    @Test
    fun `test decoding of bad HL7 message - OBX Wrong data type`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)

        val goodData1 = """
MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
OBX|1|test|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
        """.trimIndent()
        hl7Reader.getMessages(goodData1)
        assertThat(actionLogger.hasErrors()).isTrue()
        assertThat(actionLogger.logs[0].detail.message).contains("Data type error")
        actionLogger.logs.clear()
    }

    @Test
    fun `test isBatch with FSH Header`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)

        val goodData1 = """
            FHS|^~\&|||0.0.0.0.1|0.0.0.0.1|202106221314-0400
            BHS|^~\&|||0.0.0.0.1|0.0.0.0.1|202106221314-0400
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
            OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
            OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
            OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|6|CWE|82810-3^Pregnant^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            NTE|1|L|This is a note|RE
            SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600
            BTS|2
            FTS|1
        """.trimIndent()
        val messages = hl7Reader.getMessages(goodData1)
        val isBatch = hl7Reader.isBatch(goodData1, messages.size)
        assertThat(isBatch).isTrue()
    }

    @Test
    fun `test isBatch without FSH Header`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)

        val goodData1 = """          
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
            OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
            OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
            OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|6|CWE|82810-3^Pregnant^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            NTE|1|L|This is a note|RE
            SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
            OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
            OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
            OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|6|CWE|82810-3^Pregnant^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            NTE|1|L|This is a note|RE
            SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600      
        """.trimIndent()
        val messages = hl7Reader.getMessages(goodData1)
        val isBatch = hl7Reader.isBatch(goodData1, messages.size)
        assertThat(isBatch).isTrue()
    }

    @Test
    fun `test isBatch singular message`() {
        val actionLogger = ActionLogger()
        val hl7Reader = HL7Reader(actionLogger)

        val goodData1 = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
            OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
            OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
            OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            OBX|6|CWE|82810-3^Pregnant^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
            NTE|1|L|This is a note|RE
            SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600           
        """.trimIndent()
        val messages = hl7Reader.getMessages(goodData1)
        val isBatch = hl7Reader.isBatch(goodData1, messages.size)
        assertThat(isBatch).isFalse()
    }

    @Test
    fun `test getMessageType`() {
        val actionLogger = ActionLogger()
        val hL7Reader = HL7Reader(actionLogger)
        val justMSH = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO                                   
        """.trimIndent()
        val messages = hL7Reader.getMessages(justMSH)
        val type = HL7Reader.getMessageType(messages[0])
        assertThat(type).isEqualTo("ORU")
    }

    @Test
    fun `test getMessageProfile`() {
        val justMSH = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO                                   
        """.trimIndent()
        val messages = HL7Reader.getMessageProfile(justMSH)
        assertThat(messages).isEqualTo(
            HL7Reader.Companion.MessageProfile(
                "ORU",
                "NIST_ELR"
            )
        )

        val noProfile = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA                                   
        """.trimIndent()
        val messages2 = HL7Reader.getMessageProfile(noProfile)
        assertThat(messages2).isEqualTo(
            HL7Reader.Companion.MessageProfile(
                "ORU",
                ""
            )
        )
    }

    @Test
    fun `test getBirthTime_DateTime`() {
        val actionLogger = ActionLogger()
        val hL7Reader = HL7Reader(actionLogger)
        val birthTimeMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORM^O01^ORM_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||195808100102034|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val messages = hL7Reader.getMessages(birthTimeMessage)
        val type = HL7Reader.getBirthTime(messages[0])
        assertThat(type).isEqualTo("195808100102034")
    }

    @Test
    fun `test getBirthTime_Date`() {
        val actionLogger = ActionLogger()
        val hL7Reader = HL7Reader(actionLogger)
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORM^O01^ORM_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val messages = hL7Reader.getMessages(birthDateMessage)
        val type = HL7Reader.getBirthTime(messages[0])
        assertThat(type).isEqualTo("19580810")
    }

    @Test
    fun `test getPatientPath_ORM`() {
        val actionLogger = ActionLogger()
        val hL7Reader = HL7Reader(actionLogger)
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORM^O01^ORM_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val messages = hL7Reader.getMessages(birthDateMessage)
        val type = HL7Reader.getPatientPath(messages[0])
        assertThat(type).isEqualTo("PATIENT")
    }

    @Test
    fun `test getPatientPath_OML`() {
        val actionLogger = ActionLogger()
        val hL7Reader = HL7Reader(actionLogger)
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||OML^O21^OML_O21|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val messages = hL7Reader.getMessages(birthDateMessage)
        val type = HL7Reader.getPatientPath(messages[0])
        assertThat(type).isEqualTo("PATIENT")
    }

    @Test
    fun `test getPatientPath_ORU`() {
        val actionLogger = ActionLogger()
        val hL7Reader = HL7Reader(actionLogger)
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val messages = hL7Reader.getMessages(birthDateMessage)
        val type = HL7Reader.getPatientPath(messages[0])
        assertThat(type).isEqualTo("PATIENT_RESULT/PATIENT")
    }

    @Test
    fun `test getPatientPath_Other`() {
        val actionLogger = ActionLogger()
        val hL7Reader = HL7Reader(actionLogger)
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||TEST^O01^TEST_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val messages = hL7Reader.getMessages(birthDateMessage)
        val type = HL7Reader.getPatientPath(messages[0])
        assertThat(type).isEqualTo(null)
    }

    @Test
    fun `get getMessages no mapped models`() {
        val actionLogger = ActionLogger()
        val hl7Reader = spyk(HL7Reader(actionLogger), recordPrivateCalls = true)
        val mockedLogger = mockk<KotlinLogger>()

        every { hl7Reader.logger } returns mockedLogger
        every { mockedLogger.warn(any<String>()) } returns Unit

        val goodData1 = """           
            MSH|^~\&|ADT1|GOOD HEALTH HOSPITAL|GHH LAB, INC.|GOOD HEALTH HOSPITAL|198808181126|SECURITY|ADT^A01^ADT_A01|MSG00001|P|2.5.1||
            EVN|A01|200708181123||
            PID|1||PATID1234^5^M11^ADT1^MR^GOOD HEALTH HOSPITAL~123456789^^^USSSA^SS||EVERYMAN^ADAM^A^III||19610615|M||C|2222 HOME STREET^^GREENSBORO^NC^27401-1020|GL|(555) 555-2004|(555)555-2004||S||PATID12345001^2^M10^ADT1^AN^A|444333333|987654^NC|
            NK1|1|NUCLEAR^NELDA^W|SPO^SPOUSE||||NK^NEXT OF KIN
            PV1|1|I|2000^2012^01||||004777^ATTEND^AARON^A|||SUR||||ADM|A0|        
        """.trimIndent()
        val messages = hl7Reader.getMessages(goodData1)
        assertThat(messages).hasSize(1)
        verify {
            mockedLogger.warn("ADT did not have any mapped message model classes, using default behavior")
        }
    }

    @Test
    fun `get getMessages v27 succeeds`() {
        val actionLogger = ActionLogger()
        val hL7Reader = HL7Reader(actionLogger)

        val data = """
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
            OBR|1|sphlspid^SPHL-000048^2.16.840.1.114222.4.1.10765^ISO|3015894676_04608646^STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|68991-9^Epidemiologically Important Information^LN^^^^2.69^^^CDC-10516^Poxvirus Serology^L^^2.16.840.1.113883.6.1|||202302101135|||||||||SPHL-000148^CA-Veterans Affairs Palo Alto Healthcare System^^^^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX|^NET^Internet^mark.holodniy@va.gov|||||202302131116-0500|||F
            OBX|1|CWE|68993-5^Human RNase P RNA XXX Ql NAA+probe^LN^3844^RNaseP human DNA^L^2.69^v_unknown^RNaseP human DNA|ZZYGNASR-1|260385009^Negative^SCT^^^^09012018^^Negative (No human DNA present)||||||C|||202302101135|11D0668319^Centers for Disease Control and Prevention^CLIA^47^Poxvirus Laboratory/Poxvirus and Rabies Branch^L|UIE8@CDC.GOV^Perkins ^Kayla|||20230213102132||||Centers for Disease Control and Prevention^L^^^^CLIA&2.16.840.1.113883.4.7&ISO^XX^^^11D0668319|1600 Clifton Rd^^Atlanta^GA^30329^USA^B
            NTE|1|L|This is a note|RE
            SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600
        """.trimIndent()

        val messages = hL7Reader.getMessages(data)
        assertThat(messages).hasSize(1)
        assertThat(actionLogger.hasErrors()).isFalse()
    }

    @Test
    fun `get getMessages v27 fails v251 succeeds`() {
        val actionLogger = ActionLogger()
        val hL7Reader = HL7Reader(actionLogger)

        val data = """
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
            OBR|1|sphlspid^SPHL-000048^2.16.840.1.114222.4.1.10765^ISO|3015894676_04608646^STARLIMS.CDC.Stag^2.16.840.1.114222.4.3.3.2.1.2^ISO|68991-9^Epidemiologically Important Information^LN^^^^2.69^^^CDC-10516^Poxvirus Serology^L^^2.16.840.1.113883.6.1|||202302101135|||||||||SPHL-000148^CA-Veterans Affairs Palo Alto Healthcare System^^^^^^^STARLIMS.CDC.Stag&2.16.840.1.114222.4.3.3.2.1.2&ISO^^^^XX|^NET^Internet^mark.holodniy@va.gov|||||202302131116-0500|||F
            OBX|1|CE|68993-5^Human RNase P RNA XXX Ql NAA+probe^LN^3844^RNaseP human DNA^L^2.69^v_unknown^RNaseP human DNA|ZZYGNASR-1|260385009^Negative^SCT^^^^09012018^^Negative (No human DNA present)||||||C|||202302101135|11D0668319^Centers for Disease Control and Prevention^CLIA^47^Poxvirus Laboratory/Poxvirus and Rabies Branch^L|UIE8@CDC.GOV^Perkins ^Kayla|||20230213102132||||Centers for Disease Control and Prevention^L^^^^CLIA&2.16.840.1.113883.4.7&ISO^XX^^^11D0668319|1600 Clifton Rd^^Atlanta^GA^30329^USA^B
            NTE|1|L|This is a note|RE
            SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600|202102090000-0600
        """.trimIndent()

        val messages = hL7Reader.getMessages(data)
        assertThat(messages).hasSize(1)
        assertThat(actionLogger.hasWarnings()).isTrue()
        assertThat(actionLogger.warnings[0].detail.message)
            .isEqualTo("Data type error: 'CE' in record 1 is invalid for version 2.7 at OBX-2(0)")
    }
}