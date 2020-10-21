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
) {
    // The Element is the fundamental data element of the schema.
    // It overlays many different concerns
    data class Element(
        val name: String,
        val type: Type = Type.TEXT,
        val format: String = "",
        val codeSystem: CodeSystem = CodeSystem.NONE,
        val code: String = "",
        val optional: Boolean = true,
        val pii: Boolean = false,
        val phi: Boolean = false,
        val default: String? = "",
        val hl7_field: String? = null,
        val hl7_operation: String? = null,
        val hl7_validation: String? = null,
        val hl7_template: String? = null,
        val csv_field: String? = null,
    ) {
        enum class Type {
            TEXT,
            NUMBER,
            DATE,
            DURATION,
            CODED,
            ID,
            ID_DLN,
            ID_SSN,
            ADDRESS,
            POSTAL_CODE,
            PERSON_NAME,
            TELEPHONE,
            EMAIL,
        }

        enum class CodeSystem {
            NONE,
            LOINC,
            SNOMED
        }
    }

    // A mapping maps from one schema to another
    data class Mapping(
        val toSchema: Schema,
        val fromSchema: Schema,
        val useFromName: Map<String, String>,
        val useDefault: Set<String>,
        val missing: Set<String>
    )

    fun findElement(name: String): Element? {
        return elements.find { it.name == name }
    }

    fun findUsingCsvField(name: String): Element? {
        return elements.find { it.csv_field == name || it.name == name }
    }

    fun buildMapping(toSchema: Schema): Mapping {
        if (toSchema.topic != this.topic) error("Trying to match schema with different topics")
        val useFromName = HashMap<String, String>()
        val useDefault = HashSet<String>()
        val missing = HashSet<String>()
        toSchema.elements.forEach {
            val mappedName = findMatchingElement(it)
            if (mappedName != null) {
                useFromName[it.name] = mappedName
            } else {
                if (it.optional) {
                    useDefault.add(it.name)
                } else {
                    missing.add(it.name)
                }
            }
        }
        return Mapping(toSchema, this, useFromName, useDefault, missing)
    }

    private fun findMatchingElement(matchElement: Element): String? {
        // TODO: Much more can be done here
        val matchName = normalizeName(matchElement.name)
        for (element in elements) {
            if (matchName == normalizeName(element.name)) return element.name
        }
        return null
    }

    private fun normalizeName(name: String): String {
        return name.replace("_|\\s".toRegex(),"").toLowerCase()
    }

    companion object {
        val schemas: Map<String, Schema> get() = schemaStore

        private val schemaStore = HashMap<String, Schema>()
        private const val defaultCatalog = "./metadata/schemas"
        private const val schemaExtension = ".schema"
        private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

        // Load the schema catalog either from the default location or from the passed location
        fun loadSchemaCatalog(catalog: String? = null) {
            val newSchemas = HashMap<String, Schema>()
            val rootDir = File(catalog ?: defaultCatalog)

            fun addSchemasInADirectory(dirSubPath: String) {
                val schemaExtFilter = FilenameFilter { _: File, name: String -> name.endsWith(schemaExtension) }
                val dir = File(rootDir.absolutePath, dirSubPath)
                val files = dir.listFiles(schemaExtFilter) ?: return
                files.forEach {
                    val schema = mapper.readValue<Schema>(it.inputStream())
                    val fullName = if (dirSubPath.isEmpty()) schema.name else dirSubPath + "/" + schema.name
                    newSchemas[fullName] = schema
                }

                val subDirs = dir.listFiles { file -> file.isDirectory } ?: return
                subDirs.forEach {
                    addSchemasInADirectory(if (dirSubPath.isEmpty()) it.name else dirSubPath + "/" + it.name)
                }
            }

            if (!rootDir.isDirectory) error("Expected ${rootDir.absolutePath} to be a directory")
            addSchemasInADirectory("")
            loadSchemas(newSchemas)
        }

        fun loadSchemas(schemas: Map<String, Schema>) {
            schemaStore.clear()
            schemaStore.putAll(schemas)
        }
    }
}