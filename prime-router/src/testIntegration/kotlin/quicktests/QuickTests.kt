package gov.cdc.prime.router.tests.quicktests

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import gov.cdc.prime.router.cli.ProcessData
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.Test

/**
 * Run a which WATERS test that translates fake data then sends the resulting data
 * back through the translator.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WatersTest {
    @Test
    fun `test waters data translation`() {
        val watersFilenameRegex = Regex("[/\\\\]waters.*\\.csv")
        var dataGenerator = ProcessData()
        var args = mutableListOf(
            "--input-fake", "50", "--input-schema", "waters/waters-covid-19",
            "--output-dir", "build/csv_test_files", "--target-states", "CA",
            "--target-counties", "Santa Clara",
            "--output-format", "CSV"
        )
        dataGenerator.main(args)
        assertThat(dataGenerator.outputReportFiles.size).isEqualTo(1)
        assertThat(
            watersFilenameRegex
                .containsMatchIn(dataGenerator.outputReportFiles[0])
        ).isTrue()

        // Now run the generated file back through
        args = mutableListOf(
            "--input-schema", "waters/waters-covid-19",
            "--input", dataGenerator.outputReportFiles[0],
            "--output-dir", "build/csv_test_files"
        )
        dataGenerator = ProcessData()
        dataGenerator.main(args)
        assertThat(dataGenerator.outputReportFiles.size).isEqualTo(1)
        assertThat(
            watersFilenameRegex
                .containsMatchIn(dataGenerator.outputReportFiles[0])
        ).isTrue()
    }
}

/**
 * Perform a routing test using canned data.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutingTest {
    @Test
    fun `test routing using canned file`() {
        val dataGenerator = ProcessData()
        val args = mutableListOf(
            "--input-schema", "primedatainput/pdi-covid-19",
            "--input", "./src/test/csv_test_files/input/simplereport.csv",
            "--output-dir", "build/csv_test_files", "--route"
        )
        dataGenerator.main(args)
        assertThat(dataGenerator.outputReportFiles.size).isGreaterThan(0)
    }
}

/**
 * Perform a data merge test.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MergeTest {
    /**
     * The regex for checking the output filename
     */
    private val stracsFilenameRegex = Regex("[/\\\\]strac-covid-19.*\\.csv")

    /**
     * Number of fake records to generate.
     */
    private val numFakeRecords = 5

    /**
     * Generate STRAC data using the provided [county].
     * @return the pathname of the generated file
     */
    private fun generateStracData(county: String): String {
        val dataGenerator = ProcessData()
        val args = mutableListOf(
            "--input-fake", numFakeRecords.toString(), "--input-schema", "strac/strac-covid-19",
            "--target-counties", county,
            "--output-dir", "build/csv_test_files"
        )
        dataGenerator.main(args)
        assertThat(dataGenerator.outputReportFiles.size).isEqualTo(1)
        assertThat(
            stracsFilenameRegex
                .containsMatchIn(dataGenerator.outputReportFiles[0])
        ).isTrue()
        return dataGenerator.outputReportFiles[0]
    }

    /**
     * Check for the [numExpected] records for the given [searchString] in the [pathname] file.
     */
    private fun checkNumRecords(pathname: String, searchString: String, numExpected: Int) {
        var count = 0
        File(pathname).forEachLine {
            if (it.contains(searchString)) count++
        }
        assertThat(count).isEqualTo(numExpected)
    }

    @Test
    fun `test merge using fake data`() {
        val fileList = mutableListOf<String>()
        val countyList = listOf("lilliput", "brobdingnag", "houyhnhnm")

        // Generate all the fake data 
        countyList.forEach {
            fileList.add(generateStracData(it))
        }

        // Now merge it
        val dataGenerator = ProcessData()
        val args = mutableListOf(
            "--merge", "${fileList[0]},${fileList[1]},${fileList[2]}", "--input-schema", "strac/strac-covid-19",
            "--output-dir", "build/csv_test_files"
        )
        dataGenerator.main(args)
        assertThat(dataGenerator.outputReportFiles.size).isEqualTo(1)
        assertThat(
            stracsFilenameRegex
                .containsMatchIn(dataGenerator.outputReportFiles[0])
        ).isTrue()

        // Check that all the county records are in the merged file
        for (i in countyList.indices) {
            checkNumRecords(fileList[i], countyList[i], numFakeRecords)
        }

        // Make sure we have the correct number of rows in the merged file including the header row
        checkNumRecords(dataGenerator.outputReportFiles[0], ",", numFakeRecords * countyList.size + 1)
    }
}