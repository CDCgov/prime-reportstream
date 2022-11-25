package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.metadata.LivdTableColumns
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
import java.io.FileOutputStream
import java.net.URL

/**
 * LivdTableUpdate is the command line interface for the livd-table-update command. It parses the command line
 * for option given as below.
 *
 * It reads in the LIVD file from the supplied directly using the --input-file parameter.
 * Next, it builds the output Lookup Table (<./build/LIVD-SARS-CoV-2.csv> file) with the table name. Finally, it updates
 * the LIVD-SARS-CoV-2 lookup tables in the database as the new version of the table. It updates new version of the
 * lookup table in the given --env [local, test, staging, or prod] with the default to "local" environment.
 *
 * Note, this command will always create new version of the lookup table.
 *
 * Example:
 * The command below reads the LIVE-SARS-CoV-2-2011-10-19.xlsx file from junk directory, extracts the LOINC Mapping
 * Sheet, Converts to CSV, uploads to database, and activate the table.
 *
 *  ./prime livd-table-update --input-file junk/LIVE-SARS-CoV-2-2011-10-19.xlsx -a
 *
 */

class LivdTableUpdate : CliktCommand(
    name = "livd-table-update",
    help = """
        This updates the LIVD lookup table with a new version.
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

    private val inputFile by option(
        "-i", "--input-file", help = "Input file to update LIVD table"
    ).file()

    override fun run() {
        echo("Updating the LIVD table ...")
        FileUtils.forceMkdir(File(defaultOutputDir))

        val tempRawLivdOutFile = extractLivdTable(sheetName, inputFile as File, defaultOutputDir)
        // Merge the supplemental LIVD table with the raw.
        val tempMergedLivdOutFile = mergeLivdSupplementalTable(tempRawLivdOutFile)
        tempRawLivdOutFile.delete()

        // Now, store the data as a LIVD lookup table.
        if (!updateTheLivdLookupTable(tempMergedLivdOutFile))
            error("There was an error storing the LIVD lookup table.")
        else
            echo("The lookup table was updated successfully.")
    }

    /**
     * Extracts LIVD table from sheet [sheetName] of the input Excel format [inputfile] file, converts to
     * csv format, and output to a CSV formatted file.
     * @param sheetName is the LOINC Mapping sheet from the downloaded LOINC data code file.
     * @param inputfile is the input Excel file name.
     * @param inputfile is the output CSV file name.
     * @return the CSV formatted file with the LIVD data
     */
    fun extractLivdTable(sheetName: String, inputfile: File, outputDir: String): File {
        // Check for input file exist
        if (!inputfile.exists()) {
            error("$inputfile file does not exist.")
        }

        val data = StringBuffer() // Buffer and output file for CSV data
        val ext: String = FilenameUtils.getExtension(inputfile.name)
        if (!ext.equals("xlsx", ignoreCase = true)) {
            error("$inputfile is unsupported since it is not Excel xlsx format file.")
        }

        try {
            val workbook: Workbook = XSSFWorkbook(inputfile)

            val outputfile = File.createTempFile(
                livdSARSCov2FilenamePrefix, "_orig.csv",
                File(outputDir)
            )
            val fileOutputStream = FileOutputStream(outputfile)

            // Get the LOINC Mapping sheet
            val sheet: Sheet = workbook.getSheet(sheetName)
                ?: error("Sheet \"$sheetName\" doesn't exist in the $inputfile file.")
            val rowStart = sheet.firstRowNum // Get starting row number
            val rowEnd = sheet.lastRowNum // Get ending row number

            val lastColumn: Short = sheet.getRow(0).lastCellNum

            // Start scan each row of the sheet.
            for (rowNum in rowStart until rowEnd + 1) {
                val row: Row = sheet.getRow(rowNum) ?: continue // Skip the empty row.

                // Scan each column of the sheet

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

            return removeBlankRowFromCsv(outputfile)
        } catch (e: Exception) {
            error("Extract Livd Table failed: $e.")
        }
    }

    /**
     * Merge the supplemental LIVD data into one table with the LIVD data in [rawLivdFile] and generate a CSV
     * file.
     * @return the CSV formatted file with the merged LIVD data
     */
    fun mergeLivdSupplementalTable(rawLivdFile: File): File {
        // First load both tables
        val rawLivdReaderOptions = CsvReadOptions.builder(rawLivdFile)
            .columnTypesToDetect(listOf(ColumnType.STRING))
            .build()
        val rawLivdTable = Table.read().usingOptions(rawLivdReaderOptions)
            .sortAscendingOn(LivdTableColumns.MANUFACTURER.colName, LivdTableColumns.MODEL.colName)
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
            } catch (e: IllegalStateException) {
                missingColList.add(supplCol.name())
            }
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
            if (!silent) echo("Here is the list of changes added from $livdSupplementalPathname")
            when {
                selector == null -> {
                    echo("Found row #${supplRow.rowNumber} with no device information.")
                    echo(supplRow)
                    badRows++
                }

                selector!!.isEmpty -> {
                    // A new row is needed
                    val newRow = rawLivdTable.appendRow()
                    commonColList.forEach { newRow.setString(it, supplRow.getString(it)) }
                    missingColList.forEach { newRow.setString(it, supplRow.getString(it)) }
                    if (!silent) echo("ADDING RECORD from row #${supplRow.rowNumber} : $newRow")
                    addedRows++
                }

                selector!!.size() == 1 -> {
                    // Merge into an existing row
                    missingColList.forEach { rawLivdTable.stringColumn(it).set(selector, supplRow.getString(it)) }
                    modRows++
                }

                else -> {
                    if (!silent) echo("Found NON-UNIQUE record in row #${supplRow.rowNumber} : $supplRow")
                    nonUniqueRows++
                }
            }
        }

        // Print out the results of the merge.
        if (!silent) {
            echo("Modified $modRows LIVD records with supplemental LIVD information.")
            echo("Added $addedRows LIVD records from supplemental LIVD information.")
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
     * removeBlankRowFromCsv moves/cleans blank row(s) from the given cvs file.  And, it returns
     * the cleaned file without blank.
     */
    private fun removeBlankRowFromCsv(downloadFile: File): File {
        val rows: List<List<String>> = csvReader().readAll(downloadFile)
        val nonEmptyRows: MutableList<List<String>>? = mutableListOf()
        rows.forEach {
            for (tmp in it) {
                if (tmp.isNotEmpty()) {
                    nonEmptyRows!!.add(it)
                    break
                }
            }
        }
        csvWriter().writeAll(nonEmptyRows!!.toList(), downloadFile)
        return downloadFile
    }

    /**
     * Updates the LIVD lookup table name [livdLookupTable] of CSV file to set up PRIME CLI Lookup Table Create
     * Command line options.  And then, it calls the create lookup table command to create the new version of lookup
     * table.  Note, it always creates the new version regardless since it uses -f option.
     */
    fun updateTheLivdLookupTable(livdLookupTable: File): Boolean {

        // The environment the command needs to run on.
        val environment = Environment.get(env)

        echo("Creating $livdSARSCov2FilenamePrefix table ...")
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