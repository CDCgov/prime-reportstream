package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
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
import java.io.IOException
import java.net.URL

private const val cdcLOINCTestCodeMappingPage = "https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html"
private const val livdSARSCov2File = "LIVD-SARS-CoV-2"
private const val sheetName = "LOINC Mapping"

/**
 * LivdTableDownload is the command line interface for the livd-table-download command. It parses the command line
 * for option given as below.
 */
class LivdTableDownload() : CliktCommand(
    name = "livd-table-download",
    help = """
    livd-table downloads the latest LOINC test data, so it can be ingested automatically. 
    
    It looks for the LIVD-SAR-CoV-2-yyyy-MM-dd.xlsx file from $cdcLOINCTestCodeMappingPage.
	If the file is found, it downloads the file into the directory specified by the --output-dir <path> option.
	If the option is not specified, it will download the file to ./build directory.
        
    Example: The following command will download the latest LIVD-SARS-CoV-2-yyyy-MM-dd.xlsx from the above URL.  
      It will store the LIVD-SARS-CoV-2-xyz.csv file under the ./junk directory.
      
         ./prime livd-table-download --output-file ./junk/LIVD-SARS-CoV-2-xyz.csv
    """
) {
    private val defaultOutputDir = "./build"
    private val outputDirFile by option(
        "--output-file",
        metavar = "<path/filename>",
        help = "Directory/Filename for the CSV output file."
    ).required()

    override fun run() {
        val downloadedDirFile = downloadFile(defaultOutputDir)
        if (downloadedDirFile.isEmpty()) return

        extractsLivdTable(sheetName, downloadedDirFile, outputDirFile)
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
        //
        // Get the link to the LIVD-SARS-CoV-2-yyyy-MM-dd.xlsx file
        //
        var livdFile = search(cdcLOINCTestCodeMappingPage, livdSARSCov2File)
        if (livdFile.isEmpty()) {
            TermUi.echo("\tError: unable to find LOINC code data to download!")
            return ""
        } else {
        }
        var livdFileUrl = "https://cdc.gov/" + livdFile.get(0)

        //
        // Create the local file in the specified directory
        //
        val localFilename = livdFileUrl.split('/').filter { it.contains(livdSARSCov2File) }.get(0)
        val outputFile = File(outputDir, localFilename)

        //
        // Read the file from the website and store it in local directory
        //
        URL(livdFileUrl).openStream().use { input ->
            if (outputFile.exists()) {
                val c = prompt("\t$outputFile file is already existed: You want to overwrite it (y/n)?", "n")
                if (c?.lowercase() == "n") {
                    return ""
                } else {
                    TermUi.echo("\tThe $outputFile file is overwriting.")
                }
            }

            try {
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            } catch (e: Exception) {
                TermUi.echo("\tERROR: $e")
                return ""
            }
        }

        TermUi.echo("\tSUCCESS: The $outputFile file is downloaded.")
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
     * Extracts LIVD table from sheet [sheetName] of the input Excel formate [inputfile] file, converts to
     * csv format, and output to the CSV format [outpufile] file.
     * @param sheetName is the LOINC Mapping sheet from the downloaded LOINC data code file.
     * @param inputfile is the input Excel file name.
     * @param inputfile is the output CSV file name.
     * @return true for success and fales for failure.
     */
    private fun extractsLivdTable(sheetName: String, inputfile: String, outputfile: String): Boolean {

        try {
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
                    TermUi.echo("\tThe $outputfile file is overwriting.")
                }
            }

            val fileOutputStream = FileOutputStream(File(outputfile))

            // Get the LOINC Mapping sheet
            val sheet: Sheet? = workbook!!.getSheet(sheetName)
            if (sheet == null) {
                print("\tERROR: Sheet \"$sheetName\" doesn't exist in the $inputfile file.")
                return false
            }

            val rowStart = sheet.getFirstRowNum()
            val rowEnd = sheet.getLastRowNum()

            for (rowNum in rowStart until rowEnd + 1) {
                val row: Row = sheet.getRow(rowNum) ?: continue

                val lastColumn: Short = row.lastCellNum
                for (cn in 0 until lastColumn) {
                    var delimiterChar = ","
                    if (cn + 1 == lastColumn.toInt()) delimiterChar = ""

                    val cell = row.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)

                    if (cell == null) {
                        data.append("" + delimiterChar)
                    } else {
                        when (cell.cellType) {
                            CellType.BOOLEAN -> data.append(cell.booleanCellValue.toString() + delimiterChar)
                            CellType.NUMERIC -> data.append(cell.numericCellValue.toString() + delimiterChar)
                            CellType.STRING -> {
                                val stringValue = cell.stringCellValue.replace("*", "")
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
                data.append('\n') // End of each row
            }

            fileOutputStream.write(data.toString().toByteArray())
            fileOutputStream.close()

            TermUi.echo("\tSUCCESS: The $outputfile file is created.\n")
            return true
        } catch (e: IOException) {
            TermUi.echo("\tFAILURE: ${e.message}.\n")
            return false
        }
    }
}