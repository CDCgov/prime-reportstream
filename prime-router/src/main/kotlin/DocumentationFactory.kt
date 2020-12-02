package gov.cdc.prime.router

import kotlin.text.StringBuilder

// a singleton instance to let us build documentation off of a schema or off of an element
object DocumentationFactory {
    // will generate a documentation string based on markdown that can then be presented
    // to end users or be converted into HTML if we want to be fancy
    fun getElementDocumentation(element: Element) : String {
        return """
            Name: ${element.name}
            Type: ${element.type?.name}
            Format: ${element.format ?: ""}
        """.trimIndent()
    }

    fun getSchemaDocumentation(schema: Schema) : String {
        val sb = StringBuilder()

        sb.appendLine("""
            Schema: ${schema.name}
        """.trimIndent())
        sb.appendLine("---")

        schema.elements.forEach { element ->
            sb.appendLine(getElementDocumentation(element))
        }

        return sb.toString()
    }
}