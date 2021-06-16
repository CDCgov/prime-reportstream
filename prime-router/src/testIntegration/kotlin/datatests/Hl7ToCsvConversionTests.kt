package gov.cdc.prime.router.serializers.datatests

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.parser.PipeParser
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.serializers.Hl7Serializer
import net.jcip.annotations.NotThreadSafe
import org.apache.commons.io.FilenameUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import java.io.File
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Runs data comparison tests for HL7 ORU R01 messages based on files in the test folder.
 * This test takes each HL7 file and compares its data to the internal.csv companion file in the
 * same test folder.  For example:  for a file named CE-20200415-0001.hl7 the data will
 * be compared to the file CE-20200415-0001.internal.csv.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// This keeps this test class from running in parallel with other test classes and letting the time zone change
// affect other tests
@NotThreadSafe
class Hl7ToCsvConversionTests : ConversionTest {

    /**
     * The folder from the classpath that contains the test files
     */
    private val testFileDir = "/datatests/HL7_to_CSV"

    /**
     * The input file extension.
     */
    private val inputFileSuffix = ".hl7"

    /**
     * The original timezone of the JVM
     */
    private val origDefaultTimeZone = TimeZone.getDefault()

    /**
     * Set the default timezone to GMT to match the build and deployment environments.
     */
    @BeforeAll
    fun setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT0"))
    }

    /**
     * Reset the timezone back to the original
     */
    @AfterAll
    fun resetDefaultTimezone() {
        TimeZone.setDefault(origDefaultTimeZone)
    }

    /**
     * Generate individual unit tests for each test file in the test folder.
     * @return a list of dynamic unit tests
     */
    @TestFactory
    fun generateDataTests(): Collection<DynamicTest> {
        return getTestFiles(testFileDir, inputFileSuffix).map {
            DynamicTest.dynamicTest("Test $testFileDir/${FilenameUtils.getBaseName(it)}", FileTest(it))
        }
    }

    /**
     * Perform the unit test for the given HL7 [hl7AbsolutePath].  This test will compare the number of provided reports
     * (e.g. HL7 batch files have multiple reports) and verifies all data elements match the expected values in the
     * related .internal file that exists in the same folder as the HL7 test file.
     *
     * Limitations: Date times in the HL7 data without a specified time zone are bound by the JVM default timezone and hence
     * will generate an error against the GMT0 expected result.  GMT is the timezone of the build and deployment environments.
     */
    class FileTest(private val hl7AbsolutePath: String) : Executable {
        /**
         * The schema to use.
         */
        private val schemaName = "hl7/hl7-ingest-covid-19"

        /**
         * The HAPI HL7 parser.
         */
        private val parser: PipeParser

        /**
         * The HL7 serializer.
         */
        private val serializer: Hl7Serializer

        init {
            // Make sure we have some content on the given HL7 file
            assertTrue(File(hl7AbsolutePath).length() > 0)

            // Initialize the HL7 parser
            val hapiContext = DefaultHapiContext()
            val mcf = CanonicalModelClassFactory("2.5.1")
            hapiContext.modelClassFactory = mcf
            parser = hapiContext.pipeParser

            // Setup the HL7 serializer
            val metadata = Metadata("./metadata")
            serializer = Hl7Serializer(metadata)
        }

        override fun execute() {
            val expectedResultAbsolutePath = "${FilenameUtils.removeExtension(hl7AbsolutePath)}.internal.csv"
            val testFilename = FilenameUtils.getName(hl7AbsolutePath)

            println("Testing file $testFilename ...")
            if (File(expectedResultAbsolutePath).exists()) {
                val report = readActualResult()
                val expectedResult = readExpectedResult(expectedResultAbsolutePath)
                compareToExpected(report, expectedResult)
                assertTrue(true)
            } else {
                fail("The expected data file $expectedResultAbsolutePath was not found for this test.")
            }
            println("PASSED: $testFilename")
            println("--------------------------------------------------------")
        }

        /**
         * Get the report for the HL7 file and check for errors.
         * @return the HL7 report
         */
        private fun readActualResult(): Report {
            val result = serializer.readExternal(schemaName, File(hl7AbsolutePath).inputStream(), TestSource)
            val filename = FilenameUtils.getName(hl7AbsolutePath)
            assertNotNull(result)
            assertNotNull(result.report)

            if (result.errors.isNotEmpty()) {
                println("HL7 file $filename has ${result.errors.size} HL7 decoding errors:")
                result.errors.forEach { println("   SCHEMA ERROR: ${it.details}") }
            }
            if (result.warnings.isNotEmpty()) {
                println("HL7 file $filename has ${result.warnings.size} HL7 decoding warnings:")
                result.warnings.forEach { println("   SCHEMA WARNING: ${it.details}") }
            }
            assertTrue(result.errors.isEmpty(), "There were data errors in the HL7 file.")
            return result.report!!
        }

        /**
         * Read the file [expectedFileAbsolutePath] that has the expected result.
         * @return a list of data rows
         */
        private fun readExpectedResult(expectedFileAbsolutePath: String): List<List<String>> {
            val file = File(expectedFileAbsolutePath)
            return csvReader().readAll(file)
        }

        /**
         * Compare the data in the [actual] report to the data in the [expected] report.  This
         * comparison uses the column names in the expected data to match it to the proper actual data,
         * hence the order of columns in the expected data is not important.
         * Errors are generated when:
         *  1. The number of reports is different
         *  2. A column in the expected values does not exist in the actual values
         *  3. A expected value does not match the actual value
         *
         * Warnings are generated when:
         *  1. An actual value exists, but no expected value.
         *  2. There are more columns in the actual data than the expected data
         *
         */
        private fun compareToExpected(actual: Report, expected: List<List<String>>) {
            assertTrue(actual.schema.elements.isNotEmpty())

            // Make sure we have a header in the expected data file
            val firstColName = if (expected.isNotEmpty() && expected[0].isNotEmpty()) expected[0][0] else ""
            // If we found an element it means we have a header in the data
            if (firstColName.isBlank() || actual.schema.findElement(firstColName) == null) {
                fail("Expected data file must have a header as the first row.")
            }

            // Check the number of reports
            assertEquals(actual.itemCount, expected.size - 1, "Number of reports or headers do not match.")

            // Now check the data in each report.
            val errorMsgs = ArrayList<String>()
            val warningMsgs = ArrayList<String>()

            // Check to see if actual has more columns.  Less columns is checked later on a column by column basis
            if (expected[0].size < actual.schema.elements.size) {
                warningMsgs.add(
                    "   DATA WARNING: Actual record(s) has more columns than expected record(s)"
                )
            }

            val expectedHeaders = expected[0]
            for (i in 0 until actual.itemCount) {
                val actualRow = actual.getRow(i)
                val expectedRow = expected[i + 1] // +1 to skip the header

                for (j in expectedRow.indices) {
                    val actualValueIndex = actual.schema.findElementColumn(expectedHeaders[j])
                    if (actualValueIndex != null) {
                        // We want to error on differences when the expected data is not empty.
                        if (expectedRow[j].isNotBlank() &&
                            actualRow[actualValueIndex].trim() != expectedRow[j].trim()
                        ) {
                            errorMsgs.add(
                                "   DATA ERROR: Data value does not match in report $i column #${j + 1}, " +
                                    "'${expectedHeaders[j]}'. Expected: '${expectedRow[j].trim()}', " +
                                    "Actual: '${actualRow[actualValueIndex].trim()}'"
                            )
                        } else if (expectedRow[j].trim().isEmpty() &&
                            actualRow[actualValueIndex].trim().isNotEmpty()
                        ) {
                            warningMsgs.add(
                                "   DATA WARNING: Actual data has value in report $i for column " +
                                    "'${expectedHeaders[j]}' - column #${j + 1}, but no expected value.  " +
                                    "Actual: '${actualRow[actualValueIndex].trim()}'"
                            )
                        }
                    } else {
                        fail(
                            "Column #${j + 1}/${expectedHeaders[j]} from the " +
                                "expected data is missing in the actual data"
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
    }
}