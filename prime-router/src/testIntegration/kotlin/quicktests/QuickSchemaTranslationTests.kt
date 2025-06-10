package gov.cdc.prime.router.tests.quicktests

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.core.main
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.cli.ProcessData
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import quicktests.QuickTestUtils
import kotlin.test.fail

/**
 * Test the translation of data and file naming convention as configured in /quicktests/schema-translations.csv.
 * The configuration file has the following parameters:
 *   Num Records - the number of fake records to generate
 *   Input Schema - the schema to use for the fake records
 *   Target state - (OPTIONAL) a target state for the data
 *   Target county - (OPTIONAL) a target county for the data
 *   Output format - the output format (CSV, HL7, HL7_BATCH)
 *   Name format - (OPTIONAL) a filename name format
 *   Recv organization - (OPTIONAL) a receiving organization for the fake data
 *   Output filename pattern - The pattern to test for when testing the output filename
 *   Extra args - extra arguments to be passed in to the data processor separated by spaces
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuickSchemaTranslationTests {

    /**
     * Location of configuration file in the classpath
     */
    private val configFile = "/quicktests/schema-translations.csv"

    /**
     * Test configuration data class.
     */
    data class TestConfig(
        val numRecords: Int,
        val inputSchema: String,
        val targetState: String,
        val targetCounty: String,
        val outputFormat: String,
        val outputNameFormat: String,
        val outputRecvOrg: String,
        val expectedOutputFilePattern: String,
        val extraArgs: String,
    )

    /**
     * The fields in the test configuration file
     */
    enum class ConfigColumns(val colName: String) {
        /**
         * Number of records.
         */
        NUM_RECORDS("Num Records"),

        /**
         * Input schema.
         */
        INPUT_SCHEMA("Input Schema"),

        /**
         * Target state.
         */
        TARGET_STATE("Target state"),

        /**
         * Target county in the state or empty is none.
         */
        TARGET_COUNTY("Target county"),

        /**
         * The output format.
         */
        OUTPUT_FORMAT("Output format"),

        /**
         * The name format for the filename.
         */
        OUTPUT_NAME_FORMAT("Name format"),

        /**
         * The receiving organization.
         */
        OUTPUT_RECV_ORG("Recv organization"),

        /**
         * The expected output filename pattern to test.
         */
        OUTPUT_PATTERN("Output filename pattern"),

        /**
         * Any extra arguments to be passed in to the data processor separated by spaces.
         */
        EXTRA_ARGS("Extra args"),
    }

    /**
     * Read the configuration file.
     * @return the list of configurations
     */
    private fun readTestConfig(): List<TestConfig> {
        val config: List<TestConfig>
        // Note we can only use input streams since the file may be in a JAR
        val resourceStream = this::class.java.getResourceAsStream(configFile)
        if (resourceStream != null) {
            config = csvReader().readAllWithHeader(resourceStream).map {
                // Make sure we have all the fields we need.
                if (!it[ConfigColumns.NUM_RECORDS.colName].isNullOrBlank() &&
                    !it[ConfigColumns.INPUT_SCHEMA.colName].isNullOrBlank() &&
                    !it[ConfigColumns.TARGET_STATE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.OUTPUT_FORMAT.colName].isNullOrBlank() &&
                    !it[ConfigColumns.OUTPUT_PATTERN.colName].isNullOrBlank()
                ) {
                    TestConfig(
                        it[ConfigColumns.NUM_RECORDS.colName]!!.trim().toInt(),
                        it[ConfigColumns.INPUT_SCHEMA.colName]!!.trim(),
                        it[ConfigColumns.TARGET_STATE.colName]!!.trim(),
                        it[ConfigColumns.TARGET_COUNTY.colName]?.trim() ?: "",
                        it[ConfigColumns.OUTPUT_FORMAT.colName]!!.trim(),
                        it[ConfigColumns.OUTPUT_NAME_FORMAT.colName]?.trim() ?: "",
                        it[ConfigColumns.OUTPUT_RECV_ORG.colName]?.trim() ?: "",
                        it[ConfigColumns.OUTPUT_PATTERN.colName]!!.trim(),
                        it[ConfigColumns.EXTRA_ARGS.colName]?.trim() ?: ""
                    )
                } else {
                    fail("One or more required config columns in $configFile are empty.")
                }
            }
        } else {
            fail("Test configuration file $configFile not found in classpath.")
        }
        return config
    }

    /**
     * Generate the tests based on the configuration.
     * @return the tests
     */
    @TestFactory
    fun generateDataTests(): Collection<DynamicTest> {
        val config = readTestConfig()
        return config.map {
            DynamicTest.dynamicTest("${it.inputSchema}->${it.targetState}", SchemaTranslationTest(it))
        }
    }

    /**
     * Test a schema translation and file name pattern based on the [config] provided.
     */
    inner class SchemaTranslationTest(private val config: TestConfig) : Executable {

        /**
         * The data generator.
         */
        private val dataGenerator = ProcessData(QuickTestUtils.metadata, QuickTestUtils.fileSettings)

        /**
         * Run the data generator.
         */
        private fun generateData() {
            val args = mutableListOf(
                "--input-fake", config.numRecords.toString(), "--input-schema", config.inputSchema,
                "--output-dir", QuickTestUtils.outputDir, "--target-states", config.targetState,
                "--output-format", config.outputFormat
            )
            if (config.targetCounty.isNotBlank()) {
                args.addAll(listOf("--target-counties", config.targetCounty))
            }
            if (config.outputNameFormat.isNotBlank()) {
                args.addAll(listOf("--name-format", config.outputNameFormat))
            }
            if (config.outputRecvOrg.isNotBlank()) {
                args.addAll(listOf("--output-receiving-org", config.outputRecvOrg))
            }
            if (config.extraArgs.isNotBlank()) {
                args.addAll(config.extraArgs.split(" "))
            }
            dataGenerator.main(args)
        }

        override fun execute() {
            generateData()
            assertThat(dataGenerator.outputReportFiles.size).isEqualTo(1)
            QuickTestUtils.checkFilename(dataGenerator.outputReportFiles[0], config.expectedOutputFilePattern)
        }
    }
}