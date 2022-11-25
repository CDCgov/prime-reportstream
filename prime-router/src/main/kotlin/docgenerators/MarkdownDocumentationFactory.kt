package gov.cdc.prime.router.docgenerators

import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.ValueSet
import org.apache.logging.log4j.kotlin.Logging
import java.io.File

/** a singleton instance to let us build documentation off of a schema or off of an element */
object MarkdownDocumentationFactory : StringBasedDocumentationFactory(), Logging {
    // will generate a documentation string based on markdown that can then be presented
    // to end users or be converted into HTML if we want to be fancy
    private const val hl7DocumentationUrl = "https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/"

    /** The extension for this file type */
    override val fileExtension: String
        get() = "md"

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
        if (hl7Fields?.isEmpty() == false) {
            appendLabelAndList(sb, "HL7 Fields", hl7Fields.toSet().map { convertHl7FieldToUrl(it) })
        }
        if (element.hl7Field == "AOE") appendLabelAndData(sb, "LOINC Code", element.hl7AOEQuestion)
        appendLabelAndData(
            sb,
            "Cardinality",
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
    override fun getSchemaDocumentation(schema: Schema) = sequence {
        val schemaTrackingName =
            if (schema.trackingElement.isNullOrEmpty()) {
                logger.warn("Schema ${schema.name}: TrackingElement is empty")
                "none"
            } else {
                val trackingName = schema.findElement(schema.trackingElement)?.csvFields?.get(0)?.name
                if (trackingName == null)
                    "(${schema.trackingElement})"
                else
                    "$trackingName (${schema.trackingElement})"
            }
        val schemaBasedOn = if (schema.basedOn.isNullOrBlank())
            "none"
        else
            "[${schema.basedOn}](./${schema.basedOn}.md)"
        val extendName =
            if (schema.extends.isNullOrBlank()) {
                "none"
            } else {
                schema.extends.replace('/', '-')
            }
        val schemaExtends = if (schema.extends.isNullOrBlank()) "none" else "[${schema.extends}](./$extendName.md)"
        val schemaDescription = if (schema.description.isNullOrBlank()) "none" else "${schema.description}"

        yield(
            """
### Schema: ${schema.name}
### Topic: ${schema.topic.json_val}
### Tracking Element: $schemaTrackingName
### Base On: $schemaBasedOn
### Extends: $schemaExtends
#### Description: $schemaDescription

---""" + "\n"
        )

        schema.elements.filter { !it.csvFields.isNullOrEmpty() }.sortedBy { it -> it.name }.forEach { element ->
            yield(getElementDocumentation(element))
        }
        schema.elements.filter { it.csvFields.isNullOrEmpty() }.sortedBy { it -> it.name }.forEach { element ->
            yield(getElementDocumentation(element))
        }
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
    override fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String,
        outputFileName: String?,
        includeTimestamps: Boolean
    ) {
        // change any slashes to dashes for the file name
        val sb = StringBuilder()
        getSchemaDocumentation(schema).forEach {
            sb.append(it)
        }

        // Generate the markup file
        File(
            ensureOutputDirectory(outputDir),
            getOutputFileName(outputFileName, schema, includeTimestamps, this.fileExtension)
        ).writeText(sb.toString())
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