package gov.cdc.prime.router.datatests

import assertk.assertThat
import assertk.assertions.isTrue
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.TopicSender
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.common.StringUtilities.trimToNull
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.ReadResult
import org.apache.commons.io.FilenameUtils
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
        RESULT("Result"),

        /**
         * A comma-delimited list of fields to ignore
         */
        IGNORE_FIELDS("Ignore Fields"),

        /**
         * The default sender for the report
         */
        SENDER("Sender"),
    }

    /**
     * A test configuration.
     */
    data class TestConfig(
        val inputFile: String,
        val inputFormat: Report.Format,
        val inputSchema: String?,
        val expectedFile: String,
        val expectedFormat: Report.Format,
        val expectedSchema: String?,
        val shouldPass: Boolean = true,
        /** are there any fields we should ignore when doing the comparison */
        val ignoreFields: List<String>? = null,
        /** should we hardcode the sender for comparison? */
        val sender: String? = null
    )

    /**
     * Generate individual unit tests for each test file in the test folder.
     * @return a list of dynamic unit tests
     */
    @TestFactory
    fun generateDataTests(): Collection<DynamicTest> {
        val config = readTestConfig("$testDataDir/$testConfigFile")
        return config.map {
            DynamicTest.dynamicTest("Test ${it.inputFile}, ${it.expectedSchema} schema", FileConversionTest(it))
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
                    val inputFormat = getFormat(it[ConfigColumns.INPUT_FILE.colName]!!)
                    val inputSchema = it[ConfigColumns.INPUT_SCHEMA.colName]
                    val expectedSchema = it[ConfigColumns.OUTPUT_SCHEMA.colName]
                    val sender = it[ConfigColumns.SENDER.colName].trimToNull()
                    val ignoreFields = it[ConfigColumns.IGNORE_FIELDS.colName].let { colNames ->
                        colNames?.split(",") ?: emptyList()
                    }

                    val shouldPass = !it[ConfigColumns.RESULT.colName].isNullOrBlank() &&
                        it[ConfigColumns.RESULT.colName].equals("PASS", true)

                    TestConfig(
                        it[ConfigColumns.INPUT_FILE.colName]!!,
                        inputFormat,
                        inputSchema,
                        it[ConfigColumns.EXPECTED_FILE.colName]!!,
                        expectedFormat,
                        expectedSchema,
                        shouldPass,
                        ignoreFields,
                        sender
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
            // these next two calls look for an embedded resource in the class and pull them out
            // by their name, rather than look for them explicitly on disk
            val inputStream = this::class.java.getResourceAsStream(inputFile)
            val expectedStream = this::class.java.getResourceAsStream(expectedFile)
            if (inputStream != null && expectedStream != null) {
                if (result.passed) {
                    when {
                        // Compare the output of an HL7 to FHIR conversion
                        config.expectedFormat == Report.Format.FHIR -> {
                            // Currently only supporting one HL7 message
                            check(config.inputFormat == Report.Format.HL7)
                            val actualStream = translateToFhir(inputStream)
                            result.merge(
                                CompareData().compare(
                                    expectedStream, actualStream, config.expectedFormat,
                                    null
                                )
                            )
                        }

                        // Compare the output of an HL7 to FHIR to HL7 conversion
                        config.expectedFormat == Report.Format.HL7 && config.inputFormat == Report.Format.HL7 -> {
                            check(!config.expectedSchema.isNullOrBlank())
                            val bundle = translateToFhir(inputStream)
                            val actualStream =
                                translateFromFhir(bundle, config.expectedSchema)
                            result.merge(
                                CompareData().compare(expectedStream, actualStream, null, null)
                            )
                        }

                        // All other conversions related to the Topic pipeline
                        else -> {
                            check(!config.inputSchema.isNullOrBlank())
                            check(!config.expectedSchema.isNullOrBlank())
                            val inputSchema = metadata.findSchema(config.inputSchema)
                                ?: fail("Schema ${config.inputSchema} was not found.")
                            val expectedSchema = metadata.findSchema(config.expectedSchema)
                                ?: fail("Schema ${config.expectedSchema} was not found.")
                            val inputReport = readReport(
                                inputStream,
                                inputSchema,
                                config.inputFormat,
                                result,
                                config.sender
                            )
                            val actualStream = if (inputReport != null) {
                                val translatedReport = translateReport(inputReport, inputSchema, expectedSchema)
                                outputReport(translatedReport, config.expectedFormat)
                            } else fail("Error reading input report.")
                            result.merge(
                                CompareData().compare(
                                    expectedStream,
                                    actualStream,
                                    config.expectedFormat,
                                    expectedSchema,
                                    fieldsToIgnore = config.ignoreFields
                                )
                            )
                        }
                    }
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
         * Translate a [bundle] to an HL7 message as text using the given [schema].
         * @return an HL7 message as an input stream
         */
        private fun translateFromFhir(bundle: InputStream, schema: String): InputStream {
            val fhirBundle = FhirTranscoder.decode(bundle.bufferedReader().readText())
            val hl7 =
                FhirToHl7Converter(FilenameUtils.getName(schema), FilenameUtils.getPath(schema)).convert(fhirBundle)
            return hl7.encode().byteInputStream()
        }

        /**
         * Read the report from an [input] based on the provided [schema] and [format]. Merges the result into [result].
         * @return the report
         */
        private fun readReport(
            input: InputStream,
            schema: Schema,
            format: Report.Format,
            result: CompareData.Result,
            senderName: String? = null
        ): Report? {
            // if we have a sender name we want to work off of, we will look it up by organization name here.
            // NOTE: if you pass in a sender name that does not match anything that exists, you will get a null
            // value for the sender, and your test will fail. This is not a bug.
            val sender = if (senderName != null) {
                settings.senders.firstOrNull { it.organizationName.lowercase() == senderName.lowercase() }
            } else {
                settings.senders.filter { it is TopicSender && it.schemaName == schema.name }.randomOrNull()
            }
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
                    Report.Format.CSV -> {
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
                    else -> {
                        result.passed = false
                        val actionLogger = ActionLogger()
                        actionLogger.error(
                            InvalidReportMessage(
                                "Format for report not handled in this test. Received $format"
                            )
                        )
                        ReadResult(
                            Report(
                                schema,
                                listOf(listOf("")),
                                TestSource,
                                null,
                                format
                            ),
                            actionLogger
                        ).report
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
         * Translate a [report] based on the provided [inputSchema] and [expectedSchema].
         * @return the translated report
         */
        private fun translateReport(report: Report, inputSchema: Schema, expectedSchema: Schema): Report {
            val mapping = translator.buildMapping(
                expectedSchema,
                inputSchema,
                emptyMap()
            )
            if (mapping.missing.isNotEmpty()) {
                fail(
                    "When translating to $'${expectedSchema.name} " +
                        "missing fields for ${mapping.missing.joinToString(", ")}"
                )
            }
            return report.applyMapping(mapping)
        }
    }
}