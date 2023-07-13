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