package gov.cdc.prime.router.fhirengine.translator

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.hl7.fhir.r4.model.Bundle
import kotlin.test.Test

class HL7toFhirTranslatorTests {
    private val supportedHL7 = """
MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
    """.trimIndent()

    @Test
    fun `test get message templates`() {
        // Make sure the message templates are fetched, which means the FHIR configuration is good.
        assertThat(HL7toFhirTranslator.defaultMessageTemplates.size).isGreaterThan(0)
    }

    @Test
    fun `test get message template`() {
        val message = HL7Reader(ActionLogger()).getMessages(supportedHL7)
        assertThat(message.size).isEqualTo(1)
        assertThat(HL7toFhirTranslator.getInstance().getMessageTemplateType(message[0])).isEqualTo("ORU_R01")
    }

    @Test
    fun `test get message model`() {
        var message = HL7Reader(ActionLogger()).getMessages(supportedHL7)
        assertThat(message.size).isEqualTo(1)
        val model = HL7toFhirTranslator.getInstance().getHL7MessageModel(message[0])
        assertThat(model).isNotNull()
        assertThat(model.messageName).isEqualTo("ORU_R01")

        // Source: https://confluence.hl7.org/display/OO/v2+Sample+Messages
        val unsupportedHL7 = """
MSH|^~\&#|NIST EHR|NIST EHR Facility|NIST Test Lab APP|NIST Lab Facility|20130211184101-0500||OML^O21^OML_O21|NIST-LOI_5.0_1.1-NG|T|2.5.1|||AL|AL|||||
PID|1||PATID5421^^^NIST MPI^MR||Wilson^Patrice^Natasha^^^^L||19820304|F||2106-3^White^HL70005|144 East 12th Street^^Los Angeles^CA^90012^^H||^PRN^PH^^^203^2290210|||||||||N^Not Hispanic or Latino^HL70189
NK1|1|Wilson^Phillip^Arthur^^^^L|SPO^Spouse^HL70063|144 East 12th Street^^Los Angeles^CA^90012^^H|||||||||
ORC|NW|ORD448811^NIST EHR|||||||20120628070100|||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI
OBR|1|ORD448811^NIST EHR||1000^Hepatitis A  B  C Panel^99USL|||20120628070100|||||||||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI
DG1|1||F11.129^Opioid abuse with intoxication,unspecified^I10C|||W|||||||||1
        """.trimIndent()
        message = HL7Reader(ActionLogger()).getMessages(unsupportedHL7)
        assertThat(message.size).isEqualTo(1)
        assertThat { HL7toFhirTranslator.getInstance().getHL7MessageModel(message[0]) }.isFailure()
    }

    @Test
    fun `test a quick translation to FHIR`() {
        // Note that FHIR content will be tested as an integration test
        val message = HL7Reader(ActionLogger()).getMessages(supportedHL7)
        assertThat(message.size).isEqualTo(1)
        val bundle = HL7toFhirTranslator.getInstance().translate(message[0])
        assertThat(bundle).isNotNull()
        assertThat(bundle.type).isEqualTo(Bundle.BundleType.MESSAGE)
        assertThat(bundle.id).isNotEmpty()
    }
}