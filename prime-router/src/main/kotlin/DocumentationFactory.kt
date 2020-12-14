package gov.cdc.prime.router

import java.io.File
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

        sb.appendLine(
            """
**Name**:           ${element.name}

**Type**:           ${element.type?.name}

**Format**:         ${csvField?.format ?: ""}
"""
        )

        // build the valuesets
        if (element.valueSet?.isNotEmpty() == true) {

            sb.appendLine("**Valuesets**\n")
            sb.appendLine("Code | Display")
            sb.appendLine("---- | -------")

            element.valueSetRef?.values?.forEach { vs ->
                sb.appendLine("${vs.code}|${vs.display}")
            }
            sb.appendLine("")
        }

        if (element.altValues?.isNotEmpty() == true) {
            sb.appendLine("**Alt Valuesets**")
            sb.appendLine("Code | Display")
            sb.appendLine("---- | -------")

            element.altValues.forEach { vs ->
                sb.appendLine("${vs.code}|${vs.display}")
            }
            sb.appendLine("")
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
    fun writeDocumentationForSchema(schema: Schema, outputDir: String? = null, outputFileName: String? = null) {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val createDate = LocalDate.now().format(formatter)
        // change any slashes to dashes for the file name
        val schemaName = schema.name.replace("/", "-")

        val oName = (outputFileName ?: schemaName) + "-$createDate.md"
        val oDir = (outputDir ?: "documentation")
        val path = Paths.get(oDir)
        if (!Files.exists(path)) {
            Files.createDirectory(path)
        }

        val outputPath = "$oDir/$oName"
        File(outputPath).writeText(getSchemaDocumentation(schema))
    }
}