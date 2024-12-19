package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.parser.EncodingNotSupportedException
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import org.apache.commons.lang3.exception.ExceptionUtils
import java.lang.Exception
import kotlin.test.Test

class HL7ReaderTests {

    @Test
    fun `test getMessageType for a 23 message`() {
        @Suppress("ktlint:standard:max-line-length")
        val rawMessage =
            "MSH|^~\\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.3|||NE|NE|USA"
        val type = HL7Reader.getMessageType(rawMessage)
        assertThat(type).isEqualTo(HL7Reader.Companion.HL7MessageType("ORU_R01", "2.3", ""))
    }

    @Test
    fun `test getMessageType for a 27 message`() {
        @Suppress("ktlint:standard:max-line-length")
        val rawMessage =
            "MSH|^~\\&#|Sending Application^2.1.3.1^ISO|Facility^3.1.2^UUID|Receiving Application^2.1.5^ISO|Receiving Facility^7.1.1^ISO|20230501102531-0400|security|ORU^R01^ORU_R01|3003786103_4988249_33033|T|2.7|10|abc|ACCEPT|ACKNOLWEDGE|USA|UTF-8~UNICODE|EN^^^EN-US|UTF-16|LAB_PRU_COMPONENT^^2.16.840.1.113883.9.82^ISO~LAB_TO_COMPONENT^^2.16.840.1.113883.9.22^ISO|Sending Org^1234-5&TestText&LN&1234-5&TestAltText&LN&1&2&OriginalText^123^Check Digit^C1^Assigning Authority&2.1.4.1&ISO^MD^Hospital A&2.16.840.1.113883.9.11&ISO|Receiving Org^1234-5&TestText&LN&1234-5&TestAltText&LN&1&2&OriginalText^123^Check Digit^C1^Assigning Authority&2.1.4.1&ISO^MD^Hospital A&2.16.840.1.113883.9.11&ISO|Alt Sending App^Alt Sending App Universal^UUID|Receiving Network^2.1.1^ISO"
        val type = HL7Reader.getMessageType(rawMessage)
        assertThat(type).isEqualTo(HL7Reader.Companion.HL7MessageType("ORU_R01", "2.7", "2.16.840.1.113883.9.82"))
    }

    @Test
    fun `test decoding of bad HL7 messages`() {
        // Empty data
        val badData1 = ""
        try {
            HL7Reader.parseHL7Message(badData1, null)
        } catch (e: Exception) {
            assertThat(e is HL7Exception).isTrue()
            assertThat(ExceptionUtils.getRootCause(e) is EncodingNotSupportedException).isTrue()
        }

        // Some CSV was sent
        val badData2 = """
            a,b,c
            1,2,3
        """.trimIndent()
        try {
            HL7Reader.parseHL7Message(badData2, null)
        } catch (e: Exception) {
            assertThat(e is HL7Exception).isTrue()
            assertThat(ExceptionUtils.getRootCause(e) is EncodingNotSupportedException).isTrue()
        }

        // Some truncated HL7
        val badData3 = "MSH|^~\\&#|MEDITECH^2.16.840.1.114222.4.3.2.2.1.321.111^ISO|COCAA^1.2."
        try {
            HL7Reader.parseHL7Message(badData3, null)
        } catch (e: Exception) {
            assertThat(e is HL7Exception).isTrue()
            assertThat(ExceptionUtils.getRootCause(e) is EncodingNotSupportedException).isTrue()
        }
    }

    @Test
    fun `test isBatch with FSH Header`() {
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
        val isBatch = HL7Reader.isBatch(
            goodData1,
            Hl7InputStreamMessageStringIterator(goodData1.byteInputStream()).asSequence().count()
        )
        assertThat(isBatch).isTrue()
    }

    @Test
    fun `test isBatch without FSH Header`() {
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
        val isBatch = HL7Reader.isBatch(
            goodData1,
            Hl7InputStreamMessageStringIterator(goodData1.byteInputStream()).asSequence().count()
        )
        assertThat(isBatch).isTrue()
    }

    @Test
    fun `test isBatch singular message`() {
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
        val isBatch = HL7Reader.isBatch(
            goodData1,
            Hl7InputStreamMessageStringIterator(goodData1.byteInputStream()).asSequence().count()
        )
        assertThat(isBatch).isFalse()
    }

    @Test
    fun `test getMessageType`() {
        val justMSH = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO                                   
        """.trimIndent()
        val message = HL7Reader.parseHL7Message(justMSH, null)
        val type = HL7Reader.getMessageType(message)
        assertThat(type).isEqualTo("ORU")
    }

    @Test
    @Suppress("DEPRECATION")
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
        val birthTimeMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORM^O01^ORM_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||195808100102034|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val message = HL7Reader.parseHL7Message(birthTimeMessage, null)
        val type = HL7Reader.getBirthTime(message)
        assertThat(type).isEqualTo("195808100102034")
    }

    @Test
    fun `test getBirthTime_Date`() {
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORM^O01^ORM_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val message = HL7Reader.parseHL7Message(birthDateMessage, null)
        val type = HL7Reader.getBirthTime(message)
        assertThat(type).isEqualTo("19580810")
    }

    @Test
    fun `test getPatientPath_ORM`() {
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORM^O01^ORM_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val message = HL7Reader.parseHL7Message(birthDateMessage, null)
        val type = HL7Reader.getPatientPath(message)
        assertThat(type).isEqualTo("PATIENT")
    }

    @Test
    fun `test getPatientPath_OML`() {
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||OML^O21^OML_O21|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val message = HL7Reader.parseHL7Message(birthDateMessage, null)
        val type = HL7Reader.getPatientPath(message)
        assertThat(type).isEqualTo("PATIENT")
    }

    @Test
    fun `test getPatientPath_ORU`() {
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val message = HL7Reader.parseHL7Message(birthDateMessage, null)
        val type = HL7Reader.getPatientPath(message)
        assertThat(type).isEqualTo("PATIENT_RESULT/PATIENT")
    }

    @Test
    fun `test getPatientPath_Other`() {
        val birthDateMessage = """           
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||TEST^O01^TEST_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N                       
        """.trimIndent()
        val message = HL7Reader.parseHL7Message(birthDateMessage, null)
        val type = HL7Reader.getPatientPath(message)
        assertThat(type).isEqualTo(null)
    }

    @Test
    fun `extract MSH segment values`() {
        @Suppress("ktlint:standard:max-line-length")
        val rawMessage =
            "MSH|^~\\&|Epic|Hospital|LIMS|StatePHL|20241003000000||ORM^O01^ORM_O01|4AFA57FE-D41D-4631-9500-286AAAF797E4|T|2.5.1|||AL|NE"
        val message = HL7Reader.parseHL7Message(rawMessage, null)

        assertThat(
            HL7Reader.getSendingApplication(message)
        ).isEqualTo("Epic")
        assertThat(
            HL7Reader.getSendingFacility(message),
        ).isEqualTo("Hospital")
        assertThat(
            HL7Reader.getMessageControlId(message)
        ).isEqualTo("4AFA57FE-D41D-4631-9500-286AAAF797E4")
        assertThat(
            HL7Reader.getAcceptAcknowledgmentType(message)
        ).isEqualTo("AL")
    }
}