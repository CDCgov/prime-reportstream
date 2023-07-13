package gov.cdc.prime.router.fhirengine.engine

import com.fasterxml.jackson.annotation.JsonProperty
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.ValueSetMap
import java.util.SortedMap

data class LookupTableValueSetConfig(
    val tableName: String,
    val keyColumn: String,
    val valueColumn: String
)

class LookupTableValueSet
(@JsonProperty("values") override val values: LookupTableValueSetConfig) : ValueSetMap<LookupTableValueSetConfig> {
    private val metadata = Metadata.getInstance()
    private var mapValues: SortedMap<String, String>? = null
    override fun getMapValues(): SortedMap<String, String> {
        if (mapValues == null) {
            val lookupTable = metadata.findLookupTable(name = values.tableName)
                ?: throw SchemaException("Specified lookup table not found")

            if (!lookupTable.hasColumn(values.keyColumn)) {
                throw SchemaException("Key column not found in specified lookup table")
            }

            if (!lookupTable.hasColumn(values.valueColumn)) {
                throw SchemaException("Value column not found in specified lookup table")
            }

            val filterTable = lookupTable.table.retainColumns(values.keyColumn, values.valueColumn)
            val result = mutableMapOf<String, String>()
            filterTable.forEach { row ->
                result[row.getString(values.keyColumn)] = row.getString(values.valueColumn)
            }

            return result.toSortedMap()
        } else
            return mapValues as SortedMap<String, String>
    }
}