package gov.cdc.prime.router.translation

import assertk.assertThat
import assertk.assertions.isEqualTo
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.encoding.HL7
import gov.cdc.prime.router.encoding.getValue
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Test

class FHIRtoHL7Tests : Logging {
    @Test
    fun `parse fhir path`() {
        // Create a Patient to create
        val patient = Patient()
        patient.setActive(true)
        patient.addIdentifier().setSystem("http://foo").setValue("bar")

        val bundle = Bundle()
        bundle.addEntry().setResource(patient).setFullUrl(patient.getId())

        data class TestCase(
            val path: String,
            val resource: IBase,
        )

        val testCases = listOf(
            TestCase(
                "Patient.identifier.value",
                patient,
            ),
            TestCase(
                "Bundle.entry.resource.as(Patient).identifier.value",
                bundle,
            )
        )

        testCases.forEach { case ->
            assertThat(case.resource.getValue<StringType>(case.path).first().toString()).isEqualTo("bar")
        }
    }

    @Test
    fun `translate message segment`() {
        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "P")

        // Create a Patient to create
        val patient = Patient()
        patient.setActive(true)
        patient.addIdentifier().setSystem("http://foo").setValue("bar")

        val bundle = Bundle()
        bundle.addEntry().setResource(patient).setFullUrl(patient.getId())

        val translation = FHIRtoHL7.MappingTemplate(
            hl7Path = "/.PID-3-1",
            fhirPath = "Bundle.entry.resource.as(Patient).identifier.value",
            value = "{{resource}}",
        )

        val terser = Terser(message)
        terser.translate(FHIRtoHL7.processMapping(translation, bundle))

        assertThat(terser.get("/.PID-3-1")).isEqualTo("bar")
    }

    val rawHL7 = """MSH|^~\&|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|Winchester House^05D2222542^ISO|CDPH CA REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|20210803131511.0147+0000||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210726
PID|1||09d12345-0987-1234-1234-111b1ee0879f^^^Winchester House&05D2222542&ISO^PI^&05D2222542&ISO||Bunny^Bugs^C^^^^L||19000101|M||2106-3^White^HL70005^^^^2.5.1|12345 Main St^^San Jose^CA^95125^USA^^^06085||(123)456-7890^PRN^PH^^1^123^4567890|||||||||N^Non Hispanic or Latino^HL70189^^^^2.9||||||||N
ORC|RE|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|||||||||1679892871^Doolittle^Doctor^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||(123)456-7890^WPN^PH^^1^123^4567890|20210802||||||Winchester House|6789 Main St^^San Jose^CA^95126^^^^06085|(123)456-7890^WPN^PH^^1^123^4567890|6789 Main St^^San Jose^CA^95126
OBR|1|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|1234d1d1-95fe-462c-8ac6-46728dba581c^Winchester House^05D2222542^ISO|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.68|||202108020000-0500|202108020000-0500||||||||1679892871^Doolittle^Doctor^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|(123)456-7890^WPN^PH^^1^123^4567890|||||202108020000-0500|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN^^^^2.68||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078^^^^2.7|||F|||202108020000-0500|05D2222542^ISO||10811877011290_DIT^^99ELR^^^^2.68^^10811877011290_DIT||20210802||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126^^^^06085
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202108020000-0500|05D2222542||||202108020000-0500||||Winchester House^^^^^ISO&2.16.840.1.113883.19.4.6&ISO^XX^^^05D2222542|6789 Main St^^San Jose^CA^95126-5285^^^^06085|||||QST
SPM|1|1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO^1234d1d1-95fe-462c-8ac6-46728dba581c&&05D2222542&ISO||445297001^Swab of internal nose^SCT^^^^2.67||||53342003^Internal nose structure (body structure)^SCT^^^^2020-09-01|||||||||202108020000-0500|20210802000006.0000-0500"""

    @Test
    fun `translate message`() {
        val testMessage = HL7.decode(rawHL7).first()
        val bundle = HL7toFHIR.translate(testMessage)
        val translations = FHIRtoHL7.readMappings("./metadata/hl7_mapping/ORU_R01.yml")

        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "P")
        val terser = Terser(message)
        val mappings = FHIRtoHL7.processMapping(translations, bundle)

        terser.translate(mappings)

        val testTerser = Terser(testMessage)

        mappings.forEach { mapping ->
            assertThat(terser.get(mapping.hl7Path)).isEqualTo(testTerser.get(mapping.hl7Path))
        }

        val encodedResult = message.encode()
        logger.info("\n$encodedResult")

        /*
        val result = CompareHl7Data().compare(rawHL7.byteInputStream(), encodedResult.byteInputStream())
        logger.info("\n${result.errors.joinToString("\n")}")
        assertThat(result.errors.size).isEqualTo(0)
        assertThat(result.warnings).isEmpty()
        assertThat(result.passed).isTrue()
        */
    }
}