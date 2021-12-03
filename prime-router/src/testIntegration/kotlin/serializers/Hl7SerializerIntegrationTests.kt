package gov.cdc.prime.router.serializers

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.fail

class Hl7SerializerIntegrationTests {
    private val hl7TestFileDir = "./src/test/hl7_test_files/"
    private val hl7SchemaName = "hl7/test-covid-19"
    private val testReport: Report
    private val context = DefaultHapiContext()
    private val serializer: Hl7Serializer
    private val csvSerializer: CsvSerializer
    private val covid19Schema: Schema
    private val sampleHl7Message: String
    private val sampleHl7MessageWithRepeats: String
    private val metadata = Metadata.getInstance()

    init {
        val settings = FileSettings("./settings")
        val inputStream = File("./src/test/unit_test_files/fake-pdi-covid-19.csv").inputStream()
        covid19Schema = metadata.findSchema(hl7SchemaName) ?: fail("Could not find target schema")
        csvSerializer = CsvSerializer(metadata)
        serializer = Hl7Serializer(metadata, settings)
        testReport = csvSerializer.readExternal("primedatainput/pdi-covid-19", inputStream, TestSource).report ?: fail()
        sampleHl7Message = """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
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
        sampleHl7MessageWithRepeats = """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
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

    /**
     * Create a mock Hl7 COnfiguration object.
     * @returns mock object
     */
    private fun getMockHl7Configuration(): Hl7Configuration {
        return mockkClass(Hl7Configuration::class).also {
            every { it.format }.returns(Report.Format.HL7)
            every { it.useTestProcessingMode }.returns(false)
            every { it.suppressQstForAoe }.returns(false)
            every { it.suppressAoe }.returns(false)
            every { it.suppressHl7Fields }.returns(null)
            every { it.useBlankInsteadOfUnknown }.returns(null)
            every { it.convertTimestampToDateTime }.returns(null)
            every { it.truncateHDNamespaceIds }.returns(false)
            every { it.phoneNumberFormatting }.returns(Hl7Configuration.PhoneNumberFormatting.STANDARD)
            every { it.usePid14ForPatientEmail }.returns(false)
            every { it.reportingFacilityName }.returns(null)
            every { it.reportingFacilityId }.returns(null)
            every { it.reportingFacilityIdType }.returns(null)
        }
    }

    /**
     * Create a mock receiver.
     * @return mock object
     */
    private fun getMockReceiver(hl7Config: Hl7Configuration): Receiver {
        return mockkClass(Receiver::class).also {
            every { it.translation }.returns(hl7Config)
            every { it.format }.returns(Report.Format.HL7)
            every { it.organizationName }.returns("ca-dph")
        }
    }

    @Test
    fun `Test write batch`() {
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

        val hl7Config = getMockHl7Configuration().also {
            every { it.replaceValue }.returns(mapOf("PID-22-3" to "CDCREC,-,testCDCREC", "MSH-9" to "MSH-10"))
            every { it.cliaForOutOfStateTesting }.returns("1234FAKECLIA")
            every { it.useOrderingFacilityName }.returns(Hl7Configuration.OrderingFacilityName.STANDARD)
            every { it.cliaForSender }.returns(mapOf())
            every { it.valueSetOverrides }.returns(mapOf())
        }
        val receiver = getMockReceiver(hl7Config)

        val testReport = csvSerializer.readExternal(schema, inputStream, listOf(TestSource), receiver).report ?: fail()
        val output = serializer.createMessage(testReport, 2)
        assertThat(output).isNotNull()
    }

    @Test
    fun `test converting hl7 into mapped list of values`() {
        val mappedMessage = serializer.convertMessageToMap(sampleHl7Message, covid19Schema)
        val mappedValues = mappedMessage.row
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
        val mappedMessage = serializer.convertMessageToMap(message, covid19Schema)
        val mappedValues = mappedMessage.row
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
        mappedMessage.errors.forEach {
            println(it)
        }
        println("Warnings:")
        mappedMessage.warnings.forEach {
            println(it)
        }
    }

    @Test
    fun `test reading HL7 batch and creating report instance`() {
        val inputFile = "$hl7TestFileDir/batch_message.hl7"
        val message = File(inputFile)
        val source = FileSource(inputFile)
        val readResult = serializer.readExternal(hl7SchemaName, message.inputStream(), source)
        val report = readResult.report ?: fail("Report was null and should not be")
        assertThat(report.getString(0, "patient_city")).isEqualTo("South Rodneychester")
        assertThat(report.getString(1, "patient_city")).isEqualTo("North Taylor")
        assertThat(report.itemCount == 2).isTrue()
        val hospitalized = (0 until report.itemCount).map { report.getString(it, "hospitalized") }
        assertThat(hospitalized.toSet()).isEqualTo(setOf(""))
    }

    @Test
    fun `test getSchoolId`() {
        // Get a bunch of k12 rows
        val testCSV = File("./src/test/unit_test_files/pdi-covid-19-wa-k12.csv").inputStream()
        val testReport = csvSerializer
            .readExternal("primedatainput/pdi-covid-19", testCSV, TestSource)
            .report ?: fail()

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
            .report ?: fail()

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
            .report ?: fail()

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
        assertThat(result.report!!.itemCount).isEqualTo(0)

        val csvContent = ByteArrayInputStream("a,b,c\n1,2,3".toByteArray())
        result = serializer.readExternal(hl7SchemaName, csvContent, TestSource)
        assertThat(result.errors).isNotEmpty()
        assertThat(result.report).isNull()

        val incompleteHL7 = ByteArrayInputStream("MSH|^~\\&|CD".toByteArray())
        result = serializer.readExternal(hl7SchemaName, incompleteHL7, TestSource)
        assertThat(result.errors).isNotEmpty()
        assertThat(result.report).isNull()

        // This data will throw a EncodingNotSupportedException in the serializer when parsing the message
        val incompleteHL7v2 = ByteArrayInputStream(
            """
            MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
            SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME ReportStream|0.1-SNAPSHOT||20210210
            PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Doe^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
            """.trimIndent().toByteArray()
        )
        result = serializer.readExternal(hl7SchemaName, incompleteHL7v2, TestSource)
        assertThat(result.errors).isNotEmpty()
        assertThat(result.report).isNull()

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
        result = serializer.readExternal(hl7SchemaName, wrongHL7Version, TestSource)
        assertThat(result.errors).isNotEmpty()
        assertThat(result.report).isNull()
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
        val csvContent = ByteArrayInputStream("senderId,testOrdered,testName,testCodingSystem,testResult,testResultText,testPerformed,testResultCodingSystem,testResultDate,testReportDate,testOrderedDate,specimenCollectedDate,deviceIdentifier,deviceName,specimenId,serialNumber,patientAge,patientAgeUnits,patientDob,patientRace,patientRaceText,patientEthnicity,patientEthnicityText,patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname,orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName,performingFacilityStreet,performingFacilityStreet2,performingFacilityCity,performingFacilityState,performingFacilityZip,performingFacilityCounty,performingFacilityPhone,orderingFacilityName,orderingFacilityStreet,orderingFacilityStreet2,orderingFacilityCity,orderingFacilityState,orderingFacilityZip,orderingFacilityCounty,orderingFacilityPhone,specimenSource,patientNameLast,patientNameFirst,patientNameMiddle,patientUniqueId,patientHomeAddress,patientHomeAddress2,patientCity,patientState,patientPhone,patientPhoneArea,orderingProviderAddress,orderingProviderAddress2,orderingProviderCity,orderingProviderState,orderingProviderPhone,orderingProviderPhoneArea,firstTest,previousTestType,previousTestDate,previousTestResult,correctedTestId,healthcareEmployee,healthcareEmployeeType,symptomatic,symptomsList,hospitalized,hospitalizedCode,symptomsIcu,congregateResident,congregateResidentType,pregnant,pregnantText,patientEmail,reportingFacility\nfake,94531-1,SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection,LN,260415000,Not Detected,94558-4,SCT,202110062022-0400,202110062022-0400,20211007,20211007,00382902560821,BD Veritor System for Rapid Detection of SARS-CoV-2*,4efd9df8-9424-4e50-b168-f3aa894bfa42,4efd9df8-9424-4e50-b168-f3aa894bfa42,45,yr,1975-10-10,2106-3,White,2135-2,Hispanic or Latino,M,93307,Kern County,1760085880,,,93312,05D2191150,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,445297001,Tapia,Jose,,e553c462-6bad-4e42-ab1e-0879b797aa31,1211 Dawn st,,Bakersfield,CA,+16614933107,661,9902 BRIMHALL RD STE 100,,BAKERSFIELD,CA,+16618297861,661,UNK,,,,,,,UNK,,NO,,NO,NO,,261665006,UNK,,1760085880".toByteArray()) // ktlint-disable max-line-length
        val schema = "direct/direct-covid-19"

        val hl7Config = getMockHl7Configuration().also {
            every { it.replaceValue }.returns(mapOf())
            every { it.cliaForOutOfStateTesting }.returns(null)
            every { it.useOrderingFacilityName }.returns(Hl7Configuration.OrderingFacilityName.STANDARD)
            every { it.cliaForSender }.returns(mapOf("fake1" to "ABCTEXT123", "fake" to "10D1234567"))
            every { it.defaultAoeToUnknown }.returns(false)
            every { it.valueSetOverrides }.returns(mapOf())
        }
        val receiver = getMockReceiver(hl7Config)

        val testReport = csvSerializer.readExternal(schema, csvContent, listOf(TestSource), receiver).report ?: fail()
        val output = serializer.createMessage(testReport, 0)

        val cleanedMessage = reg.replace(output, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        val cliaTersed = terser.get("/MSH-4-2")

        assertThat(cliaTersed).isEqualTo("10D1234567")

        // Test when sender is not found or blank
        val csvContentSenderNotFound = ByteArrayInputStream("senderId,testOrdered,testName,testCodingSystem,testResult,testResultText,testPerformed,testResultCodingSystem,testResultDate,testReportDate,testOrderedDate,specimenCollectedDate,deviceIdentifier,deviceName,specimenId,serialNumber,patientAge,patientAgeUnits,patientDob,patientRace,patientRaceText,patientEthnicity,patientEthnicityText,patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname,orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName,performingFacilityStreet,performingFacilityStreet2,performingFacilityCity,performingFacilityState,performingFacilityZip,performingFacilityCounty,performingFacilityPhone,orderingFacilityName,orderingFacilityStreet,orderingFacilityStreet2,orderingFacilityCity,orderingFacilityState,orderingFacilityZip,orderingFacilityCounty,orderingFacilityPhone,specimenSource,patientNameLast,patientNameFirst,patientNameMiddle,patientUniqueId,patientHomeAddress,patientHomeAddress2,patientCity,patientState,patientPhone,patientPhoneArea,orderingProviderAddress,orderingProviderAddress2,orderingProviderCity,orderingProviderState,orderingProviderPhone,orderingProviderPhoneArea,firstTest,previousTestType,previousTestDate,previousTestResult,correctedTestId,healthcareEmployee,healthcareEmployeeType,symptomatic,symptomsList,hospitalized,hospitalizedCode,symptomsIcu,congregateResident,congregateResidentType,pregnant,pregnantText,patientEmail,reportingFacility\nfake,94531-1,SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection,LN,260415000,Not Detected,94558-4,SCT,202110062022-0400,202110062022-0400,20211007,20211007,00382902560821,BD Veritor System for Rapid Detection of SARS-CoV-2*,4efd9df8-9424-4e50-b168-f3aa894bfa42,4efd9df8-9424-4e50-b168-f3aa894bfa42,45,yr,1975-10-10,2106-3,White,2135-2,Hispanic or Latino,M,93307,Kern County,1760085880,,,93312,05D2191150,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,445297001,Tapia,Jose,,e553c462-6bad-4e42-ab1e-0879b797aa31,1211 Dawn st,,Bakersfield,CA,+16614933107,661,9902 BRIMHALL RD STE 100,,BAKERSFIELD,CA,+16618297861,661,UNK,,,,,,,UNK,,NO,,NO,NO,,261665006,UNK,,1760085880".toByteArray()) // ktlint-disable max-line-length

        val hl7ConfigSenderNotFound = getMockHl7Configuration().also {
            every { it.replaceValue }.returns(mapOf())
            every { it.cliaForOutOfStateTesting }.returns(null)
            every { it.useOrderingFacilityName }.returns(Hl7Configuration.OrderingFacilityName.STANDARD)
            every { it.cliaForSender }.returns(mapOf("NotFound" to "ABCTEXT123", "" to "FAKETXT123"))
            every { it.defaultAoeToUnknown }.returns(false)
            every { it.valueSetOverrides }.returns(mapOf())
        }

        val receiverSenderNotFound = getMockReceiver(hl7ConfigSenderNotFound)

        val testRptSenderNotFound = csvSerializer.readExternal(schema, csvContentSenderNotFound, listOf(TestSource), receiverSenderNotFound).report ?: fail() // ktlint-disable max-line-length
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
        val csvContent = ByteArrayInputStream("senderId,testOrdered,testName,testCodingSystem,testResult,testResultText,testPerformed,testResultCodingSystem,testResultDate,testReportDate,testOrderedDate,specimenCollectedDate,deviceIdentifier,deviceName,specimenId,serialNumber,patientAge,patientAgeUnits,patientDob,patientRace,patientRaceText,patientEthnicity,patientEthnicityText,patientSex,patientZip,patientCounty,orderingProviderNpi,orderingProviderLname,orderingProviderFname,orderingProviderZip,performingFacility,performingFacilityName,performingFacilityStreet,performingFacilityStreet2,performingFacilityCity,performingFacilityState,performingFacilityZip,performingFacilityCounty,performingFacilityPhone,orderingFacilityName,orderingFacilityStreet,orderingFacilityStreet2,orderingFacilityCity,orderingFacilityState,orderingFacilityZip,orderingFacilityCounty,orderingFacilityPhone,specimenSource,patientNameLast,patientNameFirst,patientNameMiddle,patientUniqueId,patientHomeAddress,patientHomeAddress2,patientCity,patientState,patientPhone,patientPhoneArea,orderingProviderAddress,orderingProviderAddress2,orderingProviderCity,orderingProviderState,orderingProviderPhone,orderingProviderPhoneArea,firstTest,previousTestType,previousTestDate,previousTestResult,correctedTestId,healthcareEmployee,healthcareEmployeeType,symptomatic,symptomsList,hospitalized,hospitalizedCode,symptomsIcu,congregateResident,congregateResidentType,pregnant,pregnantText,patientEmail,reportingFacility\nfake,94531-1,SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection,LN,260415000,Not Detected,94558-4,SCT,202110062022-0400,202110062022-0400,20211007,20211007,00382902560821,BD Veritor System for Rapid Detection of SARS-CoV-2*,4efd9df8-9424-4e50-b168-f3aa894bfa42,4efd9df8-9424-4e50-b168-f3aa894bfa42,45,yr,1975-10-10,2106-3,White,2135-2,Hispanic or Latino,M,93307,Kern County,1760085880,,,93312,05D2191150,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,Inovia Pharmacy,9902 Brimhall rd ste 100,,Bakersfield,CA,93312,Kern County,+16618297861,445297001,Tapia,Jose,,e553c462-6bad-4e42-ab1e-0879b797aa31,1211 Dawn st,,Bakersfield,CA,+16614933107,661,9902 BRIMHALL RD STE 100,,BAKERSFIELD,CA,+16618297861,661,UNK,,,,,,,UNK,,NO,,NO,NO,,261665006,UNK,,1760085880".toByteArray()) // ktlint-disable max-line-length
        val schema = "direct/direct-covid-19"

        val hl7Config = getMockHl7Configuration().also {
            every { it.replaceValue }.returns(
                mapOf(
                    "" to "ABCTEXT123",
                    "fake1" to "ABCTEXT123",
                    "MSH-4-1" to "success",
                    "MSH-4-2" to "correctText,-,YES!",
                    "MSH-4-3" to "MSH-4-2",
                    "MSH-10" to "yeah,/,MSH-4-1"
                )
            )
            every { it.cliaForOutOfStateTesting }.returns(null)
            every { it.useOrderingFacilityName }.returns(Hl7Configuration.OrderingFacilityName.STANDARD)
            every { it.cliaForSender }.returns(mapOf("fake1" to "ABCTEXT123", "fake" to "10D1234567"))
            every { it.defaultAoeToUnknown }.returns(false)
            every { it.valueSetOverrides }.returns(null)
        }
        val receiver = getMockReceiver(hl7Config)

        val testReport = csvSerializer.readExternal(schema, csvContent, listOf(TestSource), receiver).report ?: fail()
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
}