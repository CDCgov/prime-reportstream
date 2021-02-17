package gov.cdc.prime.router

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
    val fullName: String get() = "$organizationName$fullNameSeparator$name"

    enum class Format(val mimeType: String) {
        CSV("text/csv")
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