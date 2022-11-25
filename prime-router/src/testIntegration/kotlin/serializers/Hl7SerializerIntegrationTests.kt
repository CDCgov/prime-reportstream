package gov.cdc.prime.router.serializers

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.cli.main
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertFailsWith
import kotlin.test.fail

class Hl7SerializerIntegrationTests {
    private val context = DefaultHapiContext()
    private val hl7SchemaName = "hl7/test-covid-19"
    private val serializer: Hl7Serializer
    private val csvSerializer: CsvSerializer
    private val covid19Schema: Schema
    private val metadata = Metadata.getInstance()
    private val hl7TestFileDir = "./src/test/hl7_test_files/"
    private val testReport: Report
    private val translator = Translator(metadata, FileSettings())
    private val sampleHl7Message: String
    private val sampleHl7MessageWithRepeats: String
    private val mcf = CanonicalModelClassFactory("2.5.1")

    init {
        val settings = FileSettings("./settings")
        val inputStream = File("./src/test/unit_test_files/fake-pdi-covid-19.csv").inputStream()
        covid19Schema = metadata.findSchema(hl7SchemaName) ?: fail("Could not find target schema")
        csvSerializer = CsvSerializer(metadata)
        serializer = Hl7Serializer(metadata, settings)
        testReport = csvSerializer.readExternal("primedatainput/pdi-covid-19", inputStream, TestSource).report
        sampleHl7Message =
            """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
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
        sampleHl7MessageWithRepeats =
            """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^NET^Internet^roscoe.wilkinson@email.com~(211)224-0784^PRN^PH^^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
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
    }

    private fun createConfig(
        replaceValue: Map<String, String> = emptyMap(),
        cliaForSender: Map<String, String> = emptyMap(),
        cliaForOutOfStateTesting: String? = null,
        truncateHl7Fields: String? = null,
        suppressNonNPI: Boolean = false,
        truncateHDNamespaceIds: Boolean = false,
        stripInvalidCharsRegex: String? = null,
        replaceUnicodeWithAscii: Boolean = false,
        useBatchHeaders: Boolean = false,
    ): Hl7Configuration {
        return Hl7Configuration(
            messageProfileId = "",
            receivingApplicationOID = "",
            receivingApplicationName = "",
            receivingFacilityName = "",
            receivingFacilityOID = "",
            receivingOrganization = "",
            cliaForOutOfStateTesting = cliaForOutOfStateTesting,
            cliaForSender = cliaForSender,
            replaceValue = replaceValue,
            truncateHl7Fields = truncateHl7Fields,
            suppressNonNPI = suppressNonNPI,
            truncateHDNamespaceIds = truncateHDNamespaceIds,
            stripInvalidCharsRegex = stripInvalidCharsRegex,
            replaceUnicodeWithAscii = replaceUnicodeWithAscii,
            useBatchHeaders = useBatchHeaders
        )
    }

    @Test
    fun `test write batch`() {
        val outputStream = ByteArrayOutputStream()
        serializer.writeBatch(testReport, outputStream)
        val output = outputStream.toString(StandardCharsets.UTF_8)
        assertThat(output).isNotNull()
    }

    @Test
    fun `test write a message`() {
        val output = serializer.createMessage(testReport, 0)
        assertThat(output).isNotNull()
    }

    @Test
    fun `test write a message with Receiver`() {
        val inputStream = File("./src/test/unit_test_files/ak_test_file.csv").inputStream()
        val schema = "primedatainput/pdi-covid-19"
        val hl7Config = createConfig(
            replaceValue = mapOf("PID-22-3" to "CDCREC,-,testCDCREC", "MSH-9" to "MSH-10"),
            cliaForOutOfStateTesting = "1234FAKECLIA"
        )
        val receiver = Receiver("mock", "ca-phd", Topic.COVID_19, translation = hl7Config)
        val testReport = csvSerializer.readExternal(schema, inputStream, listOf(TestSource), receiver).report
        val output = serializer.createMessage(testReport, 2)
        assertThat(output).isNotNull()
    }

    @Test
    fun `test message with bad NPI`() {
        val inputStream = File("./src/test/unit_test_files/fake-pdi-covid-19.csv").inputStream()
        val schema = "primedatainput/pdi-covid-19"
        val hl7Config = createConfig(suppressNonNPI = false)
        val receiver = Receiver("mock", "ca-phd", Topic.COVID_19, translation = hl7Config)
        val pdiInput = csvSerializer.readExternal(schema, inputStream, listOf(TestSource), receiver).report
        val testReport = translator.translateByReceiver(pdiInput, receiver)
        val output = serializer.buildMessage(testReport, 2)
        val orderingProvider = output.patienT_RESULT.ordeR_OBSERVATION.orc.getOrderingProvider(0)
        assertThat(orderingProvider.assigningAuthority.isEmpty).isTrue()
        assertThat(orderingProvider.identifierTypeCode.value).isEqualTo("U")
    }

    @Test
    fun `test message with characters that need stripping`() {
        val inputStream = File("./src/test/unit_test_files/bad-character-replacements.csv").inputStream()
        val schema = "strac/strac-covid-19"
        val hl7Config = createConfig(stripInvalidCharsRegex = "\u0019")
        val receiver = Receiver("mock", "pa-phd", Topic.COVID_19, translation = hl7Config)
        val stracInput = csvSerializer.readExternal(schema, inputStream, listOf(TestSource), receiver).report
        val testReport = translator.translateByReceiver(stracInput, receiver)
        val output = serializer.buildMessage(testReport, 0)
        val patientName = output.patienT_RESULT.patient.pid.patientName[0]
        assertThat(patientName.givenName.value).isEqualTo("Marie")
        assertThat(patientName.familyName.surname.value).isEqualTo("OConnell")
    }

    @Test
    fun `test message with bad NPI and suppressed`() {
        val inputStream = File("./src/test/unit_test_files/fake-pdi-covid-19.csv").inputStream()
        val schema = "primedatainput/pdi-covid-19"
        val hl7Config = createConfig(suppressNonNPI = true)
        val receiver = Receiver("mock", "ca-phd", Topic.COVID_19, translation = hl7Config)
        val pdiInput = csvSerializer.readExternal(schema, inputStream, listOf(TestSource), receiver).report
        val testReport = translator.translateByReceiver(pdiInput, receiver)
        val output = serializer.buildMessage(testReport, 2)
        val orderingProvider = output.patienT_RESULT.ordeR_OBSERVATION.orc.getOrderingProvider(0)
        assertThat(orderingProvider.idNumber.isEmpty).isTrue()
        assertThat(orderingProvider.assigningAuthority.isEmpty).isTrue()
        assertThat(orderingProvider.identifierTypeCode.isEmpty).isTrue()
    }

    @Test
    fun `test write a message with Receiver for VT with HD truncation and OBX-23-1 with 50 chars`() {
        val inputStream = File("./src/test/unit_test_files/vt_test_file.csv").inputStream()
        val schema = "primedatainput/pdi-covid-19"

        val hl7Config = createConfig(truncateHl7Fields = "OBX-23-1, MSH-4-1")
        val receiver = Receiver("test", "vt-phd", Topic.COVID_19, translation = hl7Config)

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
    fun `test write a message converting unicode to ASCII`() {
        val inputStream = File("./src/test/unit_test_files/ca_test_file.csv").inputStream()
        val schema = "primedatainput/pdi-covid-19"
        val hl7Config = createConfig(replaceUnicodeWithAscii = true)
        val receiver = Receiver("mock", "ca-phd", Topic.COVID_19, translation = hl7Config)
        val testReport = csvSerializer.readExternal(schema, inputStream, listOf(TestSource), receiver).report
        val output = serializer.createMessage(testReport, 3)
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(output, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        // assert
        assertThat(output).isNotNull()
        assertThat(
            terser.get(
                "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-23-1"
            )
        ).isEqualTo("Any lab USA") // Ãny lab USA
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-11-4")).isEqualTo("CA") // ÇA
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-11-6")).isEqualTo("USA") // ÙSA
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-5-4")).isEqualTo("III") // ÎÎÎ
        assertThat(
            terser.get(
                "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/NTE-3"
            )
        ).isEqualTo("OOO-AAAAAA-EEEE-I-U-CCC") // ÔÔÔ-ÀÁÂÃÄÅ-ÈÉÊË-Î-Ù-ÇÇÇ
    }

    @Test
    fun `test write a message that skips unicode to ASCII`() {
        val inputStream = File("./src/test/unit_test_files/ca_test_file.csv").inputStream()
        val schema = "primedatainput/pdi-covid-19"
        val hl7Config = createConfig(replaceUnicodeWithAscii = false)
        val receiver = Receiver("mock", "ca-phd", Topic.COVID_19, translation = hl7Config)
        val testReport = csvSerializer.readExternal(schema, inputStream, listOf(TestSource), receiver).report
        val output = serializer.createMessage(testReport, 3)
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(output, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        // assert
        assertThat(output).isNotNull()
        assertThat(
            terser.get(
                "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-23-1"
            )
        ).isEqualTo("Ãny lab USA") // Ãny lab USA
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-11-4")).isEqualTo("ÇA") // ÇA
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-11-6")).isEqualTo("ÙSA") // ÙSA
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-5-4")).isEqualTo("ÎÎÎ") // ÎÎÎ
        assertThat(
            terser.get(
                "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/NTE-3"
            )
        ).isEqualTo("ÔÔÔ-ÀÁÂÃÄÅ-ÈÉÊË-Î-Ù-ÇÇÇ") // ÔÔÔ-ÀÁÂÃÄÅ-ÈÉÊË-Î-Ù-ÇÇÇ
    }

    @Test
    fun `test zip code logic`() {
        // arrange
        val inputStream = File("./src/test/unit_test_files/ca_test_file.csv").inputStream()
        val schema = "primedatainput/pdi-covid-19"
        val hl7Config = createConfig(useBatchHeaders = true)
        val receiver = Receiver("mock", "ca-phd", Topic.COVID_19, translation = hl7Config, deidentify = true)
        val rawInput = csvSerializer.readExternal(schema, inputStream, listOf(TestSource), receiver).report
        var deidentifiedTestReport = translator.translateByReceiver(rawInput, receiver)
        val singleOutput = serializer.buildMessage(deidentifiedTestReport, 0)
        assertThat(singleOutput).isNotNull()
        // act
        val actual = singleOutput.patienT_RESULT.patient.pid.pid11_PatientAddress[0].zipOrPostalCode
        val expected = "93900"
        println("\ntest zip code logic:")
        print(" testing single report...")
        // assert
        assertThat(actual.value).isEqualTo(expected)
        println(" done")
        // arrange
        val outputStream = ByteArrayOutputStream()
        serializer.writeBatch(deidentifiedTestReport, outputStream)
        val batchOutput = outputStream.toString(StandardCharsets.UTF_8)
        assertThat(batchOutput).isNotNull()
        // act
        val mappedMessage = serializer.convertBatchMessagesToMap(batchOutput, covid19Schema)
        val mappedValues = mappedMessage.mappedRows
        assertThat(mappedValues.containsKey("patient_city")).isTrue()
        val patientZipCode = mappedValues.get("patient_zip_code")
        print(" testing batch report...")
        // assert
        assertThat(patientZipCode?.get(0) ?: "Null value").isEqualTo("93900") // 93923
        assertThat(patientZipCode?.get(1) ?: "Null value").isEqualTo("00000") // 03611
        assertThat(patientZipCode?.get(2) ?: "Null value").isEqualTo("00000") // 05912-1423
        assertThat(patientZipCode?.get(3) ?: "Null value").isEqualTo("00000") // 10263
        assertThat(patientZipCode?.get(4) ?: "Null value").isEqualTo("95500") // 95531
        println(" done\n")
    }

    /**
     * InTOfReplaceValueAwithBSettingFeature - Integration Test for the replaceValueAwithB setting for
     *  the ReportStream.  Note, the test case uses the ./setting/orgnization.yml file for the setting
     *  and the ./src/testIntegration/resources/datatests/CSV_to_HL7/sample-pdi-covid-19-nh.csv file for the test
     *  message(s).  It tests data flow between all classes in the execution path of the prime cli command
     *  below:
     *      ./prime data --input [FILE PATH OF TEST MESSAGE] --input-schema [FILE PATH OF SCHEMA TO USE]
     *          --output [FILE PATH OF TEST OUTPUT] --route
     *  Testing steps:
     *      1. Prepare the testInputFile - test message(s)
     *      2. Set the output file name and path
     *      3. Prepare assertThat("HL7 FIELD").isEqualTo("EXPECTED VALUE")
     *      4. Run the test case
     *  For more technical architecture detail (see: ./docs/playbooks/replaceValueAwithB-integration-test.md
     */
    @Test
    fun `InTOfReplaceValueAwithBSettingFeature`() {
        // Input and Output test files
        val testInputFile = "src/testIntegration/resources/datatests/CSV_to_HL7/sample-pdi-covid-19-nh.csv"
        val testSettingDir = "src/testIntegration/resources/settings"
        val testOutputFile = "build/tmp/testIntegration/pdi-covid-19-nh-InT-output.hl7"

        // Prepare test arguments
        val args: Array<String> = arrayOf(
            "data", "--input", testInputFile,
            "--test-dir-setting", testSettingDir,
            "--input-schema", "primedatainput/pdi-covid-19", "--output", testOutputFile, "--route"
        )

        // Execute primeCLI command.  Note, upon success, it will generate the HL7 output file in $testOutputFile
        main(args)

        val message = File(testOutputFile).readLines().drop(2).joinToString("\r")
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        val hapiMsg = parser.parse(message)
        val terser = Terser(hapiMsg)

        // Check for blank filler (see: src/testIntegration/resource/setting/organization.yml.
        // .. ORC-2 is not blank in hl7 output.  It contains value of 998121.  Therefore, it won't replace.
        // .. ORC-4  is blank in hl7 output.  Therefore, it replaced it with value in organization.yml (ORC4BLANKFILLER)
        // .. To findout value in terser, use IntelliJ debug break here and view terser.finder.root
        assertThat(terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-2")).isEqualTo("998121")
        assertThat(terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-4")).isEqualTo("ORC4BLANKFILLER")

        // Verify MSH Segment replacement
        assertThat(terser.get("/MSH-3-1")).isEqualTo("CDC PRIME - Atlanta, Georgia (Dekalb)")
        assertThat(terser.get("/MSH-3-2")).isEqualTo("2.16.840.1.114222.4.1.237821")
        assertThat(terser.get("/MSH-3-3")).isEqualTo("ISO")
        // MSH-4
        assertThat(terser.get("/MSH-4-1")).isEqualTo("CDC PRIME")
        assertThat(terser.get("/MSH-4-2")).isEqualTo("11D2030855")
        assertThat(terser.get("/MSH-4-3")).isEqualTo("CLIA")
        // MSH-5
        assertThat(terser.get("/MSH-5-1")).isEqualTo("NH_ELR")
        assertThat(terser.get("/MSH-5-2")).isEqualTo("2.16.840.1.114222.4.3.2.2.3.600.4")
        assertThat(terser.get("/MSH-5-3")).isEqualTo("ISO")
        // MSH-6
        assertThat(terser.get("/MSH-6-1")).isEqualTo("NH_DHHS")
        assertThat(terser.get("/MSH-6-2")).isEqualTo("2.16.840.1.114222.4.1.3669")
        assertThat(terser.get("/MSH-6-3")).isEqualTo("ISO")
        // MSH-21
        assertThat(terser.get("/MSH-21-1")).isEqualTo("PHLabReport-Batch")
        assertThat(terser.get("/MSH-21-3")).isEqualTo("2.16.840.1.113883.9.11")
        assertThat(terser.get("/MSH-21-4")).isEqualTo("ISO")

        // Verify OBX Segment replacement
        assertThat(terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-17(0)-1"))
            .isEqualTo("DEVICE ID 1")
        assertThat(terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-17(0)-2"))
            .isEqualTo(null)
        assertThat(terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-17(0)-3"))
            .isEqualTo("99ELR")
        assertThat(terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-17(1)-1"))
            .isEqualTo("DEVICE ID 2")
        assertThat(terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-17(1)-2"))
            .isEqualTo(null)
        assertThat(terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-17(1)-3"))
            .isEqualTo("99ELR")

        println(" done\n")
    }

    @Test
    fun `test reading message from serializer`() {
        // arrange
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(sampleHl7Message, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        // these messages are of type ORU_R01, so we can cast to that
        // as well, and let's test that while we're here as well
        val oru = hapiMsg as ORU_R01
        // assert
        assertThat(terser.get("/MSH-3-1")).isEqualTo("CDC PRIME - Atlanta, Georgia (Dekalb)")
        assertThat(terser.get("/MSH-3-2")).isEqualTo("2.16.840.1.114222.4.1.237821")
        assertThat(terser.get("/.PID-11-3")).isEqualTo("South Rodneychester")
        // check the oru cast
        assertThat(oru).isNotNull()
        assertThat(oru.patienT_RESULT.patient).isNotNull()
        assertThat(oru.patienT_RESULT.patient.pid).isNotNull()
        println(oru.printStructure())
    }

    @Test
    fun `test reading pid repeats`() {
        // arrange
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(sampleHl7MessageWithRepeats, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        // these messages are of type ORU_R01, so we can cast to that
        // as well, and let's test that while we're here as well
        val oru = hapiMsg as ORU_R01
        // ^NET^Internet^roscoe.wilkinson@email.com~(211)224-0784^PRN^PH^^1^211^2240784
        // assert
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-13(0)-2")).isEqualTo("NET")
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-13(0)-3")).isEqualTo("Internet")
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-13(1)-1")).isEqualTo("(211)224-0784")
        assertThat(terser.get("/PATIENT_RESULT/PATIENT/PID-13(1)-2")).isEqualTo("PRN")
        println(terser.get("/PATIENT_RESULT/PATIENT/PID-13(0)"))
        println(terser.get("/PATIENT_RESULT/PATIENT/PID-13(1)"))
        // check the oru cast
        assertThat(oru).isNotNull()
        assertThat(oru.patienT_RESULT.patient).isNotNull()
        assertThat(oru.patienT_RESULT.patient.pid).isNotNull()
        assertThat(oru.patienT_RESULT.patient.pid.phoneNumberHome).isNotNull()
        assertThat(oru.patienT_RESULT.patient.pid.phoneNumberHome).hasSize(2)
        println(oru.patienT_RESULT.patient.pid.phoneNumberHome[0])
        println(oru.printStructure())
    }

    @Test
    fun `test converting hl7 into mapped list of values`() {
        val mappedMessage = serializer.convertMessageToMap(sampleHl7Message, 1, covid19Schema)
        val mappedValues = mappedMessage.item
        println("\ntest converting hl7 into mapped list of values:\n")
        mappedValues.forEach {
            println("${it.key}: ${it.value.joinToString()}")
        }
        assertThat(mappedValues.containsKey("patient_city")).isTrue()
        assertThat(mappedValues["patient_city"]?.get(0)).isEqualTo("South Rodneychester")
    }

    @Test
    fun `test reading HL7 message from file`() {
        val inputFile = "$hl7TestFileDir/single_message.hl7"
        val message = File(inputFile).readText()
        val mappedMessage = serializer.convertMessageToMap(message, 1, covid19Schema)
        val mappedValues = mappedMessage.item
        mappedValues.forEach {
            println("${it.key}: ${it.value.joinToString()}")
        }
        assertThat(mappedValues.containsKey("patient_city")).isTrue()
        assertThat(mappedValues["patient_city"]?.get(0)).isEqualTo("South Rodneychester")
    }

    @Test
    fun `test reading HL7 batch message from file`() {
        val inputFile = "$hl7TestFileDir/batch_message.hl7"
        val message = File(inputFile).readText()
        val mappedMessage = serializer.convertBatchMessagesToMap(message, covid19Schema)
        val mappedValues = mappedMessage.mappedRows
        println("\ntest reading HL7 batch message from file:\n")
        mappedValues.forEach {
            println("${it.key}: ${it.value.joinToString()}")
        }
        assertThat(mappedValues.containsKey("patient_city")).isTrue()
        val cities = mappedValues["patient_city"]?.toSet()
        assertThat(cities).isEqualTo(setOf("North Taylor", "South Rodneychester"))
        println("Errors:")
        mappedMessage.items.forEach {
            it.errors.forEach {
                println(it)
            }
        }
        println("Warnings:")
        mappedMessage.items.forEach {
            it.warnings.forEach {
                println(it)
            }
        }
    }

    @Test
    fun `test reading HL7 batch and creating report instance`() {
        val inputFile = "$hl7TestFileDir/batch_message.hl7"
        val message = File(inputFile)
        val source = FileSource(inputFile)
        val readResult = serializer.readExternal(hl7SchemaName, message.inputStream(), source)
        val report = readResult.report
        assertThat(report.getString(0, "patient_city")).isEqualTo("South Rodneychester")
        assertThat(report.getString(1, "patient_city")).isEqualTo("North Taylor")
        assertThat(report.itemCount == 2).isTrue()
        val hospitalized = (0 until report.itemCount).map { report.getString(it, "hospitalized") }
        assertThat(hospitalized.toSet()).isEqualTo(setOf(""))
    }

    @Test
    fun `test checkLIVDValueExists`() {

        // happy path success
        val modelCOVIDSeqTest = serializer.checkLIVDValueExists("Model", "COVIDSeq Test")
        assertThat(modelCOVIDSeqTest).isTrue()

        // happy path fail
        val modelFake = serializer.checkLIVDValueExists("Model", "Fake")
        assertThat(modelFake).isFalse()

        // unhappy path input 1
        val fakeCOVIDSeqTest = serializer.checkLIVDValueExists("fake", "COVIDSeq Test")
        assertThat(fakeCOVIDSeqTest).isFalse()

        // unhappy path input 1 and 2
        val fakeFake = serializer.checkLIVDValueExists("fake", "fake")
        assertThat(fakeFake).isFalse()
    }

    /**
     * confirmObservationOrder takes in a message then reviews LOINC codes in the OBX segments. The first LOINC
     * should be present in the LIVD table. If that is not the case, then the OBX segments are reordered.
     */
    @Test
    fun `test confirmObservationOrder`() {

        // message where first OBX segment contains the test result
        val sampleRegMessage = File("./src/testIntegration/resources/serializers/test_result_first_rep.hl7")
            .readText()

        arrangeTest(sampleRegMessage).run {
            val oruR01SampleReg: ORU_R01? = this as? ORU_R01
            // save OBX-3 for first OBX segment prior to the function run
            val sampleObsID = oruR01SampleReg
                ?.patienT_RESULT?.ordeR_OBSERVATION?.observation?.obx?.observationIdentifier?.identifier

            // assert
            serializer.organizeObservationOrder(this).run {
                val happyORUR01 = this as? ORU_R01
                // extract OBX-3 for first OBX segment after function run
                val happyFullObsIdentifier = happyORUR01
                    ?.patienT_RESULT?.ordeR_OBSERVATION?.observation?.obx?.observationIdentifier?.identifier
                // since first OBX segment does contain the test result, the sample and final OBX-3 values should match
                assertThat(happyFullObsIdentifier).isEqualTo(sampleObsID)
            }
        }
        // message where first OBX segment does not contain the test result
        val sampleComplexMessage = File("./src/testIntegration/resources/serializers/test_result_not_first_rep.hl7")
            .readText()

        arrangeTest(sampleComplexMessage).run {
            val oruR01SampleComplex: ORU_R01? = this as? ORU_R01
            // save OBX-3 for first OBX segment prior to the function run
            val sampleComplexObsID = oruR01SampleComplex
                ?.patienT_RESULT?.ordeR_OBSERVATION?.observation?.obx?.observationIdentifier?.identifier

            // assert
            serializer.organizeObservationOrder(this).run {
                val happyComplexORUR01 = this as? ORU_R01
                // extract OBX-3 for first OBX segment after function run
                val happyComplexObsIdentifier = happyComplexORUR01
                    ?.patienT_RESULT?.ordeR_OBSERVATION?.observation?.obx?.observationIdentifier?.identifier
                // since first OBX segment does not contain the test result, the sample and final OBX-3 values
                // should not match
                assertThat(happyComplexObsIdentifier).isNotEqualTo(sampleComplexObsID)

                // the OBX segments were rearranged. But the OBX set IDs should still be sequential
                val happyComplexSetID = happyComplexORUR01
                    ?.patienT_RESULT?.ordeR_OBSERVATION?.observation?.obx?.setIDOBX
                assertThat(happyComplexSetID.toString()).isEqualTo("1")

                // the OBX segment with the test result has an attached NTE. That should be moved along with the OBX
                val happyComplexNTE = happyComplexORUR01?.patienT_RESULT?.ordeR_OBSERVATION?.observation?.nte
                assertThat(happyComplexNTE).isNotNull()
            }
        }
    }

    @Test
    fun `test getSchoolId`() {
        // Get a bunch of k12 rows
        val testCSV = File("./src/test/unit_test_files/pdi-covid-19-wa-k12.csv").inputStream()
        val testReport = csvSerializer
            .readExternal("primedatainput/pdi-covid-19", testCSV, TestSource)
            .report

        // This row is the happy path
        val rawValidFacilityName = testReport.getString(0, "ordering_facility_name") ?: fail()
        val validNCES = serializer.getSchoolId(testReport, 0, rawValidFacilityName)
        assertThat(validNCES).isEqualTo("530825001381")

        // This row doesn't match on zip code
        val rawInvalidZip = testReport.getString(8, "ordering_facility_name") ?: fail()
        val invalidZip = serializer.getSchoolId(testReport, 8, rawInvalidZip)
        assertThat(invalidZip).isNull()

        // This row does a best match
        val rawPartialName = testReport.getString(10, "ordering_facility_name") ?: fail()
        val partialNCES = serializer.getSchoolId(testReport, 10, rawPartialName)
        assertThat(partialNCES).isEqualTo("530825001381")

        // This row doesn't match on site type
        val rawInvalidSite = testReport.getString(11, "ordering_facility_name") ?: fail()
        val invalidSite = serializer.getSchoolId(testReport, 11, rawInvalidSite)
        assertThat(invalidSite).isNull()

        // There are three schools that have the same first name in this zip-code
        val rawHighSchool = testReport.getString(12, "ordering_facility_name") ?: fail()
        val highSchool = serializer.getSchoolId(testReport, 12, rawHighSchool)
        assertThat(highSchool).isEqualTo("530042000099")

        // There are three schools that have the same first name in this zip-code. This one has a very long name.
        val rawPartnershipSchool = testReport.getString(13, "ordering_facility_name") ?: fail()
        val partnershipSchool = serializer.getSchoolId(testReport, 13, rawPartnershipSchool)
        assertThat(partnershipSchool).isEqualTo("530042003476")
    }

    @Test
    fun `test setOrderingFacilityComponent`() {
        val mockTerser = mockk<Terser>()
        every { mockTerser.set(any(), any()) } returns Unit
        val facilityName = "Very Long Facility Name That Should Truncate After Here"
        // Get a bunch of k12 rows
        val testCSV = File("./src/test/unit_test_files/pdi-covid-19-wa-k12.csv").inputStream()
        val testReport = csvSerializer
            .readExternal("primedatainput/pdi-covid-19", testCSV, TestSource)
            .report

        // Test with STANDARD
        serializer.setOrderingFacilityComponent(
            mockTerser,
            facilityName,
            useOrderingFacilityName = Hl7Configuration.OrderingFacilityName.STANDARD,
            testReport,
            row = 0
        )

        verify {
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-21-1", facilityName.take(50))
        }
    }

    @Test
    fun `test setOrderingFacilityComponent with Organization Name`() {
        val mockTerser = mockk<Terser>()
        every { mockTerser.set(any(), any()) } returns Unit
        val facilityName = "Very Long Facility Name That Should Truncate After Here"
        // Get a bunch of k12 rows
        val testCSV = File("./src/test/unit_test_files/pdi-covid-19-wa-k12.csv").inputStream()
        val testReport = csvSerializer
            .readExternal("primedatainput/pdi-covid-19", testCSV, TestSource)
            .report

        // Test with STANDARD
        serializer.setOrderingFacilityComponent(
            mockTerser,
            facilityName,
            useOrderingFacilityName = Hl7Configuration.OrderingFacilityName.ORGANIZATION_NAME,
            testReport,
            row = 0
        )

        verify {
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-21-1", "Spokane School District")
        }
    }

    @Test
    fun `test incorrect HL7 content`() {
        val settings = FileSettings("./settings")
        val serializer = Hl7Serializer(metadata, settings)

        val emptyHL7 = ByteArrayInputStream("".toByteArray())
        var result = serializer.readExternal(hl7SchemaName, emptyHL7, TestSource)
        assertThat(result.report).isNotNull()
        assertThat(result.report.itemCount).isEqualTo(0)

        val csvContent = ByteArrayInputStream("a,b,c\n1,2,3".toByteArray())
        assertFailsWith<ActionError> {
            result = serializer.readExternal(hl7SchemaName, csvContent, TestSource)
        }

        val incompleteHL7 = ByteArrayInputStream("MSH|^~\\&|CD".toByteArray())
        assertFailsWith<ActionError> {
            result = serializer.readExternal(hl7SchemaName, incompleteHL7, TestSource)
        }

        // This data will throw a EncodingNotSupportedException in the serializer when parsing the message
        val incompleteHL7v2 = ByteArrayInputStream(
            """
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Doe^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            """.trimIndent().toByteArray()
        )
        assertFailsWith<ActionError> {
            result = serializer.readExternal(hl7SchemaName, incompleteHL7v2, TestSource)
        }

        // This data will throw a HL7Exception in the serializer when parsing the message
        val wrongHL7Version = ByteArrayInputStream(
            """
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|5.0.0|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Doe^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
            OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85||94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
            OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.4.7&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
            """.trimIndent().toByteArray()
        )
        assertFailsWith<ActionError> {
            result = serializer.readExternal(hl7SchemaName, wrongHL7Version, TestSource)
        }
    }

    @Test
    fun `test cliaForOutOfStateTesting`() {
        // arrange
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()

        val hl7Config = createConfig(cliaForOutOfStateTesting = "10DfakeCL")
        val receiver = Receiver("test", "ca-dph", Topic.COVID_19, translation = hl7Config)
        val schema = "direct/direct-covid-19"
        val csvHeader = """senderId,testOrdered,testName,testCodingSystem,testResult,testResultText,testPerformed,
            testResultCodingSystem,testResultDate,testReportDate,testOrderedDate,specimenCollectedDate,deviceIdentifier,
            deviceName,specimenId,serialNumber,patientAge,patientAgeUnits,patientDob,patientRace,patientRaceText,
            patientEthnicity,patientEthnicityText,patientSex,patientZip,patientCounty,orderingProviderNpi,
            orderingProviderLname,orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName,
            performingFacilityStreet,performingFacilityStreet2,performingFacilityCity,performingFacilityState,
            performingFacilityZip,performingFacilityCounty,performingFacilityPhone,orderingFacilityName,
            orderingFacilityStreet,orderingFacilityStreet2,orderingFacilityCity,orderingFacilityState,orderingFacilityZip,
            orderingFacilityCounty,orderingFacilityPhone,specimenSource,patientNameLast,patientNameFirst,
            patientNameMiddle,patientUniqueId,patientHomeAddress,patientHomeAddress2,patientCity,patientState,
            patientPhone,patientPhoneArea,orderingProviderAddress,orderingProviderAddress2,orderingProviderCity,
            orderingProviderState,orderingProviderPhone,orderingProviderPhoneArea,firstTest,previousTestType,previousTestDate,
            previousTestResult,correctedTestId,healthcareEmployee,healthcareEmployeeType,symptomatic,symptomsList,hospitalized,
            hospitalizedCode,symptomsIcu,congregateResident,congregateResidentType,pregnant,pregnantText,patientEmail,reportingFacility"""

        val csvBlankState =
            """fake,94531-1,SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection,LN,260415000,
            Detected,94558-4,SCT,202110062022-0400,202110062022-0400,20211007,20211007,00382902560821,
            BD Veritor System for Rapid Detection of SARS-CoV-2*,4efd9df8-9424-4e50-b168-f3aa894bfa42,4efd9df8-9424-4e50-b168-f3aa894bfa42,
            45,yr,1975-10-10,2106-3,White,2135-2,Hispanic or Latino,M,93307,Kern County,1760085880,,,93312,05D2191150,Inovia Pharmacy,
            9902 Brimhall rd ste 100,,Bakersfield,,93312,Kern County,+16618297861,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,,
            93312,Kern County,+16618297861,445297001,Tapia,Jose,,e553c462-6bad-4e42-ab1e-0879b797aa31,1211 Dawn st,,Bakersfield,CA,+16614933107,
            ,9902 BRIMHALL RD STE 100,,BAKERSFIELD,,+16618297861,661,
            UNK,,,,,,,UNK,,NO,,NO,NO,,261665006,UNK,,1760085880"""

        val csvContentBlankState = ByteArrayInputStream(
            csvHeader.replace("\n            ", "")
                .plus("\n")
                .plus(csvBlankState.replace("\n            ", ""))
                .toByteArray()
        )

        val testReportBlankState = csvSerializer
            .readExternal(schema, csvContentBlankState, listOf(TestSource), receiver)
            .report

        val outputBlankState = serializer.createMessage(testReportBlankState, 0)

        val cleanedMessageBlankState = reg.replace(outputBlankState, "\r")
        val hapiMsgBlankState = parser.parse(cleanedMessageBlankState)
        val terserBlankState = Terser(hapiMsgBlankState)
        val cliaTersedBlankState = terserBlankState.get("/MSH-4-2")

        assertThat(cliaTersedBlankState).isNotEqualTo("10DfakeCL")

        val csvCompleteProviderState =
            """fake,94531-1,SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection,LN,260415000,
            Not Detected,94558-4,SCT,202110062022-0400,202110062022-0400,20211007,20211007,00382902560821,
            BD Veritor System for Rapid Detection of SARS-CoV-2*,4efd9df8-9424-4e50-b168-f3aa894bfa42,4efd9df8-9424-4e50-b168-f3aa894bfa42,45,
            yr,1975-10-10,2106-3,White,2135-2,Hispanic or Latino,M,93307,Kern County,1760085880,,,93312,05D2191150,Inovia Pharmacy,
            9902 Brimhall rd ste 100,,Bakersfield,TX,93312,Kern County,+16618297861,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,,
            93312,Kern County,+16618297861,445297001,Tapia,Jose,,e553c462-6bad-4e42-ab1e-0879b797aa31,1211 Dawn st,,Bakersfield,CA,+16614933107,
            ,9902 BRIMHALL RD STE 100,,BAKERSFIELD,CA,+16618297861,661,UNK,,,,,,,UNK,,NO,,NO,NO,,261665006,UNK,,1760085880"""

        // SenderID is set to "fake" in this CSV
        val csvContentProviderState = ByteArrayInputStream(
            csvHeader.replace("\n            ", "")
                .plus("\n")
                .plus(csvCompleteProviderState.replace("\n            ", ""))
                .toByteArray()
        )

        val testReportProviderState = csvSerializer
            .readExternal(schema, csvContentProviderState, listOf(TestSource), receiver)
            .report

        val outputProviderState = serializer.createMessage(testReportProviderState, 0)

        val cleanedMessageProviderState = reg.replace(outputProviderState, "\r")
        val hapiMsgProviderState = parser.parse(cleanedMessageProviderState)
        val terserProviderState = Terser(hapiMsgProviderState)
        val cliaTersedProviderState = terserProviderState.get("/MSH-4-2")

        assertThat(cliaTersedProviderState).isEqualTo("10DfakeCL")

        val csvCompleteFacilityState =
            """fake,94531-1,SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection,LN,260415000,
            Not Detected,94558-4,SCT,202110062022-0400,202110062022-0400,20211007,20211007,00382902560821,
            BD Veritor System for Rapid Detection of SARS-CoV-2*,4efd9df8-9424-4e50-b168-f3aa894bfa42,4efd9df8-9424-4e50-b168-f3aa894bfa42,45,
            yr,1975-10-10,2106-3,White,2135-2,Hispanic or Latino,M,93307,Kern County,1760085880,,,93312,05D2191150,Inovia Pharmacy,
            9902 Brimhall rd ste 100,,Bakersfield,,93312,Kern County,+16618297861,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,TX,93312,
            Kern County,+16618297861,445297001,Tapia,Jose,,e553c462-6bad-4e42-ab1e-0879b797aa31,1211 Dawn st,,Bakersfield,CA,+16614933107,661,
            9902 BRIMHALL RD STE 100,,BAKERSFIELD,TX,+16618297861,661,UNK,,,,,,,UNK,,NO,,NO,NO,,261665006,UNK,,1760085880"""

        // SenderID is set to "fake" in this CSV
        val csvContentFacilityState = ByteArrayInputStream(
            csvHeader.replace("\n            ", "")
                .plus("\n")
                .plus(csvCompleteFacilityState.replace("\n            ", ""))
                .toByteArray()
        )

        val testReportFacilityState = csvSerializer
            .readExternal(schema, csvContentFacilityState, listOf(TestSource), receiver)
            .report

        val outputFacilityState = serializer.createMessage(testReportFacilityState, 0)

        val cleanedMessageFacilityState = reg.replace(outputFacilityState, "\r")
        val hapiMsgFacilityState = parser.parse(cleanedMessageFacilityState)
        val terserFacilityState = Terser(hapiMsgFacilityState)
        val cliaTersedFacilityState = terserFacilityState.get("/MSH-4-2")

        assertThat(cliaTersedFacilityState).isEqualTo("10DfakeCL")
    }

    @Test
    fun `test cliaForSender`() {
        // arrange
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()

        // SenderID is set to "fake" in this CSV
        val csvContent = ByteArrayInputStream(
            "senderId,testOrdered,testName,testCodingSystem,testResult,testResultText,testPerformed,testResultCodingSystem,testResultDate,testReportDate,testOrderedDate,specimenCollectedDate,deviceIdentifier,deviceName,specimenId,serialNumber,patientAge,patientAgeUnits,patientDob,patientRace,patientRaceText,patientEthnicity,patientEthnicityText,patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname,orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName,performingFacilityStreet,performingFacilityStreet2,performingFacilityCity,performingFacilityState,performingFacilityZip,performingFacilityCounty,performingFacilityPhone,orderingFacilityName,orderingFacilityStreet,orderingFacilityStreet2,orderingFacilityCity,orderingFacilityState,orderingFacilityZip,orderingFacilityCounty,orderingFacilityPhone,specimenSource,patientNameLast,patientNameFirst,patientNameMiddle,patientUniqueId,patientHomeAddress,patientHomeAddress2,patientCity,patientState,patientPhone,patientPhoneArea,orderingProviderAddress,orderingProviderAddress2,orderingProviderCity,orderingProviderState,orderingProviderPhone,orderingProviderPhoneArea,firstTest,previousTestType,previousTestDate,previousTestResult,correctedTestId,healthcareEmployee,healthcareEmployeeType,symptomatic,symptomsList,hospitalized,hospitalizedCode,symptomsIcu,congregateResident,congregateResidentType,pregnant,pregnantText,patientEmail,reportingFacility\nfake,94531-1,SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection,LN,260415000,Not Detected,94558-4,SCT,202110062022-0400,202110062022-0400,20211007,20211007,00382902560821,BD Veritor System for Rapid Detection of SARS-CoV-2*,4efd9df8-9424-4e50-b168-f3aa894bfa42,4efd9df8-9424-4e50-b168-f3aa894bfa42,45,yr,1975-10-10,2106-3,White,2135-2,Hispanic or Latino,M,93307,Kern County,1760085880,,,93312,05D2191150,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,445297001,Tapia,Jose,,e553c462-6bad-4e42-ab1e-0879b797aa31,1211 Dawn st,,Bakersfield,CA,+16614933107,661,9902 BRIMHALL RD STE 100,,BAKERSFIELD,CA,+16618297861,661,UNK,,,,,,,UNK,,NO,,NO,NO,,261665006,UNK,,1760085880".toByteArray() // ktlint-disable max-line-length
        )
        val schema = "direct/direct-covid-19"

        val hl7Config = createConfig(cliaForSender = mapOf("fake1" to "ABCTEXT123", "fake" to "10D1234567"))
        val receiver = Receiver("test", "ca-dph", Topic.COVID_19, translation = hl7Config)

        val testReport = csvSerializer.readExternal(schema, csvContent, listOf(TestSource), receiver).report
        val output = serializer.createMessage(testReport, 0)

        val cleanedMessage = reg.replace(output, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        val cliaTersed = terser.get("/MSH-4-2")

        assertThat(cliaTersed).isEqualTo("10D1234567")

        // Test when sender is not found or blank
        val csvContentSenderNotFound = ByteArrayInputStream(
            "senderId,testOrdered,testName,testCodingSystem,testResult,testResultText,testPerformed,testResultCodingSystem,testResultDate,testReportDate,testOrderedDate,specimenCollectedDate,deviceIdentifier,deviceName,specimenId,serialNumber,patientAge,patientAgeUnits,patientDob,patientRace,patientRaceText,patientEthnicity,patientEthnicityText,patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname,orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName,performingFacilityStreet,performingFacilityStreet2,performingFacilityCity,performingFacilityState,performingFacilityZip,performingFacilityCounty,performingFacilityPhone,orderingFacilityName,orderingFacilityStreet,orderingFacilityStreet2,orderingFacilityCity,orderingFacilityState,orderingFacilityZip,orderingFacilityCounty,orderingFacilityPhone,specimenSource,patientNameLast,patientNameFirst,patientNameMiddle,patientUniqueId,patientHomeAddress,patientHomeAddress2,patientCity,patientState,patientPhone,patientPhoneArea,orderingProviderAddress,orderingProviderAddress2,orderingProviderCity,orderingProviderState,orderingProviderPhone,orderingProviderPhoneArea,firstTest,previousTestType,previousTestDate,previousTestResult,correctedTestId,healthcareEmployee,healthcareEmployeeType,symptomatic,symptomsList,hospitalized,hospitalizedCode,symptomsIcu,congregateResident,congregateResidentType,pregnant,pregnantText,patientEmail,reportingFacility\nfake,94531-1,SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection,LN,260415000,Not Detected,94558-4,SCT,202110062022-0400,202110062022-0400,20211007,20211007,00382902560821,BD Veritor System for Rapid Detection of SARS-CoV-2*,4efd9df8-9424-4e50-b168-f3aa894bfa42,4efd9df8-9424-4e50-b168-f3aa894bfa42,45,yr,1975-10-10,2106-3,White,2135-2,Hispanic or Latino,M,93307,Kern County,1760085880,,,93312,05D2191150,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,445297001,Tapia,Jose,,e553c462-6bad-4e42-ab1e-0879b797aa31,1211 Dawn st,,Bakersfield,CA,+16614933107,661,9902 BRIMHALL RD STE 100,,BAKERSFIELD,CA,+16618297861,661,UNK,,,,,,,UNK,,NO,,NO,NO,,261665006,UNK,,1760085880".toByteArray() // ktlint-disable max-line-length
        )
        val receiverSenderNotFound = Receiver("test", "ca-dph", Topic.COVID_19, translation = hl7Config)

        val testRptSenderNotFound = csvSerializer.readExternal(
            schema,
            csvContentSenderNotFound,
            listOf(TestSource),
            receiverSenderNotFound
        ).report
        val outputSenderNotFound = serializer.createMessage(testRptSenderNotFound, 0)

        val cleanedMessageSenderNotFound = reg.replace(outputSenderNotFound, "\r")
        val hapiMsgSenderNotFound = parser.parse(cleanedMessageSenderNotFound)
        val terserSenderNotFound = Terser(hapiMsgSenderNotFound)
        val cliaTersedSenderNotFound = terserSenderNotFound.get("/MSH-4-2")

        assertThat(cliaTersedSenderNotFound).isNotEqualTo("ABCTEXT123")
        assertThat(cliaTersedSenderNotFound).isNotEqualTo("FAKETXT123")
    }

    @Test
    fun `test replaceValue`() {
        // arrange
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()

        // SenderID is set to "fake" in this CSV
        val csvContent = ByteArrayInputStream(
            "senderId,testOrdered,testName,testCodingSystem,testResult,testResultText,testPerformed,testResultCodingSystem,testResultDate,testReportDate,testOrderedDate,specimenCollectedDate,deviceIdentifier,deviceName,specimenId,serialNumber,patientAge,patientAgeUnits,patientDob,patientRace,patientRaceText,patientEthnicity,patientEthnicityText,patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname,orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName,performingFacilityStreet,performingFacilityStreet2,performingFacilityCity,performingFacilityState,performingFacilityZip,performingFacilityCounty,performingFacilityPhone,orderingFacilityName,orderingFacilityStreet,orderingFacilityStreet2,orderingFacilityCity,orderingFacilityState,orderingFacilityZip,orderingFacilityCounty,orderingFacilityPhone,specimenSource,patientNameLast,patientNameFirst,patientNameMiddle,patientUniqueId,patientHomeAddress,patientHomeAddress2,patientCity,patientState,patientPhone,patientPhoneArea,orderingProviderAddress,orderingProviderAddress2,orderingProviderCity,orderingProviderState,orderingProviderPhone,orderingProviderPhoneArea,firstTest,previousTestType,previousTestDate,previousTestResult,correctedTestId,healthcareEmployee,healthcareEmployeeType,symptomatic,symptomsList,hospitalized,hospitalizedCode,symptomsIcu,congregateResident,congregateResidentType,pregnant,pregnantText,patientEmail,reportingFacility\nfake,94531-1,SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection,LN,260415000,Not Detected,94558-4,SCT,202110062022-0400,202110062022-0400,20211007,20211007,00382902560821,BD Veritor System for Rapid Detection of SARS-CoV-2*,4efd9df8-9424-4e50-b168-f3aa894bfa42,4efd9df8-9424-4e50-b168-f3aa894bfa42,45,yr,1975-10-10,2106-3,White,2135-2,Hispanic or Latino,M,93307,Kern County,1760085880,,,93312,05D2191150,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,445297001,Tapia,Jose,,e553c462-6bad-4e42-ab1e-0879b797aa31,1211 Dawn st,,Bakersfield,CA,+16614933107,661,9902 BRIMHALL RD STE 100,,BAKERSFIELD,CA,+16618297861,661,UNK,,,,,,,UNK,,NO,,NO,NO,,261665006,UNK,,1760085880".toByteArray() // ktlint-disable max-line-length
        )
        val schema = "direct/direct-covid-19"

        val hl7Config = createConfig(
            replaceValue = mapOf(
                "" to "ABCTEXT123",
                "fake1" to "ABCTEXT123",
                "MSH-4-1" to "success",
                "MSH-4-2" to "correctText,-,YES!",
                "MSH-4-3" to "MSH-4-2",
                "MSH-10" to "yeah,/,MSH-4-1"
            )
        )
        val receiver = Receiver("mock", "ca-dph", Topic.COVID_19, translation = hl7Config)

        val testReport = csvSerializer.readExternal(schema, csvContent, listOf(TestSource), receiver).report
        val output = serializer.createMessage(testReport, 0)

        val cleanedMessage = reg.replace(output, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        val msh41 = terser.get("/MSH-4-1")
        val msh42 = terser.get("/MSH-4-2")
        val msh43 = terser.get("/MSH-4-3")
        val msh10 = terser.get("/MSH-10")

        assertThat(msh41).isEqualTo("success")
        assertThat(msh42).isEqualTo("correctText-YES!")
        assertThat(msh43).isEqualTo("correctText-YES!")
        assertThat(msh10).isEqualTo("yeah/success")
    }

    @Test
    fun `test NTE Source`() {
        val parser = context.pipeParser

        val uploadStream = File("./src/testIntegration/resources/serializers/csv-upload-test.csv").inputStream()
        val uploadSchema = "upload-covid-19"
        val sender = CovidSender("default", "upload", Sender.Format.CSV, CustomerStatus.TESTING, uploadSchema)
        val testReport = csvSerializer.readExternal(uploadSchema, uploadStream, TestSource, sender).report
        val output = serializer.buildMessage(testReport, 0)
        val hapiMsg = parser.parse(output.toString())
        val terser = Terser(hapiMsg)
        val nte22 = terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/NTE(0)-2-2")

        assertThat(nte22).isNull()
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