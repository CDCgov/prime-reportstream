package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import gov.cdc.prime.router.Sender.PrimarySubmissionMethod.automated
import gov.cdc.prime.router.Sender.PrimarySubmissionMethod.manual
import gov.cdc.prime.router.Sender.ProcessingType.async
import gov.cdc.prime.router.Sender.ProcessingType.sync
import gov.cdc.prime.router.Sender.SenderType.dataAggregator
import gov.cdc.prime.router.Sender.SenderType.facility
import gov.cdc.prime.router.Sender.SenderType.hospitalSystem
import gov.cdc.prime.router.Sender.SenderType.testManufacturer
import gov.cdc.prime.router.azure.SettingAPI
import gov.cdc.prime.router.tokens.Jwk
import gov.cdc.prime.router.tokens.JwkSet
import java.time.OffsetDateTime

/**
 * Used by senders to indicate that they carry a schema, so we can get the schema name
 */
interface HasSchema {
    var schemaName: String
}

/**
 * A `Sender` represents the agent that is sending reports to
 * the data hub (minus the credentials used by that agent, of course). It is an abstract base class that represents
 * either a full ELR sender or a Covid sender. In the former case, it will be used to pass data into the FHIR
 * pipeline, in the latter it will go through the legacy covid pipeline
 *
 * @property name the name of this sender - if only one send for an org, it is default
 * @property organizationName the name of the organization that this sender belongs to
 * @property format the primary format of the reports from the sender
 * @property topic the topic of the reports from the sender - determines if the sender is covid or full ELR
 * @property customerStatus the status of the sender active inactive
 * @property keys used to track server-to-server auths for this sender via public keys sets
 * @property processingType sync or async
 * @property allowDuplicates if false a duplicate submission will be rejected
 * @property senderType one of four broad sender categories
 * @property primarySubmissionMethod Sender preference for submission - manual or automatic
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "topic"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FullELRSender::class, name = "full-elr"),
    JsonSubTypes.Type(value = CovidSender::class, name = "covid-19"),
    JsonSubTypes.Type(value = MonkeypoxSender::class, name = "monkeypox")
)
abstract class Sender(
    val topic: Topic,
    override val name: String,
    override val organizationName: String,
    val format: Format,
    val customerStatus: CustomerStatus = CustomerStatus.INACTIVE,
    val keys: List<JwkSet>? = null,
    val processingType: ProcessingType = sync,
    val allowDuplicates: Boolean = true,
    val senderType: SenderType? = null,
    val primarySubmissionMethod: PrimarySubmissionMethod? = null,
    override var version: Int? = null,
    override var createdBy: String? = null,
    override var createdAt: OffsetDateTime? = null,
) : SettingAPI {

    /**
     * Makes a copy of the concrete Sender with a new scope and jwk
     */
    abstract fun makeCopyWithNewScopeAndJwk(scope: String, jwk: Jwk): Sender

    /**
     * Makes a copy of the concrete Sender
     */
    abstract fun makeCopy(): Sender

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
     * The format this sender makes submissions in
     */
    enum class Format(val mimeType: String) {
        CSV("text/csv"),
        HL7("application/hl7-v2"),
        FHIR("application/fhir+json")
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

    @get:JsonIgnore
    val fullName: String get() = ClientSource(organizationName, name).name

    /**
     * Calculate the customer's default processingModeCode based on their
     * CustomerStatus value. Note that the default customerStatus is
     * INACTIVE which will put that customer in "T" mode.
     * Official values for processing_mode_code are in hl70103.
     * Don't remove this - it is used via reflection, not by a direct usage.
     */
    @get:JsonIgnore
    val processingModeCode: String
        get() = when (customerStatus) {
            CustomerStatus.ACTIVE -> "P"
            CustomerStatus.INACTIVE -> "T"
            CustomerStatus.TESTING -> "T"
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

/**
 *  This sender represents a sender that is sending full ELR data, not just covid data. It has all the same parameters
 *  as the base Sender abstract class, although may be extended / modified in the future.
 */
class FullELRSender : Sender {
    @JsonCreator
    constructor(
        name: String,
        organizationName: String,
        format: Format,
        customerStatus: CustomerStatus = CustomerStatus.INACTIVE,
        keys: List<JwkSet>? = null,
        processingType: ProcessingType = sync,
        allowDuplicates: Boolean = true,
        senderType: SenderType? = null,
        primarySubmissionMethod: PrimarySubmissionMethod? = null
    ) : super(
        Topic.FULL_ELR,
        name,
        organizationName,
        format,
        customerStatus,
        keys,
        processingType,
        allowDuplicates,
        senderType,
        primarySubmissionMethod
    )

    constructor(copy: FullELRSender) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.customerStatus,
        if (copy.keys != null) ArrayList(copy.keys) else null
    )

    // constructor that copies and adds a key
    constructor(copy: FullELRSender, newScope: String, newJwk: Jwk) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.customerStatus,
        addJwkSet(copy.keys, newScope, newJwk)
    )

    /**
     * To ensure existing functionality, we need to be able to create a copy of this FullELRSender with
     * a different scope and jwk.
     */
    override fun makeCopyWithNewScopeAndJwk(scope: String, jwk: Jwk): Sender {
        return FullELRSender(this, scope, jwk)
    }

    /**
     * To ensure existing functionality, we need to be able to create a straight copy of this FullELRSender
     */
    override fun makeCopy(): Sender {
        return FullELRSender(this)
    }

    /**
     * For validation, not used in this context. Maybe refactor in the future.
     */
    override fun consistencyErrorMessage(metadata: Metadata): String? {
        return null
    }
}

open class TopicSender : Sender, HasSchema {
    final override var schemaName: String

    @JsonCreator
    constructor(
        name: String,
        organizationName: String,
        format: Format,
        customerStatus: CustomerStatus = CustomerStatus.INACTIVE,
        schemaName: String,
        topic: Topic,
        keys: List<JwkSet>? = null,
        processingType: ProcessingType = sync,
        allowDuplicates: Boolean = true,
        senderType: SenderType? = null,
        primarySubmissionMethod: PrimarySubmissionMethod? = null
    ) : super(
        topic,
        name,
        organizationName,
        format,
        customerStatus,
        keys,
        processingType,
        allowDuplicates,
        senderType,
        primarySubmissionMethod
    ) {
        this.schemaName = schemaName
    }

    constructor(copy: TopicSender) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.customerStatus,
        copy.schemaName,
        copy.topic,
        if (copy.keys != null) ArrayList(copy.keys) else null
    )

    // constructor that copies and adds a key
    constructor(copy: TopicSender, newScope: String, newJwk: Jwk) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.customerStatus,
        copy.schemaName,
        copy.topic,
        addJwkSet(copy.keys, newScope, newJwk)
    )

    /**
     * To ensure existing functionality, we need to be able to create a copy of this CovidSender with
     * a different scope and jwk.
     */
    override fun makeCopyWithNewScopeAndJwk(scope: String, jwk: Jwk): Sender {
        return TopicSender(this, scope, jwk)
    }

    /**
     * To ensure existing functionality, we need to be able to create a straight copy of this CovidSender
     */
    override fun makeCopy(): Sender {
        return TopicSender(this)
    }

    /**
     * For validation, not used in this context. Maybe refactor in the future.
     */
    override fun consistencyErrorMessage(metadata: Metadata): String? {
        return null
    }
}

/**
 * Represents a Sender that is specifically used for covid data, and not for full ELR data. The topic of this
 * sender will be 'COVID-19', and a [schemaName] must be specified
 *
 * @property schemaName the name of the schema used by the sender
 */
class CovidSender : TopicSender, HasSchema {
    @JsonCreator
    constructor(
        name: String,
        organizationName: String,
        format: Format,
        customerStatus: CustomerStatus = CustomerStatus.INACTIVE,
        schemaName: String,
        keys: List<JwkSet>? = null,
        processingType: ProcessingType = sync,
        allowDuplicates: Boolean = true,
        senderType: SenderType? = null,
        primarySubmissionMethod: PrimarySubmissionMethod? = null
    ) : super(
        name,
        organizationName,
        format,
        customerStatus,
        schemaName,
        Topic.COVID_19,
        keys,
        processingType,
        allowDuplicates,
        senderType,
        primarySubmissionMethod
    )

    constructor(copy: CovidSender) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.customerStatus,
        copy.schemaName,
        if (copy.keys != null) ArrayList(copy.keys) else null
    )

    // constructor that copies and adds a key
    constructor(copy: CovidSender, newScope: String, newJwk: Jwk) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.customerStatus,
        copy.schemaName,
        addJwkSet(copy.keys, newScope, newJwk)
    )

    /**
     * To ensure existing functionality, we need to be able to create a copy of this CovidSender with
     * a different scope and jwk.
     */
    override fun makeCopyWithNewScopeAndJwk(scope: String, jwk: Jwk): Sender {
        return CovidSender(this, scope, jwk)
    }

    /**
     * To ensure existing functionality, we need to be able to create a straight copy of this CovidSender
     */
    override fun makeCopy(): Sender {
        return CovidSender(this)
    }
}

/**
 * Our monkeypox sender
 */
class MonkeypoxSender : TopicSender, HasSchema {
    @JsonCreator
    constructor(
        name: String,
        organizationName: String,
        format: Format,
        customerStatus: CustomerStatus = CustomerStatus.INACTIVE,
        schemaName: String,
        keys: List<JwkSet>? = null,
        processingType: ProcessingType = sync,
        allowDuplicates: Boolean = true,
        senderType: SenderType? = null,
        primarySubmissionMethod: PrimarySubmissionMethod? = null
    ) : super(
        name,
        organizationName,
        format,
        customerStatus,
        schemaName,
        Topic.MONKEYPOX,
        keys,
        processingType,
        allowDuplicates,
        senderType,
        primarySubmissionMethod
    )

    constructor(copy: MonkeypoxSender) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.customerStatus,
        copy.schemaName,
        if (copy.keys != null) ArrayList(copy.keys) else null
    )

    // constructor that copies and adds a key
    constructor(copy: MonkeypoxSender, newScope: String, newJwk: Jwk) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.customerStatus,
        copy.schemaName,
        addJwkSet(copy.keys, newScope, newJwk)
    )

    /**
     * To ensure existing functionality, we need to be able to create a copy of this CovidSender with
     * a different scope and jwk.
     */
    override fun makeCopyWithNewScopeAndJwk(scope: String, jwk: Jwk): Sender {
        return MonkeypoxSender(this, scope, jwk)
    }

    /**
     * To ensure existing functionality, we need to be able to create a straight copy of this CovidSender
     */
    override fun makeCopy(): Sender {
        return MonkeypoxSender(this)
    }

    /**
     * For validation, not used in this context. Maybe refactor in the future.
     */
    override fun consistencyErrorMessage(metadata: Metadata): String? {
        return null
    }
}