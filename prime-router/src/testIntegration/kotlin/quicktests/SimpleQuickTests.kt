package gov.cdc.prime.router.tests.quicktests

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.Report
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.TestInstance
import quicktests.QuickTestUtils
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Run a which WATERS test that translates fake data then sends the resulting data
 * back through the translator.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleQuickTests {
    @Test
    fun `test waters data translation`() {
        val watersFilenameRegex = "waters.*\\.csv"
        var watersFile = QuickTestUtils.generateFakeData(
            "waters/waters-covid-19", 50, Report.Format.CSV,
            "CA", "Santa Clara"
        )
        QuickTestUtils.checkFilename(watersFile, watersFilenameRegex)

        // Now run the generated file back through
        watersFile = QuickTestUtils.translateReport("waters/waters-covid-19", watersFile)
        QuickTestUtils.checkFilename(watersFile, watersFilenameRegex)
    }

    @Test
    fun `test fake data generator`() {
        val reddyFMCFilenameRegex = "reddyfmc-la-covid-19.*\\.csv"
        val reddyFMCNumFakeRecords = 1

        /**
         * Generate ReddyFMC-LA data using the provided [county].
         * @return the pathname of the generated file
         */
        fun generateStracData(county: String): String {
            val reddyFMCFile = QuickTestUtils.generateFakeData(
                "iPatientCare/ReddyFMC-LA-covid-19", reddyFMCNumFakeRecords,
                Report.Format.CSV, "AL", county
            )
            QuickTestUtils.checkFilename(reddyFMCFile, reddyFMCFilenameRegex)
            return reddyFMCFile
        }

        val fileList = mutableListOf<String>()
        val countyList = listOf("Jefferson")

        // Generate the fake data
        countyList.forEach {
            fileList.add(generateStracData(it))
        }
    }

    @Test
    fun `test merge using fake data`() {
        val stracsFilenameRegex = "strac-covid-19.*\\.csv"
        val stracsNumFakeRecords = 5

        /**
         * Generate STRAC data using the provided [county].
         * @return the pathname of the generated file
         */
        fun generateStracData(county: String): String {
            val stracsFile = QuickTestUtils.generateFakeData(
                "strac/strac-covid-19", stracsNumFakeRecords,
                Report.Format.CSV, county
            )
            QuickTestUtils.checkFilename(stracsFile, stracsFilenameRegex)
            return stracsFile
        }

        /**
         * Check for the [numExpected] records for the given [searchString] in the [pathname] file.
         */
        fun checkNumRecords(pathname: String, searchString: String, numExpected: Int) {
            var count = 0
            File(pathname).forEachLine {
                if (it.contains(searchString)) count++
            }
            assertThat(count).isEqualTo(numExpected)
        }

        val fileList = mutableListOf<String>()
        val countyList = listOf("lilliput", "brobdingnag", "houyhnhnm")

        // Generate all the fake data
        countyList.forEach {
            fileList.add(generateStracData(it))
        }

        // Now merge it
        val mergedFile = QuickTestUtils.mergeData("strac/strac-covid-19", fileList)
        QuickTestUtils.checkFilename(mergedFile, stracsFilenameRegex)

        // Check that all the county records are in the merged file
        for (i in countyList.indices) {
            checkNumRecords(fileList[i], countyList[i], stracsNumFakeRecords)
        }

        // Make sure we have the correct number of rows in the merged file including the header row
        checkNumRecords(mergedFile, ",", stracsNumFakeRecords * countyList.size + 1)
    }

    @org.junit.jupiter.api.Test
    fun `test report data with comparison`() {
        val azFileRegex = Regex("[/\\\\]az.*\\.csv")
        val pdFileRegex = Regex("[/\\\\]pdi-covid-19")
        val expectedDir = "./src/test/csv_test_files/expected"
        val expectedAzFile = "$expectedDir/simplereport-az.csv"
        val simpleReportFile = "./src/test/csv_test_files/input/simplereport.csv"

        // First route the test file and compare the output.
        var reports = QuickTestUtils.routeReport("primedatainput/pdi-covid-19", simpleReportFile)

        var actualAzFile: String? = null
        reports.forEach {
            if (it.contains(azFileRegex)) actualAzFile = it
        }
        assertThat(actualAzFile).isNotNull().isNotEmpty()

        assertTrue(FileUtils.contentEquals(File(actualAzFile!!), File(expectedAzFile)), "SimpleReport->AZ")

        // Now read the data back in to their own schema and export again.
        val actualAzFile2 = QuickTestUtils.translateReport("az/az-covid-19", actualAzFile!!)
        assertTrue(FileUtils.contentEquals(File(actualAzFile2), File(actualAzFile!!)), "AZ->AZ")

        // And now generate some fake simplereport data
        val generatedFakeDataFile = QuickTestUtils.generateFakeData(
            "primedatainput/pdi-covid-19", 50, Report.Format.CSV,
            "IG", "CSV"
        )
        assertThat(pdFileRegex.containsMatchIn(generatedFakeDataFile)).isTrue()

        // Now send that fake data thru the router.
        reports = QuickTestUtils.routeReport("primedatainput/pdi-covid-19", generatedFakeDataFile)
        var actualAzFile3: String? = null
        reports.forEach {
            if (it.contains(azFileRegex)) actualAzFile3 = it
        }
        assertThat(actualAzFile3).isNotNull().isNotEmpty()

        // Now send _those_ results back in to their own schema and export again!
        val actualAzFile4 = QuickTestUtils.translateReport("az/az-covid-19", actualAzFile3!!)
        assertTrue(FileUtils.contentEquals(File(actualAzFile4), File(actualAzFile3!!)), "AZ->AZ")
    }
}