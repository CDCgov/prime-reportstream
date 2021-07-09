package gov.cdc.prime.router.serializers.datatests

import assertk.assertThat
import assertk.assertions.isNotNull
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.ReadResult
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import java.io.File
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConversionTest {
    private val testDataDir = "/datatests"
    private val testConfigFile = "conversion-test-config.csv"
    private val metadata = Metadata("./metadata")
    private val translator = Translator(metadata, FileSettings(FileSettings.defaultSettingsDirectory))

    /**
     * The fields in the test configuration file
     */
    enum class ConfigColumns(val colName: String) {
        /**
         * The input file.
         */
        INPUT_FILE("Input File"),

        /**
         * The expected output file.
         */
        EXPECTED_FILE("Expected File"),

        /**
         * The name of the sender to use including the organization and client name.
         */
        INPUT_SCHEMA("Input schema"),

        /**
         * The organization name for the receiver.
         */
        OUTPUT_SCHEMA("Output schema")
    }

    data class TestConfig(
        val inputFile: String,
        val inputFormat: Report.Format,
        val inputSchema: Schema,
        val expectedFile: String,
        val expectedFormat: Report.Format,
        val expectedSchema: Schema
    )

    /**
     * Generate individual unit tests for each test file in the test folder.
     * @return a list of dynamic unit tests
     */
    @TestFactory
    fun generateDataTests(): Collection<DynamicTest> {
        val config = readTestConfig("$testDataDir/$testConfigFile")
        return config.map {
            DynamicTest.dynamicTest("Test ${it.inputFile}", FileConversionTest(it))
        }
    }

    /**
     * Read the configuration file [configPathname] for this test.
     * @return a map of input file to expected results
     */
    private fun readTestConfig(configPathname: String): List<TestConfig> {
        var config = emptyList<TestConfig>()
        // Note we can only use input streams since the file may be in a JAR
        val resourceStream = this::class.java.getResourceAsStream(configPathname)
        if (resourceStream != null) {
            config = csvReader().readAllWithHeader(resourceStream).map {
                // Make sure we have all the fields we need.
                if (!it[ConfigColumns.INPUT_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.EXPECTED_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.INPUT_SCHEMA.colName].isNullOrBlank() &&
                    !it[ConfigColumns.OUTPUT_SCHEMA.colName].isNullOrBlank()
                ) {
                    val inputSchema = metadata.findSchema(it[ConfigColumns.INPUT_SCHEMA.colName]!!)
                    val expectedSchema = metadata.findSchema(it[ConfigColumns.OUTPUT_SCHEMA.colName]!!)

                    if (inputSchema != null && expectedSchema != null) {
                        TestConfig(
                            it[ConfigColumns.INPUT_FILE.colName]!!,
                            getFormat(it[ConfigColumns.INPUT_FILE.colName]!!),
                            inputSchema,
                            it[ConfigColumns.EXPECTED_FILE.colName]!!,
                            getFormat(it[ConfigColumns.EXPECTED_FILE.colName]!!),
                            expectedSchema
                        )
                    } else if (inputSchema == null) {
                        fail("Input schema $inputSchema was not found.")
                    } else {
                        fail("Output schema $expectedSchema was not found.")
                    }
                } else {
                    fail("One or more config columns in $configPathname are empty.")
                }
            }
        }
        return config
    }

    private fun getFormat(filename: String): Report.Format {
        return when {
            File(filename).extension.uppercase() == "INTERNAL" || filename.uppercase().endsWith("INTERNAL.CSV") -> {
                Report.Format.INTERNAL
            }
            File(filename).extension.uppercase() == "HL7" -> {
                Report.Format.HL7
            }
            else -> {
                Report.Format.CSV
            }
        }
    }

    inner class FileConversionTest(private val config: TestConfig) : Executable {
        override fun execute() {
            val inputFile = "$testDataDir/${config.inputFile}"
            val outputFile = "$testDataDir/${config.expectedFile}"
            val inputStream = this::class.java.getResourceAsStream(inputFile)
            val outputStream = this::class.java.getResourceAsStream(outputFile)
            if (inputStream != null && outputStream != null) {
                val inputReport = readReport(inputFile, inputStream, config.inputSchema, config.inputFormat)
                val expectedReport = readReport(outputFile, outputStream, config.expectedSchema, config.expectedFormat)
                val translatedReport = translate(inputReport, config)
                // Sanity check. Stay sane my friends!
                assertThat(translatedReport.schema).equals(expectedReport.schema)
                compare(translatedReport, expectedReport, config.expectedSchema)
            } else if (inputStream == null) {
                fail("The file ${config.inputFile} was not found.")
            } else {
                fail("The file ${config.expectedFile} was not found.")
            }
        }

        private fun readReport(
            inputFile: String,
            input: InputStream,
            schema: Schema,
            format: Report.Format
        ): Report {

            return when (format) {
                Report.Format.HL7 -> {
                    val result = Hl7Serializer(metadata).readExternal(
                        schema.name,
                        input,
                        TestSource
                    )
                    checkResult(result, inputFile)
                    result.report!!
                }
                Report.Format.INTERNAL -> {
                    CsvSerializer(metadata).readInternal(
                        schema.name,
                        input,
                        listOf(TestSource),
                        useDefaultsForMissing = true
                    )
                }
                else -> {
                    val result = CsvSerializer(metadata).readExternal(
                        schema.name,
                        input,
                        TestSource
                    )
                    checkResult(result, inputFile)
                    result.report!!
                }
            }
        }

        private fun checkResult(result: ReadResult, filename: String) {
            assertTrue(
                result.errors.isEmpty(),
                "There were ${result.errors.size} errors while reading $filename with " +
                    " ${result.warnings.size} warning(s)\n" + result.errors.joinToString("\n") + "\n" +
                    result.warnings.joinToString("\n")
            )
            if (result.warnings.isNotEmpty()) println(result.warnings.joinToString("\n"))
        }

        private fun translate(inputReport: Report, config: TestConfig): Report {
            val mapping = translator.buildMapping(
                config.expectedSchema,
                config.inputSchema,
                emptyMap()
            )
            if (mapping.missing.isNotEmpty()) {
                fail(
                    "When translating to $'${config.expectedSchema.name} " +
                        "missing fields for ${mapping.missing.joinToString(", ")}"
                )
            }
            return inputReport.applyMapping(mapping)
        }
    }

    private fun compare(actualReport: Report, expectedReport: Report, schema: Schema) {
        assertThat(actualReport).isNotNull()
        assertThat(expectedReport).isNotNull()
        assertTrue(actualReport.schema.elements.isNotEmpty())

        assertEquals(
            actualReport.itemCount, expectedReport.itemCount,
            "The number of rows in the reports is not the same."
        )

        // Now check the data in each report.
        val errorMsgs = ArrayList<String>()
        val warningMsgs = ArrayList<String>()

        // Loop through all the rows
        for (i in 0 until actualReport.itemCount) {
            val actualRow = actualReport.getRow(i)
            val expectedRow = findRow(actualRow, expectedReport, schema)

            for (j in actualRow.indices) {
                val element = schema.elements[j]

                // We want to error on differences when the expected data is not empty.
                if (expectedRow[j].isNotBlank() &&
                    actualRow[j].trim() != expectedRow[j].trim()
                ) {
                    errorMsgs.add(
                        "Data value does not match in report $i column #$j, " +
                            "'${element.name}'. Expected: '${expectedRow[j].trim()}', " +
                            "Actual: '${actualRow[j].trim()}'"
                    )
                } else if (expectedRow[j].isBlank() &&
                    actualRow[j].isNotBlank()
                ) {
                    warningMsgs.add(
                        "Actual data has value in report $i for column " +
                            "'${element.name}' - column #$j, but no expected value.  " +
                            "Actual: '${actualRow[j].trim()}'"
                    )
                }
            }
        }

        // Add the errors and warnings to the assert message, so they show up in the build results.
        assertTrue(
            errorMsgs.size == 0,
            "There were ${errorMsgs.size} incorrect data value(s) detected with ${warningMsgs.size} warning(s)\n" +
                errorMsgs.joinToString("\n") + "\n" + warningMsgs.joinToString("\n")
        )
        // Print the warning messages if any
        if (errorMsgs.size == 0 && warningMsgs.size > 0) println(warningMsgs.joinToString("\n"))
    }

    private fun findRow(inputRow: List<String>, expectedReport: Report, schema: Schema): List<String> {
        val lastNameIndex = schema.findElementColumn("patient_last_name")
        val stateIndex = schema.findElementColumn("patient_state")
        val messageIdIndex = schema.findElementColumn("message_id")
        val inputLastName = lastNameIndex?.let { inputRow[it] }
        val inputState = stateIndex?.let { inputRow[it] }
        val inputMessageId = messageIdIndex?.let { inputRow[it] }

        for (i in 0..expectedReport.itemCount) {
            val expectedRow = expectedReport.getRow(i)
            val expectedLastName = lastNameIndex?.let { expectedRow[it] }
            val expectedState = stateIndex?.let { expectedRow[it] }
            val expectedMessageId = messageIdIndex?.let { expectedRow[it] }
            val messageIdMatches = !inputMessageId.isNullOrBlank() && !expectedMessageId.isNullOrBlank() &&
                inputMessageId == expectedMessageId
            val stateMatches = !inputState.isNullOrBlank() && !expectedState.isNullOrBlank() &&
                inputState == expectedState
            val lastNameMatches = !inputLastName.isNullOrBlank() && !expectedLastName.isNullOrBlank() &&
                inputLastName == expectedLastName
            if (messageIdMatches || (stateMatches && lastNameMatches)) {
                return expectedRow
            }
        }
        fail("A record was not found")
    }
}