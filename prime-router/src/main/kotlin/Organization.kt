package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.prime.router.tokens.Jwk
import gov.cdc.prime.router.tokens.JwkSet

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
    // enabled features for organization. Features defined in lookup table rs_feature_flags
    val featureFlags: List<String>? = emptyList(),
    val keys: List<JwkSet>? = emptyList()
) {
    constructor(org: Organization) : this(
        org.name, org.description, org.jurisdiction, org.stateCode, org.countyName, org.filters, org.featureFlags,
        org.keys
    )

    constructor(copy: Organization, keys: List<JwkSet>) : this(
        copy.name,
        copy.description,
        copy.jurisdiction,
        copy.stateCode,
        copy.countyName,
        copy.filters,
        copy.featureFlags,
        keys
    )

    constructor(copy: Organization, newScope: String, newJwk: Jwk) : this(
        copy.name,
        copy.description,
        copy.jurisdiction,
        copy.stateCode,
        copy.countyName,
        copy.filters,
        copy.featureFlags,
        JwkSet.addJwkSet(copy.keys, newScope, newJwk)
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

    fun makeCopyWithNewScopeAndJwk(scope: String, jwk: Jwk): Organization {
        return Organization(this, scope, jwk)
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
    featureFlags: List<String>? = emptyList(),
    keys: List<JwkSet>? = emptyList(),
    val senders: List<Sender> = emptyList(),
    val receivers: List<Receiver> = emptyList(),
) : Organization(name, description, jurisdiction, stateCode, countyName, filters, featureFlags, keys) {
    constructor(org: Organization, senders: List<Sender>, receivers: List<Receiver>) :
        this(
            org.name, org.description, org.jurisdiction, org.stateCode, org.countyName, org.filters, org.featureFlags,
            org.keys, senders, receivers
        )
}