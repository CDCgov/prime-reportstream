package gov.cdc.prime.router

import java.io.File
import kotlin.test.*

//
// Using JUnit here, but this is not a unit test.  This tests end-to-end:  ingesting a csv file,
// creating transformed objects, writing them to output csv files, then doing a simple 'diff'
// to see if they match expected output files.
//
class CsvFileTests {
    private val defaultSchema = "test-schema"
    private val inputPath = "./src/test/csv_test_files/input/"
    private val expectedResultsPath = "./src/test/csv_test_files/expected/"
    private val outputPath = "./target/csv_test_files/"

    // There is no 'BeforeAll in kotlin.test (?).   Using BeforeEach, which should be OK as this is idempotent.
    @BeforeTest
    fun setup() {
        val outputDirectory = File(outputPath)
        outputDirectory.mkdirs()

        val expectedDir = File(expectedResultsPath)
        assertTrue(expectedDir.exists())

        loadTestSchemas()
        loadTestReceivers()
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
        assertTrue(file.exists())
        val schema = Metadata.findSchema(defaultSchema) ?: error("$defaultSchema not found.")

        // 1) Ingest the file
        val inputMappableTable = CsvConverter.read(file.name, schema, file.inputStream())
        // 2) Create transformed objects, according to the receiver table rules
        val outputMappableTables = Receiver.mapByReceivers(inputMappableTable, Metadata.receivers)
        assertEquals(2, outputMappableTables.size)
        // 3) Write transformed objs to files, and check they are correct
        outputMappableTables.forEach { table ->
            val outputFile = File(outputPath, table.name)
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }
            outputFile.outputStream().use {
                CsvConverter.write(table, it)
            }
            compareTestResultsToExpectedResults(outputFile.absolutePath, table.name)
        }
    }


    private fun compareTestResultsToExpectedResults(testFile: String, expectedResultsName: String) {
        val expectedResultsFile = expectedResultsPath + expectedResultsName
        println("CsvFileTests: diff'ing actual vs expected: $testFile to $expectedResultsFile")
        // A bit of a hack:  diff the two files.  
        val testFileLines = File(testFile).readLines()
        val expectedResultsLines = File(expectedResultsFile).readLines()
        assertEquals(expectedResultsLines, testFileLines)
    }

    private fun loadTestReceivers() {
        val loadingStream = File(inputPath + "test-receivers.yml").inputStream()
        Metadata.loadReceiversList(loadingStream)
        assertEquals(2, Metadata.receivers.size)
        assertEquals(2, Metadata.findReceiver("federal-test-receiver", "covid-19")?.patterns?.size)
    }

    private fun loadTestSchemas() {
        Metadata.loadSchemaCatalog(inputPath)
        val schema = Metadata.findSchema(defaultSchema)
        assertNotNull(schema);
        assertEquals(7, schema.elements.size)
        assertEquals("lab", schema.elements[0].name)
    }


}

