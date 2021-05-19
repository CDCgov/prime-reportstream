package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * A `Sender` represents the agent that is sending reports to
 * to the data hub (minus the credentials used by that agent, of course). It
 * contains information about the specific topic and schema that the sender uses.
 */
open class Sender(
    val name: String,
    val organizationName: String,
    val format: Format,
    val topic: String,
    val schemaName: String,
) {
    constructor(copy: Sender) : this(copy.name, copy.organizationName, copy.format, copy.topic, copy.schemaName)

    @get:JsonIgnore
    val fullName: String get() = "$organizationName$fullNameSeparator$name"

    enum class Format(val mimeType: String) {
        CSV("text/csv"),
        HL7("text/hl7"),   // todo correct this.  Maybe  x-application/hl7-v2+er7
    }

    /**
     * Validate the object and return null or an error message
     */
    fun consistencyErrorMessage(metadata: Metadata): String? {
        if (metadata.findSchema(schemaName) == null) return "Invalid schemaName: $schemaName"
        return null
    }

    companion object {
        const val fullNameSeparator = "."

        fun parseFullName(fullName: String): Pair<String, String> {
            val splits = fullName.split(fullNameSeparator)
            return when (splits.size) {
                1 -> Pair(splits[0], "default")
                2 -> Pair(splits[0], splits[1])
                else -> error("Internal Error: Invalid fullName: $fullName")
            }
        }

        fun canonicalizeFullName(fullName: String): String {
            val splits = fullName.split(fullNameSeparator)
            return when (splits.size) {
                1 -> "${fullName}${fullNameSeparator}default"
                2 -> fullName
                else -> error("Internal Error: Invalid fullName: $fullName")
            }
        }
    }
}