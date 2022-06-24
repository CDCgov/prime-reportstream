package gov.cdc.prime.router.datatests

import assertk.assertThat
import assertk.assertions.isTrue
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.fail

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TranslationTests {
    /**
     * The folder in the resources folder where the configuration and test data reside.
     */
    private val testDataDir = "/datatests"

    /**
     * The path to the configuration file from the test data directory.
     */
    private val testConfigFile = "translation-test-config.csv"

    /**
     * The metadata
     */
    private val metadata = Metadata.getInstance()

    /**
     * The settings
     */
    private val settings = FileSettings("./settings")

    /**
     * The translator
     */
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
        OUTPUT_SCHEMA("Output schema"),

        /**
         * The organization name for the receiver.
         */
        OUTPUT_FORMAT("Output format"),

        /**
         * The expected result of the test
         */
        RESULT("Result")
    }

    /**
     * A test configuration.
     */
    data class TestConfig(
        val inputFile: String,
        val inputFormat: Report.Format,
        val inputSchema: Schema?,
        val expectedFile: String,
        val expectedFormat: Report.Format,
        val expectedSchema: Schema?,
        val shouldPass: Boolean = true
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
     * @return a list of tests to perform
     */
    private fun readTestConfig(configPathname: String): List<TestConfig> {
        val config: List<TestConfig>
        // Note we can only use input streams since the file may be in a JAR
        val resourceStream = this::class.java.getResourceAsStream(configPathname)
        if (resourceStream != null) {
            config = csvReader().readAllWithHeader(resourceStream).map {
                // Make sure we have all the fields we need.
                if (!it[ConfigColumns.INPUT_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.EXPECTED_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.OUTPUT_FORMAT.colName].isNullOrBlank()
                ) {
                    val expectedFormat = Report.Format.safeValueOf(it[ConfigColumns.OUTPUT_FORMAT.colName])
                    val inputSchema = if (!it[ConfigColumns.INPUT_SCHEMA.colName].isNullOrBlank())
                        metadata.findSchema(it[ConfigColumns.INPUT_SCHEMA.colName]!!)
                    else null
                    val expectedSchema = if (!it[ConfigColumns.OUTPUT_SCHEMA.colName].isNullOrBlank())
                        metadata.findSchema(it[ConfigColumns.OUTPUT_SCHEMA.colName]!!)
                    else null

                    if (expectedFormat != Report.Format.FHIR)
                        if (inputSchema == null) {
                            fail("Input schema $inputSchema was not found.")
                        } else if (expectedSchema == null) {
                            fail("Output schema $expectedSchema was not found.")
                        }
                    val shouldPass = !it[ConfigColumns.RESULT.colName].isNullOrBlank() &&
                        it[ConfigColumns.RESULT.colName].equals("PASS", true)

                    TestConfig(
                        it[ConfigColumns.INPUT_FILE.colName]!!,
                        getFormat(it[ConfigColumns.INPUT_FILE.colName]!!),
                        inputSchema,
                        it[ConfigColumns.EXPECTED_FILE.colName]!!,
                        expectedFormat,
                        expectedSchema,
                        shouldPass
                    )
                } else {
                    fail("One or more config columns in $configPathname are empty.")
                }
            }
        } else {
            fail("Test configuration file $configPathname not found in classpath.")
        }
        return config
    }

    /**
     * Get the report format from the extension of a [filename].
     * @return the report format
     */
    private fun getFormat(filename: String): Report.Format {
        return when {
            File(filename).extension.uppercase() == "INTERNAL" || filename.uppercase().endsWith("INTERNAL.CSV") -> {
                Report.Format.INTERNAL
            }
            File(filename).extension.uppercase() == "HL7" -> {
                Report.Format.HL7
            }
            File(filename).extension.uppercase() == "FHIR" -> {
                Report.Format.FHIR
            }
            else -> {
                Report.Format.CSV
            }
        }
    }

    /**
     * Perform test based on the given configuration.
     */
    inner class FileConversionTest(private val config: TestConfig) : Executable {
        override fun execute() {
            val result = CompareData.Result()
            // First read in the data
            val inputFile = "$testDataDir/${config.inputFile}"
            val expectedFile = "$testDataDir/${config.expectedFile}"
            val inputStream = this::class.java.getResourceAsStream(inputFile)
            val expectedStream = this::class.java.getResourceAsStream(expectedFile)
            if (inputStream != null && expectedStream != null) {

                if (result.passed) {
                    val actualStream = if (config.expectedFormat == Report.Format.FHIR) {
                        // Currently only supporting one HL7 message
                        check(config.inputFormat == Report.Format.HL7)
                        translateToFhir(inputStream)
                    } else {
                        check(config.inputSchema != null)
                        check(config.expectedSchema != null)
                        val inputReport = readReport(inputStream, config.inputSchema, config.inputFormat, result)
                        if (inputReport != null) {
                            val translatedReport = translateReport(inputReport, config)
                            outputReport(translatedReport, config.expectedFormat)
                        } else fail("Error reading input report.")
                    }
                    result.merge(
                        CompareData().compare(
                            expectedStream, actualStream, config.expectedFormat,
                            config.expectedSchema
                        )
                    )
                }
                expectedStream.close()

                if (!config.shouldPass && result.passed) result.errors.add("Test was expected to fail, but passed.")
                assertEquals(
                    config.shouldPass, result.passed,
                    result.errors.joinToString(System.lineSeparator(), "ERRORS:${System.lineSeparator()}") +
                        System.lineSeparator() +
                        result.warnings.joinToString(System.lineSeparator(), "WARNINGS:${System.lineSeparator()}")
                )
                // Print the errors and warnings after the test completed successfully.
                if (result.errors.isNotEmpty()) println(
                    result.errors
                        .joinToString(System.lineSeparator(), "ERRORS:${System.lineSeparator()}")

                )
                if (result.warnings.isNotEmpty()) println(
                    result.warnings
                        .joinToString(
                            System.lineSeparator(), "WARNINGS:${System.lineSeparator()}"
                        )
                )
            } else if (inputStream == null) {
                fail("The file ${config.inputFile} was not found.")
            } else {
                fail("The file ${config.expectedFile} was not found.")
            }
        }

        /**
         * Translate an [hl7] to a FHIR bundle as JSON.
         * @return a FHIR bundle as a JSON input stream
         */
        private fun translateToFhir(hl7: InputStream): InputStream {
            val hl7messages = HL7Reader(ActionLogger()).getMessages(hl7.bufferedReader().readText())
            val fhirBundles = hl7messages.map { message ->
                HL7toFhirTranslator.getInstance().translate(message)
            }
            check(fhirBundles.size == 1)
            val fhirJson = FhirTranscoder.encode(fhirBundles[0])
            return fhirJson.byteInputStream()
        }

        /**
         * Read the report from an [input] based on the provided [schema] and [format].
         * @return the report
         */
        private fun readReport(
            input: InputStream,
            schema: Schema,
            format: Report.Format,
            result: CompareData.Result
        ): Report? {
            val sender = settings.senders.filter { it is CovidSender && it.schemaName == schema.name }.randomOrNull()
            return try {
                when (format) {
                    // Get a random sender name that uses the provided schema, or null if no sender is found.
                    Report.Format.HL7 -> {
                        val readResult = Hl7Serializer(metadata, settings).readExternal(
                            schema.name,
                            input,
                            TestSource,
                            sender
                        )
                        readResult.actionLogs.errors.forEach { result.errors.add(it.detail.message) }
                        readResult.actionLogs.warnings.forEach { result.warnings.add(it.detail.message) }
                        result.passed = !readResult.actionLogs.hasErrors()
                        readResult.report
                    }
                    Report.Format.INTERNAL -> {
                        CsvSerializer(metadata).readInternal(
                            schema.name,
                            input,
                            listOf(TestSource)
                        )
                    }
                    else -> {
                        val readResult = CsvSerializer(metadata).readExternal(
                            schema.name,
                            input,
                            TestSource,
                            sender
                        )
                        readResult.actionLogs.errors.forEach { result.errors.add(it.detail.message) }
                        readResult.actionLogs.warnings.forEach { result.warnings.add(it.detail.message) }
                        result.passed = !readResult.actionLogs.hasErrors()
                        readResult.report
                    }
                }
            } catch (e: ActionError) {
                e.details.forEach { result.errors.add(it.detail.message) }
                result.passed = false
                null
            }
        }

        /**
         * Outputs a [report] to the specified [format].
         * @return the report output
         */
        private fun outputReport(
            report: Report,
            format: Report.Format
        ): InputStream {
            val outputStream = ByteArrayOutputStream()
            when (format) {
                Report.Format.HL7_BATCH -> Hl7Serializer(metadata, settings).writeBatch(report, outputStream)
                Report.Format.HL7 -> Hl7Serializer(metadata, settings).write(report, outputStream)
                Report.Format.INTERNAL -> CsvSerializer(metadata).writeInternal(report, outputStream)
                else -> CsvSerializer(metadata).write(report, outputStream)
            }
            assertThat(outputStream.size() > 0).isTrue()

            return ByteArrayInputStream(outputStream.toByteArray())
        }

        /**
         * Translate an [report] based on the provided [config].
         */
        private fun translateReport(report: Report, config: TestConfig): Report {
            check(config.expectedSchema != null)
            check(config.inputSchema != null)
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
            return report.applyMapping(mapping)
        }
    }
}