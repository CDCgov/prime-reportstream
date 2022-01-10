package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
<<<<<<< HEAD
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
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
=======
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.FileInputStream
import java.io.FileOutputStream
import org.apache.commons.io.FilenameUtils
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

>>>>>>> 9780d2a7f3424144133d6e13a6a0e50d69791329

private const val sheetName = "LOINC Mapping"

/**
 * LivdTableExtracts is the commena line interface for the livd-table-extracts command. It parses the command line
 * for option given as below.
 */
class LivdTableExtracks() : CliktCommand(
    name = "livd-table-extracts",
    help = """
    The command extracts LIVD Table from the LOINC Mapping sheet of the downloaded the latest LOINC test data. 
    
    It reads the provided input file of --input-file option (i.e. --input-file ./junk/LIVD-SAR-CoV-2-yyyy-MM-dd.xlsx)
	Next, it extracts the LOINC Mapping sheet, converts cell-by-cell, and output to the output csv format file of
    --output-file option (i.e. --output-file ./junk/livd-table.csv)
        
    Example:
      The following command will extract the latest LIVD table from the ./junk/LIVD-SARS-CoV-2-2021-12-15.xlsx Excel 
      format file.  And, it outputs the CSV format file to the ./junk/livd-table.cvs.
      
      ./prime livd-table-extracts --input-file ./junk/LIVD-SARS-CoV-2-2021-12-15.xslx 
        --output-file ./junk/livd-table.csv
    """
) {
    private val inputFile by option(
        "--input-file",
        metavar = "<path>",
        help = "interpret `--input` relative to this directory and file"
    ).required()
<<<<<<< HEAD

    private val outputFile by option(
=======
    
        private val outputFile by option(
>>>>>>> 9780d2a7f3424144133d6e13a6a0e50d69791329
        "--output-file",
        metavar = "<path>",
        help = "interpret `--output` relative to this directory and file"
    ).required()

    override fun run() {
        extractsLivdTable(sheetName, inputFile, outputFile)
    }

    companion object {
        /**
         * Extracts LIVD table from sheet [sheetName] of the input Excel formate [inputfile] file, converts to
         * csv format, and output to the CSV format [outpufile] file.
         * @param sheetName is the LOINC Mapping sheet from the downloaded LOINC data code file.
         * @param inputfile is the input Excel file name.
         * @param inputfile is the output CSV file name.
         * @return true for success and fales for failure.
         */
<<<<<<< HEAD
        fun extractsLivdTable(sheetName: String, inputfile: String, outputfile: String): Boolean {
            // Buffer and output file for CSV data
            val data = StringBuffer()
            val fos = FileOutputStream(File(outputfile))

            val fis = FileInputStream(File(inputfile))
            var workbook: Workbook? = null
            val ext: String = FilenameUtils.getExtension(inputfile)
            if (ext.equals("xlsx", ignoreCase = true)) {
                workbook = XSSFWorkbook(fis)
            } else if (ext.equals("xls", ignoreCase = true)) {
                workbook = HSSFWorkbook(fis)
            }

            // Get the LOINC Mapping sheet
            val sheet: Sheet = workbook!!.getSheet(sheetName)

            val rowStart = sheet.getFirstRowNum()
            val rowEnd = sheet.getLastRowNum()

            for (rowNum in rowStart until rowEnd + 1) {
                val r: Row = sheet.getRow(rowNum) ?: continue

                val lastColumn: Short = r.lastCellNum
                for (cn in 0 until lastColumn) {
                    var delimiterChar = ","
                    if (cn + 1 == lastColumn.toInt()) delimiterChar = ""

                    val cell = r.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)

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
                data.append('\n') // appending new line after each row
            }

            fos.write(data.toString().toByteArray())
            fos.close()
            return true
=======
        fun extractsLivdTable(sheetName: String, inputfile: String, outputfile: String) : Boolean {
            // Buffer and output file for CSV data
                val data = StringBuffer()
                val fos = FileOutputStream(File(outputfile))

                val fis = FileInputStream(File(inputfile))
                var workbook: Workbook? = null
                val ext: String = FilenameUtils.getExtension(inputfile)
                if (ext.equals("xlsx", ignoreCase = true)) {
                    workbook = XSSFWorkbook(fis)
                } else if (ext.equals("xls", ignoreCase = true)) {
                    workbook = HSSFWorkbook(fis)
                }

                // Get the LOINC Mapping sheet
                val sheet: Sheet = workbook!!.getSheet(sheetName)

                val rowStart = sheet.getFirstRowNum()
                val rowEnd = sheet.getLastRowNum()

                for (rowNum in rowStart until rowEnd + 1) {
                    val r: Row = sheet.getRow(rowNum) ?: continue

                    val lastColumn: Short = r.lastCellNum
                    for (cn in 0 until lastColumn) {
                        var delimiterChar = ","
                        if (cn + 1 == lastColumn.toInt()) delimiterChar = ""

                        val cell = r.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)

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
                    data.append('\n') // appending new line after each row
                }

                fos.write(data.toString().toByteArray())
                fos.close()
                return true
>>>>>>> 9780d2a7f3424144133d6e13a6a0e50d69791329
        }
    }
}