package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FilenameFilter
import java.io.InputStream

/**
 * The metadata object is a singleton representing all metadata loaded for MappableTables
 */
object Metadata {
    private const val schemaExtension = ".schema"
    private const val valueSetExtension = ".valuesets"
    private const val defaultMetadataDirectory = "./metadata"
    private const val schemasSubdirectory = "schemas"
    private const val valuesetsSubdirectory = "valuesets"
    private const val receiversList = "receivers.yml"

    private var schemas = mapOf<String, Schema>()
    private var mappers = listOf(
        MiddleInitialMapper(),
        UseMapper(),
        IfPresentMapper(),
    )
    private var valueSets = mapOf<String, ValueSet>()
    private var receiversStore: List<Receiver> = ArrayList()
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    fun loadAll(metadataPath: String? = null) {
        val metadataDir = File(metadataPath ?: defaultMetadataDirectory)
        if (!metadataDir.isDirectory) error("Expected metadata directory")
        loadSchemaCatalog(metadataDir.toPath().resolve(schemasSubdirectory).toString())
        loadValueSetCatalog(metadataDir.toPath().resolve(valuesetsSubdirectory).toString())
        loadReceiversList(metadataDir.toPath().resolve(receiversList).toString())
    }

    /*
     * Schema
     */

    // Load the schema catalog either from the default location or from the passed location
    fun loadSchemaCatalog(catalog: String) {
        val catalogDir = File(catalog)
        if (!catalogDir.isDirectory) error("Expected ${catalogDir.absolutePath} to be a directory")
        loadSchemas(readAllSchemas(catalogDir, ""))
    }

    fun loadSchemas(schemas: List<Schema>) {
        this.schemas = extendSchemas(schemas).map { normalizeSchemaName(it.name) to it }.toMap()
    }

    fun findSchema(name: String): Schema? {
        return schemas[normalizeSchemaName(name)]
    }

    private fun readSchema(dirRelPath: String, file: File): Schema {
        val fromSchemaFile = mapper.readValue<Schema>(file.inputStream())
        val schemaName = normalizeSchemaName(
            if (dirRelPath.isEmpty()) fromSchemaFile.name else dirRelPath + "/" + fromSchemaFile.name
        )
        return fromSchemaFile.copy(name = schemaName)
    }

    private fun readAllSchemas(catalogDir: File, dirRelPath: String): List<Schema> {
        val outputSchemas = mutableListOf<Schema>()

        // read the .schema files in the director
        val schemaExtFilter = FilenameFilter { _: File, name: String -> name.endsWith(schemaExtension) }
        val dir = File(catalogDir.absolutePath, dirRelPath)
        val files = dir.listFiles(schemaExtFilter) ?: emptyArray()
        outputSchemas.addAll(files.map { readSchema(dirRelPath, it) })

        // read the schemas in the sub-directory
        val subDirs = dir.listFiles { file -> file.isDirectory } ?: emptyArray()
        subDirs.forEach {
            val childPath = if (dirRelPath.isEmpty()) it.name else dirRelPath + "/" + it.name
            outputSchemas.addAll(readAllSchemas(catalogDir, childPath))
        }

        return outputSchemas
    }

    private fun extendSchemas(schemas: List<Schema>): List<Schema> {
        return schemas.map { schema ->
            val expandedElements: List<Element> = schema.elements.map { element ->
                if (element.name.contains('.')) {
                    val splitName = element.name.split('.')
                    if (splitName.size != 2) error("${element.name} is not a valid base name")
                    val baseSchemaName = normalizeSchemaName(splitName[0])
                    val basedElement = schemas.find { it.name == baseSchemaName }?.findElement(splitName[1])
                        ?: error("${element.name} does not exists in $baseSchemaName")
                    element.extendFrom(basedElement)
                } else {
                    element
                }
            }
            schema.copy(elements = expandedElements)
        }
    }

    private fun normalizeSchemaName(name: String): String {
        return name.toLowerCase()
    }

    /*
     * Mappers
     */

    fun findMapper(name: String): Mapper? {
        return mappers.find { it.name.equals(name, ignoreCase = true) }
    }

    /*
     * ValueSet
     */

    fun loadValueSetCatalog(catalog: String) {
        val catalogDir = File(catalog)
        if (!catalogDir.isDirectory) error("Expected ${catalogDir.absolutePath} to be a directory")
        loadValueSets(readAllValueSets(catalogDir))
    }

    fun loadValueSets(sets: List<ValueSet>) {
        this.valueSets = sets.map { normalizeValueSetName(it.name) to it }.toMap()
    }

    fun findValueSet(name: String): ValueSet? {
        return valueSets[normalizeValueSetName(name)]
    }

    private fun readAllValueSets(catalogDir: File): List<ValueSet> {
        // read the .valueset files in the director
        val valueSetExtFilter = FilenameFilter { _, name -> name.endsWith(valueSetExtension) }
        val files = File(catalogDir.absolutePath).listFiles(valueSetExtFilter) ?: emptyArray()
        return files.flatMap { readValueSets(it) }
    }

    private fun readValueSets(file: File): List<ValueSet> {
        return mapper.readValue(file.inputStream())
    }

    private fun normalizeValueSetName(name: String): String {
        return name.toLowerCase()
    }

    /*
     * Receiver
     */

    private data class ReceiversList(
        val receivers: List<Receiver>
    )

    val receivers get() = receiversStore

    fun loadReceiversList(filePath: String) {
        loadReceiversList(File(filePath).inputStream())
    }

    fun loadReceiversList(receiversStream: InputStream) {
        val loadingStream = receiversStream
        val receiversList = mapper.readValue<ReceiversList>(loadingStream)
        loadReceivers(receiversList.receivers)
    }

    fun loadReceivers(receivers: List<Receiver>) {
        receiversStore = receivers
    }

    fun findReceiver(name: String, topic: String): Receiver? {
        return receiversStore.first {
            it.name.equals(name, ignoreCase = true) && it.topic.equals(topic, ignoreCase = true)
        }
    }
}