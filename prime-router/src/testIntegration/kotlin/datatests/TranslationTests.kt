package gov.cdc.prime.router.datatests

import assertk.assertThat
import assertk.assertions.isTrue
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.fhirconverter.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.reportstream.shared.StringUtilities.trimToNull
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.LegacyPipelineSender
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.engine.encodePreserveEncodingChars
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Context
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.helpers.SchemaReferenceResolverHelper
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import gov.cdc.prime.router.fhirengine.utils.filterObservations
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.ReadResult
import io.mockk.mockk
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
    private val settings = FileSettings("./src/testIntegration/resources/settings")

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
         * The output message's schema.
         */
        OUTPUT_SCHEMA("Output schema"),

        /**
         * The output message's format.
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

        /**
         * The sender transform for the report
         */
        SENDER_TRANSFORM("Sender Transform"),

        /**
         * The receiver
         */
        RECEIVER("Receiver"),

        /**
         * The condition filter
         */
        RECEIVER_CONDITION_FILTER("Condition Filter"),

        /**
         * The enrichment schema file name(s)
         */
        ENRICHMENT_SCHEMAS("Enrichment Schema Names"),
    }

    /**
     * A test configuration.
     */
    data class TestConfig(
        val inputFile: String,
        val inputFormat: MimeFormat,
        val inputSchema: String?,
        val expectedFile: String,
        val expectedFormat: MimeFormat,
        val outputSchema: String?,
        val shouldPass: Boolean = true,
        /** are there any fields we should ignore when doing the comparison */
        val ignoreFields: List<String>? = null,
        /** should we hardcode the sender for comparison? */
        val sender: String? = null,
        val senderTransform: String?,
        val receiver: String? = null,
        val conditionFiler: String? = null,
        val enrichmentSchemas: String? = null,
        val profile: String? = null,
    )

    /**
     * Generate individual unit tests for each test file in the test folder.
     * @return a list of dynamic unit tests
     */
    @TestFactory
    fun generateDataTests(): Collection<DynamicTest> {
        val config = readTestConfig("$testDataDir/$testConfigFile")

        val map1 = config.map {
            DynamicTest.dynamicTest("Test ${it.inputFile}, ${it.outputSchema} schema", FileConversionTest(it))
        }

        return map1
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
                    val expectedFormat = MimeFormat.safeValueOf(it[ConfigColumns.OUTPUT_FORMAT.colName])
                    val inputFormat = getFormat(it[ConfigColumns.INPUT_FILE.colName]!!)
                    val inputSchema = it[ConfigColumns.INPUT_SCHEMA.colName]
                    var outputSchema = it[ConfigColumns.OUTPUT_SCHEMA.colName]
                    val sender = it[ConfigColumns.SENDER.colName].trimToNull()
                    val receiver = it[ConfigColumns.RECEIVER.colName].trimToNull()
                    var senderTransform = it[ConfigColumns.SENDER_TRANSFORM.colName].trimToNull()
                    val conditionFilter = it[ConfigColumns.RECEIVER_CONDITION_FILTER.colName].trimToNull()
                    var enrichmentSchemas = it[ConfigColumns.ENRICHMENT_SCHEMAS.colName].trimToNull()
                    val ignoreFields = it[ConfigColumns.IGNORE_FIELDS.colName].let { colNames ->
                        colNames?.split(",") ?: emptyList()
                    }

                    if (senderTransform.isNullOrEmpty() && !sender.isNullOrEmpty()) {
                        val senderSettings = settings.senders.firstOrNull { potentialSender ->
                            potentialSender.organizationName.plus(".").plus(potentialSender.name)
                                .lowercase() == sender.lowercase()
                        }
                        senderTransform = senderSettings?.schemaName
                    }
                    if (outputSchema.isNullOrEmpty() && !receiver.isNullOrEmpty()) {
                        val receiverSettings = settings.receivers.firstOrNull { potentialReceiver ->
                            potentialReceiver.organizationName.plus(".").plus(potentialReceiver.name)
                                .lowercase() == receiver.lowercase()
                        }
                        outputSchema = receiverSettings?.schemaName
                    }
                    if (enrichmentSchemas.isNullOrEmpty() && !receiver.isNullOrEmpty()) {
                        val receiverSettings = settings.receivers.firstOrNull { potentialReceiver ->
                            potentialReceiver.organizationName.plus(".").plus(potentialReceiver.name)
                                .lowercase() == receiver.lowercase()
                        }
                        enrichmentSchemas = receiverSettings?.enrichmentSchemaNames?.joinToString()
                    }

                    val shouldPass = !it[ConfigColumns.RESULT.colName].isNullOrBlank() &&
                        it[ConfigColumns.RESULT.colName].equals("PASS", true)

                    TestConfig(
                        it[ConfigColumns.INPUT_FILE.colName]!!,
                        inputFormat,
                        inputSchema,
                        it[ConfigColumns.EXPECTED_FILE.colName]!!,
                        expectedFormat,
                        outputSchema,
                        shouldPass,
                        ignoreFields,
                        sender,
                        senderTransform,
                        receiver,
                        conditionFilter,
                        enrichmentSchemas
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
    private fun getFormat(filename: String): MimeFormat = when {
            File(filename).extension.uppercase() == "INTERNAL" || filename.uppercase().endsWith("INTERNAL.CSV") -> {
                MimeFormat.INTERNAL
            }

            File(filename).extension.uppercase() == "HL7" -> {
                MimeFormat.HL7
            }

            File(filename).extension.uppercase() == "FHIR" -> {
                MimeFormat.FHIR
            }

            else -> {
                MimeFormat.CSV
            }
        }

    /**
     * Perform test based on the given configuration.
     */
    inner class FileConversionTest(private val config: TestConfig) : Executable {
        override fun execute() {
            runTest()
        }

        fun runTest(): CompareData.Result {
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
                        config.expectedFormat == MimeFormat.FHIR -> {
                            val rawHL7 = inputStream.bufferedReader().readText()
                            val expectedRawFhir = expectedStream.bufferedReader().readText()
                            verifyHL7toFhir(rawHL7, result, expectedRawFhir, config.profile)
                        }

                        // Compare the output of an HL7 to FHIR to HL7 conversion
                        config.expectedFormat == MimeFormat.HL7 && config.inputFormat == MimeFormat.HL7 -> {
                            check(!config.outputSchema.isNullOrBlank())
                            val bundle = translateToFhir(inputStream.bufferedReader().readText(), config.profile)
                            val afterEnrichment = if (config.enrichmentSchemas != null) {
                                runSenderTransformOrEnrichment(bundle, config.enrichmentSchemas)
                            } else {
                                bundle
                            }

                            val afterSenderTransform = if (config.senderTransform != null) {
                                runSenderTransformOrEnrichment(afterEnrichment, config.senderTransform)
                            } else {
                                bundle
                            }
                            val actualStream =
                                translateFromFhir(afterSenderTransform, config.outputSchema, config.receiver)
                            result.merge(
                                CompareData().compare(expectedStream, actualStream, null, null)
                            )
                        }
                        // Compare the output of a FHIR to HL7 conversion
                        config.expectedFormat == MimeFormat.HL7 && config.inputFormat == MimeFormat.FHIR -> {
                            val afterEnrichment = if (config.enrichmentSchemas != null) {
                                runSenderTransformOrEnrichment(inputStream, config.enrichmentSchemas)
                            } else {
                                inputStream
                            }
                            val afterSenderTransform = if (config.senderTransform != null) {
                                runSenderTransformOrEnrichment(afterEnrichment, config.senderTransform)
                            } else {
                                afterEnrichment
                            }
                            check(!config.outputSchema.isNullOrBlank())
                            val actualStream =
                                translateFromFhir(afterSenderTransform, config.outputSchema, config.receiver)
                            result.merge(
                                CompareData().compare(expectedStream, actualStream, null, null)
                            )
                        }

                        // All other conversions related to the Topic pipeline
                        else -> {
                            check(!config.inputSchema.isNullOrBlank())
                            check(!config.outputSchema.isNullOrBlank())
                            val inputSchema = metadata.findSchema(config.inputSchema)
                                ?: fail("Schema ${config.inputSchema} was not found.")
                            val expectedSchema = metadata.findSchema(config.outputSchema)
                                ?: fail("Schema ${config.outputSchema} was not found.")
                            val enrichmentSchema = if (!config.enrichmentSchemas.isNullOrEmpty()) {
                                metadata.findSchema(config.enrichmentSchemas)
                                    ?: fail("Schema ${config.enrichmentSchemas} was not found.")
                            } else {
                                null
                            }

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
                            } else {
                                fail("Error reading input report.")
                            }
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
                if (result.errors.isNotEmpty()) {
                    println(
                        result.errors
                            .joinToString(System.lineSeparator(), "ERRORS:${System.lineSeparator()}")

                    )
                }
                if (result.warnings.isNotEmpty()) {
                    println(
                        result.warnings
                            .joinToString(
                                System.lineSeparator(), "WARNINGS:${System.lineSeparator()}"
                            )
                    )
                }
            } else if (inputStream == null) {
                fail("The file ${config.inputFile} was not found.")
            } else {
                fail("The file ${config.expectedFile} was not found.")
            }

            return result
        }

        /**
         * Helper function to convert an HL7 string into FHIR and verify it matches the passed expected value
         *
         * @param rawHL7 the HL7 message as a string
         * @param result container that holds the results of running the comparisons
         * @param expectedRawFhir the expected raw FHIR string
         * @param an optional profile value that when passed it will run the conversion with that mappings specific to the profile
         */
        private fun verifyHL7toFhir(
            rawHL7: String,
            result: CompareData.Result,
            expectedRawFhir: String,
            profile: String?,
        ) {
            // Currently only supporting one HL7 message
            check(config.inputFormat == MimeFormat.HL7)
            val actualStream = translateToFhir(rawHL7, profile)
            val enrichedStream = if (!config.enrichmentSchemas.isNullOrEmpty()) {
                runSenderTransformOrEnrichment(actualStream, config.enrichmentSchemas)
            } else {
                actualStream
            }

            result.merge(
                CompareData().compare(
                    expectedRawFhir.byteInputStream(), enrichedStream, config.expectedFormat,
                    null
                )
            )
        }

        /**
         * Translate an [hl7] to a FHIR bundle as JSON.
         * @return a FHIR bundle as a JSON input stream
         */
        private fun translateToFhir(hl7: String, profile: String? = null): InputStream {
            val hl7message = HL7Reader.parseHL7Message(hl7)
            val fhirBundle = if (profile == null) {
                HL7toFhirTranslator().translate(hl7message)
            } else {
                HL7toFhirTranslator(profile).translate(hl7message)
            }
            val fhirJson = FhirTranscoder.encode(fhirBundle)
            return fhirJson.byteInputStream()
        }

        /**
         * Translate a [bundle] to an HL7 message as text using the given [schema].
         * @return an HL7 message as an input stream
         */
        private fun translateFromFhir(bundle: InputStream, schema: String, receiverName: String? = null): InputStream {
            var fhirBundle = FhirTranscoder.decode(bundle.bufferedReader().readText())
            val receiver = settings.receivers.firstOrNull {
                it.organizationName.plus(".").plus(it.name).lowercase() == receiverName?.lowercase()
            }
            val translationConfig = if (receiver?.translation is Hl7Configuration) {
                val hl7Config = receiver.translation as Hl7Configuration
                HL7TranslationConfig(hl7Config, receiver)
            } else {
                HL7TranslationConfig(
                    Hl7Configuration(
                        receivingApplicationOID = null,
                        receivingFacilityOID = null,
                        messageProfileId = null,
                        receivingApplicationName = null,
                        receivingFacilityName = null,
                        receivingOrganization = null,
                    ),
                    null
                )
            }

            if (!config.conditionFiler.isNullOrBlank()) {
                fhirBundle = fhirBundle.filterObservations(listOf(config.conditionFiler))
            }

            val hl7 = FhirToHl7Converter(
                SchemaReferenceResolverHelper.retrieveHl7SchemaReference(
                    schema,
                    mockk<BlobAccess.BlobContainerMetadata>()
                ),
                strict = false,
                context = FhirToHl7Context(
                    CustomFhirPathFunctions(),
                    config = translationConfig,
                    translationFunctions = CustomTranslationFunctions()
                ),
                errors = mutableListOf(),
                warnings = mutableListOf(),
            ).process(fhirBundle)
            return hl7.encodePreserveEncodingChars().byteInputStream()
        }

        /**
         * Applies the sender transform or enrichment ([schema]) to the [bundle]
         * @return returns the updated bundle as a byte input stream
         */
        private fun runSenderTransformOrEnrichment(bundle: InputStream, schema: String?): InputStream {
            var fhirBundle = FhirTranscoder.decode(bundle.bufferedReader().readText())
            if (!schema.isNullOrEmpty()) {
                schema.split(",").forEach { currentEnrichmentSchema ->
                    fhirBundle = FhirTransformer(
                        SchemaReferenceResolverHelper.retrieveFhirSchemaReference(
                            currentEnrichmentSchema,
                        mockk<BlobAccess.BlobContainerMetadata>()
                        )
                    ).process(fhirBundle)
                }
            }
            val fhirJson = FhirTranscoder.encode(fhirBundle)
            return fhirJson.byteInputStream()
        }

        /**
         * Read the report from an [input] based on the provided [schema] and [format]. Merges the result into [result].
         * @return the report
         */
        private fun readReport(
            input: InputStream,
            schema: Schema,
            format: MimeFormat,
            result: CompareData.Result,
            senderName: String? = null,
        ): Report? {
            // if we have a sender name we want to work off of, we will look it up by organization name here.
            // NOTE: if you pass in a sender name that does not match anything that exists, you will get a null
            // value for the sender, and your test will fail. This is not a bug.
            val sender = if (senderName != null) {
                settings.senders.firstOrNull {
                    it.organizationName.plus(".").plus(it.name).lowercase() == senderName.lowercase()
                }
            } else {
                settings.senders.filter { it is LegacyPipelineSender && it.schemaName == schema.name }.randomOrNull()
            }
            return try {
                when (format) {
                    // Get a random sender name that uses the provided schema, or null if no sender is found.
                    MimeFormat.HL7 -> {
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

                    MimeFormat.INTERNAL -> {
                        CsvSerializer(metadata).readInternal(
                            schema.name,
                            input,
                            listOf(TestSource)
                        )
                    }

                    MimeFormat.CSV -> {
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
        private fun outputReport(report: Report, format: MimeFormat): InputStream {
            val outputStream = ByteArrayOutputStream()
            when (format) {
                MimeFormat.HL7_BATCH -> Hl7Serializer(metadata, settings).writeBatch(report, outputStream)
                MimeFormat.HL7 -> Hl7Serializer(metadata, settings).write(report, outputStream)
                MimeFormat.INTERNAL -> CsvSerializer(metadata).writeInternal(report, outputStream)
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