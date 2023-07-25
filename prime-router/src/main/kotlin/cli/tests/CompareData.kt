package gov.cdc.prime.router.cli.tests

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Terser
import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.common.DateUtilities.toOffsetDateTime
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.utils.CompareFhirData
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * Uses test data provided via a configuration file, sends the data to the API, then checks the response,
 * lineage results and compares the actual data with expected data provided in the configuration file.  This
 * test is run via a configuration file that specifies what the input and expected reports are for the test.
 * compare-test-config.csv has the following columns:
 *   Input File,Sender,Expected File,Organization name,Receiver name,Expected count
 * Where:
 *   Input File = input HL7 or CSV file relative to the config file folder
 *   Sender = the name of the sender including organization (e.g. ignore.ignore-hl7)
 *   Expected File = the name of one of the expected output files relative to the config file folder
 *   Organization name = the name of the receiver organization (e.g. ignore)
 *   Receiver name = the name of the receiver (e.g. CSV)
 *   Expected count = The number of expected records sent as an integer
 * You can specify one or more expected files for a given input file by having multiple lines with the same input
 * file.  E.g.
 *   Input File,Sender,Expected File,Organization name,Receiver name,Expected count
 *   test-0001-input-covid-19.hl7,ignore.ignore-hl7,test-0001-az-covid-19-hl7.hl7,ignore,HL7_BATCH,5
 *   test-0001-input-covid-19.hl7,ignore.ignore-hl7,test-0001-pima-az-covid-19.csv,ignore,CSV,5
 *
 * LIMITATIONS: Only supports CSV and HL7_BATCH receivers.  Tests are run sequentially.
 */
class DataCompareTest : CoolTest() {
    override val name = "compare"
    override val description = "Send data to API based on config file, check lineage and compare data against expected"
    override val status = TestStatus.DRAFT

    private val testDataDir = "/clitests/compare-test-files"
    private val testConfigFile = "compare-test-config.csv"

    /**
     * The input information for a test.
     */
    data class TestInput(val inputFile: String, val sender: String)

    /**
     * The expected output from ReportStream.
     */
    data class TestOutput(
        val outputFile: String,
        val orgName: String,
        val receiverName: String,
        val expectedCount: Int,
        var receiver: Receiver? = null
    )

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
        SENDER("Sender"),

        /**
         * The organization name for the receiver.
         */
        RECEIVER_ORG_NAME("Organization name"),

        /**
         * The receiver name.
         */
        RECEIVER_NAME("Receiver name"),

        /**
         * The number of expected reports in the output.  This is used to check the results in the lineage.
         */
        EXPECTED_COUNT("Expected count")
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        var passed = true
        val configs = readTestConfig("$testDataDir/$testConfigFile")

        // Go through each input file
        if (configs.isNotEmpty()) {
            configs.forEach { config ->
                // Collect some useful data first
                val input = config.key
                val inputFilePath = "$testDataDir/${config.key.inputFile}"
                val inputFile = this::class.java.getResourceAsStream(inputFilePath)
                val outputList = config.value
                val sender = settings.findSender(input.sender)
                    ?: error("Unable to find sender ${input.sender}")
                val receivers = outputList.map { testOutput -> // TODO do we need receivers
                    // We expect only one receiver back
                    val rec = settings.receivers.filter { receiver ->
                        receiver.organizationName == testOutput.orgName && receiver.name == testOutput.receiverName
                    }
                    if (rec.isNotEmpty()) {
                        if (rec[0].format == Report.Format.HL7_BATCH || rec[0].format == Report.Format.CSV) {
                            testOutput.receiver = rec[0]
                            rec[0]
                        } else {
                            error("This test only supports receivers with CSV and HL7_BATCH formats.")
                        }
                    } else {
                        error("Receiver not found")
                    }
                }

                if (inputFile != null) {
                    // Send the input file to ReportStream
                    val (responseCode, json) =
                        HttpUtilities.postReportBytes(environment, inputFile.readBytes(), sender, options.key)
                    inputFile.close()
                    if (responseCode != HttpURLConnection.HTTP_CREATED) {
                        bad("***$name Test FAILED***:  response code $responseCode")
                        passed = false
                    } else {
                        good("Posting of report succeeded with response code $responseCode")
                    }

                    // Check the response from the endpoint
                    echo(json)
                    passed = passed and examinePostResponse(json, !options.asyncProcessMode)

                    // Compare the data
                    val reportId = getReportIdFromResponse(json)
                    if (reportId != null) {
                        // if testing async, verify process result
                        if (options.asyncProcessMode) {
                            // gets back the id of the internal report
                            val internalReportId = getSingleChildReportId(reportId)
                                ?: return bad("***$name FAILED***: Internal report id null")

                            pollForStepResult(internalReportId, TaskAction.process)

                            val processResults = pollForStepResult(internalReportId, TaskAction.process)
                            // verify each result is valid
                            for (result in processResults.values)
                                passed = passed && examineProcessResponse(result)
                            if (!passed) {
                                bad("***$name FAILED***: Process result invalid")
                            }
                        }

                        // Look at the lineage results
                        waitABit(25, environment)
                        var totalItemCount = 0
                        outputList.forEach { totalItemCount += it.expectedCount }
                        passed = passed and pollForLineageResults(
                            reportId,
                            receivers,
                            totalItemCount,
                            asyncProcessMode = options.asyncProcessMode
                        )

                        // Compare the data
                        outputList.forEach { output ->
                            passed = passed and compareSentReports(
                                reportId,
                                output,
                                options.sftpDir
                            )
                        }
                    }
                } else {
                    error("***$name Test FAILED***: Input file $inputFilePath not found.")
                }
            }
        }
        return passed
    }

    /**
     * Read the configuration file [configPathname] for this test.
     * @return a map of input file to expected results
     */
    private fun readTestConfig(configPathname: String): Map<TestInput, List<TestOutput>> {
        val config = mutableMapOf<TestInput, MutableList<TestOutput>>()
        // Note we can only use input streams since the file may be in a JAR
        val resourceStream = this::class.java.getResourceAsStream(configPathname)
        if (resourceStream != null) {
            csvReader().readAllWithHeader(resourceStream).forEach {
                // Make sure we have all the fields we need.
                if (!it[ConfigColumns.INPUT_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.SENDER.colName].isNullOrBlank() &&
                    !it[ConfigColumns.RECEIVER_ORG_NAME.colName].isNullOrBlank() &&
                    !it[ConfigColumns.RECEIVER_NAME.colName].isNullOrBlank() &&
                    !it[ConfigColumns.EXPECTED_COUNT.colName].isNullOrBlank()
                ) {
                    val input = TestInput(
                        it[ConfigColumns.INPUT_FILE.colName]!!.trim(),
                        it[ConfigColumns.SENDER.colName]!!.trim()
                    )
                    val output = TestOutput(
                        it[ConfigColumns.EXPECTED_FILE.colName]?.trim() ?: "",
                        it[ConfigColumns.RECEIVER_ORG_NAME.colName]!!.trim(),
                        it[ConfigColumns.RECEIVER_NAME.colName]!!.trim(),
                        Integer.parseInt(it[ConfigColumns.EXPECTED_COUNT.colName]!!)
                    )

                    // Add to an existing input file config if it exists, otherwise create a new one
                    if (config.containsKey(input)) {
                        config[input]!!.add(output)
                    } else {
                        config[input] = mutableListOf(output)
                    }
                } else {
                    error("***$name Test FAILED***: One or more config columns in $configPathname are empty.")
                }
            }
        }
        return config
    }

    /**
     * Compare the expected reports for the [reportId] to the actual [output] by getting the outputs from the
     * database and comparing them to the expected results saved to the [sftpDir] as specified in the config
     * file for the test.
     * @return true if all actuals match the expected values, false otherwise
     */
    private fun compareSentReports(
        reportId: ReportId,
        output: TestOutput,
        sftpDir: String
    ): Boolean {
        var passed = true
        db = WorkflowEngine().db
        db.transact { txn ->
            // Get the output files from the database
            val outputFilename = sftpFilenameQuery(txn, reportId, output.receiver!!.name)
            if (!outputFilename.isNullOrBlank()) {
                val outputFile = File(sftpDir, outputFilename)
                val expectedOutputPath = "$testDataDir/${output.outputFile}"
                // Note we can only use input streams since the file may be in a JAR
                val expectedOutputStream = this::class.java.getResourceAsStream(expectedOutputPath)
                val schema = metadata.findSchema(output.receiver!!.schemaName)
                if (outputFile.canRead() && expectedOutputStream != null && schema != null) {
                    echo("----------------------------------------------------------")
                    echo("Comparing expected data from $expectedOutputPath")
                    echo("with actual data from $sftpDir/$outputFilename")
                    echo("using schema ${schema.name}...")
                    val result = CompareData().compare(
                        expectedOutputStream,
                        outputFile.inputStream(),
                        output.receiver!!.format,
                        schema
                    )
                    if (result.passed) {
                        good("Test passed: Data comparison")
                    } else {
                        bad("***$name Test FAILED***: Data comparison FAILED")
                    }
                    if (result.errors.size > 0) bad(result.errors.joinToString("\n", "ERROR: "))
                    if (result.warnings.size > 0) echo(result.warnings.joinToString("\n", "WARNING: "))
                    echo("")
                    passed = passed and result.passed
                }
            } else {
                bad("***$name Test FAILED***: Unable to get SFTP filename from database")
            }
        }
        return passed
    }
}

/**
 * Compares two data files or messages.
 */
class CompareData {
    /**
     * The result of the comparison including warnings and errors.
     */
    data class Result(
        var passed: Boolean = true,
        val errors: ArrayList<String> = ArrayList(),
        val warnings: ArrayList<String> = ArrayList()
    ) {
        /**
         * Merge results by adding [anotherResult] errors and warnings and setting the passed flag.
         */
        fun merge(anotherResult: Result) {
            passed = passed and anotherResult.passed
            errors.addAll(anotherResult.errors)
            warnings.addAll(anotherResult.warnings)
        }

        override fun toString(): String {
            return """passed: $passed
errors: ${errors.joinToString()}
warnings: ${warnings.joinToString()}
            """
        }
    }

    /**
     * Compares two files, an [actual] and [expected], of the same [schema] and [format].
     * @return the result for the comparison, with result.passed true if the comparison was successful
     */
    fun compare(
        expected: File,
        actual: File,
        format: Report.Format?,
        schema: Schema
    ): Result {
        val result = Result()
        fun checkFile(file: File) {
            if (!file.canRead()) {
                result.errors.add("Unable to read ${file.absolutePath}")
                result.passed = false
            }
        }

        checkFile(expected)
        checkFile(actual)
        if (result.passed) {
            compare(
                expected.inputStream(),
                actual.inputStream(),
                format,
                schema,
                result
            )
        }
        return result
    }

    /**
     * Compares two input streams, an [actual] and [expected], of the same [schema] and [format].
     * @param fieldsToIgnore is a list of fields that should be ignored when doing a comparison of field values.
     * @return the result for the comparison, with result.passed true if the comparison was successful
     */
    fun compare(
        expected: InputStream,
        actual: InputStream,
        format: Report.Format?,
        schema: Schema?,
        result: Result = Result(),
        fieldsToIgnore: List<String>? = null
    ): Result {
        check((format == Report.Format.CSV && schema != null) || format != Report.Format.CSV) { "Schema is required" }
        val compareResult = when (format) {
            Report.Format.CSV, Report.Format.CSV_SINGLE, Report.Format.INTERNAL ->
                CompareCsvData().compare(expected, actual, schema!!, fieldsToIgnore)
            Report.Format.HL7, Report.Format.HL7_BATCH -> CompareHl7Data().compare(expected, actual)
            Report.Format.FHIR -> CompareFhirData().compare(expected, actual)
            else -> CompareFile().compare(expected, actual)
        }
        result.merge(compareResult)
        return result
    }
}

/**
 * Compares two HL7 files.
 * @property result object to contain the result of the comparison
 */
class CompareHl7Data(
    val result: CompareData.Result = CompareData.Result(),
    private val ignoredFields: List<String> = covidDynamicHl7Fields
) {
    companion object {
        /**
         * The list of fields that contain dynamic values that cannot be compared.  Source:
         * Hl7Serializer.setLiterals()
         */
        private val covidDynamicHl7Fields = listOf("MSH-7", "SFT-2", "SFT-4", "SFT-6")
    }

    /**
     * Compare the data in the [actual] report to the data in the [expected] report.  This
     * comparison uses steps through all the segments in the HL7 messages and compares all the values in the
     * existing fields.
     * @return the result for the comparison, with result.passed true if the comparison was successful
     * Errors are generated when:
     *  1. The number of reports is different
     *  2. A segment in the expected values does not exist in the actual values
     *  3. A component expected value does not match the actual value
     *
     * Warnings are generated when:
     *  1. A component actual value exists, but no expected value.
     */
    fun compare(
        expected: InputStream,
        actual: InputStream,
        result: CompareData.Result = CompareData.Result()
    ): CompareData.Result {
        var passed = true
        val mcf = CanonicalModelClassFactory("2.5.1")
        val hapiContext = DefaultHapiContext()
        hapiContext.modelClassFactory = mcf
        val expectedMsgs = Hl7InputStreamMessageIterator(expected, hapiContext)
        val actualMsgs = Hl7InputStreamMessageIterator(actual, hapiContext)

        var recordNum = 1

        // Loop through the messages.  In the case of a batch message there will be multiple messages
        while (actualMsgs.hasNext()) {
            val actualMsg = actualMsgs.next()
            val expectedMsg = try {
                expectedMsgs.next()
            } catch (e: NoSuchElementException) {
                result.errors.add(
                    "The number of expected messages is less than the actual $recordNum messages."
                )
                passed = false
                break
            }
            val actualTerser = Terser(actualMsg)
            val expectedTerser = Terser(expectedMsg)

            while (true) {
                try {
                    val actualSegmentName = actualTerser.finder.iterate(true, true)
                    val expectedSegmentName = expectedTerser.finder.iterate(true, true)

                    if (actualSegmentName != expectedSegmentName) {
                        result.errors.add(
                            "Actual HL7 segment name does not match expected. " +
                                "Actual: $actualSegmentName, Expected: $expectedSegmentName"
                        )
                        passed = false
                        break
                    }

                    // The HAPI finder iteration does not give a clear indication when it is done with the entire
                    // message vs an error when it is set to not loop, but with loop=true we get an empty segment name.
                    if (actualSegmentName.isNullOrBlank() || expectedSegmentName.isNullOrBlank()) {
                        if (!expectedSegmentName.isNullOrBlank()) {
                            result.errors.add(
                                "There is an extra segment named $expectedSegmentName missing from " +
                                    " the actual message. "
                            )
                            passed = false
                        }
                        break
                    }

                    val actualSegment = actualTerser.getSegment(actualSegmentName)
                    val expectedSegment = expectedTerser.getSegment(expectedSegmentName)
                    if (actualSegment.numFields() != expectedSegment.numFields()) {
                        result.errors.add(
                            "Actual number of fields in HL7 segment does not match expected. " +
                                "Actual: ${actualSegment.numFields()}, Expected: ${expectedSegment.numFields()}"
                        )
                        passed = false
                        break
                    }

                    // Loop through all the fields in the segment.
                    for (fieldIndex in 1..actualSegment.numFields()) {
                        val actualField = actualSegment.getField(fieldIndex)
                        val expectedField = expectedSegment.getField(fieldIndex)
                        passed = passed and compareField(
                            recordNum,
                            "$actualSegmentName-$fieldIndex",
                            actualSegment.names[fieldIndex - 1],
                            actualField,
                            expectedField,
                            result
                        )
                    }
                } catch (e: HL7Exception) {
                    result.errors.add("There was an error parsing the HL7 messages: $e")
                    passed = false
                }
            }
            recordNum++
        }
        result.passed = result.passed and passed
        return result
    }

    /**
     * Compare an [actualFieldContents] to an [expectedFieldContents] HL7 field for a given [recordNum], [fieldSpec],
     * and [fieldName]. All components in a field are compared and dynamic fields are checked
     * they have content.
     * @return true if the field data is equal, false otherwise
     */
    fun compareField(
        recordNum: Int,
        fieldSpec: String,
        fieldName: String,
        actualFieldContents: Array<Type>,
        expectedFieldContents: Array<Type>,
        result: CompareData.Result
    ): Boolean {
        var passed = true
        val maxNumRepetitions = if (actualFieldContents.size > expectedFieldContents.size) actualFieldContents.size
        else expectedFieldContents.size
        if (maxNumRepetitions > 0) {
            // Loop through all the components in a field and compare their values.
            for (repetitionIndex in 0 until maxNumRepetitions) {
                // If this is not a dynamic value then check it against the expected values
                if (!ignoredFields.contains(fieldSpec)) {
                    val expectedFieldValue = if (repetitionIndex < expectedFieldContents.size) {
                        expectedFieldContents[repetitionIndex].toString().trim()
                    } else ""
                    val actualFieldValue = if (repetitionIndex < actualFieldContents.size) {
                        actualFieldContents[repetitionIndex].toString().trim()
                    } else ""
                    passed = passed and compareComponent(
                        actualFieldValue,
                        expectedFieldValue,
                        recordNum,
                        "$fieldSpec($repetitionIndex)",
                        fieldName,
                        result
                    )
                }
                // For dynamic values we expect them to be have something
                else if ((actualFieldContents.getOrNull(repetitionIndex)?.isEmpty ?: false)) {
                    result.errors.add(
                        "No date/time of message for record $recordNum in field $fieldSpec"
                    )
                    passed = false
                }
            }
        }
        result.passed = result.passed and passed
        return passed
    }

    /**
     * Compare the components of a given [actualFieldValue] and [expectedFieldValue] for the given [recordNum],
     * [fieldSpec] and [fieldName]
     * @return true if the field data is equal, false otherwise
     */
    fun compareComponent(
        actualFieldValue: String,
        expectedFieldValue: String,
        recordNum: Int,
        fieldSpec: String,
        fieldName: String,
        result: CompareData.Result
    ): Boolean {
        var passed = true
        // Get the components.  HAPI can return a string with a type (e.g. HD[blah^blah]) or a string
        // with a single value
        val typeRegex = Regex(".*\\[ *(.*) *].*")
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
        val maxNumComponents = if (actualValueComponents.size > expectedValueComponents.size) {
            actualValueComponents.size
        } else expectedValueComponents.size
        for (componentIndex in 0 until maxNumComponents) {
            val expectedComponentValue = if (componentIndex < expectedValueComponents.size) {
                expectedValueComponents[componentIndex].trim()
            } else ""
            val actualComponentValue = if (componentIndex < actualValueComponents.size) {
                actualValueComponents[componentIndex].trim()
            } else ""

            // If we have more than one component then show the component number is the messages
            val componentSpec = if (maxNumComponents > 1) "$fieldSpec-${componentIndex + 1}" else fieldSpec

            if (expectedComponentValue.isNotBlank() && expectedComponentValue != actualComponentValue) {
                result.errors.add(
                    "Data value does not match in report $recordNum for " +
                        "$componentSpec|$fieldName. Expected: '$expectedComponentValue', " +
                        "Actual: '$actualComponentValue'"
                )
                passed = false
            } else if (expectedComponentValue.isBlank() && actualComponentValue.isNotBlank()) {
                result.warnings.add(
                    "Actual data has value in report $recordNum for " +
                        "$componentSpec|$fieldName but no expected value. Actual: '$actualComponentValue'"
                )
            }
        }
        result.passed = result.passed and passed
        return passed
    }
}

/**
 * Compares two CSV files that use the same schema.
 */
class CompareCsvData {

    /**
     * Compare the data in the [actual] report to the data in the [expected] report that use the same [schema].
     * Errors are generated when:
     *  1. The number of reports is different (number of rows)
     *  2. A column in the expected values does not exist in the actual values
     *  3. An expected value does not match the actual value
     *  4. Cannot find the actual row in the expected records
     *
     * Warnings are generated when:
     *  1. An actual value exists, but no expected value.
     *  2. There are more columns in the actual data than the expected data
     *
     * @return the result for the comparison, with result.passed true if the comparison was successful
     */
    fun compare(
        expected: InputStream,
        actual: InputStream,
        schema: Schema,
        fieldsToIgnore: List<String>? = null,
        result: CompareData.Result = CompareData.Result()
    ): CompareData.Result {
        val expectedRows = csvReader().readAll(expected)
        val actualRows = csvReader().readAll(actual)
        val schemaMsgIdIndex = schema.findElementColumn("message_id")
        val schemaPatLastNameIndex = schema.findElementColumn("patient_last_name")
        val schemaPatStateIndex = schema.findElementColumn("patient_state")

        // Sanity check.  The schema need either the message ID, or patient last name and state.
        if (schemaMsgIdIndex == null && (schemaPatLastNameIndex == null || schemaPatStateIndex == null)) {
            error("Schema ${schema.name} needs to have message ID or (patient last name and state) for the test.")
        }

        // Check that we have the same number of records
        if (expectedRows.size == actualRows.size) {
            val expectedMsgIdIndex = getCsvColumnIndex(schema.findElement("message_id"), expectedRows[0])
            val expectedPatLastNameIndex = getCsvColumnIndex(schema.findElement("patient_last_name"), expectedRows[0])
            val expectedPatStateIndex = getCsvColumnIndex(schema.findElement("patient_state"), expectedRows[0])

            // Loop through all the actual rows ignoring the header row
            for (i in actualRows.indices) {
                val rowIndex = i + 1
                val actualRow = actualRows[i]
                val actualMsgId = if (schemaMsgIdIndex != null) actualRow[schemaMsgIdIndex].trim() else null
                val actualLastName = if (schemaPatLastNameIndex != null) {
                    actualRow[schemaPatLastNameIndex].trim()
                } else null
                val actualPatState = if (schemaPatStateIndex != null) {
                    actualRow[schemaPatStateIndex].trim()
                } else null

                // Find the expected row that matches the actual record
                val matchingExpectedRow = expectedRows.filter {
                    val expectedMsgId = if (expectedMsgIdIndex >= 0) it[expectedMsgIdIndex].trim() else null
                    val expectedLastName = if (expectedPatLastNameIndex >= 0) {
                        it[expectedPatLastNameIndex].trim()
                    } else null
                    val expectedPatState = if (expectedPatStateIndex >= 0) {
                        it[expectedPatStateIndex].trim()
                    } else null

                    schemaMsgIdIndex != null && expectedMsgId == actualMsgId ||
                        (
                            schemaPatLastNameIndex != null && schemaPatStateIndex != null &&
                                expectedLastName == actualLastName &&
                                expectedPatState == actualPatState
                            )
                }
                if (matchingExpectedRow.size == 1) {
                    if (
                        !compareCsvRow(
                            actualRow,
                            matchingExpectedRow[0],
                            expectedRows[0],
                            schema,
                            rowIndex,
                            fieldsToIgnore,
                            result
                        )
                    ) {
                        result.errors.add("Comparison for row #$rowIndex FAILED")
                    }
                } else {
                    result.errors.add(
                        "Could not find row in expected data for message $rowIndex. " +
                            "Was looking for ID='$actualMsgId', or patient last name='$actualLastName' " +
                            "and state='$actualPatState'"
                    )
                    result.passed = false
                }
            }
        } else {
            result.errors.add(
                "Number of records does not match.  Expected ${expectedRows.size - 1} " +
                    "but got ${actualRows.size - 1}"
            )
            result.passed = false
        }

        return result
    }

    /**
     * Compare the data in the [actualRow] report to the data in the [expectedRow] report that use the same [schema].
     * Errors are generated when:
     *  1. The number of columns in the actual data is less than in the expected data
     *  2. A column in the expected values does not exist in the actual values
     *  3. An expected value does not match the actual value
     *
     * Warnings are generated when:
     *  1. The number of columns in the actual data is more than in the expected data
     *  2. An actual data value exists, but there is no matching expected data
     *
     * @return true if the field data is equal, false otherwise
     */
    fun compareCsvRow(
        actualRow: List<String>,
        expectedRow: List<String>,
        expectedHeaders: List<String>,
        schema: Schema,
        actualRowNum: Int,
        fieldsToIgnore: List<String>?,
        result: CompareData.Result
    ): Boolean {
        var passed = true
        if (actualRow.isEmpty()) {
            result.errors.add(
                "The actual report had no rows"
            )
            passed = false
        } else if (actualRow.size < expectedRow.size) {
            result.errors.add(
                "There are too few columns in the actual report.  Expected ${expectedRow.size} or more, but got" +
                    "${actualRow.size}"
            )
            passed = false
        } else {
            if (actualRow.size > expectedRow.size) {
                result.warnings.add(
                    "Actual report has more columns than expected data.  Actual has ${actualRow.size} and expected " +
                        "${expectedRow.size}"
                )
            }

            // Loop through all the actual columns
            for (j in actualRow.indices) {
                val actualValue = actualRow[j].trim()
                val colName = schema.elements[j].name

                // check to see if we should skip a specific field. some fields may contain some dynamic data
                // that we don't want to check, like a date field for example, so we can pass that through
                // as an option to skip
                if (fieldsToIgnore?.contains(colName) == true) {
                    continue
                }

                val expectedColIndex = getCsvColumnIndex(schema.elements[j], expectedHeaders)
                val expectedValue = if (expectedColIndex >= 0) {
                    expectedRow[expectedColIndex].trim()
                } else ""

                // If there is an expected value then compare it.
                if (expectedValue.isNotBlank()) {
                    // For date/time values, the string has timezone offsets that can differ per environment, so
                    // compare the numeric value instead of just the string
                    if (schema.elements[j].type != null &&
                        schema.elements[j].type == Element.Type.DATETIME &&
                        actualValue.isNotBlank()
                    ) {
                        try {
                            val expectedTime =
                                DateUtilities.parseDate(expectedValue).toOffsetDateTime().toEpochSecond()
                            val actualTime =
                                DateUtilities.parseDate(actualValue).toOffsetDateTime().toEpochSecond()
                            if (expectedTime != actualTime) {
                                result.errors.add(
                                    "Date time value does not match in report $actualRowNum " +
                                        "column #${j + 1}, '$colName'. Expected: '$expectedValue', " +
                                        "Actual: '$actualValue, EpochSec: $expectedTime/$actualTime'"
                                )
                                passed = false
                            }
                        } catch (e: Throwable) {
                            // This is not a true date/time since it was not parse, probably a date.  Compare as strings.
                            if (actualValue != expectedValue) {
                                result.errors.add(
                                    "Data value does not match in report $actualRowNum column #${j + 1}, " +
                                        "'$colName'. Expected: '$expectedValue', " +
                                        "Actual: '$actualValue'"
                                )
                                passed = false
                            }
                        }
                    } else if (actualValue != expectedValue) {
                        result.errors.add(
                            "Data value does not match in report $actualRowNum column #${j + 1}, " +
                                "'$colName'. Expected: '$expectedValue', " +
                                "Actual: '$actualValue'"
                        )
                        passed = false
                    }
                } else if (actualRow[j].isNotBlank()) {
                    result.warnings.add(
                        "Actual data has value in report $actualRowNum for column " +
                            "'$colName' - column #${j + 1}, but no expected value.  " +
                            "Actual: '${actualRow[j].trim()}'"
                    )
                }
            }
        }
        result.passed = result.passed and passed
        return passed
    }

    /**
     * Get the index of an [element]'s data column in the expected data [expectedHeaders].
     * @return the index of the column or -1 if it is not found
     */
    private fun getCsvColumnIndex(element: Element?, expectedHeaders: List<String>?): Int {
        if (element == null || expectedHeaders.isNullOrEmpty()) return -1

        // Find the proper column in the expected data, so we do not rely on column ordering
        // Searching both by element name and CSV name allows for having internal.csv files.
        val possibleCsvHeaders = element.csvFields?.map { it.name }
        val expectedColIndexByElementIndex = expectedHeaders.indexOf(element.name)
        val expectedColIndexByCsvIndex = possibleCsvHeaders?.let {
            var index = -1
            possibleCsvHeaders.forEach csvLoop@{
                if (expectedHeaders.indexOf(it) >= 0) {
                    index = expectedHeaders.indexOf(it)
                    return@csvLoop
                }
            }
            index
        }
        return if (expectedColIndexByCsvIndex != null && expectedColIndexByCsvIndex >= 0) {
            expectedColIndexByCsvIndex
        } else expectedColIndexByElementIndex
    }
}

/**
 * Compare the raw contents of a file.
 * @property result the result of the comparison
 */
class CompareFile(
    val result: CompareData.Result = CompareData.Result()
) {
    /**
     * Compare the contents of a file [actual] vs [expected] and provide the [result].
     */
    fun compare(
        expected: InputStream,
        actual: InputStream,
        result: CompareData.Result = CompareData.Result()
    ): CompareData.Result {
        // Read the data
        val expectedData = expected.bufferedReader().readLines()
        val actualData = actual.bufferedReader().readLines()

        // Generate the diff
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .inlineDiffByWord(true)
            .ignoreWhiteSpaces(true)
            .oldTag { start: Boolean? ->
                if (true == start) "\u001B[9;101m" else "\u001B[0m" // Use strikethrough and red for deleted changes
            }
            .newTag { start: Boolean? ->
                if (true == start) "\u001B[1;42m" else "\u001B[0m" // Use bold and green for additions
            }
            .build()
        val diff = generator.generateDiffRows(expectedData, actualData)
        val hasChanges = diff.any { it.tag != DiffRow.Tag.EQUAL }

        // Populate the result.
        result.passed = !hasChanges
        diff.filter { it.tag != DiffRow.Tag.EQUAL }.forEach { result.errors.add(it.oldLine) }
        return result
    }
}