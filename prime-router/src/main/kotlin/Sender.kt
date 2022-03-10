package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import gov.cdc.prime.router.tokens.Jwk
import gov.cdc.prime.router.tokens.JwkSet

/**
 * A `Sender` represents the agent that is sending reports to
 * the data hub (minus the credentials used by that agent, of course). It
 * contains information about the specific topic and schema that the sender uses.
 *
 * @property name the name of this sender - if only one send for an org, it is default
 * @property organizationName the name of the organization that this sender belongs to
 * @property format the primary format of the reports from the sender
 * @property topic the topic of the reports from the sender currently always covid 19
 * @property customerStatus the status of the sender active inactive
 * @property schemaName the name of the schema used by the sender
 * @property keys used to track server-to-server auths for this sender via public keys sets
 * @property processingType sync or async
 * @property allowDuplicates if false a duplicate submission will be rejected
 * @property senderType one of four broad sender categories
 * @property primarySubmissionMethod Sender preference for submission - manual or automatic
 */
open class Sender(
    val name: String,
    val organizationName: String,
    val format: Format,
    val topic: String,
    val customerStatus: CustomerStatus = CustomerStatus.INACTIVE,
    val schemaName: String,
    val keys: List<JwkSet>? = null,
    val processingType: ProcessingType = ProcessingType.sync,
    val allowDuplicates: Boolean = true,
    val senderType: SenderType? = null,
    val primarySubmissionMethod: PrimarySubmissionMethod? = null
) {
    /**
     * Enumeration representing whether a submission will be processed follow the synchronous or asynchronous
     * message pipeline. Within the code this defaults to Sync unless the PROCESSING_TYPE_PARAMETER query
     * string value is 'async'
     *
     * @property sync
     * @property async
     */
    enum class ProcessingType {
        sync,
        async;

        companion object {
            fun valueOfIgnoreCase(value: String): ProcessingType {
                ProcessingType.values().forEach {
                    if (it.name.equals(value, true)) {
                        return it
                    }
                }
                throw IllegalArgumentException()
            }
        }
    }

    /**
     * Enumeration that divides a Sender into four subcategories or types for data management
     *
     * @property testManufacturer Sender a test manufacturer
     * @property dataAggregator Sender is a data aggregator
     * @property facility Sender is a facility
     * @property hospitalSystem Sender is a hospital or large hospital system
     */
    enum class SenderType {
        testManufacturer,
        dataAggregator,
        facility,
        hospitalSystem
    }

    /**
     * Enumeration to describe the Primary or default method of submission for a Sender
     *
     * @property automated Directly sent to the API
     * @property manual Uploaded via the UI
     */
    enum class PrimarySubmissionMethod {
        automated,
        manual
    }

    constructor(copy: Sender) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.topic,
        copy.customerStatus,
        copy.schemaName,
        if (copy.keys != null) ArrayList(copy.keys) else null
    )

    // constructor that copies and adds a key
    constructor(copy: Sender, newScope: String, newJwk: Jwk) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.topic,
        copy.customerStatus,
        copy.schemaName,
        addJwkSet(copy.keys, newScope, newJwk)
    )

    @get:JsonIgnore
    val fullName: String get() = "$organizationName$fullNameSeparator$name"

    /**
     * Calculate the customer's default processingModeCode based on their
     * CustomerStatus value. Note that the default customerStatus is
     * INACTIVE which will put that customer in "T" mode.
     * Official values for processing_mode_code are in hl70103.
     * Don't remove this - it is used via reflection, not by a direct usage.
     */
    @get:JsonIgnore
    val processingModeCode: String get() = when (customerStatus) {
        CustomerStatus.ACTIVE -> "P"
        CustomerStatus.INACTIVE -> "T"
        CustomerStatus.TESTING -> "T"
    }

    enum class Format(val mimeType: String) {
        CSV("text/csv"),
        HL7("application/hl7-v2"),
    }

    /**
     * Validate the object and return null or an error message
     */
    fun consistencyErrorMessage(metadata: Metadata): String? {
        if (metadata.findSchema(schemaName) == null) return "Invalid schemaName: $schemaName"
        return null
    }

    fun findKeySetByScope(scope: String): JwkSet? {
        if (keys == null) return null
        return keys.find { it.scope == scope }
    }

    companion object {
        const val fullNameSeparator = "."

        /**
         * Copy an old set of authorizations to a new set, and add one to it, if needed.
         * This whole list of lists thing is confusing:
         * The 'orig' obj, and the return val, are list of JwkSets.   And each JwkSet has a list of Jwks.
         */
        fun addJwkSet(orig: List<JwkSet>?, newScope: String, newJwk: Jwk): List<JwkSet> {
            if (orig == null) {
                return listOf(JwkSet(newScope, listOf(newJwk))) // create brand new
            }
            val newJwkSetList = mutableListOf<JwkSet>()
            var done = false
            orig.forEach {
                if (it.scope == newScope) {
                    if (it.keys.contains(newJwk)) {
                        // The orig already has this key with this scope.  Just use it.
                        newJwkSetList.add(it)
                    } else {
                        // Don't create a whole new JwkSet.   Instead, add this Jwk to existing JwkSet.
                        val newJwkList = it.keys.toMutableList()
                        newJwkList.add(newJwk)
                        newJwkSetList.add(JwkSet(newScope, newJwkList))
                    }
                    done = true
                } else {
                    newJwkSetList.add(it) // existing different scope, make sure we keep it.
                }
            }
            // If the old/new scopes didn't match, then the new scope was never added.  Add a new JwkSet now.
            if (!done) {
                newJwkSetList.add(JwkSet(newScope, listOf(newJwk)))
            }
            return newJwkSetList
        }

        /**
         * returns a tuple of the sender name split into parts, with the
         * second part defaulted to 'default' if not provided
         */
        fun parseFullName(fullName: String): Pair<String, String> {
            val splits = fullName.split(fullNameSeparator)
            return when (splits.size) {
                1 -> Pair(splits[0], "default")
                2 -> Pair(splits[0], splits[1])
                else -> error("Internal Error: Invalid fullName: $fullName")
            }
        }

        /**
         * Given the full name for a sender, it makes sure it matches the format we would expect.
         * For example, if the name of the sender is IGNORE, this would return IGNORE.default.
         * If the name is IGNORE.default, it returns that directly
         */
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