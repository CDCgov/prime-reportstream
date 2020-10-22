package gov.cdc.prime.router

// The Element is the fundamental data element of the schema.
// It overlays many different schemas

data class Element(
    val name: String,
    val type: Type = Type.TEXT,
    val format: String = "",
    val codeSystem: CodeSystem = CodeSystem.NONE,
    val code: String = "",
    val optional: Boolean = true,
    val pii: Boolean = false,
    val phi: Boolean = false,
    val default: String? = "",
    val hl7_field: String? = null,
    val hl7_operation: String? = null,
    val hl7_validation: String? = null,
    val hl7_template: String? = null,
    val csv_field: String? = null,
) {
    enum class Type {
        TEXT,
        NUMBER,
        DATE,
        DURATION,
        CODED,
        ID,
        ID_DLN,
        ID_SSN,
        ADDRESS,
        POSTAL_CODE,
        PERSON_NAME,
        TELEPHONE,
        EMAIL,
    }

    enum class CodeSystem {
        NONE,
        LOINC,
        SNOMED
    }
}
