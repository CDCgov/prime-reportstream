package gov.cdc.prime.router.fhirengine.translator

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.utils.HL7MessageHelpers
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import io.github.linuxforhealth.hl7.data.Hl7RelatedGeneralUtils
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Patient
import kotlin.test.Test

class HL7toFhirTranslatorTests {
    private val translator = HL7toFhirTranslator()
    private val supportedHL7 = """
MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
    """.trimIndent()

    private val birthDateTime = "20230504131023-0500"
    private val supportedHL7ORMWithBirthDateTime = """
MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORM^O01^ORM_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||$birthDateTime|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
    """.trimIndent()

    private val birthDate = "20230506"
    private val supportedHL7ORMWithBirthDate = """
    MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime ReportStream|20210210170737||ORM^O01^ORM_O01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
    SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
    PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||$birthDate|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071^^^^48077||7275555555:1:^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
    ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
    OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
    OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
    """.trimIndent()

    @Test
    fun `test get message template`() {
        val message = HL7Reader.parseHL7Message(supportedHL7)
        assertThat(
            HL7MessageHelpers.messageCount(supportedHL7)
        ).isEqualTo(1)
        assertThat(translator.getMessageTemplateType(message)).isEqualTo("ORU_R01")
    }

    @Test
    fun `test get message model`() {
        val supportedMessage = HL7Reader.parseHL7Message(supportedHL7)
        assertThat(
            HL7MessageHelpers.messageCount(supportedHL7)
        ).isEqualTo(1)
        val model = translator.getHL7MessageModel(supportedMessage)
        assertThat(model).isNotNull()
        assertThat(model.messageName).isEqualTo("ORU_R01")

        // Source: https://confluence.hl7.org/display/OO/v2+Sample+Messages
        val unsupportedHL7 = """
MSH|^~\&#|NIST EHR|NIST EHR Facility|NIST Test Lab APP|NIST Lab Facility|20130211184101-0500||OML^O33^OML_O33|NIST-LOI_5.0_1.1-NG|T|2.5.1|||AL|AL|||||
PID|1||PATID5421^^^NIST MPI^MR||Wilson^Patrice^Natasha^^^^L||19820304|F||2106-3^White^HL70005|144 East 12th Street^^Los Angeles^CA^90012^^H||^PRN^PH^^^203^2290210|||||||||N^Not Hispanic or Latino^HL70189
NK1|1|Wilson^Phillip^Arthur^^^^L|SPO^Spouse^HL70063|144 East 12th Street^^Los Angeles^CA^90012^^H|||||||||
ORC|NW|ORD448811^NIST EHR|||||||20120628070100|||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI
OBR|1|ORD448811^NIST EHR||1000^Hepatitis A  B  C Panel^99USL|||20120628070100|||||||||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI
DG1|1||F11.129^Opioid abuse with intoxication,unspecified^I10C|||W|||||||||1
        """.trimIndent()
        val unsupportedMessage = HL7Reader.parseHL7Message(unsupportedHL7)
        assertThat(
            HL7MessageHelpers.messageCount(unsupportedHL7)
        ).isEqualTo(1)
        assertFailure { translator.getHL7MessageModel(unsupportedMessage) }
    }

    @Test
    fun `test a quick translation to FHIR`() {
        // Note that FHIR content will be tested as an integration test
        val message = HL7Reader.parseHL7Message(supportedHL7)
        assertThat(
            HL7MessageHelpers.messageCount(supportedHL7)
        ).isEqualTo(1)
        val bundle = translator.translate(message)
        assertThat(bundle).isNotNull()
        assertThat(bundle.type).isEqualTo(Bundle.BundleType.MESSAGE)
        assertThat(bundle.id).isNotEmpty()
    }

    @Test
    fun `test birth date extension addition`() {
        val message = HL7Reader.parseHL7Message(supportedHL7ORMWithBirthDateTime)
        assertThat(
            HL7MessageHelpers.messageCount(supportedHL7ORMWithBirthDateTime)
        ).isEqualTo(1)
        val bundle = translator.translate(message)
        assertThat(bundle).isNotNull()
        assertThat(bundle.type).isEqualTo(Bundle.BundleType.MESSAGE)
        assertThat(bundle.id).isNotEmpty()
        val patient = bundle.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient

        val extensionValue = patient.birthDateElement.getExtensionByUrl(
            "http://hl7.org/fhir/StructureDefinition/patient-birthTime"
        ).value as DateTimeType

        assertThat(
            extensionValue.valueAsString
        ).isEqualTo(Hl7RelatedGeneralUtils.dateTimeWithZoneId(birthDateTime, ""))

        assertThat(
            patient.birthDateElement.valueAsString
        ).isEqualTo(Hl7RelatedGeneralUtils.dateTimeWithZoneId(birthDateTime.take(8), ""))
    }

    @Test
    fun `test birth date extension is missing when birthdate is only date`() {
        val message = HL7Reader.parseHL7Message(supportedHL7ORMWithBirthDate)
        assertThat(
            HL7MessageHelpers.messageCount(supportedHL7ORMWithBirthDateTime)
        ).isEqualTo(1)
        val bundle = translator.translate(message)
        assertThat(bundle).isNotNull()
        assertThat(bundle.type).isEqualTo(Bundle.BundleType.MESSAGE)
        assertThat(bundle.id).isNotEmpty()

        val patient = bundle.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient

        assertThat(
            patient.birthDateElement.getExtensionByUrl(
                "http://hl7.org/fhir/StructureDefinition/patient-birthTime"
            )
        ).isNull()

        assertThat(
            patient.birthDateElement.valueAsString
        ).isEqualTo(Hl7RelatedGeneralUtils.dateTimeWithZoneId(birthDate, ""))
    }
}