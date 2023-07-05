package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.ReadResult
import gov.cdc.prime.router.unittest.UnitTestUtils
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

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
        metadata = UnitTestUtils.simpleMetadata
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
        assertThat(result.actionLogs.isEmpty()).isTrue()
        val inputReport = result.report
        translateReport(inputReport, baseName, listOf("federal-test-receiver-"))
    }

    @Test
    fun `test a csv file with column headers but no data`() {
        val file = File(inputPath + "column-headers-only.csv")
        val baseName = file.name
        val result = ingestFile(file)
        assertThat(result.actionLogs.isEmpty()).isFalse()
        assertThat(result.actionLogs.hasErrors()).isFalse()
        val inputReport = result.report
        translateReport(inputReport, baseName, emptyList<String>())
    }

    private fun ingestFile(file: File): ReadResult {
        assertThat(file).exists()
        val schema = metadata.findSchema(defaultSchema) ?: error("$defaultSchema not found.")
        return csvSerializer.readExternal(schema.name, file.inputStream(), TestSource)
    }

    private fun translateReport(inputReport: Report, baseName: String, expected: List<String>) {
        val (outputReports, _) = Translator(metadata, settings).filterAndTranslateByReceiver(inputReport)
        assertThat(outputReports).hasSize(expected.size)

        // Write transformed objs to files, and check they are correct
        outputReports
            .map { (report, _) -> report }
            .zip(expected)
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
        ).isNotNull().hasSize(1)
    }

    private fun loadTestSchemas(metadata: Metadata) {
        metadata.loadSchemaCatalog(inputPath)
        val schema = metadata.findSchema(defaultSchema)
        assertThat(schema).isNotNull()
        assertThat(schema!!.elements).hasSize(7)
        assertThat(schema.elements[0].name).isEqualTo("lab")
    }
}