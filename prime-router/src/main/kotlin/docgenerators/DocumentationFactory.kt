package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Schema
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The base documentation factory with core logic */
abstract class DocumentationFactory {
    abstract val fileExtension: String

    abstract fun getSchemaDocumentation(schema: Schema): Sequence<*>

    abstract fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String = ".",
        outputFileName: String? = null,
        includeTimestamps: Boolean = false
    )

    companion object {
        /** The output pattern for the dates in the file names */
        const val createdDateFormatterPattern = "yyyy.MM.dd"

        /** The formatter we use for the file names */
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(createdDateFormatterPattern)

        /** Given a schema name, convert slashes to dashes, so they don't break path logic */
        private fun canonicalizeSchemaName(schema: Schema) = schema.name.replace("/", "-")

        /**
         * Gets the output file name based on the settings passed in
         */
        fun getOutputFileName(
            outputFileName: String?,
            schema: Schema,
            includeTimestamps: Boolean,
            fileExtension: String
        ): String {
            return (outputFileName ?: canonicalizeSchemaName(schema)) + if (includeTimestamps) {
                "-${LocalDate.now().format(formatter)}.$fileExtension"
            } else {
                ".$fileExtension"
            }
        }

        /**
         * Verifies the output directory exists and creates it if it doesn't
         */
        fun ensureOutputDirectory(outputDir: String): String {
            val path = Paths.get(outputDir)
            if (!Files.exists(path)) {
                Files.createDirectory(path)
            }
            return outputDir
        }
    }
}

/**
 * A string based documentation factory that returns a sequence of strings,
 * as opposed to the [TableBasedDocumentationFactory] that returns a sequence of a list of strings
 * for generating things like CSV and Excel. This is used for Markdown and HTML which have a non-table
 * layout
 **/
abstract class StringBasedDocumentationFactory : DocumentationFactory() {
    abstract override fun getSchemaDocumentation(schema: Schema): Sequence<String>
}

/** A documentation factory that is based on a table of data */
abstract class TableBasedDocumentationFactory : DocumentationFactory() {
    open val delimiter = "|"

    /** Gets the headers for the documentation factory */
    abstract fun getHeaders(): Array<String>

    /** Override for the  */
    abstract override fun getSchemaDocumentation(schema: Schema): Sequence<List<String>>
}