package gov.cdc.prime.router.unittest

import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Schema

/**
 * Utilities specific to unit testing.
 */
object UnitTestUtils {

    /**
     * A simple schema for testing.
     */
    val simpleSchema = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))

    /**
     * A simple metadata instance that does not use the real configuration.
     */
    val simpleMetadata by lazy {
        Metadata(simpleSchema)
    }

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
        )
    }
}