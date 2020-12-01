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
    private const val tableExtension = ".csv"
    private const val defaultMetadataDirectory = "./metadata"
    private const val schemasSubdirectory = "schemas"
    private const val valuesetsSubdirectory = "valuesets"
    private const val tableSubdirectory = "tables"
    private const val organizationsList = "organizations.yml"

    private var schemas = mapOf<String, Schema>()
    private var mappers = listOf(
        MiddleInitialMapper(),
        UseMapper(),
        IfPresentMapper(),
    )
    private var valueSets = mapOf<String, ValueSet>()
    private var organizationStore: List<Organization> = ArrayList()
    private var organizationServiceStore: List<OrganizationService> = ArrayList()
    private var organizationClientStore: List<OrganizationClient> = ArrayList()
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    fun loadAll(metadataPath: String? = null) {
        val metadataDir = File(metadataPath ?: defaultMetadataDirectory)
        if (!metadataDir.isDirectory) error("Expected metadata directory")
        loadSchemaCatalog(metadataDir.toPath().resolve(schemasSubdirectory).toString())
        loadValueSetCatalog(metadataDir.toPath().resolve(valuesetsSubdirectory).toString())
        loadOrganizationList(metadataDir.toPath().resolve(organizationsList).toString())
        loadLookupTables(metadataDir.toPath().resolve(tableSubdirectory).toString())
    }

    /*
     * Schema
     */

    // Load the schema catalog either from the default location or from the passed location
    fun loadSchemaCatalog(catalog: String) {
        val catalogDir = File(catalog)
        if (!catalogDir.isDirectory) error("Expected ${catalogDir.absolutePath} to be a directory")
        try {
            loadSchemas(readAllSchemas(catalogDir, ""))
        } catch (e: Exception) {
            throw Exception("Error loading schema catalog: $catalog", e)
        }
    }

    fun loadSchemas(schemas: List<Schema>) {
        this.schemas = extendSchemas(schemas).map { normalizeSchemaName(it.name) to it }.toMap()
    }

    fun findSchema(name: String): Schema? {
        return schemas[normalizeSchemaName(name)]
    }

    private fun readSchema(dirRelPath: String, file: File): Schema {
        try {
            val fromSchemaFile = mapper.readValue<Schema>(file.inputStream())
            val schemaName = normalizeSchemaName(
                if (dirRelPath.isEmpty()) fromSchemaFile.name else dirRelPath + "/" + fromSchemaFile.name
            )
            return fromSchemaFile.copy(name = schemaName)
        } catch (e: Exception) {
            throw Exception("Error parsing '${file.name}'", e)
        }
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
            val expandedElements: List<Element> = schema.elements.map { expandElement(schemas, it) }
            schema.copy(elements = expandedElements)
        }
    }

    private fun expandElement(schemas: List<Schema>, element: Element): Element {
        return if (element.name.contains('.')) {
            // names can have the form <schema>.<name> which means we should copy the values of the referenced element
            val splitName = element.name.split('.')
            if (splitName.size != 2) error("${element.name} is not a valid base name")
            val baseSchemaName = normalizeSchemaName(splitName[0])
            // Find the element in the schemas list
            val basedElement = schemas.find { it.name == baseSchemaName }?.findElement(splitName[1])
                ?: error("${element.name} does not exists in $baseSchemaName")
            element.extendFrom(basedElement)
        } else {
            element
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
        try {
            loadValueSets(readAllValueSets(catalogDir))
        } catch (e: Exception) {
            throw Exception("Error loading $catalog", e)
        }
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
        try {
            return mapper.readValue(file.inputStream())
        } catch (e: Exception) {
            throw Exception("Error reading '${file.name}'", e)
        }
    }

    private fun normalizeValueSetName(name: String): String {
        return name.toLowerCase()
    }

    /*
     * Organizations
     */

    val organizations get() = this.organizationStore
    val organizationClients get() = this.organizationClientStore
    val organizationServices get() = this.organizationServiceStore

    fun loadOrganizationList(filePath: String) {
        try {
            loadOrganizationList(File(filePath).inputStream())
        } catch (e: Exception) {
            throw Exception("Error loading: $filePath", e)
        }
    }

    fun loadOrganizationList(organizationStream: InputStream) {
        val list = mapper.readValue<List<Organization>>(organizationStream)
        loadOrganizations(list)
    }

    fun loadOrganizations(organizations: List<Organization>) {
        this.organizationStore = organizations
        this.organizationClientStore = organizations.flatMap { it.clients }
        this.organizationServiceStore = organizations.flatMap { it.services }
    }

    fun findOrganization(name: String): Organization? {
        if (name.isBlank()) return null
        return this.organizations.first {
            it.name.equals(name, ignoreCase = true)
        }
    }

    fun findService(name: String): OrganizationService? {
        if (name.isBlank()) return null
        val (orgName, clientName) = parseName(name)
        return findOrganization(orgName)?.services?.first {
            it.name.equals(clientName, ignoreCase = true)
        }
    }

    fun findClient(name: String): OrganizationClient? {
        if (name.isBlank()) return null
        val (orgName, clientName) = parseName(name)
        return findOrganization(orgName)?.clients?.first {
            it.name.equals(clientName, ignoreCase = true)
        }
    }

    private fun parseName(name: String): Pair<String, String> {
        val subNames = name.split('.')
        return when (subNames.size) {
            2 -> Pair(subNames[0], subNames[1])
            1 -> Pair(subNames[0], "default")
            else -> error("too many sub-names")
        }
    }

    /*
     * Lookup Tables
     */
    var lookupTableStore = mapOf<String, LookupTable>()
    val lookupTables get() = lookupTableStore

    fun loadLookupTables(filePath: String) {
        val catalogDir = File(filePath)
        if (!catalogDir.isDirectory) error("Expected ${catalogDir.absolutePath} to be a directory")
        try {
            readAllTables(catalogDir) { tableName: String, table: LookupTable ->
                addLookupTable(tableName, table)
            }
        } catch (e: Exception) {
            throw Exception("Error loading tables in '$filePath'", e)
        }
    }

    fun addLookupTable(name: String, table: LookupTable) {
        lookupTableStore = lookupTableStore.plus(name to table)
    }

    fun addLookupTable(name: String, tableStream: InputStream) {
        val table = LookupTable.read(tableStream)
        addLookupTable(name, table)
    }

    private fun readAllTables(catalogDir: File, block: (String, LookupTable) -> Unit) {
        val extFilter = FilenameFilter { _, name -> name.endsWith(tableExtension) }
        val files = File(catalogDir.absolutePath).listFiles(extFilter) ?: emptyArray()
        files.forEach { file ->
            val table = LookupTable.read(file.inputStream())
            val name = file.nameWithoutExtension
            block(name, table)
        }
    }
}

