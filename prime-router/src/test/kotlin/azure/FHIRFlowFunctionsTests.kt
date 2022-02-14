package gov.cdc.prime.router.azure

import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FHIRFlowFunctionsTests {

    val testHL7NonBulk = """MSH|^~\&|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|Winchester House^05D2222542^ISO|CDPH CA REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|20210803131511.0147+0000||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
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
    val testOtherHL7NonBulk = """MSH|^~\&|CDC PRIME - New York,^2.16.840.1.114222.4.1.237821^ISO|Winchester House^05D2222542^ISO|CDPH CA REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|20210803131511.0147+0000||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLabReport-NoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
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
    fun `process and test fhir`() {

        data class TestCase(
            val name: String,
            val message: String,
            val exception: Throwable?,
        )

        val testCases = listOf(
            TestCase(
                "success",
                "{\"class\":\"gov.cdc.prime.router.engine.RawSubmission\"," +
                    "\"blobURL\":\"http://localhost:10000/devstoreaccount1/reports/receive%2Fsimple_report.hl7test%2F" +
                    "test-covid-19-4990af55-c25b-474b-b4f6-7ccf155706b0-20220210113202.hl7\"," +
                    "\"digest\":\"2bffffff90ffffffa22674fffffff713ffffffbd64ffffffb26c663426877fffffff6ffffffa91ae" +
                    "ffffffd4ffffff82313b5fffffffc1ffffffaf653a387323\"," +
                    "\"sender\":\"simple_report.hl7test\"}",
                null,
            ),
            TestCase(
                "bad-digest",
                "{\"class\":\"gov.cdc.prime.router.engine.RawSubmission\"," +
                    "\"blobURL\":\"http://localhost:10000/devstoreaccount1/reports/receive%2Fsimple_report.hl7test%2F" +
                    "test-covid-19-4990af55-c25b-474b-b4f6-7ccf155706b0-20220210113202.hl7\"," +
                    "\"digest\":\"badffffff9bffffffcb46fffffff5ffffff886fffffff84ffffff9efffffffaffffffd23bfffffff3" +
                    "ffffff8d4d7ffffffff2156fffffffbaffffff963ffffffff6ffffffa0397b10607fffffffa36cffffffbf71\"," +
                    "\"sender\":\"simple_report.hl7test\"}",
                IllegalStateException(
                    "FHIR - Downloaded file does not match expected file " +
                        "badffffff9bffffffcb46fffffff5ffffff886fffffff84ffffff9efffffffaffffffd23bfffffff3ffffff8d4d7" +
                        "ffffffff2156fffffffbaffffff963ffffffff6ffffffa0397b10607fffffffa36cffffffbf71" +
                        " | " +
                        "2bffffff90ffffffa22674fffffff713ffffffbd64ffffffb26c663426877fffffff6ffffffa91aeffffffd4" +
                        "ffffff82313b5fffffffc1ffffffaf653a387323"
                ),
            ),
        )

        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.downloadBlob(any()) } returns testHL7NonBulk.toByteArray()
        val fhirEngine = FHIREngine()
        testCases.forEach { test ->
            try {
                fhirEngine.process(test.message)
                assertNull(test.exception, test.name)
            } catch (t: Throwable) {
                if (test.exception == null) {
                    throw t
                }
                assertEquals(test.exception::class, t::class, test.name)
                assertEquals(test.exception.message, t.message, test.name)
            }
        }
    }

    @Test
    fun `test results`() {
        data class TestCase(
            val name: String,
            val input: String,
            val output: String,
            val exception: Throwable? = null
        )

        val testCases = listOf(
            TestCase(
                "matching",
                testHL7NonBulk,
                testHL7NonBulk,
            ),
            TestCase(
                "not matching",
                testHL7NonBulk,
                testOtherHL7NonBulk,
                IllegalStateException("FHIR - HL7 processing failed")
            )
        )

        val fhirEngine = FHIREngine()
        testCases.forEach { case ->
            try {
                fhirEngine.compare(case.input, case.output)
                assertNull(case.exception, case.name)
            } catch (t: Throwable) {
                if (case.exception == null) {
                    throw Error(case.name, t)
                }
                assertEquals(case.exception::class, t::class, case.name)
                assertEquals(case.exception.message, t.message, case.name)
            }
        }
    }
}