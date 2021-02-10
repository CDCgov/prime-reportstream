package gov.cdc.prime.router

/**
 * A `Sender` represents the agent that is sending reports to
 * to the data hub (minus the credentials used by that agent, of course). It
 * contains information about the specific topic and schema that the sender uses.
 */
data class Sender(
    val name: String,
    val organizationName: String,
    val format: Format,
    val topic: String,
    val schemaName: String,
) {
    val fullName: String get() = "$organizationName.$name"

    enum class Format(val mimeType: String) {
        CSV("text/csv")
    }
}