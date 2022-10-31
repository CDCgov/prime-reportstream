package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Schema
import org.apache.logging.log4j.kotlin.Logging
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

/**
 * Generates Excel documents as a data dictionary
 */
object ExcelDocumentationFactory : TableBasedDocumentationFactory(), Logging {
    /** No magic numbers */
    private const val headerRowIndex = 0

    /** Our file extension */
    override val fileExtension: String
        get() = "xlsx"

    /** Call into the CsvDocumentationFactory and get its headers */
    override fun getHeaders() = CsvDocumentationFactory.getHeaders()

    /**
     * The underlying structure of the document is the same as the CSV document, so rather than
     * recreate it, we will just call into it and get the same data
     **/
    override fun getSchemaDocumentation(schema: Schema) = CsvDocumentationFactory.getSchemaDocumentation(schema)

    override fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String,
        outputFileName: String?,
        includeTimestamps: Boolean
    ) {
        // ensure the output directory
        ensureOutputDirectory(outputDir)
        // generate a file name
        val outName = getOutputFileName(outputFileName, schema, includeTimestamps, this.fileExtension)
        // create our output file, wrap it in a stream, and write to it
        FileOutputStream(File(outputDir, outName)).also { outputStream ->
            getExcelWorkbook(schema).also { workbook ->
                workbook.write(outputStream)
            }
            outputStream.flush()
        }.close() // close out the stream now that we're done with it
    }

    /** Generate our workbook and its contents */
    private fun getExcelWorkbook(schema: Schema): Workbook {
        // create the workbook
        return XSSFWorkbook().also { workbook ->
            // create our sheet
            workbook.createSheet("${schema.name} - Data Dictionary").also { sheet ->
                // create a row for the header
                sheet.createRow(headerRowIndex).also { headerRow ->
                    // get the headers for the document
                    getHeaders().forEachIndexed { index, s ->
                        headerRow.createCell(index).also { cell ->
                            cell.setCellValue(s)
                        }
                    }
                }
                // write out the schema documentation next
                getSchemaDocumentation(schema).forEachIndexed { index, s ->
                    // create the next row
                    sheet.createRow(index + 1).also { row ->
                        // split the values
                        s.forEachIndexed { cellIndex, cellValue ->
                            row.createCell(cellIndex).setCellValue(cellValue)
                        }
                    }
                }
            }
        }
    }
}