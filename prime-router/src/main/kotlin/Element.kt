package gov.cdc.prime.router

// The Element is the fundamental data element of the schema.
// It overlays many different schemas

data class Element(
    val name: String,
    val basedOn: String? = null,

    // General information
    val type: Type? = null,
    val format: String? = null,
    val valueSetId: String? = null,
    val valueSet: List<String> = emptyList(),
    val required: Boolean = false,
    val pii: Boolean = false,
    val phi: Boolean = false,
    val default: String? = null,

    // Correspondence to the national standards
    val hhsGuidanceField: String? = null,
    val uscdiField: String? = null,
    val natFlatFileField: String? = null,

    // Format specific information used to format the table

    // HL7 specific information
    val hl7Field: String? = null,
    val hl7Operation: String? = null,
    val hl7Validation: String? = null,
    val hl7Template: String? = null,
    // CSV specific information
    val csvField: String? = null,
) {
    enum class Type {
        TEXT,
        NUMBER,
        DATE,
        DATETIME,
        DURATION,
        CODED,
        CODED_LONIC,
        CODED_SNOMED,
        ID,
        ID_DLN,
        ID_SSN,
        STREET,
        CITY,
        STATE,
        COUNTY,
        POSTAL_CODE,
        PERSON_NAME,
        TELEPHONE,
        EMAIL,
    }
}
