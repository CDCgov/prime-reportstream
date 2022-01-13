package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import gov.cdc.prime.router.common.Environment
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.eachHref
import it.skrape.selects.html5.a
import org.apache.commons.io.FilenameUtils
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL

/**
 * cdcLOINCTestCodeMappingPageUrl is the CDC URL that contains the LIVD-SARS-CoV-2-yyyyMMdd.xlsx file.
 */
private const val cdcLOINCTestCodeMappingPageUrl = "https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html"

/**
 * livdSARSCov2File is the prefix of the LIVD-SARS-CoV-2-yyyyMMdd.xlsx file to download.
 */
private const val livdSARSCov2File = "LIVD-SARS-CoV-2"

/**
 * sheetName is the sheet name with the LIVID-SARS-CoV-2-yyyyMMdd.xlsx downloaded file.
 */
private const val sheetName = "LOINC Mapping"

/**
 * LivdTableDownload is the command line interface for the livd-table-download command. It parses the command line
 * for option given as below.
 */
class LivdTableDownload() : CliktCommand(
    name = "livd-table-download",
    help = """
    livd-table-download downloads the latest LOINC test data from CDC website, so it can be used to update the 
    new version of the lookup table in the database automatically. 
    
    It looks for the LIVD-SAR-CoV-2-yyyy-MM-dd.xlsx file from $cdcLOINCTestCodeMappingPageUrl.  If the file is found, 
    it downloads the file into the ./build directory.  If not found, it will prompt error accordingly.  Next, it build 
    the output Lookup Table (<tableName.csv> file) with the table name specified by --output-file <path/filename> 
    option. If the --output-file is not specified, it will use the ./metadata/tables/$livdSARSCov2File-latest.csv as
    the default output file.  Finally, it uses the output filename as the new lookup table name to update the 
    lookup tables in the database as the new version of the table.  It updates new version of the lookup table in 
    the given --env [local, test, staging, or prod] with the default to "local" environment.
    
    Note, this command will always create new version of the lookup table.
        
    Example: The following command will download the latest LIVD-SARS-CoV-2-yyyy-MM-dd.xlsx from the above URL.  
      It will store the LIVD-SARS-CoV-2-xyz.csv file under the ./junk directory and update the LIVD-SARS-CoV-2-xyz
      lookup table in the database.
      
         ./prime livd-table-download --output-file ./junk/LIVD-SARS-CoV-2-xyz.csv
         
      The command below will download the latest LIVD-SARS-CoV-2-yyyy-MM-dd.xlsx file from the above UTL.
      It will crate the LIVD-SARS-CoV-2-latest.csv file under the ./metadata/tables directory.
      
        ./prime livd-table-download
    """
) {
    private val defaultOutputDir = "./build"
    private val defaultOutPutFile = "./metadata/tables/$livdSARSCov2File-latest.csv"
    private val outputFile by option(
        "--output-file",
        metavar = "<path/filename>",
        help = "Output pathname where to store the LIVD data as CSV."
    ).default(defaultOutPutFile)

    /**
     * The environment to connect to.
     */
    private val env by option(
        "-e", "--env",
        metavar = "<name>",
        envvar = "PRIME_ENVIRONMENT",
        help = "Connect to <name> environment.\nChoose between [local|test|staging|prod]"
    )
        .choice(
            Environment.LOCAL.envName, Environment.STAGING.envName,
            Environment.TEST.envName, Environment.PROD.envName
        )
        .default(Environment.LOCAL.envName, "local environment")

    /**
     * The environment the command needs to run on.
     */
    internal val environment get() = Environment.get(env)

    /**
     * The reference to the table creator command.
     */
    private val tableCreator = LookupTableCreateCommand()

    override fun run() {
        // Download the LIVD-SARS-CoV2-yyyyMMdd.xlsx from CDC web site given above.
        val downloadedDirFile = downloadFile(defaultOutputDir)
        if (downloadedDirFile.isEmpty()) return

        // Extracts the "LIONC Mapping" sheet from the Excel and output to the specified output CSV format file
        // specified by --output-file option.
        if (!extractLivdTable(sheetName, downloadedDirFile, outputFile)) return

        // Now, upload the LIVD-SARS-CoV-2-yyyyMMdd (LIVD lookup table) to a new version of a lookup tables
        // in database.
        updateTheLivdLookupTable(File(outputFile))
    }

    /**
     *  The downloadFile downloads the latest LOINC test data, so it can be ingested automatically.
     *  It looks for the LIVD-SAR-CoV-2-yyyy-MM-dd.xlsx file from the URL below:
     *      https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html
     *  If the file is found, it downloads the file into the directory specified by the [outputDir] <path> option.
     *      ANd, it returns the string "Directory/downloadedFile".  If unsuccessful download, it will return and
     *      empty string ("").  If the option is not specified, it will download the file to ./build directory.
     */
    private fun downloadFile(outputDir: String): String {
        // Get the link to the LIVD-SARS-CoV-2-yyyy-MM-dd.xlsx file
        val livdFile = search(cdcLOINCTestCodeMappingPageUrl, livdSARSCov2File)
        if (livdFile.isEmpty()) {
            TermUi.echo("\tERROR: unable to find LOINC code data to download!")
            return ""
        }
        val livdFileUrl = "https://cdc.gov/" + livdFile.get(0)

        // Create the local file in the specified directory
        val localFilename = livdFileUrl.split('/').filter { it.contains(livdSARSCov2File) }.get(0)
        val outputfile = File(outputDir, localFilename)

        // Read the file from the website and store it in local directory
        URL(livdFileUrl).openStream().use { input ->
            if (outputfile.exists()) {
                val c = prompt("\t$outputfile file is already existed: You want to overwrite it (y/n)?", "n")
                if (c?.lowercase() == "n") {
                    return ""
                } else {
                    TermUi.echo("\tOverwriting the $outputfile file.")
                }
            }

            try {
                FileOutputStream(outputfile).use { output ->
                    input.copyTo(output)
                }
            } catch (e: Exception) {
                TermUi.echo("\tERROR: Unable to write the downloaded file - $e")
                return ""
            }
        }

        TermUi.echo("\tSUCCESS: The $outputfile file is downloaded.")
        return "$outputDir/$localFilename"
    }

    /**
     * Search - searching for the URI link to the LIVID-SARS-CoV-2-yyyy-MM-dd file
     * @param - urlToSearch is the URL to the web page that we will search.
     * @param - partialHref is the substring that we are searching for.
     * @return - List of the URI that contain the substring
     */
    private fun search(urlToSearch: String, partialHref: String): List<String> {
        val allLinks =
            skrape(HttpFetcher) {
                request {
                    url = urlToSearch
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
     * csv format, and output to the CSV format [outputfile] file.
     * @param sheetName is the LOINC Mapping sheet from the downloaded LOINC data code file.
     * @param inputfile is the input Excel file name.
     * @param inputfile is the output CSV file name.
     * @return true for success and false for failure.
     */
    private fun extractLivdTable(sheetName: String, inputfile: String, outputfile: String): Boolean {
        // Check for input file exist
        if (!File(inputfile).exists()) {
            TermUi.echo("\tERROR: The $inputfile file does not exist.")
        }

        val data = StringBuffer() // Buffer and output file for CSV data
        val fileInputStream = FileInputStream(File(inputfile))
        var workbook: Workbook? = null
        val ext: String = FilenameUtils.getExtension(inputfile)
        if (ext.equals("xlsx", ignoreCase = true)) {
            workbook = XSSFWorkbook(fileInputStream)
        } else if (ext.equals("xls", ignoreCase = true)) {
            workbook = HSSFWorkbook(fileInputStream)
        }

        // Check for output file duplication
        if (File(outputfile).exists()) {
            val c = TermUi.prompt("\t$outputfile file is already existed: You want to overwrite it (y/n)?", "n")
            if (c?.lowercase() == "n") {
                return false
            } else {
                TermUi.echo("\tOverwriting the $outputfile file.")
            }
        }

        val fileOutputStream = FileOutputStream(File(outputfile))

        // Get the LOINC Mapping sheet
        val sheet: Sheet? = workbook!!.getSheet(sheetName)
        if (sheet == null) {
            TermUi.echo("\tERROR: Sheet \"$sheetName\" doesn't exist in the $inputfile file.")
            return false
        }

        val rowStart = sheet.getFirstRowNum() // Get starting row number
        val rowEnd = sheet.getLastRowNum() // Get ending row number

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

                if (cell == null) {
                    data.append("" + delimiterChar) // Insert blank if cell is null.
                } else {
                    // Fill in string csv data according the cell type.
                    when (cell.cellType) {
                        CellType.BOOLEAN -> data.append(cell.booleanCellValue.toString() + delimiterChar)
                        CellType.NUMERIC -> data.append(cell.numericCellValue.toString() + delimiterChar)
                        CellType.STRING -> {
                            // Drop '*' if it is at the end of the string.'
                            val stringValue = if (cell.stringCellValue.last() == '*')
                                cell.stringCellValue.dropLast(1)
                            else
                                cell.stringCellValue
                            // Add " to string that contains "string" (i.e ""string"")
                            if (stringValue.contains("\n") || stringValue.contains(",") ||
                                (stringValue.contains("\""))
                            ) {
                                // String that contain special character(s)
                                data.append(
                                    "\"" + stringValue.replace("\"", "\"\"") +
                                        "\"" + delimiterChar
                                )
                            } else {
                                data.append(stringValue + delimiterChar)
                            }
                        }
                        CellType.BLANK -> data.append("" + delimiterChar)
                        else -> data.append("$cell,")
                    }
                }
            }
            data.append(System.lineSeparator()) // End of each row
        }

        // Write to CSV file.
        fileOutputStream.write(data.toString().toByteArray())
        fileOutputStream.close()

        TermUi.echo("\tSUCCESS: The $outputfile file is created.\n")
        return true
    }

    /**
     * Updates the LIVD lookup table name [livdLookupTable] of CSV file.  It setups PRIME CLI Lookup Table Create
     * Command line options.  And then, it calls the create lookup table command to create the new version of lookup
     * table.  Note, it always create the new version regardless since it uses -f option.
     */
    private fun updateTheLivdLookupTable(livdLookupTable: File) {
        val tableName = livdLookupTable.nameWithoutExtension
        TermUi.echo("Creating table $tableName...")
        val args: MutableList<String> = mutableListOf(
            "-e", environment.toString().lowercase(), "-n", tableName,
            "-i", livdLookupTable.absolutePath, "-s", "-a", "-f"
        )
        tableCreator.main(args)
    }
}