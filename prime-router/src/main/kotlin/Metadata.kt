package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FilenameFilter
import java.io.InputStream

/**
 * A metadata object contains all the metadata including schemas, tables, valuesets, and organizations.
 */
class Metadata {
    private var schemaStore = mapOf<String, Schema>()
    private var mappers = listOf(
        MiddleInitialMapper(),
        UseMapper(),
        IfPresentMapper(),
        LookupMapper(),
        ConcatenateMapper(),
        Obx17Mapper(),
        Obx17TypeMapper(),
    )

    private var jurisdictionalFilters = listOf(
        FilterByCounty(),
        Matches(),
        DoesNotMatch(),
    )
    private var valueSets = mapOf<String, ValueSet>()
    private var organizationStore: List<Organization> = ArrayList()
    private var organizationServiceStore: List<OrganizationService> = ArrayList()
    private var organizationClientStore: List<OrganizationClient> = ArrayList()
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    /**
     * Load all parts of the metadata catalog from a directory and its sub-directories
     */
    constructor(metadataPath: String, orgExt: String? = null) {
        val organizationsFilename = "$organizationsBaseName${orgExt ?: ""}.yml"
        val metadataDir = File(metadataPath)
        if (!metadataDir.isDirectory) error("Expected metadata directory")
        loadValueSetCatalog(metadataDir.toPath().resolve(valuesetsSubdirectory).toString())
        loadOrganizations(metadataDir.toPath().resolve(organizationsFilename).toString())
        loadLookupTables(metadataDir.toPath().resolve(tableSubdirectory).toString())
        loadSchemaCatalog(metadataDir.toPath().resolve(schemasSubdirectory).toString())
    }

    /**
     * Useful for test versions of the metadata catalog
     */
    constructor(
        schema: Schema? = null,
        valueSet: ValueSet? = null,
        tableName: String? = null,
        table: LookupTable? = null,
        organization: Organization? = null,
    ) {
        valueSet?.let { loadValueSets(it) }
        table?.let { loadLookupTable(tableName ?: "", it) }
        organization?.let { loadOrganizations(it) }
        schema?.let { loadSchemas(it) }
    }

    /*
     * Schema
     */
    val schemas: Collection<Schema> get() = schemaStore.values

    // Load the schema catalog either from the default location or from the passed location
    fun loadSchemaCatalog(catalog: String): Metadata {
        val catalogDir = File(catalog)
        if (!catalogDir.isDirectory) error("Expected ${catalogDir.absolutePath} to be a directory")
        try {
            return loadSchemaList(readAllSchemas(catalogDir, ""))
        } catch (e: Exception) {
            throw Exception("Error loading schema catalog: $catalog", e)
        }
    }

    fun loadSchemas(vararg schemas: Schema): Metadata {
        return loadSchemaList(schemas.toList())
    }

    fun loadSchemaList(schemas: List<Schema>): Metadata {
        this.schemaStore = fixupSchemas(schemas)
            .map { normalizeSchemaName(it.name) to it }
            .toMap()
        return this
    }

    fun findSchema(name: String): Schema? {
        return schemaStore[normalizeSchemaName(name)]
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

    private fun fixupSchemas(schemas: List<Schema>): List<Schema> {
        return schemas.map { schema ->
            // find the base schema
            val extendsSchema = schema.extends?.let { extendSchemaName ->
                schemas.find {
                    normalizeSchemaName(it.name) == normalizeSchemaName(extendSchemaName)
                } ?: error("extends schema does not exist for '${schema.name}'")
            }
            val basedOnSchema = schema.basedOn?.let { basedOnSchemaName ->
                schemas.find {
                    normalizeSchemaName(it.name) == normalizeSchemaName(basedOnSchemaName)
                } ?: error("basedOn schema does not exist for '${schema.name}'")
            }
            val baseSchema = extendsSchema ?: basedOnSchema

            // Inherit properties from the base schema
            var schemaElements = schema.elements.map { element ->
                baseSchema?.findElement(element.name)
                    ?.let { fixupElement(element, it) }
                    ?: fixupElement(element)
            }

            // Extend the schema if there is a extend element
            extendsSchema?.let {
                val extendElements = it.elements.mapNotNull { element ->
                    if (schema.containsElement(element.name)) null else fixupElement(element)
                }
                schemaElements = schemaElements.plus(extendElements)
            }
            schema.copy(elements = schemaElements, basedOnRef = basedOnSchema, extendsRef = extendsSchema)
        }
    }

    private fun normalizeSchemaName(name: String): String {
        return name.toLowerCase()
    }

    /**
     * The fixup process fills in references and inherited attributes.
     */
    private fun fixupElement(element: Element, baseElement: Element? = null): Element {
        if (element.canBeBlank == true && element.default != null)
            error("Schema Error: '${element.name}' has both a default and a canBeBlank field")
        if (element.canBeBlank == true && element.mapper != null)
            error("Schema Error: '${element.name}' has both a mapper and a canBeBlank field")
        val valueSet = element.valueSet ?: baseElement?.valueSet
        val valueSetRef = valueSet?.let {
            findValueSet(it)
                ?: error("Schema Error: '$valueSet' is missing in element '{$element.name}'")
        }
        val table = element.table ?: baseElement?.table
        val tableRef = table?.let {
            findLookupTable(it)
                ?: error("Schema Error: '$table' is missing in element '{$element.name}'")
        }
        val mapper = element.mapper ?: baseElement?.mapper
        val refAndArgs: Pair<Mapper, List<String>>? = mapper?.let {
            val (name, args) = Mappers.parseMapperField(it)
            val ref: Mapper = findMapper(name)
                ?: error("Schema Error: Could not find mapper '$name' in element '{$element.name}'")
            Pair(ref, args)
        }
        val fullElement = if (baseElement != null) element.inheritFrom(baseElement) else element
        val usageRequirement = fullElement.usage?.let {
            when {
                it == "required" -> Element.UsageRequirement(Element.Usage.REQUIRED)
                it == "requested" -> Element.UsageRequirement(Element.Usage.REQUESTED)
                it.startsWith("requiredIfPresent") -> {
                    TODO()
                }
                it.startsWith("requiredIfNotPresent") -> {
                    TODO()
                }
                it == "optional" -> Element.UsageRequirement(Element.Usage.OPTIONAL)
                else -> error("Schema Error: Invalid usage field '$it' on '${element.name}'")
            }
        } ?: Element.UsageRequirement(Element.Usage.OPTIONAL)

        return fullElement.copy(
            valueSetRef = valueSetRef,
            tableRef = tableRef,
            mapperRef = refAndArgs?.first,
            mapperArgs = refAndArgs?.second,
            usageRequirement = usageRequirement
        )
    }

    /*
     * Mappers
     */

    fun findMapper(name: String): Mapper? {
        return mappers.find { it.name.equals(name, ignoreCase = true) }
    }

    /*
    * JurisdictionalFilters
    */

    fun findJurisdictionalFilter(name: String): JurisdictionalFilter? {
        return jurisdictionalFilters.find { it.name.equals(name, ignoreCase = true) }
    }

    /*
      * ValueSet
      */

    fun loadValueSetCatalog(catalog: String): Metadata {
        val catalogDir = File(catalog)
        if (!catalogDir.isDirectory) error("Expected ${catalogDir.absolutePath} to be a directory")
        try {
            return loadValueSetList(readAllValueSets(catalogDir))
        } catch (e: Exception) {
            throw Exception("Error loading $catalog", e)
        }
    }

    fun loadValueSets(vararg sets: ValueSet): Metadata {
        return loadValueSetList(sets.toList())
    }

    fun loadValueSetList(sets: List<ValueSet>): Metadata {
        this.valueSets = sets.map { normalizeValueSetName(it.name) to it }.toMap()
        return this
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

    fun loadOrganizations(filePath: String): Metadata {
        try {
            return loadOrganizations(File(filePath).inputStream())
        } catch (e: Exception) {
            throw Exception("Error loading: $filePath", e)
        }
    }

    fun loadOrganizations(organizationStream: InputStream): Metadata {
        val list = mapper.readValue<List<Organization>>(organizationStream)
        return loadOrganizationList(list)
    }

    fun loadOrganizations(vararg organizations: Organization): Metadata {
        return loadOrganizationList(organizations.toList())
    }

    fun loadOrganizationList(organizations: List<Organization>): Metadata {
        this.organizationStore = organizations
        this.organizationClientStore = organizations.flatMap { it.clients }
        this.organizationServiceStore = organizations.flatMap { it.services }
        // Check values
        this.organizationServiceStore.forEach { service ->
            service.batch?.let {
                if (!it.isValid())
                    error("Internal Error: improper batch value for ${service.fullName}")
            }
        }
        return this
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

    fun loadLookupTables(filePath: String): Metadata {
        val catalogDir = File(filePath)
        if (!catalogDir.isDirectory) error("Expected ${catalogDir.absolutePath} to be a directory")
        try {
            readAllTables(catalogDir) { tableName: String, table: LookupTable ->
                loadLookupTable(tableName, table)
            }
            return this
        } catch (e: Exception) {
            throw Exception("Error loading tables in '$filePath'", e)
        }
    }

    fun loadLookupTable(name: String, table: LookupTable): Metadata {
        lookupTableStore = lookupTableStore.plus(name.toLowerCase() to table)
        return this
    }

    fun loadLookupTable(name: String, tableStream: InputStream): Metadata {
        val table = LookupTable.read(tableStream)
        return loadLookupTable(name, table)
    }

    fun findLookupTable(name: String): LookupTable? {
        return lookupTableStore[name.toLowerCase()]
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

    companion object {
        const val schemaExtension = ".schema"
        const val valueSetExtension = ".valuesets"
        const val tableExtension = ".csv"
        const val defaultMetadataDirectory = "./metadata"
        const val schemasSubdirectory = "schemas"
        const val valuesetsSubdirectory = "valuesets"
        const val tableSubdirectory = "tables"
        const val organizationsBaseName = "organizations"
    }
}