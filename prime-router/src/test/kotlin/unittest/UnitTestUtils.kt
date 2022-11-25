package gov.cdc.prime.router.unittest

import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Topic

/**
 * Utilities specific to unit testing.
 */
object UnitTestUtils {

    /**
     * A new instance of a simple schema for testing. Note a new schema instance is created each time this
     * [simpleSchema] variable is referenced, so unit tests do not interfere with each other.
     */
    val simpleSchema
        get() =
            Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))

    /**
     * A new instance of a simple metadata instance that does not use the real configuration. Note a new
     * Metadata instance is created each time this [simpleMetadata] variable is referenced, so unit tests do not
     * interfere with each other.
     */
    val simpleMetadata get() = Metadata(simpleSchema)

    /** returns a "mocked" hl7Config class */
    fun createConfig(
        replaceValue: Map<String, String> = emptyMap(),
        cliaForSender: Map<String, String> = emptyMap(),
        cliaForOutOfStateTesting: String? = null,
        truncateHl7Fields: String? = null,
        suppressNonNPI: Boolean = false,
        truncateHDNamespaceIds: Boolean = false,
        convertPositiveDateTimeOffsetToNegative: Boolean = false,
        useHighPrecisionHeaderDateTimeFormat: Boolean = false,
        convertDateTimesToReceiverLocalTime: Boolean = false,
        useTestProcessingMode: Boolean = false,
        schemaName: String = "covid-19"
    ): Hl7Configuration {
        return Hl7Configuration(
            messageProfileId = "",
            receivingApplicationOID = "",
            receivingApplicationName = "",
            receivingFacilityName = "",
            receivingFacilityOID = "",
            receivingOrganization = "",
            cliaForOutOfStateTesting = cliaForOutOfStateTesting,
            cliaForSender = cliaForSender,
            replaceValue = replaceValue,
            truncateHl7Fields = truncateHl7Fields,
            suppressNonNPI = suppressNonNPI,
            truncateHDNamespaceIds = truncateHDNamespaceIds,
            convertPositiveDateTimeOffsetToNegative = convertPositiveDateTimeOffsetToNegative,
            useHighPrecisionHeaderDateTimeFormat = useHighPrecisionHeaderDateTimeFormat,
            convertDateTimesToReceiverLocalTime = convertDateTimesToReceiverLocalTime,
            useTestProcessingMode = useTestProcessingMode,
            schemaName = schemaName
        )
    }
}