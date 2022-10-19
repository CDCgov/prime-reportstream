package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Schema

abstract class DocumentationFactory {
    abstract fun getSchemaDocumentation(schema: Schema): String

    abstract fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String = ".",
        outputFileName: String? = null,
        includeTimestamps: Boolean = false
    )

    fun canonicalizeSchemaName(schema: Schema) = schema.name.replace("/", "-")
}