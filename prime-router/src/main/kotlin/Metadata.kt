package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.metadata.DatabaseLookupTable
import gov.cdc.prime.router.metadata.LookupTable
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import java.io.File
import java.io.FilenameFilter
import java.io.InputStream
import java.time.Instant

/**
 * A metadata object contains all the metadata including schemas, tables, valuesets, and organizations.
 */
class Metadata : Logging {
    private var schemaStore = mapOf<String, Schema>()
    private var fileNameTemplatesStore = mapOf<String, FileNameTemplate>()
    private var mappers = listOf(
        MiddleInitialMapper(),
        UseMapper(),
        IfPresentMapper(),
        LookupMapper(),
        LIVDLookupMapper(),
        ConcatenateMapper(),
        Obx17Mapper(),
        Obx17TypeMapper(),
        Obx8Mapper(),
        DateTimeOffsetMapper(),
        CoalesceMapper(),
        TrimBlanksMapper(),
        StripPhoneFormattingMapper(),
        StripNonNumericDataMapper(),
        StripNumericDataMapper(),
        SplitMapper(),
        ZipCodeToCountyMapper(),
        SplitByCommaMapper(),
        TimestampMapper(),
        HashMapper(),
        NullMapper(),
    )
    private var jurisdictionalFilters = listOf(
        FilterByCounty(),
        Matches(),
        DoesNotMatch(),
        OrEquals(),
        HasValidDataFor(),
        HasAtLeastOneOf(),
        AllowAll(),
        IsValidCLIA(),
    )
    private var valueSets = mapOf<String, ValueSet>()
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    /**
     * The database lookup table access.
     */
    private var tableDbAccess: DatabaseLookupTableAccess

    /**
     * Load all parts of the metadata catalog located at the default catalog folder.
     * @param tableDbAccess database lookup table access for dependency injection purposes
     */
    internal constructor(tableDbAccess: DatabaseLookupTableAccess? = null) :
        this(defaultMetadataDirectory, tableDbAccess)

    /**
     * Load all parts of the metadata catalog located at [metadataPath] from a directory and its sub-directories.
     * @param tableDbAccess database lookup table access for dependency injection purposes
     */
    internal constructor(
        metadataPath: String,
        tableDbAccess: DatabaseLookupTableAccess? = null
    ) {
        this.tableDbAccess = tableDbAccess ?: DatabaseLookupTableAccess()
        val metadataDir = File(metadataPath)
        if (!metadataDir.isDirectory) error("Expected metadata directory")
        loadValueSetCatalog(metadataDir.toPath().resolve(valuesetsSubdirectory).toString())
        loadLookupTables(metadataDir.toPath().resolve(tableSubdirectory).toString())
        loadDatabaseLookupTables()
        loadSchemaCatalog(metadataDir.toPath().resolve(schemasSubdirectory).toString())
        loadFileNameTemplates(metadataDir.toPath().resolve(fileNameTemplatesSubdirectory).toString())
        logger.trace("Metadata initialized.")
    }

    /**
     * FOR UNIT TESTING ONLY.  Useful for test versions of the metadata catalog
     * @param tableDbAccess database lookup table access for dependency injection purposes
     */
    constructor(
        schema: Schema? = null,
        valueSet: ValueSet? = null,
        tableName: String? = null,
        table: LookupTable? = null,
        tableDbAccess: DatabaseLookupTableAccess? = null
    ) : this() {
        if (tableDbAccess != null) this.tableDbAccess = tableDbAccess
        valueSet?.let { loadValueSets(it) }
        table?.let { loadLookupTable(tableName ?: "", it) }
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

    private fun loadSchemaList(schemas: List<Schema>): Metadata {
        val fixedUpSchemas = mutableMapOf<String, Schema>()

        fun fixupSchema(name: String): Schema {
            val normalName = normalizeSchemaName(name)
            if (fixedUpSchemas.containsKey(normalName)) return fixedUpSchemas.getValue(normalName)

            val schema = schemas.find { normalizeSchemaName(it.name) == normalName }
                ?: error("extends schema does not exist for '$normalName'")
            val extendsSchema = schema.extends?.let { fixupSchema(it) }
            val basedOnSchema = schema.basedOn?.let { fixupSchema(it) }
            val fixedUpSchema = fixupElements(schema, basedOnSchema, extendsSchema)
            fixedUpSchemas[normalName] = fixedUpSchema
            return fixedUpSchema
        }

        schemas.forEach { fixupSchema(it.name) }
        schemaStore = fixedUpSchemas
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

    private fun fixupElements(schema: Schema, basedOnSchema: Schema?, extendsSchema: Schema?): Schema {
        // Inherit properties from the base schema
        val baseSchema = extendsSchema ?: basedOnSchema
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
        return schema.copy(elements = schemaElements, basedOnRef = basedOnSchema, extendsRef = extendsSchema)
    }

    private fun normalizeSchemaName(name: String): String {
        return name.lowercase()
    }

    /**
     * The fixup process fills in references and inherited attributes.
     */
    private fun fixupElement(element: Element, baseElement: Element? = null): Element {
        if (element.canBeBlank && element.default != null)
            error("Schema Error: '${element.name}' has both a default and a canBeBlank field")
        if (element.canBeBlank && element.mapper != null)
            error("Schema Error: '${element.name}' has both a mapper and a canBeBlank field")
        val valueSet = element.valueSet ?: baseElement?.valueSet
        val valueSetRef = valueSet?.let {
            val ref = findValueSet(it)
                ?: error("Schema Error: '$valueSet' is missing in element '{$element.name}'")
            ref.mergeAltValues(element.altValues)
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

        if (fullElement.maxLength != null && fullElement.maxLength < 0)
            error("Schema Error: maxLength ${fullElement.maxLength} for ${fullElement.name} must be >= 0")

        return fullElement.copy(
            valueSetRef = valueSetRef,
            tableRef = tableRef,
            mapperRef = refAndArgs?.first,
            mapperArgs = refAndArgs?.second
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

    private fun loadValueSetList(sets: List<ValueSet>): Metadata {
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
        return name.lowercase()
    }

    /**
     * Lookup Tables
     */
    internal var lookupTableStore = mapOf<String, LookupTable>()

    /**
     * Last time the database lookup tables were checked.
     */
    internal var tablelastCheckedAt = Instant.now()

    private fun loadLookupTables(filePath: String): Metadata {
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
        lookupTableStore = lookupTableStore.plus(name.lowercase() to table)
        return this
    }

    fun loadLookupTable(name: String, tableStream: InputStream): Metadata {
        val table = LookupTable.read(tableStream)
        return loadLookupTable(name, table)
    }

    /**
     * Load the database lookup tables.
     */
    @Synchronized
    fun checkForDatabaseLookupTableUpdates() {
        // Check for tables at intervals
        if (tablelastCheckedAt.plusSeconds(tablePollInternalSecs).isAfter(Instant.now()))
            return
        loadDatabaseLookupTables()
        tablelastCheckedAt = Instant.now()
    }

    /**
     * Update the database lookup tables that have changed versions and load any new tables.
     */
    @Synchronized
    internal fun loadDatabaseLookupTables() {
        logger.trace("Checking for database lookup table updates...")
        val databaseTables = lookupTableStore.values.mapNotNull { if (it is DatabaseLookupTable) it else null }

        try {
            val activeTables = tableDbAccess.fetchTableList()
            // Let's be paranoid and check if the API is not returning what we need.
            if (activeTables.any { it.isActive == false })
                error("Database lookup table list returned an inactive table.")

            // Process existing tables
            databaseTables.forEach { dbTable ->
                val tableInfo = activeTables.firstOrNull { it.tableName == dbTable.name }
                when {
                    // The table was removed
                    tableInfo == null -> {
                        // Note that we cannot remove the table as the schema would break with the existing code.
                        dbTable.setTableData(emptyList())
                        logger.warn("Database lookup table ${dbTable.name} is no longer active.")
                    }

                    // The table version has changed
                    tableInfo.tableVersion != dbTable.version ->
                        dbTable.loadTable(tableInfo.tableVersion)
                }
            }

            // Load new tables
            val newTables = activeTables.filter { !lookupTableStore.containsKey(it.tableName.lowercase()) }
            newTables.forEach {
                loadLookupTable(
                    it.tableName,
                    DatabaseLookupTable(it.tableName, tableDbAccess).loadTable(it.tableVersion)
                )
            }
        } catch (e: DataAccessException) {
            logger.error("Error while trying to check for updates to database lookup tables.", e)
        }
    }

    fun findLookupTable(name: String): LookupTable? {
        return lookupTableStore[name.lowercase()]
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

    /*
        file name templates
    */
    val fileNameTemplates get() = fileNameTemplatesStore

    fun findFileNameTemplate(name: String): FileNameTemplate? {
        return fileNameTemplatesStore[name.lowercase()]
    }

    private fun loadFileNameTemplates(filePath: String): Metadata {
        val catalogDir = File(filePath)
        if (catalogDir.exists()) {
            fileNameTemplatesStore = readAllFileNameTemplates(catalogDir).associateBy {
                it.name?.lowercase() ?: "Error: any file name template loaded into metadata MUST have a unique name"
            }
        }
        return this
    }

    private fun readAllFileNameTemplates(catalogDir: File): List<FileNameTemplate> {
        // read the file name template files in the director
        val files = File(catalogDir.absolutePath).listFiles() ?: emptyArray()
        return files.flatMap { readFileNameTemplates(it) }
    }

    private fun readFileNameTemplates(file: File): List<FileNameTemplate> {
        try {
            return mapper.readValue(file.inputStream())
        } catch (e: Exception) {
            throw Exception("Error reading '${file.name}'", e)
        }
    }

    companion object {
        const val schemaExtension = ".schema"
        const val valueSetExtension = ".valuesets"
        const val tableExtension = ".csv"
        private const val defaultMetadataDirectory = "./metadata"
        const val schemasSubdirectory = "schemas"
        const val valuesetsSubdirectory = "valuesets"
        const val tableSubdirectory = "tables"
        const val fileNameTemplatesSubdirectory = "./file_name_templates"

        /**
         * Singleton object
         */
        private val singletonInstance: Metadata by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Metadata(defaultMetadataDirectory)
        }

        /**
         * Get the singleton instance.
         * @return the metadata instance
         */
        fun getInstance(): Metadata {
            return singletonInstance
        }

        /**
         * The amount of seconds to wait before tables are checked again for updates.
         */
        private val tablePollInternalSecs =
            try {
                System.getenv("PRIME_METADATA_POLL_INTERVAL")?.toLong()
            } catch (e: NumberFormatException) {
                null
            } ?: 30L
    }
}