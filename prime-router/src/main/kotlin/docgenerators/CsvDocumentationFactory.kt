package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Schema
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.logging.log4j.kotlin.Logging
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/** Writes out the schema documentation in a CSV data dictionary format */
object CsvDocumentationFactory : DocumentationFactory(), Logging {
    override val fileExtension: String
        get() = "csv"

    override fun getSchemaDocumentation(schema: Schema) = sequence {
        schema.elements.sortedBy { it -> it.name }.forEach { element ->
            yield(
                (element.name + "|" + (element.type?.name ?: ""))
            )
        }
    }

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
        // get a buffered writer to output to
        val bw = BufferedWriter(FileWriter(File(outputDir, outName)))
        // get the schema documentation as a sequence and then write it out to a single string

        // create a csv format
        val csvFormat = CSVFormat.Builder
            .create(CSVFormat.DEFAULT)
            .setHeader("field_name", "type")
            .build()
        // create a writer
        val csvWriter = CSVPrinter(
            bw,
            csvFormat
        )

        getSchemaDocumentation(schema).forEach { row ->
            csvWriter.printRecord(row.split("|"))
        }

        // some clean up
        csvWriter.flush()
        csvWriter.close()
    }
}