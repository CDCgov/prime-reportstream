package gov.cdc.prime.router.fhirengine.engine

/**
 * This class represents a way to group message types from an RS perspective.  As we add additional logical
 * groupings, FHIRBundleHelpers.getRSMessageType will need to be updated.
 *
 */
enum class RSMessageType {
    LAB_RESULT,
    UNKNOWN,
}