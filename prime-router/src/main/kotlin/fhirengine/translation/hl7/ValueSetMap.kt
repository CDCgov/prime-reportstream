package gov.cdc.prime.router.fhirengine.translation.hl7

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.SortedMap

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = InlineValueSet::class, name = "inlineValueset")
    // JsonSubTypes.Type(value = FhirValueset::class, name = "fhirValueset")
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

/**
 * An interface for valuesets to be retrieved by FHIRPath
 */
// @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
// @JsonSubTypes(
//    JsonSubTypes.Type(ValuesetFunction::class, name = "function")
// )
// abstract class FhirValueset {
//    companion object {
//        private val ptv = BasicPolymorphicTypeValidator.builder()
//            .build()
//        val mapper = jacksonMapperBuilder()
//            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//            .polymorphicTypeValidator(ptv)
//            .activateDefaultTyping(ptv)
//            .build()
//
//        fun deserialize(s: String): SortedMap<String, String> {
//            return mapper.readValue(s)
//        }
//    }
// }
//
// /**
// * The Message representation of a raw submission to the system, tracking the [reportId], [blobUrl],
// * [blobSubFolderName] (which is derived from the sender name), and [schemaName] from the sender settings.
// * A [digest] is also provided for checksum verification.
// */
// @JsonTypeName("function")
// abstract class ValuesetFunction(
//    val fhirPathExpression: String
// ) : SortedMap<String, String> {
//    /**
//     * Execute the FHIRPath function
//     */
//    fun callFunction(): SortedMap<String, String> {
//        // stub; will need to call FhirPathUtils.evaluateString(context, focusResource, bundle, it)
//        return sortedMapOf(Pair("a", "b"))
//    }
// }