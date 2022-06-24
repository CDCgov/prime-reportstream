package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Organization represents a partner organization of the hub. It has a jurisdiction.
 */
open class Organization(
    val name: String,
    val description: String,
    val jurisdiction: Jurisdiction,
    val stateCode: String?,
    val countyName: String?,
    val filters: List<ReportStreamFilters>? = emptyList(), // one ReportStreamFilters obj per topic.
) {
    constructor(org: Organization) : this(
        org.name, org.description, org.jurisdiction, org.stateCode, org.countyName, org.filters
    )

    enum class Jurisdiction {
        FEDERAL,
        STATE,
        COUNTY
    }

    /**
     * Validate the object and return null or an error message
     */
    fun consistencyErrorMessage(): String? {
        return when (jurisdiction) {
            Jurisdiction.FEDERAL -> {
                if (stateCode != null || countyName != null)
                    "stateCode or countyName not allowed for FEDERAL organizations"
                else null
            }
            Jurisdiction.STATE -> {
                if (stateCode == null || countyName != null)
                    "stateCode required for STATE organizations"
                else null
            }
            Jurisdiction.COUNTY -> {
                if (stateCode == null || countyName == null)
                    "stateCode and countyName required for COUNTY organizations"
                else null
            }
        }
    }
}

/**
 * Organization with senders and receivers.
 *
 * Useful to put all the information about an org in single object or file.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class DeepOrganization(
    name: String,
    description: String,
    jurisdiction: Jurisdiction,
    stateCode: String? = null,
    countyName: String? = null,
    filters: List<ReportStreamFilters>? = emptyList(),
    val senders: List<Sender> = emptyList(),
    val receivers: List<Receiver> = emptyList(),
) : Organization(name, description, jurisdiction, stateCode, countyName, filters) {
    constructor(org: Organization, senders: List<Sender>, receivers: List<Receiver>) :
        this(
            org.name, org.description, org.jurisdiction, org.stateCode, org.countyName, org.filters,
            senders, receivers
        )
}