package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FilenameFilter

// A PRIME schema contains a collection of data elements, mappings, validations and standard algorithms
// needed to translate data to the form that a public health authority needs
//
data class Schema(
    val name: String, // Name should include version
    val topic: String,
    val elements: List<Element> = emptyList(),
    val trackingElement: String? = null, // the element to use for tracking this test
    val description: String? = null,
) {
    // A mapping maps from one schema to another
    data class Mapping(
        val toSchema: Schema,
        val fromSchema: Schema,
        val useDirectly: Map<String, String>,
        val useTranslator: Map<String, Translator>,
        val useDefault: Set<String>,
        val missing: Set<String>
    )

    fun findElement(name: String): Element? {
        return elements.find { it.name.compareTo(name, ignoreCase = true) == 0 }
    }

    fun findUsingCsvField(name: String): Element? {
        return elements.find { it.csvField.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
    }

    fun buildMapping(toSchema: Schema): Mapping {
        if (toSchema.topic != this.topic) error("Trying to match schema with different topics")

        val useDirectly = mutableMapOf<String, String>()
        val useTranslator = mutableMapOf<String, Translator>()
        val useDefault = mutableSetOf<String>()
        val missing = mutableSetOf<String>()

        toSchema.elements.forEach { toElement ->
            findMatchingElement(toElement)?.let {
                useDirectly[toElement.name] = it
                return@forEach
            }
            findMatchingTranslator(toElement)?.let {
                useTranslator[toElement.name] = it
                return@forEach
            }
            if (toElement.required == true) {
                missing.add(toElement.name)
            } else {
                useDefault.add(toElement.name)
            }
        }
        return Mapping(toSchema, this, useDirectly, useTranslator, useDefault, missing)
    }

    private fun findMatchingElement(matchElement: Element): String? {
        // TODO: Much more can be done here
        val matchName = normalizeElementName(matchElement.name)
        for ((name) in elements) {
            if (matchName.equals(normalizeElementName(name), ignoreCase = true)) return name
        }
        return null
    }

    private fun findMatchingTranslator(matchElement: Element): Translator? {
        val candidates = translators[this.topic]?.get(matchElement.name) ?: return null
        return candidates.find { translator ->
            translator.fromElements.find { findElement(it) == null } == null
        }
    }

    companion object {
        var schemas = mapOf<String, Schema>()
        var translators: Map<String, Map<String, List<Translator>>> = mapOf(
            "covid-19" to mapOf("standard.Patient_middle_initial" to listOf(MITranslator()))
        )

        private const val defaultCatalog = "./metadata/schemas"
        private const val schemaExtension = ".schema"
        private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

        // Load the schema catalog either from the default location or from the passed location
        fun loadSchemaCatalog(catalog: String? = null) {
            val catalogDir = File(catalog ?: defaultCatalog)
            if (!catalogDir.isDirectory) error("Expected ${catalogDir.absolutePath} to be a directory")
            loadSchemas(readAllSchemas(catalogDir, ""))
        }

        private fun readSchema(dirRelPath: String, file: File): Pair<String, Schema> {
            val fromSchemaFile = mapper.readValue<Schema>(file.inputStream())
            val catalogName =
                if (dirRelPath.isEmpty()) fromSchemaFile.name else dirRelPath + "/" + fromSchemaFile.name
            return Pair(catalogName.toLowerCase(), fromSchemaFile)
        }

        private fun readAllSchemas(catalogDir: File, dirRelPath: String): Map<String, Schema> {
            val outputSchemas = mutableMapOf<String, Schema>()

            // read the .schema files in the director
            val schemaExtFilter = FilenameFilter { _: File, name: String -> name.endsWith(schemaExtension) }
            val dir = File(catalogDir.absolutePath, dirRelPath)
            val files = dir.listFiles(schemaExtFilter) ?: emptyArray()
            outputSchemas.putAll(files.map { readSchema(dirRelPath, it) }.toMap())

            // read the schemas in the sub-directory
            val subDirs = dir.listFiles { file -> file.isDirectory } ?: emptyArray()
            subDirs.forEach {
                val childPath = if (dirRelPath.isEmpty()) it.name else dirRelPath + "/" + it.name
                outputSchemas.putAll(readAllSchemas(catalogDir, childPath))
            }

            return outputSchemas
        }

        fun loadSchemas(schemas: Map<String, Schema>) {
            this.schemas = extendSchemas(schemas)
        }

        private fun extendSchemas(schemas: Map<String, Schema>): Map<String, Schema> {
            return schemas.mapValues { (name, schema) ->
                val expandedElements: List<Element> = schema.elements.map { element ->
                    if (element.name.contains('.')) {
                        val splitName = element.name.split('.')
                        if (splitName.size != 2) error("${element.name} is not a valid base name")
                        val baseSchemaName = normalizeSchemaName(splitName[0])
                        val basedElement = schemas[baseSchemaName]?.findElement(splitName[1])
                            ?: error("${element.name} does not exists in $name")
                        element.extendFrom(basedElement)
                    } else {
                        element
                    }
                }
                schema.copy(elements = expandedElements)
            }
        }

        private fun normalizeElementName(name: String): String {
            return name.replace("_|\\s".toRegex(), "").toLowerCase()
        }

        private fun normalizeSchemaName(name: String): String {
            return name.toLowerCase()
        }
    }
}