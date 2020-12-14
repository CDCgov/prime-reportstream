package gov.cdc.prime.router

import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    private val outputPath = "./target/csv_test_files/"
    private val metadata: Metadata
    private val csvConverter: CsvConverter

    init {
        val outputDirectory = File(outputPath)
        outputDirectory.mkdirs()

        val expectedDir = File(expectedResultsPath)
        assertTrue(expectedDir.exists())

        metadata = Metadata()
        loadTestSchemas(metadata)
        loadTestOrganizations(metadata)
        csvConverter = CsvConverter(metadata)
    }

    @Test
    fun `test the happy path`() {
        transformFileAndTest(inputPath + "happy-path.csv")
    }

    @Test
    fun `test a csv file with column headers but no data`() {
        transformFileAndTest(inputPath + "column-headers-only.csv")
    }

    private fun transformFileAndTest(fileName: String) {
        val file = File(fileName)
        val baseName = file.name
        assertTrue(file.exists())
        val schema = metadata.findSchema(defaultSchema) ?: error("$defaultSchema not found.")

        // 1) Ingest the file
        val inputReport = csvConverter.read(schema, file.inputStream(), TestSource)
        // 2) Create transformed objects, according to the receiver table rules
        val outputReports = Translator(metadata).translateByService(inputReport)
        assertEquals(2, outputReports.size)
        // 3) Write transformed objs to files, and check they are correct

        outputReports
            .zip(listOf("AZ-test-receiver-", "federal-test-receiver-"))
            .forEach { (report, prefix) ->
                val outputFile = File(outputPath, report.name)
                if (!outputFile.exists()) {
                    outputFile.createNewFile()
                }
                outputFile.outputStream().use {
                    csvConverter.write(report, it)
                }

                compareTestResultsToExpectedResults(outputFile.absolutePath, "$prefix$baseName")
            }
    }

    private fun compareTestResultsToExpectedResults(testFile: String, expectedResultsName: String) {
        val expectedResultsFile = "$expectedResultsPath$expectedResultsName"
        println("CsvFileTests: diff'ing actual vs expected: $testFile to $expectedResultsFile")
        // A bit of a hack:  diff the two files.  
        val testFileLines = File(testFile).readLines()
        val expectedResultsLines = File(expectedResultsFile).readLines()
        assertEquals(expectedResultsLines, testFileLines)
    }

    private fun loadTestOrganizations(metadata: Metadata) {
        val loadingStream = File(inputPath + "test-organizations.yml").inputStream()
        metadata.loadOrganizations(loadingStream)
        assertEquals(2, metadata.organizationServices.size)
        assertEquals(2, metadata.findService("federal-test.receiver")?.jurisdictionalFilter?.size)
    }

    private fun loadTestSchemas(metadata: Metadata) {
        metadata.loadSchemaCatalog(inputPath)
        val schema = metadata.findSchema(defaultSchema)
        assertNotNull(schema)
        assertEquals(7, schema.elements.size)
        assertEquals("lab", schema.elements[0].name)
    }
}