package gov.cdc.prime.router

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
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
        val schema = Schema.schemas[defaultSchema] ?: error("$defaultSchema not found.")

        // 1) Ingest the file
        val inputMappableTable = MappableTable(file.name, schema, file.inputStream(), MappableTable.StreamType.CSV)
        // 2) Create transformed objeccts, according to the receiver table rules
        val outputMappableTables = inputMappableTable.routeByReceiver(Receiver.receivers)
        assertEquals(2, outputMappableTables.size)
        // 3) Write transformed objs to files, and check they are correct
        outputMappableTables.forEach { table ->
            val outputFile = File(outputPath, table.name)
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }
            outputFile.outputStream().use {
                table.write(it, MappableTable.StreamType.CSV)
            }
            compareTestResultsToExpectedResults(outputFile.absolutePath, table.name)
        }
    }


    private fun compareTestResultsToExpectedResults(testFile: String, expectedResultsName: String) {
        val expectedResultsFile = expectedResultsPath + expectedResultsName
        println("CsvFileTests: diff'ing actual vs expected: $testFile to $expectedResultsFile")
        // A bit of a hack:  diff the two files.  
        assertEquals(Files.mismatch(Path.of(testFile), Path.of(expectedResultsFile)), -1L)
    }

    private fun loadTestReceivers() {
        val loadingStream = File(inputPath + "test-receivers.yml").inputStream()
        Receiver.loadReceiversList(loadingStream)
        assertEquals(2, Receiver.receivers.size)
        assertEquals(2, Receiver.get("federal-test-receiver", "covid-19")?.patterns?.size)
    }

    private fun loadTestSchemas() {
        Schema.loadSchemaCatalog(inputPath)
        assertNotNull(Schema.schemas[defaultSchema])
        assertEquals(7, Schema.schemas.getValue(defaultSchema).elements.size)
        assertEquals("lab", Schema.schemas.getValue(defaultSchema).elements[0].name)
    }


}

