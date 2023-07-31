package gov.cdc.prime.router.fhirengine.translation.hl7

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.SortedMap

@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION,
    include = JsonTypeInfo.As.PROPERTY,
    defaultImpl = InlineValueSet::class
)
/**
 * Implement this interface to create a data provider for a valueSet collection.
 */
interface ValueSetCollection {
    /**
     * @return a SortedMap<String, String> representation of the valueSet
     */
    fun toSortedMap(): SortedMap<String, String>
    /**
     * @return the value matching [keyValue] in the valueSet, or null if there is no match
     */
    fun getMappedValue(keyValue: String): String?
    /**
     * @return true if the valueSet is not empty
     */
    fun isNotEmpty(): Boolean
}

/**
 * Default implementation of [ValueSetCollection] to allow valueSet to be specified inline
 * in the sender transform schema.
 */
class InlineValueSet
(@JsonProperty("values") private val values: SortedMap<String, String>) : ValueSetCollection {

    override fun toSortedMap(): SortedMap<String, String> {
        return values
    }

    override fun getMappedValue(keyValue: String): String? {
        val lowerSet = values.mapKeys { it.key.lowercase() }
        return lowerSet[keyValue.lowercase()]
    }

    override fun isNotEmpty(): Boolean {
        return values.isNotEmpty()
    }
}