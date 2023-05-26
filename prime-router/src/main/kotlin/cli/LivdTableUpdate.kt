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

    private val inputFile by option(
        "-i", "--input-file", help = "Input file to update LIVD table"
    ).file()

    override fun run() {
        echo("Updating the LIVD table ...")
        FileUtils.forceMkdir(File(defaultOutputDir))

        val tempRawLivdOutFile = extractLivdTable(sheetName, inputFile as File, defaultOutputDir)
        // Merge the supplemental LIVD table with the raw.
        val tempUpdatedLIVDOutFile = updateLIVDTable(tempRawLivdOutFile)
        tempRawLivdOutFile.delete()

        // Now, store the data as a LIVD lookup table.
        if (!updateTheLivdLookupTable(tempUpdatedLIVDOutFile))
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
     * Update LIVD table to add test device rows
     *
     * TODO: is the supplemental table still required for the following columns which were unable to be mapped
     *       - is_unproctored
     *       - fda_ref
     *       - fda_authorization
     * @return the CSV formatted file with the merged LIVD data
     */
    private fun updateLIVDTable(rawLivdFile: File): File {
        // First load both tables
        val rawLivdReaderOptions = CsvReadOptions.builder(rawLivdFile)
            .columnTypesToDetect(listOf(ColumnType.STRING))
            .build()
        val rawLivdTable = Table.read().usingOptions(rawLivdReaderOptions)
            .sortAscendingOn(LivdTableColumns.MANUFACTURER.colName, LivdTableColumns.MODEL.colName)

        // empty values default to P
        rawLivdTable.addColumns(StringColumn.create("processing_mode_code"))

        // append test devices
        appendTestDeviceRow(
            rawLivdTable,
            mapOf(
                LivdTableColumns.MODEL to "Test_OTC_Device",
                LivdTableColumns.TESTKIT_NAME_ID to "Test_OTC_Device",
                LivdTableColumns.EQUIPMENT_UID to "Test_OTC_Device",
                LivdTableColumns.OTC_HOME_TESTING to "yes"
            )
        )
        appendTestDeviceRow(
            rawLivdTable,
            mapOf(
                LivdTableColumns.MODEL to "Test_Home_Device",
                LivdTableColumns.TESTKIT_NAME_ID to "Test_Home_Device",
                LivdTableColumns.EQUIPMENT_UID to "Test_Home_Device",
                LivdTableColumns.OTC_HOME_TESTING to "yes"
            )
        )

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
     * Appends an additional row to the LIVD table with a processing_mode_code of T
     */
    private fun appendTestDeviceRow(table: Table, cellData: Map<LivdTableColumns, String>) {
        val row = table.appendRow()
        cellData.forEach { row.setString(it.key.colName, it.value) }

        // test devices will always have processing_mode_code as T
        row.setString("processing_mode_code", "T")
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
    }
}