package gov.cdc.prime.router

/**
 * A `OrganizationClient` represents the agent that is sending reports to
 * to the data hub (minus the credentials used by that agent, of course). It
 * contains information about the specific topic and schema that the client uses.
 */
data class OrganizationClient(
    val name: String,
    val formats: List<Format> = emptyList(),
    val topic: String? = null,
    val schema: String? = null,
) {
    lateinit var organization: Organization

    enum class Format { CSV }
}