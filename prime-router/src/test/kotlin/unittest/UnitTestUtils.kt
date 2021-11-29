package gov.cdc.prime.router.unittest

import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Schema

/**
 * Utilities specific to unit testing.
 */
class UnitTestUtils {
    companion object {
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

        @Deprecated(
            "Unit tests should use minimal self-contained metadata instance.",
            ReplaceWith("simpleSchema"), DeprecationLevel.WARNING
        )
        val testMetadata by lazy { Metadata("./src/test/resources/metadata") }
    }
}