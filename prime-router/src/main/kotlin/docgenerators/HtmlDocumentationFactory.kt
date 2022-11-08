package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Schema
import org.apache.logging.log4j.kotlin.Logging
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.io.File

/** Wraps around the MD generator and creates HTML out of it */
object HtmlDocumentationFactory : StringBasedDocumentationFactory(), Logging {
    /** Returns the file extension we use for this type of documentation */
    override val fileExtension: String
        get() = "html"

    /**
     * Given a [schema] this calls into the markdown documentation factory and gets the string value there
     * */
    override fun getSchemaDocumentation(schema: Schema) = MarkdownDocumentationFactory.getSchemaDocumentation(schema)

    /**
     * Writes out the markdown to HTML
     */
    override fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String,
        outputFileName: String?,
        includeTimestamps: Boolean
    ) {
        // get the schema documentation as a sequence and then write it out to a single string
        val mdText = getSchemaDocumentation(schema).joinToString(separator = "")

        File(
            ensureOutputDirectory(outputDir),
            getOutputFileName(outputFileName, schema, includeTimestamps, this.fileExtension)
        ).writeText(convertMarkdownToHtml(mdText))
    }

    /**
     * Convert [markdown] text to HTML.
     * @return HTML text
     */
    private fun convertMarkdownToHtml(markdown: String): String {
        val parser = Parser.builder().build()
        val document = parser.parse(markdown)
        val renderer = HtmlRenderer.builder().build()
        return renderer.render(document)
    }
}