package gov.cdc.prime.router

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.show
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.ReadResult
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

//
// Using JUnit here, but this is not a unit test.  This tests end-to-end:  ingesting a csv file,
// creating transformed objects, writing them to output csv files, then doing a simple 'diff'
// to see if they match expected output files.
//
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsvFileTests {
    private val defaultSchema = "test-schema"
    private val inputPath = "./src/test/csv_test_files/input/"
    private val expectedResultsPath = "./src/test/csv_test_files/expected/"
    private val outputPath = "./build/csv_test_files/"
    private val metadata: Metadata
    private val settings: FileSettings
    private val csvSerializer: CsvSerializer

    init {
        val outputDirectory = File(outputPath)
        outputDirectory.mkdirs()
        val expectedDir = File(expectedResultsPath)
        assertThat(expectedDir).exists()
        metadata = Metadata()
        loadTestSchemas(metadata)
        settings = FileSettings()
        loadTestOrganizations(settings)
        csvSerializer = CsvSerializer(metadata)
    }

    @Test
    fun `test the happy path`() {
        val file = File(inputPath + "happy-path.csv")
        val baseName = file.name
        val result = ingestFile(file)
        assertTrue(result.warnings.isEmpty() && result.errors.isEmpty())
        assertThat(result).all {
            prop("warnings") { ReadResult::warnings.call(it) }.isEmpty()
            prop("errors") { ReadResult::errors.call(it) }.isEmpty()
        }
        assertThat(result).hasNoWarnings().hasNoErrors()
        val inputReport = result.report ?: fail()
        translateReport(inputReport, baseName)
    }

    @Test
    fun `test a csv file with column headers but no data`() {
        val file = File(inputPath + "column-headers-only.csv")
        val baseName = file.name
        val result = ingestFile(file)
        assertTrue(result.warnings.isNotEmpty() && result.errors.isEmpty())
        assertThat(result).all {
            prop("warnings") { ReadResult::warnings.call(it) }.isNotEmpty()
            prop("errors") { ReadResult::errors.call(it) }.isEmpty()
        }
        assertThat(result).hasNoErrors()
        val inputReport = result.report ?: fail()
        translateReport(inputReport, baseName)
    }

    private fun ingestFile(file: File): ReadResult {
        assertThat(file).exists()
        val schema = metadata.findSchema(defaultSchema) ?: error("$defaultSchema not found.")
        return csvSerializer.readExternal(schema.name, file.inputStream(), TestSource)
    }

    private fun translateReport(inputReport: Report, baseName: String) {
        val outputReports = Translator(metadata, settings).translateByReceiver(inputReport)
        assertThat(outputReports).hasSize(2)

        // Write transformed objs to files, and check they are correct
        outputReports
            .zip(listOf("AZ-test-receiver-", "federal-test-receiver-"))
            .forEach { (report, prefix) ->
                val outputFile = File(outputPath, report.name)
                if (!outputFile.exists()) {
                    outputFile.createNewFile()
                }
                outputFile.outputStream().use {
                    csvSerializer.write(report, it)
                }
                compareTestResultsToExpectedResults(outputFile.absolutePath, "$prefix$baseName")
            }
    }

    private fun compareTestResultsToExpectedResults(testFile: String, expectedResultsName: String) {
        val expectedResultsFile = "$expectedResultsPath$expectedResultsName"
        // A bit of a hack:  diff the two files.
        val testFileLines = File(testFile).readLines()
        val expectedResultsLines = File(expectedResultsFile).readLines()
        assertEquals(
            expectedResultsLines, testFileLines,
            "Actual data file: $testFile Expected data file: $expectedResultsFile"
        )
    }

    private fun loadTestOrganizations(settings: FileSettings) {
        val loadingStream = File(inputPath + "test-organizations.yml").inputStream()
        settings.loadOrganizations(loadingStream)
        assertThat(settings.receivers).hasSize(2)
        assertThat(
            settings.findReceiver("federal-test.receiver")?.jurisdictionalFilter
        ).isNotNull().hasSize(2)
    }

    private fun loadTestSchemas(metadata: Metadata) {
        metadata.loadSchemaCatalog(inputPath)
        val schema = metadata.findSchema(defaultSchema)
        assertThat(schema).isNotNull()
        assertThat(schema!!.elements).hasSize(7)
        assertThat(schema.elements[0].name).isEqualTo("lab")
    }

    companion object {
        private fun Assert<ReadResult>.hasNoWarnings(): Assert<ReadResult> = transform { actual ->
            if (actual.warnings.count() == 0) {
                actual
            } else {
                expected("expected: ReadResult to have no warnings, but it had ${show(actual.warnings.count())}")
            }
        }

        private fun Assert<ReadResult>.hasNoErrors(): Assert<ReadResult> = transform { actual ->
            if (actual.errors.count() == 0) {
                actual
            } else {
                expected("expected: ReadResult to have no errors, but it had ${show(actual.errors.count())}")
            }
        }
    }
}