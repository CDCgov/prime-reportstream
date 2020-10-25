package gov.cdc.prime.router

// The Element is the fundamental data element of the schema.
// It overlays many different schemas

data class Element(
    // A element can either be a new element or one based on previously defined element
    // - A name of form [A-Za-z0-9_]+ is a new element
    // - A name with a . in it is new element based on an previously defined element
    //
    val name: String,

    // General information
    val type: Type? = null,
    val format: String? = null,
    val valueSetId: String? = null,
    val valueSet: List<String>? = null, // If unspecified, any value is valid
    val required: Boolean? = null,
    val pii: Boolean? = null,
    val phi: Boolean? = null,
    val default: String? = null,

    // Correspondence to the national standards
    val hhsGuidanceField: String? = null,
    val uscdiField: String? = null,
    val natFlatFileField: String? = null,

    // Format specific information used to format the table

    // HL7 specific information
    val hl7Field: String? = null,

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
        CODED_HL7,
        HD,
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

    fun nameContains(substring: String): Boolean {
        return name.contains(substring, ignoreCase = true)
    }

    fun extendFrom(baseElement: Element): Element {
        return Element(
            name = this.name,
            type = this.type ?: baseElement.type,
            format = this.format ?: baseElement.format,
            valueSetId = this.valueSetId ?: baseElement.valueSetId,
            valueSet = this.valueSet ?: baseElement.valueSet,
            required = this.required ?: baseElement.required,
            pii = this.pii ?: baseElement.pii,
            phi = this.phi ?: baseElement.phi,
            default = this.default ?: baseElement.default,
            hhsGuidanceField = this.hhsGuidanceField ?: baseElement.hhsGuidanceField,
            uscdiField = this.uscdiField ?: baseElement.uscdiField,
            natFlatFileField = this.natFlatFileField ?: baseElement.natFlatFileField,
            hl7Field = this.hl7Field ?: baseElement.hl7Field,
            csvField = this.csvField ?: this.csvField,
        )
    }
}
