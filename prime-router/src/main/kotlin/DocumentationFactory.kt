package gov.cdc.prime.router

import org.apache.logging.log4j.kotlin.Logging
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** a singleton instance to let us build documentation off of a schema or off of an element */
object DocumentationFactory : Logging {
    // will generate a documentation string based on markdown that can then be presented
    // to end users or be converted into HTML if we want to be fancy
    private const val hl7DocumentationUrl = "https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/"

    /** converts an HL7 field to a URL at Caristix */
    private fun convertHl7FieldToUrl(segmentName: String?): String {
        if (segmentName.isNullOrEmpty()) return ""
        val formattedSegment = segmentName.replace("-", ".")
        return "[$segmentName]($hl7DocumentationUrl$formattedSegment)"
    }

    /** gets the documentation for an element */
    fun getElementDocumentation(element: Element): String {
        val csvField = if (element.csvFields?.isNotEmpty() == true) element.csvFields[0] else null
        val sb = StringBuilder()
        val displayName = csvField?.name ?: element.name
        val hl7Fields = element.hl7OutputFields?.plus(element.hl7Field)

        // our top-level element data points
        sb.appendLine("") // start with a blank line at the top
        appendLabelAndData(sb, "Name", displayName)
        appendLabelAndData(sb, "ReportStream Internal Name", element.name)
        appendLabelAndData(sb, "Type", element.type?.name)
        appendLabelAndData(sb, "PII", if (element.pii == true) "Yes" else "No")

        if (element.type?.name == "CODE") {
            when (csvField?.format) {
                "\$display",
                "\$alt" ->
                    appendLabelAndData(sb, "Format", "use value found in the Display column")
                else ->
                    appendLabelAndData(sb, "Format", "use value found in the Code column")
            }
        } else {
            appendLabelAndData(sb, "Format", csvField?.format)
        }

        appendLabelAndData(sb, "Default Value", element.default)
        if (hl7Fields?.isNullOrEmpty() == false) {
            appendLabelAndList(sb, "HL7 Fields", hl7Fields.toSet().map { convertHl7FieldToUrl(it) })
        }
        if (element.hl7Field == "AOE") appendLabelAndData(sb, "LOINC Code", element.hl7AOEQuestion)
        appendLabelAndData(
            sb, "Cardinality",
            element.cardinality?.toFormatted() ?: Element.Cardinality.ZERO_OR_ONE.toFormatted()
        )

        // output the reference url
        if (element.referenceUrl?.isNotBlank() == true) {
            appendLabelAndUrl(sb, "Reference URL", element.referenceUrl)
        }

        // build the valuesets
        if (element.valueSetRef != null) {
            appendValueSetTable(sb, "Value Sets", element)
        }

        if (element.altValues?.isNotEmpty() == true) {
            appendValueSetTable(sb, "Alt Value Sets", element)
        }

        if (element.table?.isNotEmpty() == true) {
            appendLabelAndData(sb, "Table", element.table)
            appendLabelAndData(sb, "Table Column", element.tableColumn)
        }

        if (element.documentation?.isNotEmpty() == true) {
            sb.appendLine(
                """**Documentation**:

${element.documentation}
"""
            )
        }

        // output a horizontal line
        sb.appendLine("---")

        return sb.toString()
    }

    // gets the documentation
    fun getSchemaDocumentation(schema: Schema): String {
        val sb = StringBuilder()
        var schemaTrackingName =
            if (schema.trackingElement.isNullOrEmpty()) {
                logger.warn("Schema ${schema.name}: TrackingElement is empty")
                "none"
            } else {
                var trackingName = schema.findElement(schema.trackingElement)?.csvFields?.get(0)?.name
                if (trackingName == null)
                    "(${schema.trackingElement})"
                else
                    "$trackingName (${schema.trackingElement})"
            }
        var schemabaseOn = if (schema.basedOn.isNullOrBlank()) "none" else "[${schema.basedOn}](./${schema.basedOn}.md)"
        var extendName =
            if (schema.extends.isNullOrBlank()) {
                "none"
            } else {
                schema.extends.replace('/', '-')
            }
        var schemaExtends = if (schema.extends.isNullOrBlank()) "none" else "[${schema.extends}](./$extendName.md)"
        var schemaDescription = if (schema.description.isNullOrBlank()) "none" else "${schema.description}"

        sb.appendLine(
            """
### Schema: ${schema.name}
### Topic: ${schema.topic}
### Tracking Element: $schemaTrackingName
### Base On: $schemabaseOn
### Extends: $schemaExtends
#### Description: $schemaDescription

---"""
        )

        schema.elements.filter { !it.csvFields.isNullOrEmpty() }.sortedBy { it -> it.name }.forEach { element ->
            sb.append(getElementDocumentation(element))
        }
        schema.elements.filter { it.csvFields.isNullOrEmpty() }.sortedBy { it -> it.name }.forEach { element ->
            sb.append(getElementDocumentation(element))
        }

        return sb.toString()
    }

    /**
     * Write markdown documentation for a schema to a file.
     *
     * @param schema The schema to document.
     * @param outputDir The directory to write the file to. If this directory
     *        doesn't exist, it will be created.
     * @param outputFileName Name of the file to write in `outputDir`. If not
     *        set, the file will be named after the schema.
     * @param includeTimestamps Append the current date to the filename.
     */
    fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String = ".",
        outputFileName: String? = null,
        includeTimestamps: Boolean = false,
        generateMarkupFile: Boolean = true,
        generateHtmlFile: Boolean = false
    ) {
        // Why are you even calling this
        if (!generateMarkupFile && !generateHtmlFile) return

        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val createDate = LocalDate.now().format(formatter)
        // change any slashes to dashes for the file name
        val schemaName = schema.name.replace("/", "-")

        val mdText = getSchemaDocumentation(schema)
        val path = Paths.get(outputDir)
        if (!Files.exists(path)) {
            Files.createDirectory(path)
        }

        // Generate the markup file
        if (generateMarkupFile) {
            val markupName = (outputFileName ?: schemaName) + if (includeTimestamps) {
                "-$createDate.md"
            } else {
                ".md"
            }
            File(outputDir, markupName).writeText(mdText)
        }

        // Generate the HTML file
        if (generateHtmlFile) {
            val htmlName = (outputFileName ?: schemaName) + if (includeTimestamps) {
                "-$createDate.html"
            } else {
                ".html"
            }
            File(outputDir, htmlName).writeText(convertMarkdownToHtml(mdText))
        }
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

    private fun appendLabelAndData(appendable: Appendable, label: String, value: Any?) {
        if (value != null) {
            appendable.appendLine(
                "**$label**: $value\n"
            )
        }
    }

    private fun appendLabelAndUrl(appendable: Appendable, label: String, url: String, linkText: String? = null) {
        if (url.isNotBlank()) {
            appendable.appendLine(
                """
**$label**:
[${linkText ?: url}]($url) 
"""
            )
        }
    }

    private fun appendLabelAndList(appendable: Appendable, label: String, list: List<String>) {
        appendable.appendLine("**$label**\n")
        list.sortedBy { it }.forEach {
            if (it.trim().isEmpty()) return@forEach
            appendable.appendLine("- $it")
        }
        appendable.appendLine("")
    }

    private fun appendValueSetTable(appendable: Appendable, label: String, element: Element) {
        val system = element.valueSetRef?.system ?: ValueSet.SetSystem.NULLFL
        val values: List<ValueSet.Value>? = when (label) {
            "Value Sets" -> element.valueSetRef?.values
            "Alt Value Sets" -> element.altValues
            else -> emptyList()
        }

        if (values?.isNotEmpty() == true) {

            appendable.appendLine("**$label**\n")
            appendable.appendLine("Code | Display | System")
            appendable.appendLine("---- | ------- | ------")

            values.forEach { vs ->
                val code = if (vs.code == ">") "&#62;" else vs.code // This to solve the markdown blockquote '>'
                appendable.appendLine("$code|${vs.display}|${vs.system ?: system}")
            }
            appendable.appendLine("")
        }
    }
}