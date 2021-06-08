package gov.cdc.prime.router.serializers.datatests

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.parser.PipeParser
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Terser
import datatests.ConversionTest
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import org.apache.commons.io.FilenameUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Runs data comparison tests for HL7 ORU R01 messages based on files in the test folder.
 * This test takes each HL7 file and compares its data to the internal.csv companion file in the
 * same test folder.  For example:  for a file named CareEvolution-20200415-0001.hl7 the data will
 * be compared to the file CareEvolution-20200415-0001.internal.csv.  Internal CSV files can have an
 * optional header row and follow the internal schema used by the the ReportStream router.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsvtoHl7ConversionTests : ConversionTest {

    /**
     * The folder from the classpath that contains the test files
     */
    private val testFileDir = "/datatests/CSV_to_HL7"

    private val inputFileSuffix = ".csv"

    /**
     * Generate individual unit tests for each test file in the test folder.
     * @return a list of dynamic unit tests
     */
    @TestFactory
    fun generateDataTests(): Collection<DynamicTest> {
        return getTestFiles(testFileDir, inputFileSuffix).map {
            DynamicTest.dynamicTest("Test ${FilenameUtils.getBaseName(it)}", FileTest(it))
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
    class FileTest(private val csvAbsolutePath: String) : Executable {
        /**
         * The schema to use.
         */
        private val schemaName = "primedatainput/pdi-covid-19"

        /**
         * The HAPI HL7 parser.
         */
        private val parser: PipeParser

        /**
         * The HAPI Context.
         */
        private val hapiContext = DefaultHapiContext()

        /**
         * The HL7 serializer.
         */
        private val csvSerializer: CsvSerializer

        private val hl7Serializer: Hl7Serializer

        init {
            // Make sure we have some content on the given HL7 file
            assertTrue(File(csvAbsolutePath).length() > 0)

            // Initialize the HL7 parser
            val mcf = CanonicalModelClassFactory("2.5.1")
            hapiContext.modelClassFactory = mcf
            parser = hapiContext.pipeParser

            // Setup the HL7 serializer
            val metadata = Metadata("./metadata")
            csvSerializer = CsvSerializer(metadata)
            hl7Serializer = Hl7Serializer(metadata)
        }

        override fun execute() {
            val expectedResultAbsolutePath = "${FilenameUtils.removeExtension(csvAbsolutePath)}.hl7"
            val testFilename = FilenameUtils.getName(csvAbsolutePath)

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
        private fun readActualResult(): Hl7InputStreamMessageIterator {
            val result = csvSerializer.readExternal(schemaName, File(csvAbsolutePath).inputStream(), TestSource)
            assertNotNull(result)
            assertNotNull(result.report)
            val hl7Stream = ByteArrayOutputStream()
            hl7Serializer.writeBatch(result.report!!, hl7Stream)
            return Hl7InputStreamMessageIterator(ByteArrayInputStream(hl7Stream.toByteArray()), hapiContext)
        }

        /**
         * Read the file [expectedFileAbsolutePath] that has the expected result.
         * @return a list of data rows
         */
        private fun readExpectedResult(expectedFileAbsolutePath: String): Hl7InputStreamMessageIterator {
            return Hl7InputStreamMessageIterator(File(expectedFileAbsolutePath).inputStream(), hapiContext)
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
        private fun compareToExpected(actual: Hl7InputStreamMessageIterator, expected: Hl7InputStreamMessageIterator) {
            assertThat(actual).isNotNull()
            assertThat(expected).isNotNull()
            assertThat(actual.hasNext()).isTrue()
            assertThat(expected.hasNext()).isTrue()

            // Loop through the messages.  In the case of a batch message there will be multiple messages
            while (actual.hasNext()) {
                val actualMsg = actual.next()
                val expectedMsg = expected.next()
                val actualTerser = Terser(actualMsg)
                val expectedTerser = Terser(expectedMsg)

                // Loop through the segments of the message.  Note that HAPI's list of segments may have more
                // than the message itself, but those extra ones have no data.
                var foundMshCount = 0
                while (true) {
                    try {
                        val actualSegmentName = actualTerser.finder.iterate(true, true)
                        val expectedSegmentName = expectedTerser.finder.iterate(true, true)

                        // The finder iteration does not give a clear indication when it is done with the entire
                        // message vs an error when it is set to not loop, but with loop we get an empty segment name.
                        if (actualSegmentName.isNullOrBlank() || expectedSegmentName.isNullOrBlank()) break

                        assertThat(actualSegmentName).isEqualTo(expectedSegmentName)
                        val actualSegment = actualTerser.getSegment(actualSegmentName)
                        val expectedSegment = expectedTerser.getSegment(expectedSegmentName)
                        assertThat(actualSegment.numFields()).isEqualTo(expectedSegment.numFields())

                        // Loop through all the fields in the segment.
                        for (fieldIndex in 1..actualSegment.numFields()) {
                            val actualField = actualSegment.getField(fieldIndex)
                            val expectedField = expectedSegment.getField(fieldIndex)
                            assertThat(actualField.size).isEqualTo(expectedField.size)
                            compareField(actualSegment, actualSegmentName, fieldIndex, actualField, expectedField)
                        }
                    } catch (e: HL7Exception) {
                        fail(e.message)
                    }
                }
            }
        }

        private fun compareField(
            actualSegment: Segment,
            actualSegmentName: String,
            fieldIndex: Int,
            actualField: Array<Type>,
            expectedField: Array<Type>
        ) {
            if (actualField.isNotEmpty()) {

                // Loop through all the components in a field and compare their values.
                for (componentIndex in actualField.indices) {
                    // Compare all values except the date/time of message (MSH-7).  For MSH-7 just check
                    // there is a value.
                    if ("${actualSegmentName}-$fieldIndex" != "MSH-7") {
                        assertThat(
                            actualField[componentIndex].toString(),
                            "$actualSegmentName-$fieldIndex|${actualSegment.names[fieldIndex - 1]}"
                        )
                            .isEqualTo(expectedField[componentIndex].toString())
                    } else {
                        assertThat(
                            actualField[componentIndex].isEmpty,
                            "${actualSegmentName}-$fieldIndex|${actualSegment.names[fieldIndex - 1]}"
                        ).isFalse()
                    }
                }
            }
        }
    }
}