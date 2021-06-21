package gov.cdc.prime.router.serializers.datatests

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.parser.PipeParser
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Terser
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
 * Runs data comparison tests for CSV messages converted to HL7 based on files in the test folder.
 * This test takes each CSV file and compares its data to the HL7 companion file in the
 * same test folder.  For example:  for a file named CE-20200415-0001.csv, the data will
 * be compared to the file CE-20200415-0001.hl7.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsvToHl7ConversionTests : ConversionTest {

    /**
     * The folder from the classpath that contains the test files
     */
    private val testFileDir = "/datatests/CSV_to_HL7"

    /**
     * The input file extension.
     */
    private val inputFileSuffix = ".csv"

    /**
     * The list of fields that contain dynamic values that cannot be compared.  Source:
     * Hl7Serializer.setLiterals()
     */
    private val dyanmicHl7Values = arrayOf("MSH-7", "SFT-2", "SFT-4", "SFT-6")

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
     * Perform the unit test for the given CSV [csvAbsolutePath].  This test will compare the number of provided reports
     * and verifies all data elements match the expected values in the related .hl7 file that exists in the same
     * folder as the HL7 test file.
     */
    inner class FileTest(private val csvAbsolutePath: String) : Executable {
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
         * The CSV serializer.
         */
        private val csvSerializer: CsvSerializer

        /**
         * The HL7 serializer.
         */
        private val hl7Serializer: Hl7Serializer

        /**
         * The list of test errors.
         */
        val errorMsgs = arrayListOf<String>()

        /**
         * The list of test warnings.
         */
        val warningMsgs = arrayListOf<String>()

        init {
            // Make sure we have some content on the given HL7 file
            assertTrue(File(csvAbsolutePath).length() > 0)

            // Initialize the HL7 parser
            val mcf = CanonicalModelClassFactory("2.5.1")
            hapiContext.modelClassFactory = mcf
            parser = hapiContext.pipeParser

            // Setup the serializers
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
         * Get the report for the CSV file and convert it to HL7.
         * @return the actual HL7 report
         */
        private fun readActualResult(): Hl7InputStreamMessageIterator {
            val result = csvSerializer.readExternal(schemaName, File(csvAbsolutePath).inputStream(), TestSource)
            assertNotNull(result, "Actual result")
            assertNotNull(result.report, "Actual result report")
            val hl7Stream = ByteArrayOutputStream()
            hl7Serializer.writeBatch(result.report!!, hl7Stream)
            return Hl7InputStreamMessageIterator(ByteArrayInputStream(hl7Stream.toByteArray()), hapiContext)
        }

        /**
         * Read the file [expectedFileAbsolutePath] that has the expected result.
         * @return the expected HL7 report
         */
        private fun readExpectedResult(expectedFileAbsolutePath: String): Hl7InputStreamMessageIterator {
            return Hl7InputStreamMessageIterator(File(expectedFileAbsolutePath).inputStream(), hapiContext)
        }

        /**
         * Compare the data in the [actual] report to the data in the [expected] report.  This
         * comparison uses steps through all the segments in the HL7 messages and compares all the values in the
         * existing fields.
         * Errors are generated when:
         *  1. The number of reports is different
         *  2. A segment in the expected values does not exist in the actual values
         *  3. A component expected value does not match the actual value
         *
         * Warnings are generated when:
         *  1. A component actual value exists, but no expected value.
         */
        private fun compareToExpected(actual: Hl7InputStreamMessageIterator, expected: Hl7InputStreamMessageIterator) {
            assertThat(actual).isNotNull()
            assertThat(expected).isNotNull()
            assertThat(actual.hasNext(), "Actual has messages").isTrue()
            assertThat(expected.hasNext(), "Expected has messages ").isTrue()

            var recordNum = 1

            // Loop through the messages.  In the case of a batch message there will be multiple messages
            while (actual.hasNext()) {
                val actualMsg = actual.next()
                val expectedMsg = expected.next()
                val actualTerser = Terser(actualMsg)
                val expectedTerser = Terser(expectedMsg)

                while (true) {
                    try {
                        val actualSegmentName = actualTerser.finder.iterate(true, true)
                        val expectedSegmentName = expectedTerser.finder.iterate(true, true)

                        assertThat(actualSegmentName, "Actual segment name").isEqualTo(expectedSegmentName)

                        // The HAPI finder iteration does not give a clear indication when it is done with the entire
                        // message vs an error when it is set to not loop, but with loop we get an empty segment name.
                        if (actualSegmentName.isNullOrBlank() || expectedSegmentName.isNullOrBlank()) {
                            break
                        }

                        val actualSegment = actualTerser.getSegment(actualSegmentName)
                        val expectedSegment = expectedTerser.getSegment(expectedSegmentName)
                        assertThat(actualSegment.numFields(), "Actual number of fields in segment")
                            .isEqualTo(expectedSegment.numFields())

                        // Loop through all the fields in the segment.
                        for (fieldIndex in 1..actualSegment.numFields()) {
                            val actualField = actualSegment.getField(fieldIndex)
                            val expectedField = expectedSegment.getField(fieldIndex)
                            compareField(
                                recordNum, "$actualSegmentName-$fieldIndex",
                                actualSegment.names[fieldIndex - 1], actualField, expectedField
                            )
                        }
                    } catch (e: HL7Exception) {
                        fail(e.message)
                    }
                }
                assertTrue(
                    errorMsgs.size == 0,
                    "There were ${errorMsgs.size} incorrect data value(s) detected with ${warningMsgs.size} " +
                        "warning(s)\n" + errorMsgs.joinToString("\n") + "\n" +
                        warningMsgs.joinToString("\n")
                )

                // Print the warning messages if any
                if (errorMsgs.size == 0 && warningMsgs.size > 0) println(warningMsgs.joinToString("\n"))
                recordNum++
            }
        }

        /**
         * Compare an [actualFieldContents] to an [expectedFieldContents] HL7 field for a given [recordNum], [fieldSpec],
         * and [fieldName]. All components in a field are compared and dynamic fields are checked
         * they have content.
         */
        private fun compareField(
            recordNum: Int,
            fieldSpec: String,
            fieldName: String,
            actualFieldContents: Array<Type>,
            expectedFieldContents: Array<Type>
        ) {
            val maxNumRepetitions = if (actualFieldContents.size > expectedFieldContents.size) actualFieldContents.size
            else expectedFieldContents.size
            if (maxNumRepetitions > 0) {
                // Loop through all the components in a field and compare their values.
                for (repetitionIndex in 0 until maxNumRepetitions) {
                    // If this is not a dynamic value then check it against the expected values
                    if (!dyanmicHl7Values.contains(fieldSpec)) {
                        val expectedFieldValue = if (repetitionIndex < expectedFieldContents.size)
                            expectedFieldContents[repetitionIndex].toString().trim() else ""
                        val actualFieldValue = if (repetitionIndex < actualFieldContents.size)
                            actualFieldContents[repetitionIndex].toString().trim() else ""
                        compareComponents(
                            actualFieldValue, expectedFieldValue, recordNum,
                            "$fieldSpec($repetitionIndex)",
                            fieldName
                        )
                    }
                    // For dynamic values we expect them to be have something
                    else if (actualFieldContents[repetitionIndex].isEmpty) {
                        errorMsgs.add(
                            "    DATA ERROR: No date/time of message for record $recordNum in field $fieldSpec"
                        )
                    }
                }
            }
        }

        /**
         * Compare the components of a given [actualFieldValue] and [expectedFieldValue] for the given [recordNum],
         * [fieldSpec] and [fieldName]
         */
        private fun compareComponents(
            actualFieldValue: String,
            expectedFieldValue: String,
            recordNum: Int,
            fieldSpec: String,
            fieldName: String
        ) {
            // Get the components.  HAPI can return a string with a type (e.g. HD[blah^blah]) or a string
            // with a single value
            val typeRegex = Regex(".*\\[ *(.*) *\\].*")
            val expectedValueComponents = if (expectedFieldValue.matches(typeRegex)) {
                typeRegex.find(expectedFieldValue)!!.destructured.component1().split("[", "]", "^")
            } else {
                listOf(expectedFieldValue)
            }
            val actualValueComponents = if (actualFieldValue.matches(typeRegex)) {
                typeRegex.find(actualFieldValue)!!.destructured.component1().split("[", "]", "^")
            } else {
                listOf(actualFieldValue)
            }

            // Loop through all the components
            val maxNumComponents = if (actualValueComponents.size > expectedValueComponents.size)
                actualValueComponents.size else expectedValueComponents.size
            for (componentIndex in 0 until maxNumComponents) {
                val expectedComponentValue = if (componentIndex < expectedValueComponents.size)
                    expectedValueComponents[componentIndex].trim() else ""
                val actualComponentValue = if (componentIndex < actualValueComponents.size)
                    actualValueComponents[componentIndex].trim() else ""

                // If we have more than one component then show the component number is the messages
                val componentSpec = if (maxNumComponents > 1) "$fieldSpec-${componentIndex + 1}" else fieldSpec

                if (expectedComponentValue.isNotBlank() && expectedComponentValue != actualComponentValue) {
                    errorMsgs.add(
                        "    DATA ERROR: Data value does not match in report $recordNum for " +
                            "$componentSpec|$fieldName. Expected: '$expectedComponentValue', " +
                            "Actual: '$actualComponentValue'"
                    )
                } else if (expectedComponentValue.isBlank() && actualComponentValue.isNotBlank()) {
                    warningMsgs.add(
                        "    DATA WARNING: Actual data has value in report $recordNum for " +
                            "$componentSpec|$fieldName but no expected value. Actual: '$actualComponentValue'"
                    )
                }
            }
        }
    }
}