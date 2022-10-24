package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Schema
import org.apache.logging.log4j.kotlin.Logging

/**
 * Generates Excel documents as a data dictionary
 */
object ExcelDocumentationFactory : DocumentationFactory(), Logging {
    override val fileExtension: String
        get() = "xlsx"

    override fun getSchemaDocumentation(schema: Schema) = sequence {
        yield("")
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