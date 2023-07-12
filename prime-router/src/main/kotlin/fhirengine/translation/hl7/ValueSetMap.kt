package gov.cdc.prime.router.fhirengine.translation.hl7

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import gov.cdc.prime.router.Metadata
import java.util.SortedMap

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = InlineValueSet::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = LookupTableValueSet::class, name = "lookupValueset")
)
interface ValueSetMap<T> {
    val values: T // TODO: could potentially drop
    fun getMapValues(): SortedMap<String, String>
}

class InlineValueSet
(@JsonProperty("values") override val values: SortedMap<String, String>) : ValueSetMap<SortedMap<String, String>> {
    override fun getMapValues(): SortedMap<String, String> {
        return values
    }
}

data class LookupTableValueSetConfig(
    val tableName: String,
    val keyColumn: String,
    val valueColumn: String
)

class LookupTableValueSet
(@JsonProperty("values") override val values: LookupTableValueSetConfig) : ValueSetMap<LookupTableValueSetConfig> {
    private val metadata = Metadata.getInstance()
    override fun getMapValues(): SortedMap<String, String> {
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
    }
}