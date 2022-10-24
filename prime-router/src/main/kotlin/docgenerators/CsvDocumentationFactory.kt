package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Schema
import org.apache.logging.log4j.kotlin.Logging

/** Writes out the schema documentation in a CSV data dictionary format */
object CsvDocumentationFactory : DocumentationFactory(), Logging {
    override val fileExtension: String
        get() = "csv"

    override fun getSchemaDocumentation(schema: Schema): String {
        TODO("Not yet implemented")
    }

    override fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String,
        outputFileName: String?,
        includeTimestamps: Boolean
    ) {
        TODO("Not yet implemented")
    }
}