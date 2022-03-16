package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.metadata.LivdTableColumns
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.eachHref
import it.skrape.selects.html5.a
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import tech.tablesaw.api.ColumnType
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.io.csv.CsvReadOptions
import tech.tablesaw.selection.Selection
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL

/**
 * LivdTableDownload is the command line interface for the livd-table-download command. It parses the command line
 * for option given as below.
 *
 * It looks for the LIVD-SAR-CoV-2-yyyy-MM-dd.xlsx file from $cdcLOINCTestCodeMappingPageUrl.  If the file is found,
 * it downloads the file into the ./build directory.  If not found, it will prompt error accordingly.  Next, it builds
 * the output Lookup Table (<./build/LIVD-SARS-CoV-2.csv> file) with the table name.  Finally, it updates the
 * LIVD-SARS-CoV-2 lookup tables in the database as the new version of the table.
 * It updates new version of the lookup table in the given --env [local, test, staging, or prod] with the default
 * to "local" environment.
 *
 * Note, this command will always create new version of the lookup table.
 *
 * Example:
 * The command below will download the latest LIVD mapping catalogue and create a new lookup tables as needed.
 * It took "LIVD mapping catalogue" from the CDC website.
 *
 *  ./prime livd-table-download
 *
 */
class LivdTableDownload : CliktCommand(
    name = "livd-table-download",
    help = """
    It downloads the latest LOINC test data, extract Lookup Table, and the database as a new version. 
    """
) {
    /**
     * The environment to connect to.
     */
    private val env by option(
        "-e", "--env",
        metavar = "<name>",
        envvar = "PRIME_ENVIRONMENT",
        help = "Connect to <name> environment. Choose from [local|test|staging|prod]"
    )
        .choice(
            Environment.LOCAL.envName, Environment.STAGING.envName,
            Environment.TEST.envName, Environment.PROD.envName
        )
        .default(Environment.LOCAL.envName, "local environment")

    /**
     * Silent running.  No table contents or diff output or confirmation if true.
     */
    private val silent by option("-s", "--silent", help = "Do not generate diff or ask for confirmation").flag()

    /**
     * Activate a created table in one shot.
     */
    private val activate by option("-a", "--activate", help = "Activate the table upon creation").flag()

    /**
     * Pathname to the LIVD supplemental table.
     */
    private val livdSupplementalPathname by option(
        "--livd-suppl",
        help = "The path to the LIVD supplemental file. Defaults to $defaultSupplFile"
    ).file(true).default(File(defaultSupplFile))

    override fun run() {
        TermUi.echo("Downloading the LIVD table ...")
        FileUtils.forceMkdir(File(defaultOutputDir))

        // Download the file from CDC website.
        val downloadedFile = downloadFile(defaultOutputDir)
        // Extract the data from the Excel and output to the specified output CSV format file.
        val tempRawLivdOutFile = extractLivdTable(sheetName, downloadedFile)
        // Merge the supplemental LIVD table with the raw.
        val tempMergedLivdOutFile = mergeLivdSupplementalTable(tempRawLivdOutFile)
        tempRawLivdOutFile.delete()

        // Now, store the data as a LIVD lookup table.
        if (!updateTheLivdLookupTable(tempMergedLivdOutFile))
            error("There was an error storing the LIVD lookup table.")
        else
            TermUi.echo("The lookup table was updated successfully.")
    }

    /**
     *  The downloadFile downloads the latest LOINC test data, so it can be ingested automatically.
     *  It looks for the LIVD-SAR-CoV-2-yyyy-MM-dd.xlsx file from the URL below:
     *      https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html
     *  If the file is found, it downloads the file into the directory specified by the [outputDir] <path> option.
     *      ANd, it returns the string "Directory/downloadedFile".  If unsuccessful download, it will return and
     *      empty string ("").  If the option is not specified, it will download the file to ./build directory.
     */
    private fun downloadFile(outputDir: String): File {
        // Get the link to the LIVD-SARS-CoV-2-yyyy-MM-dd.xlsx file
        val livdFile = searchForTableFile(loincMappingPageUrl, livdSARSCov2FilenamePrefix)
        if (livdFile.isEmpty()) {
            error("Unable to find LOINC code data file matching LIVD-SARS-CoV-2-yyyy-MM-dd to download!")
        }
        val livdFileUrl = URL("$loincMappingBaseUrl${livdFile[0]}")
        val outputFile = File.createTempFile(
            "${FilenameUtils.getBaseName(livdFile[0])}_",
            "_downloaded.${FilenameUtils.getExtension(livdFile[0])}",
            File(outputDir)
        )

        // Read the file from the website and store it in local directory
        livdFileUrl.openStream().use { input ->
            try {
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            } catch (e: Exception) {
                error("Unable to write the downloaded file - $e")
            }
        }

        if (outputFile.length() == 0L) error("Downloaded LIVD table file is empty.")
        return outputFile
    }

    /**
     * Search - searching for the URI link to the LIVID-SARS-CoV-2-yyyy-MM-dd file
     * @param - urlToSearch is the URL to the web page that we will search.
     * @param - partialHref is the substring that we are searching for.
     * @return - List of the URI that contain the substring
     */
    private fun searchForTableFile(urlToSearch: URL, partialHref: String): List<String> {
        val allLinks =
            skrape(HttpFetcher) {
                request {
                    url = urlToSearch.toString()
                }
                response {
                    htmlDocument {
                        a {
                            findAll {
                                eachHref
                            }
                        }
                    }
                }
            }
        return allLinks.filter { it.contains(partialHref) }
    }

    /**
     * Extracts LIVD table from sheet [sheetName] of the input Excel format [inputfile] file, converts to
     * csv format, and output to a CSV formatted file.
     * @param sheetName is the LOINC Mapping sheet from the downloaded LOINC data code file.
     * @param inputfile is the input Excel file name.
     * @param inputfile is the output CSV file name.
     * @return the CSV formatted file with the LIVD data
     */
    private fun extractLivdTable(sheetName: String, inputfile: File): File {
        // Check for input file exist
        if (!inputfile.exists()) {
            error("$inputfile file does not exist.")
        }

        val data = StringBuffer() // Buffer and output file for CSV data
        val ext: String = FilenameUtils.getExtension(inputfile.name)
        if (!ext.equals("xlsx", ignoreCase = true)) {
            error("$inputfile is unsupported since it is not Excel xlsx format file.")
        }

        val fileInputStream = FileInputStream(inputfile)
        val workbook: Workbook = XSSFWorkbook(fileInputStream)
        fileInputStream.close()

        val outputfile = File.createTempFile(
            livdSARSCov2FilenamePrefix, "_orig.csv",
            File(defaultOutputDir)
        )
        val fileOutputStream = FileOutputStream(outputfile)

        // Get the LOINC Mapping sheet
        val sheet: Sheet = workbook.getSheet(sheetName)
            ?: error("Sheet \"$sheetName\" doesn't exist in the $inputfile file.")

        val rowStart = sheet.firstRowNum // Get starting row number
        val rowEnd = sheet.lastRowNum // Get ending row number

        // Start scan each row of the sheet.
        for (rowNum in rowStart until rowEnd + 1) {
            val row: Row = sheet.getRow(rowNum) ?: continue // Skip the empty row.

            // Scan each column of the sheet
            val lastColumn: Short = row.lastCellNum
            for (cn in 0 until lastColumn) {
                var delimiterChar = ","
                if (cn + 1 == lastColumn.toInt()) delimiterChar = "" // Use blank delimiter after the last column
                // Get cell object from the sheet.
                val cell = row.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)

                val cellValue = if (cell == null) "" // Insert blank if cell is null.
                else {
                    // Fill in string csv data according the cell type.
                    when (cell.cellType) {
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        CellType.STRING -> {
                            // Do some sanitation of the strings
                            var stringValue = cell.stringCellValue

                            // Drop '*' if it is at the end of the string.'
                            if (cell.stringCellValue.last() == '*') stringValue = cell.stringCellValue.dropLast(1)

                            // Add " to a string that contains quoted strings (i.e ""string"")
                            if (stringValue.contains("\n") || stringValue.contains(",") ||
                                (stringValue.contains("\""))
                            ) {
                                // String that contain special character(s)
                                stringValue = "\"" + stringValue.replace("\"", "\"\"") +
                                    "\""
                            }

                            // Trim whitespaces
                            // Strings may have non-breaking-white-space (NBSP) codes in them
                            stringValue = stringValue.replace('\u00A0', ' ').trim()

                            stringValue
                        }
                        CellType.BLANK -> ""
                        else -> "$cell"
                    }
                }
                data.append(cellValue + delimiterChar)
            }
            data.append(System.lineSeparator()) // End of each row
        }

        // Write to CSV file.
        fileOutputStream.write(data.toString().toByteArray())
        fileOutputStream.close()

        return outputfile
    }

    /**
     * Merge the supplemental LIVD data into one table with the LIVD data in [rawLivdFile] and generate a CSV
     * file.
     * @return the CSV formatted file with the merged LIVD data
     */
    private fun mergeLivdSupplementalTable(rawLivdFile: File): File {
        // First load both tables
        val rawLivdReaderOptions = CsvReadOptions.builder(rawLivdFile).columnTypesToDetect(listOf(ColumnType.STRING))
            .build()
        val rawLivdTable = Table.read().usingOptions(rawLivdReaderOptions)
        val supplLivdReaderOptions = CsvReadOptions.builder(livdSupplementalPathname)
            .columnTypesToDetect(listOf(ColumnType.STRING)).build()
        val supplLivdTable = Table.read().usingOptions(supplLivdReaderOptions)

        // Cleanup any models that have * at the end.
        supplLivdTable.forEach {
            if (it.getString(LivdTableColumns.MODEL.colName).endsWith("*"))
                it.setString(
                    LivdTableColumns.MODEL.colName,
                    it.getString(LivdTableColumns.MODEL.colName).dropLast(1)
                )
        }

        // Get the columns we need to process and add any new columns to the LIVD table
        val commonColList = mutableListOf<String>()
        val missingColList = mutableListOf<String>()
        supplLivdTable.columns().forEach { supplCol ->
            try {
                rawLivdTable.stringColumn(supplCol.name()) // This is the test to see if the column exists
                commonColList.add(supplCol.name())
            } catch (e: IllegalStateException) { missingColList.add(supplCol.name()) }
        }
        missingColList.forEach { missingColName ->
            val col = StringColumn.create(missingColName)
            // To add columns they must have the same number of rows
            repeat(rawLivdTable.rowCount()) { col.append("") }
            rawLivdTable.addColumns(col)
        }

        // Identify if a supplemental device exists in the LIVD table or not.
        var addedRows = 0
        var modRows = 0
        var nonUniqueRows = 0
        var badRows = 0
        supplLivdTable.forEach { supplRow ->
            var selector: Selection? = null
            commonColList.forEach { colName ->
                if (!supplRow.getString(colName).isNullOrBlank()) {
                    val newSelector = rawLivdTable.stringColumn(colName).isEqualTo(supplRow.getString(colName))
                    if (selector == null)
                        selector = newSelector
                    else selector!!.and(newSelector)
                }
            }
            when {
                selector == null -> {
                    TermUi.echo("Found row #${supplRow.rowNumber} with no device information.")
                    TermUi.echo(supplRow)
                    badRows++
                }
                selector!!.isEmpty -> {
                    // A new row is needed
                    val newRow = rawLivdTable.appendRow()
                    commonColList.forEach { newRow.setString(it, supplRow.getString(it)) }
                    missingColList.forEach { newRow.setString(it, supplRow.getString(it)) }
                    if (!silent) TermUi.echo("ADDING RECORD from row #${supplRow.rowNumber} : $newRow")
                    addedRows++
                }

                selector!!.size() == 1 -> {
                    // Merge into an existing row
                    missingColList.forEach { rawLivdTable.stringColumn(it).set(selector, supplRow.getString(it)) }
                    modRows++
                }

                else -> {
                    if (!silent) TermUi.echo("Found NON-UNIQUE record in row #${supplRow.rowNumber} : $supplRow")
                    nonUniqueRows++
                }
            }
        }

        // Print out the results of the merge.
        if (!silent) {
            TermUi.echo("Modified $modRows LIVD records with supplemental LIVD information.")
            TermUi.echo("Added $addedRows LIVD records from supplemental LIVD information.")
        }
        if (badRows > 0)
            error("Found $badRows row(s) in $livdSupplementalPathname that do not have device information")
        if (nonUniqueRows > 0)
            error("Found $nonUniqueRows row(s) in $livdSupplementalPathname that do not match to a unique LIVD record.")

        val outputFile = File.createTempFile(
            livdSARSCov2FilenamePrefix, "_final.csv",
            File(defaultOutputDir)
        )
        rawLivdTable.write().csv(outputFile)
        return outputFile
    }

    /**
     * Updates the LIVD lookup table name [livdLookupTable] of CSV file.  It setups PRIME CLI Lookup Table Create
     * Command line options.  And then, it calls the create lookup table command to create the new version of lookup
     * table.  Note, it always creates the new version regardless since it uses -f option.
     */
    private fun updateTheLivdLookupTable(livdLookupTable: File): Boolean {

        // The environment the command needs to run on.
        val environment = Environment.get(env)

        TermUi.echo("Creating $livdSARSCov2FilenamePrefix table ...")
        val args: MutableList<String> = mutableListOf(
            "-e", environment.toString().lowercase(), "-n", livdSARSCov2FilenamePrefix,
            "-i", livdLookupTable.absolutePath
        )
        if (silent) args.add("-s")
        if (activate) args.add("-a")

        return try {
            LookupTableCreateCommand().main(args)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private val loincMappingBaseUrl = URL("https://www.cdc.gov")
        /**
         * cdcLOINCTestCodeMappingPageUrl is the CDC URL that contains the LIVD-SARS-CoV-2-yyyyMMdd.xlsx file.
         */
        private val loincMappingPageUrl = URL("$loincMappingBaseUrl/csels/dls/sars-cov-2-livd-codes.html")

        /**
         * livdSARSCov2File is the prefix of the LIVD-SARS-CoV-2-yyyyMMdd.xlsx file to download.
         */
        private const val livdSARSCov2FilenamePrefix = "LIVD-SARS-CoV-2"

        /**
         * Sheet name within the downloaded file Excel file.
         */
        private const val sheetName = "LOINC Mapping"

        /**
         * Default folder to write files to.
         */
        private const val defaultOutputDir = "./build/livd-download"

        /**
         * The default location of the supplemental file.
         */
        private const val defaultSupplFile = "./metadata/tables/livd/LIVD-Supplemental.csv"
    }
}