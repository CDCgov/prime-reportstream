package gov.cdc.prime.router.azure.observability

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ContainerNode
import com.fasterxml.jackson.databind.node.ObjectNode
import gov.cdc.prime.router.common.JacksonMapperUtilities

/**
 * Interface for all types that can be serialized and passed to Azure AppInsights
 */
interface AzureCustomDimensionsSerializable {

    /**
     * Takes all fields from a data class and serializes it to a simple String -> String map
     * that the Microsoft Azure AppInsights Telemetry library can handle.
     *
     * This will blow up if the implementing class serializes to something other than an object
     */
    fun serialize(): Map<String, String> = JacksonMapperUtilities.jacksonObjectMapper
            .valueToTree<ObjectNode>(this)
            .properties()
            .associate { it.key to serializeValue(it.value) }

    /**
     * All primitives will be in their string format.
     * Ex: "abc" -> "abc", 5 -> "5", true -> "true"
     *
     * All objects and arrays will be serialized to a string
     * Ex: obj -> "{\"key\": \"value\"}", arr -> "[\"a\", \"b\", \"c\"]"
     */
    private fun serializeValue(valueNode: JsonNode): String = when (valueNode) {
            is ContainerNode<*> -> valueNode.toString() // object or array
            else -> valueNode.asText()
        }
}