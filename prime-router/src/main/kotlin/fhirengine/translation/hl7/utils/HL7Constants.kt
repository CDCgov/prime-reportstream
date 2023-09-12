package gov.cdc.prime.router.fhirengine.translation.hl7.utils

object HL7Constants {

    /**
     *   From the HL7 2.5.1 Ch 2A spec...
     *
     *   The Hierarchical Designator identifies an entity that has responsibility for managing or
     *   assigning a defined set of instance identifiers.
     *
     *   The HD is designed to be used either as a local identifier (with only the <namespace ID> valued)
     *   or a publicly-assigned identifier, a UID (<universal ID> and <universal ID type> both valued)
     */

    /**
     * List of fields that have the local HD type.
     */
    val HD_FIELDS_LOCAL = listOf(
        "MSH-3-1",
        "MSH-4-1",
        "OBR-3-2",
        "OBR-2-2",
        "ORC-3-2",
        "ORC-2-2",
        "ORC-4-2",
        "PID-3-4-1",
        "PID-3-6-1",
        "SPM-2-1-2",
        "SPM-2-2-2"
    )

    /**
     * List of fields that have the universal HD type
     */
    val HD_FIELDS_UNIVERSAL = listOf(
        "MSH-3-2",
        "MSH-4-2",
        "OBR-3-3",
        "OBR-2-3",
        "ORC-3-3",
        "ORC-2-3",
        "ORC-4-3",
        "PID-3-4-2",
        "PID-3-6-2",
        "SPM-2-1-3",
        "SPM-2-2-3"
    )

    /**
     * List of fields that have a CE type. Note: this is only really used in places
     * where we need to put a CLIA marker in the field as well and there are a
     * lot of CE fields that are *NOT* CLIA fields, so use this correctly.
     */
    val CE_FIELDS = listOf("OBX-15-1")

    /** The max length for the formatted text type (FT) in HL7 */
    const val MAX_FORMATTED_TEXT_LENGTH = 65536

    /** the length to truncate HD values to. Defaults to 20 */
    const val HD_TRUNCATION_LIMIT = 20

    // Component specific sub-component length from HL7 specification Chapter 2A
    private val CE_MAX_LENGTHS = listOf(20, 199, 20, 20, 199, 20)
    private val CWE_MAX_LENGTHS = listOf(20, 199, 20, 20, 199, 20, 10, 10, 199)
    private val CX_MAX_LENGTHS = listOf(15, 1, 3, 227, 5, 227, 5, 227, 8, 8, 705, 705)
    private val EI_MAX_LENGTHS = listOf(199, 20, 199, 6)
    private val EIP_MAX_LENGTHS = listOf(427, 427)
    private val HD_MAX_LENGTHS = listOf(20, 199, 6)
    private val XTN_MAX_LENGTHS = listOf(199, 3, 8, 199, 3, 5, 9, 5, 199, 4, 6, 199)
    private val XAD_MAX_LENGTHS = listOf(184, 120, 50, 50, 12, 3, 3, 50, 20, 20, 1, 53, 26, 26)
    private val XCN_MAX_LENGTHS = listOf(
        15, 194, 30, 30, 20, 20, 5, 4, 227, 1, 1, 3, 5, 227, 1, 483, 53, 1, 26, 26, 199, 705, 705
    )
    private val XON_MAX_LENGTHS = listOf(50, 20, 4, 1, 3, 227, 5, 227, 1, 20)
    private val XPN_MAX_LENGTHS = listOf(194, 30, 30, 20, 20, 6, 1, 1, 483, 53, 1, 26, 26, 199)

    /**
     * Component length table for composite HL7 types taken from HL7 specification Chapter 2A.
     */
    private val hl7ComponentMaxLength = mapOf(
        "CE" to CE_MAX_LENGTHS,
        "CWE" to CWE_MAX_LENGTHS,
        "CX" to CX_MAX_LENGTHS,
        "EI" to EI_MAX_LENGTHS,
        "EIP" to EIP_MAX_LENGTHS,
        "HD" to HD_MAX_LENGTHS,
        "XAD" to XAD_MAX_LENGTHS,
        "XCN" to XCN_MAX_LENGTHS,
        "XON" to XON_MAX_LENGTHS,
        "XPN" to XPN_MAX_LENGTHS,
        "XTN" to XTN_MAX_LENGTHS
        // Extend further here
    )

    fun getHL7ComponentMaxLengthList(componentName: String): List<Int>? {
        return hl7ComponentMaxLength[componentName]
    }
}