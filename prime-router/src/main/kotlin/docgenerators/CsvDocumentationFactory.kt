package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Schema
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.logging.log4j.kotlin.Logging
import java.io.BufferedWriter
import java.io.FileWriter

/** Writes out the schema documentation in a CSV data dictionary format */
object CsvDocumentationFactory : DocumentationFactory(), Logging {
    override val fileExtension: String
        get() = "csv"

    override fun getSchemaDocumentation(schema: Schema) = sequence {
        schema.elements.filter { !it.csvFields.isNullOrEmpty() }.sortedBy { it -> it.name }.forEach { element ->
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
        // change any slashes to dashes for the file name
        val schemaName = canonicalizeSchemaName(schema)
        // ensure the output directory
        ensureOutputDirectory(outputDir)
        // generate a file name
        val outName = getOutputFileName(outputFileName, schemaName, includeTimestamps, this.fileExtension)
        val bw = BufferedWriter(FileWriter(outName))
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