package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Schema
import org.apache.logging.log4j.kotlin.Logging
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Wraps around the MD generator and creates HTML out of it */
object HtmlDocumentationFactory : DocumentationFactory(), Logging {
    /**
     * Given a [schema] this calls into the markdown documentation factory and gets the string value there
     * */
    override fun getSchemaDocumentation(schema: Schema): String {
        return MarkdownDocumentationFactory.getSchemaDocumentation(schema)
    }

    /**
     * Writes out the markdown to HTML
     */
    override fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String,
        outputFileName: String?,
        includeTimestamps: Boolean
    ) {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val createDate = LocalDate.now().format(formatter)
        // change any slashes to dashes for the file name
        val schemaName = canonicalizeSchemaName(schema)

        val mdText = MarkdownDocumentationFactory.getSchemaDocumentation(schema)
        val path = Paths.get(outputDir)
        if (!Files.exists(path)) {
            Files.createDirectory(path)
        }

        val htmlName = (outputFileName ?: schemaName) + if (includeTimestamps) {
            "-$createDate.html"
        } else {
            ".html"
        }
        File(outputDir, htmlName).writeText(convertMarkdownToHtml(mdText))
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