package gov.cdc.prime.router.fhirengine.engine

import com.fasterxml.jackson.annotation.JsonProperty
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.ValueSetCollection
import java.util.SortedMap

/**
 * Data class for configuring [LookupTableValueSet].
 *
 * [tableName]: name of the lookup table
 *
 * [keyColumn]: name of the lookup table column containing the key pair values
 *
 * [valueColumn]: name of the lookup table column containing the value pair values
 */
data class LookupTableValueSetConfig(
    val tableName: String,
    val keyColumn: String,
    val valueColumn: String
)

/**
 * Implementation of [ValueSetCollection] to allow valueSet to be retrieved from a lookup table.
 * Provide [LookupTableValueSetConfig] to configure the lookup table source.
 */
class LookupTableValueSet
(@JsonProperty("lookupTable") private val configData: LookupTableValueSetConfig) : ValueSetCollection {
    private val metadata = Metadata.getInstance()
    private val mapVal: SortedMap<String, String> by lazy {
        val lookupTable = metadata.findLookupTable(name = configData.tableName)
            ?: throw SchemaException("Specified lookup table not found")

        if (!lookupTable.hasColumn(configData.keyColumn)) {
            throw SchemaException("Key column not found in specified lookup table")
        }

        if (!lookupTable.hasColumn(configData.valueColumn)) {
            throw SchemaException("Value column not found in specified lookup table")
        }

        val filterTable = lookupTable.table.retainColumns(configData.keyColumn, configData.valueColumn)
        val result = mutableMapOf<String, String>()
        filterTable.forEach { row ->
            result[row.getString(configData.keyColumn)] = row.getString(configData.valueColumn)
        }

        return@lazy result.toSortedMap()
    }

    override fun toSortedMap(): SortedMap<String, String> {
        return mapVal
    }

    override fun getMappedValue(keyValue: String): String? {
        val lowerSet = toSortedMap().mapKeys { it.key.lowercase() }
        return lowerSet[keyValue.lowercase()]
    }

    override fun isNotEmpty(): Boolean {
        return toSortedMap().isNotEmpty()
    }
}