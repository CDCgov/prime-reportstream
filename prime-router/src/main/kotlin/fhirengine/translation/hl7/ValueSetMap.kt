package gov.cdc.prime.router.fhirengine.translation.hl7

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import gov.cdc.prime.router.Metadata
import java.util.SortedMap

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = InlineValueSet::class, name = "inlineValueset"),
    JsonSubTypes.Type(value = LookupTableValueSet::class, name = "lookupValueset")
)
interface ValueSetMap<T> {
    val values: T // TODO: could potentially drop
    fun getMapValues(): SortedMap<String, String>
}

class InlineValueSet
@JsonCreator constructor
(@JsonProperty("values") override val values: SortedMap<String, String>) : ValueSetMap<SortedMap<String, String>> {
    override fun getMapValues(): SortedMap<String, String> {
        return values
    }
}

class LookupTableValueSet
@JsonCreator constructor
(@JsonProperty("values") override val values: SortedMap<String, String>) : ValueSetMap<SortedMap<String, String>> {
    override fun getMapValues(): SortedMap<String, String> {
        val tableName = values["table"]
        if (tableName.isNullOrBlank()) {
            throw SchemaException("No lookup table name specified")
        }

        val keyColumn = values["keyColumn"]
        if (keyColumn.isNullOrBlank()) {
            throw SchemaException("No key column name specified")
        }

        val valueColumn = values["valueColumn"]
        if (valueColumn.isNullOrBlank()) {
            throw SchemaException("No value column name specified")
        }

        val metadata = Metadata.getInstance()
        val lookupTable = metadata.findLookupTable(name = tableName)
            ?: throw SchemaException("Specified lookup table not found")

        if (!lookupTable.hasColumn(keyColumn)) {
            throw SchemaException("Key column not found in specified lookup table")
        }

        if (!lookupTable.hasColumn(valueColumn)) {
            throw SchemaException("Value column not found in specified lookup table")
        }

        val filterTable = lookupTable.table.retainColumns(keyColumn, valueColumn)
        var result = mutableMapOf<String, String>()
        filterTable.forEach { row ->
            result[row.getString(keyColumn)] = row.getString(valueColumn)
        }

        return result.toSortedMap()
    }
}