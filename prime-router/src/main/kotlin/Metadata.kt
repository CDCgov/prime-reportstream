package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.metadata.CoalesceMapper
import gov.cdc.prime.router.metadata.ConcatenateMapper
import gov.cdc.prime.router.metadata.CountryMapper
import gov.cdc.prime.router.metadata.DateTimeOffsetMapper
import gov.cdc.prime.router.metadata.HashMapper
import gov.cdc.prime.router.metadata.IfNPIMapper
import gov.cdc.prime.router.metadata.IfNotPresentMapper
import gov.cdc.prime.router.metadata.IfPresentMapper
import gov.cdc.prime.router.metadata.IfThenElseMapper
import gov.cdc.prime.router.metadata.LIVDLookupMapper
import gov.cdc.prime.router.metadata.LookupMapper
import gov.cdc.prime.router.metadata.LookupSenderAutomationValuesets
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.metadata.Mapper
import gov.cdc.prime.router.metadata.Mappers
import gov.cdc.prime.router.metadata.MiddleInitialMapper
import gov.cdc.prime.router.metadata.NpiLookupMapper
import gov.cdc.prime.router.metadata.NullMapper
import gov.cdc.prime.router.metadata.Obx17Mapper
import gov.cdc.prime.router.metadata.Obx17TypeMapper
import gov.cdc.prime.router.metadata.Obx8Mapper
import gov.cdc.prime.router.metadata.PatientAgeMapper
import gov.cdc.prime.router.metadata.SplitByCommaMapper
import gov.cdc.prime.router.metadata.SplitMapper
import gov.cdc.prime.router.metadata.StripNonNumericDataMapper
import gov.cdc.prime.router.metadata.StripNumericDataMapper
import gov.cdc.prime.router.metadata.StripPhoneFormattingMapper
import gov.cdc.prime.router.metadata.TimestampMapper
import gov.cdc.prime.router.metadata.TrimBlanksMapper
import gov.cdc.prime.router.metadata.UseMapper
import gov.cdc.prime.router.metadata.UseSenderSettingMapper
import gov.cdc.prime.router.metadata.ZipCodeToCountyMapper
import gov.cdc.prime.router.metadata.ZipCodeToStateMapper
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import java.io.File
import java.io.FilenameFilter
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
        UseSenderSettingMapper(),
        IfPresentMapper(),
        IfNotPresentMapper(),
        IfNPIMapper(),
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
        ZipCodeToStateMapper(),
        SplitByCommaMapper(),
        TimestampMapper(),
        HashMapper(),
        NullMapper(),
        NpiLookupMapper(),
        CountryMapper(),
        LookupSenderAutomationValuesets(),
        IfThenElseMapper(),
        PatientAgeMapper()
    )
    private var reportStreamFilterDefinitions = listOf(
        FilterByCounty(),
        Matches(),
        DoesNotMatch(),
        OrEquals(),
        HasValidDataFor(),
        HasAtLeastOneOf(),
        AtLeastOneHasValue(),
        AllowAll(),
        AllowNone(),
        IsValidCLIA(),
        InDateInterval(),
        FilterOutNegativeAntigenTestType()
    )
    private var valueSets = mapOf<String, ValueSet>()
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )

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
        loadDatabaseLookupTables()
        loadSchemaCatalog(metadataDir.toPath().resolve(schemasSubdirectory).toString())
        loadFileNameTemplates(metadataDir.toPath().resolve(fileNameTemplatesSubdirectory).toString())
        logger.trace("Metadata initialized.")
        validateSchemas()
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
    ) {
        this.tableDbAccess = tableDbAccess ?: DatabaseLookupTableAccess()
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
        val valueSet = element.valueSet ?: baseElement?.valueSet
        val valueSetRef = valueSet?.let {
            val ref = findValueSet(it)
                ?: error("Schema Error: '$valueSet' is missing in element '${element.name}'")
            ref.mergeAltValues(element.altValues)
        }
        val table = element.table ?: baseElement?.table
        val tableRef = table?.let {
            findLookupTable(it)
                ?: error("Schema Error: '$table' is missing in element '${element.name}'")
        }
        val mapper = element.mapper ?: baseElement?.mapper
        val refAndArgs: Pair<Mapper, List<String>>? = mapper?.let {
            val (name, args) = Mappers.parseMapperField(it)
            val ref: Mapper = findMapper(name)
                ?: error("Schema Error: Could not find mapper '$name' in element '${element.name}'")
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
    * ReportStreamFilterDefinitions
    */

    fun findReportStreamFilterDefinitions(name: String): ReportStreamFilterDefinition? {
        return reportStreamFilterDefinitions.find { it.name.equals(name, ignoreCase = true) }
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

    fun loadLookupTable(name: String, table: LookupTable): Metadata {
        lookupTableStore = lookupTableStore.plus(name.lowercase() to table)
        return this
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
        val databaseTables = lookupTableStore.values.mapNotNull { if (it.isSourceDatabase) it else null }

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
                        dbTable.clear()
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
                    LookupTable(it.tableName, emptyList(), dbAccess = tableDbAccess).loadTable(it.tableVersion)
                )
            }
        } catch (e: DataAccessException) {
            logger.error("Error while trying to check for updates to database lookup tables.", e)
        }
    }

    fun findLookupTable(name: String): LookupTable? {
        return lookupTableStore[name.lowercase()]
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

    /**
     * Validate all the loaded schemas.
     */
    internal fun validateSchemas() {
        val validationErrors = mutableListOf<String>()
        schemaStore.values.forEach { validationErrors.addAll(it.validate()) }
        if (validationErrors.isNotEmpty())
            error(
                "There were errors validating the schemas." + System.lineSeparator() +
                    validationErrors.joinToString(System.lineSeparator())
            )
    }

    companion object {
        const val schemaExtension = ".schema"
        const val valueSetExtension = ".valuesets"
        private const val defaultMetadataDirectory = "./metadata"
        const val schemasSubdirectory = "schemas"
        const val valuesetsSubdirectory = "valuesets"
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