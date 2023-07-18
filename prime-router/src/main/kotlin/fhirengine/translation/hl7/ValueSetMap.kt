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
 * Interface for valueSet. Implement this interface to create a data provider
 * that can return a SortedMap<String, String> at runtime.
 */
interface ValueSetMap<T> {
    fun getMapValues(): SortedMap<String, String>
}

/**
 * Default implementation of ValueSetMap to allow valueSet to be specified inline in the sender transform schema.
 */
class InlineValueSet
(@JsonProperty("values") private val values: SortedMap<String, String>) : ValueSetMap<SortedMap<String, String>> {

    /**
     * @return a SortedMap<String, String> representation of the valueSet
     */
    override fun getMapValues(): SortedMap<String, String> {
        return values
    }
}