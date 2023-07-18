package gov.cdc.prime.router.fhirengine.engine

import com.fasterxml.jackson.annotation.JsonProperty
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.ValueSetMap
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
 * Implementation of ValueSetMap to allow valueSet to be retrieved from a lookup table.
 * Provide [LookupTableValueSetConfig] to configure the lookup table source.
 */
class LookupTableValueSet
(@JsonProperty("lookupTable") private val configData: LookupTableValueSetConfig) :
    ValueSetMap<LookupTableValueSetConfig> {
    private val metadata = Metadata.getInstance()
    private var mapValues: SortedMap<String, String>? = null

    /**
     * @return a SortedMap<String, String> representation of the valueSet
     */
    override fun getMapValues(): SortedMap<String, String> {
        if (mapValues == null) {
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

            mapValues = result.toSortedMap()
        }
        return mapValues as SortedMap<String, String>
    }
}