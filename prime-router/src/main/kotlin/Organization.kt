package gov.cdc.prime.router

/**
 * Organization represents a partner organization of the hub. It has a jurisdiction.
 */
open class Organization(
    val name: String,
    val description: String,
    val jurisdiction: Jurisdiction,
    val stateCode: String?,
    val countyName: String?,
) {
    enum class Jurisdiction {
        FEDERAL,
        STATE,
        COUNTY
    }
}

/**
 * Organization with senders and receivers.
 *
 * Useful to put all the information about an org in single object or file.
 */
class DeepOrganization(
    name: String,
    description: String,
    jurisdiction: Jurisdiction,
    stateCode: String?,
    countyName: String?,
    val senders: List<Sender> = emptyList(),
    val receivers: List<Receiver> = emptyList(),
) : Organization(name, description, jurisdiction, stateCode, countyName)

/**
 * Organization for Api (Serialized as JSON)
 */
class APIOrganization(
    name: String,
    description: String,
    jurisdiction: Jurisdiction,
    stateCode: String?,
    countyName: String?,
) : Organization(name, description, jurisdiction, stateCode, countyName)