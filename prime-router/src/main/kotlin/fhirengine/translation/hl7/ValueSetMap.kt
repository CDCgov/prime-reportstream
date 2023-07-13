package gov.cdc.prime.router.fhirengine.translation.hl7

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.SortedMap

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = InlineValueSet::class
)
/**
 * Interface for valueSet. Implement this interface to create a data provider
 * that can return a SortedMap<String, String> at runtime.
 */
interface ValueSetMap<T> {
    val values: T // TODO: could potentially drop
    fun getMapValues(): SortedMap<String, String>
}

/**
 * Default implementation of ValueSetMap to allow valueSet to be specified inline in the sender transform schema.
 */
class InlineValueSet
(@JsonProperty("values") override val values: SortedMap<String, String>) : ValueSetMap<SortedMap<String, String>> {
    /**
     * @return a SortedMap<String, String> representation of the valueSet
     */
    override fun getMapValues(): SortedMap<String, String> {
        return values
    }
}