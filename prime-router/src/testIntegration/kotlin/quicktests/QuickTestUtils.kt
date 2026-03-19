package quicktests

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.github.ajalt.clikt.core.main
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.cli.ProcessData
import org.apache.commons.io.FilenameUtils
import kotlin.test.assertTrue

/**
 * Utilities to run the quick tests.
 */
object QuickTestUtils {
    /**
     * Metadata instance.  This is used to speed the test as ProcessData recreates this every time by default.
     */
    val metadata = Metadata.getInstance()

    /**
     * File settings instance. This is used to speed the test as ProcessData recreates this every time by default.
     */
    val fileSettings = FileSettings(FileSettings.defaultSettingsDirectory)

    /**
     * The output directory for the test files.
     */
    val outputDir = "build/csv_test_files"

    /**
     * Translate an [inputFile] report to the given [schema].
     * @return the pathname to the generated file
     */
    fun translateReport(schema: String, inputFile: String): String {
        val dataGenerator = ProcessData(metadata, fileSettings)
        val args = mutableListOf(
            "--input-schema", schema,
            "--input", inputFile,
            "--output-dir", outputDir
        )
        dataGenerator.main(args)
        assertThat(dataGenerator.outputReportFiles.size).isEqualTo(1)
        return dataGenerator.outputReportFiles[0]
    }

    /**
     * Route a given [inputFile] using the given [schema].
     * @return the list of generated files
     */
    fun routeReport(schema: String, inputFile: String): List<String> {
        val dataGenerator = ProcessData(metadata, fileSettings)
        val args = mutableListOf(
            "--input-schema", schema,
            "--input", inputFile,
            "--output-dir", outputDir, "--route"
        )
        dataGenerator.main(args)
        assertThat(dataGenerator.outputReportFiles.size).isGreaterThan(0)
        return dataGenerator.outputReportFiles
    }

    /**
     * Generates [numReports] of fake data using the [schema].  Optionally specify a [targetCounty] and/or
     * [targetState].
     * @return the pathname to the generated file
     */
    fun generateFakeData(
        schema: String,
        numReports: Int,
        outputFormat: MimeFormat = MimeFormat.CSV,
        targetState: String = "",
        targetCounty: String = "",
    ): String {
        val dataGenerator = ProcessData(metadata, fileSettings)
        val args = mutableListOf(
            "--input-fake", numReports.toString(), "--input-schema", schema,
            "--output-dir", outputDir, "--output-format", outputFormat.toString()
        )
        if (targetState.isNotBlank()) args.addAll(listOf("--target-states", targetState))
        if (targetCounty.isNotBlank()) args.addAll(listOf("--target-counties", targetCounty))
        dataGenerator.main(args)
        assertThat(dataGenerator.outputReportFiles.size).isEqualTo(1)
        return dataGenerator.outputReportFiles[0]
    }

    /**
     * Merge two or more file in the [fileList] using the given [schema].
     * @return the pathname to the output file
     */
    fun mergeData(schema: String, fileList: List<String>): String {
        val dataGenerator = ProcessData(metadata, fileSettings)
        val args = mutableListOf(
            "--merge", fileList.joinToString(","), "--input-schema", schema,
            "--output-dir", outputDir
        )
        dataGenerator.main(args)
        assertThat(dataGenerator.outputReportFiles.size).isEqualTo(1)
        return dataGenerator.outputReportFiles[0]
    }

    /**
     * Check a [pathname] for the existing of a filename with the matching
     * [regex].
     */
    fun checkFilename(pathname: String, regex: String) {
        val filename = FilenameUtils.getName(pathname)
        assertTrue(
            Regex(regex).containsMatchIn(filename),
            "Filename $filename does not match regex $regex"
        )
    }
}