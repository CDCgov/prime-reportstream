package gov.cdc.prime.router.fhirengine.translation.hl7.config

/**
 * HL7 truncation configuration options
 */
data class TruncationConfig(
    val truncateHDNamespaceIds: Boolean,
    val truncateHl7Fields: Set<String>,
    val customLengthHl7Fields: Map<String, Int> = emptyMap(),
) {
    companion object {
        val EMPTY = TruncationConfig(false, emptySet(), emptyMap())
    }
}