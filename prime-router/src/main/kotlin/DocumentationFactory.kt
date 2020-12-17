package gov.cdc.prime.router

import java.io.File
import java.lang.Appendable
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.text.StringBuilder

// a singleton instance to let us build documentation off of a schema or off of an element
object DocumentationFactory {
    // will generate a documentation string based on markdown that can then be presented
    // to end users or be converted into HTML if we want to be fancy
    fun getElementDocumentation(element: Element): String {
        val csvField = element.csvFields?.get(0)
        val sb = StringBuilder()
        val displayName = csvField?.name ?: element.name

        // our top-level element data points
        sb.appendLine("") // start with a blank line at the top 
        appendLabelAndData(sb, "Name", displayName)
        appendLabelAndData(sb, "Type", element.type?.name)
        appendLabelAndData(sb, "Format", csvField?.format)
        appendLabelAndData(sb, "HL7 Field", element.hl7Field)

        // output the reference url
        if (element.referenceUrl?.isNotBlank() == true) {
            appendLabelAndUrl(sb, "Reference URL", element.referenceUrl)
        }

        // build the valuesets
        if (element.valueSet?.isNotEmpty() == true) {
            val valueSet = Metadata.findValueSet(element.valueSet)
            appendValueSetTable(sb, "Value Sets", valueSet?.values)
        }

        if (element.altValues?.isNotEmpty() == true) {
            appendValueSetTable(sb, "Alt Value Sets", element.altValues)
        }

        if (element.table?.isNotEmpty() == true) {
            appendLabelAndData(sb, "Table", element.table)
            appendLabelAndData(sb, "Table Column")
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

        sb.appendLine(
            """
### Schema:         ${schema.name}
#### Description:   ${schema.description}

---"""
        )

        schema.elements.forEach { element ->
            sb.append(getElementDocumentation(element))
        }

        return sb.toString()
    }

    // write all the documentation for a schema
    fun writeDocumentationForSchema(
        schema: Schema,
        outputDir: String? = null,
        outputFileName: String? = null,
        includeTimestamps: Boolean = false
    ) {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val createDate = LocalDate.now().format(formatter)
        // change any slashes to dashes for the file name
        val schemaName = schema.name.replace("/", "-")

        val oName = (outputFileName ?: schemaName) + if (includeTimestamps) {
            "-$createDate.md"
        } else {
            ".md"
        }

        val oDir = (outputDir ?: "documentation")
        val path = Paths.get(oDir)
        if (!Files.exists(path)) {
            Files.createDirectory(path)
        }

        val outputPath = "$oDir/$oName"
        File(outputPath).writeText(getSchemaDocumentation(schema))
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

    private fun appendValueSetTable(appendable: Appendable, label: String, values: Collection<ValueSet.Value>?) {
        if (values?.isNotEmpty() == true) {

            appendable.appendLine("**$label**\n")
            appendable.appendLine("Code | Display")
            appendable.appendLine("---- | -------")

            values.forEach { vs ->
                appendable.appendLine("${vs.code}|${vs.display}")
            }
            appendable.appendLine("")
        }
    }
}