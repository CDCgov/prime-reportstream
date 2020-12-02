package gov.cdc.prime.router


/**
 * An element is represents a data element (ie. a single logical value) that is contained in single row
 * of a report. A set of Elements form the main content of a Schema.
 *
 * In some sense the element is like the data element in other data schemas that engineers are familiar with.
 * For the data-hub, the data element contains information specific to public health. For a given topic,
 * there is a "standard" schema with elements. The logically the mapping process is:
 *
 *    Schema 1 -> Standard Standard -> schema 2
 *
 * To describe the intent of a element there are references to the national standards.
 */
data class Element(
    // A element can either be a new element or one based on previously defined element
    // - A name of form [A-Za-z0-9_]+ is a new element
    // - A name with a . in it is new element based on an previously defined element
    //
    val name: String,

    // General information
    val type: Type? = null,
    val format: String? = null,
    val valueSet: String? = null,
    val required: Boolean? = null,
    val pii: Boolean? = null,
    val phi: Boolean? = null,
    val default: String? = null,
    val mapper: String? = null,

    // Correspondence to the national standards
    val hhsGuidanceField: String? = null,
    val uscdiField: String? = null,
    val natFlatFileField: String? = null,

    // Format specific information used to format the table

    // HL7 specific information
    val hl7Field: String? = null,
    val hl7OutputFields: List<String>? = null,

    // CSV specific information
    val csvField: String? = null,

    // FHIR specific information
    val fhirField: String? = null,

    // a field to let us incorporate documentation data (markdown)
    // in the schema files so we can generate documentation off of
    // the file
    val documentation: String? = null,
) {
    enum class Type {
        TEXT,
        NUMBER,
        DATE,
        DATETIME,
        DURATION,
        CODE,
        HD,    // Hierarchic Designator
        ID,
        ID_CLIA,
        ID_DLN,
        ID_SSN,
        ID_NPI,
        STREET,
        CITY,
        STATE,
        COUNTY,
        POSTAL_CODE,
        PERSON_NAME,
        TELEPHONE,
        EMAIL,
    }

    val isCodeType get() = this.type == Type.CODE
    val isCode get() = this.isCodeType && !name.contains('#')
    val isCodeText get() = this.isCodeType && name.endsWith("#text")
    val isCodeSystem get() = this.isCodeType && name.endsWith("#system")
    val nameAsCode get() = if (name.contains('#')) name.split('#')[0] else name
    val nameAsCodeText get() = if (isCodeType) "$nameAsCode#text" else name
    val nameAsCodeSystem get() = if (isCodeType) "$nameAsCode#system" else name

    fun nameContains(substring: String): Boolean {
        return name.contains(substring, ignoreCase = true)
    }

    fun extendFrom(baseElement: Element): Element {
        return Element(
            name = this.name,
            type = this.type ?: baseElement.type,
            format = this.format ?: baseElement.format,
            valueSet = this.valueSet ?: baseElement.valueSet,
            required = this.required ?: baseElement.required,
            pii = this.pii ?: baseElement.pii,
            phi = this.phi ?: baseElement.phi,
            mapper = this.mapper ?: baseElement.mapper,
            default = this.default ?: baseElement.default,
            hhsGuidanceField = this.hhsGuidanceField ?: baseElement.hhsGuidanceField,
            uscdiField = this.uscdiField ?: baseElement.uscdiField,
            natFlatFileField = this.natFlatFileField ?: baseElement.natFlatFileField,
            hl7Field = this.hl7Field ?: baseElement.hl7Field,
            hl7OutputFields = this.hl7OutputFields ?: baseElement.hl7OutputFields,
            csvField = this.csvField ?: this.csvField,
            documentation = this.documentation ?: baseElement.documentation,
        )
    }
}
