package gov.cdc.prime.router

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.text.StringBuilder

// a singleton instance to let us build documentation off of a schema or off of an element
object DocumentationFactory {
    // will generate a documentation string based on markdown that can then be presented
    // to end users or be converted into HTML if we want to be fancy
    fun getElementDocumentation(element: Element) : String {
        val csvField = element.csvFields?.get(0)

        return """
            **Name**: ${element.name}
            **Type**: ${element.type?.name}
            **Format**: ${csvField?.format ?: ""}
        """.trimIndent()
    }

    fun getSchemaDocumentation(schema: Schema) : String {
        val sb = StringBuilder()

        sb.appendLine("""
            ###Schema: ${schema.name}
            ####Description: ${schema.description}
        """.trimIndent())
        sb.appendLine("---")
        sb.appendLine("")

        schema.elements.forEach { element ->
            sb.appendLine(getElementDocumentation(element))
        }

        return sb.toString()
    }

    fun writeDocumentationForSchema(schema: Schema, outputDir: String? = null, outputFileName: String? = null) {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val createDate = LocalDate.now().format(formatter)

        val oName = (outputFileName ?: "$schema.name") + "-$createDate.md"
        val oDir = (outputDir ?: "documentation")
        val path = Paths.get(oDir)
        if (!Files.exists(path)) {
            Files.createDirectory(path)
        }

        val outputPath = "$oDir/$oName"
        File(outputPath).writeText(getSchemaDocumentation(schema))
    }
}