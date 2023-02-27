package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.common.PhonePart
import gov.cdc.prime.router.common.PhoneUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.metadata.LivdLookup
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.utils.FHIRPathEngine

/**
 * Custom FHIR functions created by report stream to help map from FHIR -> HL7
 * only used in cases when the same logic couldn't be accomplished using the FHIRPath
 */
object CustomFHIRFunctions {

    /**
     * Custom FHIR Function names used to map from the string used in the FHIR path
     * to the function name in the CustomFHIRFunctions class
     */
    enum class CustomFHIRFunctionNames {
        GetPhoneNumberCountryCode,
        GetPhoneNumberAreaCode,
        GetPhoneNumberLocalNumber,
        GetPhoneNumberExtension,
        GetCodingSystemMapping,
        Split,
        GetId,
        GetIdType,
        HasPhoneNumberExtension,
        LivdTableLookup;

        companion object {
            /**
             * Get from a [functionName].
             * @return the function name enum or null if not found
             */
            fun get(functionName: String?): CustomFHIRFunctionNames? {
                return try {
                    functionName?.let { CustomFHIRFunctionNames.valueOf(it.replaceFirstChar(Char::titlecase)) }
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }

    /**
     * Get the function details for a given [functionName].
     * @return the function details
     */
    fun resolveFunction(functionName: String?): FHIRPathEngine.IEvaluationContext.FunctionDetails? {
        return when (CustomFHIRFunctionNames.get(functionName)) {
            CustomFHIRFunctionNames.GetPhoneNumberCountryCode -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails("extract country code from FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.GetPhoneNumberAreaCode -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails("extract country code from FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.GetPhoneNumberLocalNumber -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails("extract country code from FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.GetPhoneNumberExtension -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails("extract extension from FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.HasPhoneNumberExtension -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails("see if extension exists in FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.GetCodingSystemMapping -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails("convert FHIR coding system url to HL7 ID", 0, 0)
            }

            CustomFHIRFunctionNames.Split -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails("splits a string by provided delimeter", 1, 1)
            }

            CustomFHIRFunctionNames.GetId -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails("extracts an ID from a resource property", 0, 0)
            }

            CustomFHIRFunctionNames.GetIdType -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails(
                    "determines the ID type from a resource property",
                    0,
                    0
                )
            }

            CustomFHIRFunctionNames.LivdTableLookup -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails(
                    "looks up data in the LIVD table that match the information provided",
                    1,
                    1
                )
            }

            else -> null
        }
    }

    /**
     * Execute the function on a [focus] resource for a given [functionName] and [parameters].
     * @return the function result
     */
    fun executeFunction(
        focus: MutableList<Base>?,
        functionName: String?,
        parameters: MutableList<MutableList<Base>>?
    ): MutableList<Base> {
        check(focus != null)
        return (
            when (CustomFHIRFunctionNames.get(functionName)) {
                CustomFHIRFunctionNames.GetPhoneNumberCountryCode -> {
                    getPhoneNumberCountryCode(focus)
                }

                CustomFHIRFunctionNames.GetPhoneNumberAreaCode -> {
                    getPhoneNumberAreaCode(focus)
                }

                CustomFHIRFunctionNames.GetPhoneNumberLocalNumber -> {
                    getPhoneNumberLocalNumber(focus)
                }

                CustomFHIRFunctionNames.GetPhoneNumberExtension -> {
                    getPhoneNumberExtension(focus)
                }

                CustomFHIRFunctionNames.HasPhoneNumberExtension -> {
                    hasPhoneNumberExtension(focus)
                }

                CustomFHIRFunctionNames.GetCodingSystemMapping -> {
                    getCodingSystemMapping(focus)
                }

                CustomFHIRFunctionNames.Split -> {
                    split(focus, parameters)
                }

                CustomFHIRFunctionNames.GetId -> {
                    getId(focus)
                }

                CustomFHIRFunctionNames.GetIdType -> {
                    getIdType(focus)
                }

                CustomFHIRFunctionNames.LivdTableLookup -> {
                    livdTableLookup(focus, parameters)
                }

                else -> throw IllegalStateException("Tried to execute invalid FHIR Path function $functionName")
            }
            )
    }

    /**
     * Gets the phone number country code from the full FHIR phone number stored in
     * the [focus] element.
     * @return a mutable list containing the country code
     */
    fun getPhoneNumberCountryCode(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        val part = PhoneUtilities.getPhoneNumberPart(primVal, PhonePart.Country)
        return if (part != null) mutableListOf(IntegerType(part)) else mutableListOf()
    }

    /**
     * Gets the phone number area code from the full FHIR phone number stored in
     * the [focus] element.
     * @return a mutable list containing the phone number area code
     */
    fun getPhoneNumberAreaCode(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        val part = PhoneUtilities.getPhoneNumberPart(primVal, PhonePart.AreaCode)
        return if (part != null) mutableListOf(IntegerType(part)) else mutableListOf()
    }

    /**
     * Gets the local phone number from the full FHIR phone number stored in
     * the [focus] element.
     * @return a mutable list containing the local number
     */
    fun getPhoneNumberLocalNumber(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        val part = PhoneUtilities.getPhoneNumberPart(primVal, PhonePart.Local)
        return if (part != null) mutableListOf(IntegerType(part)) else mutableListOf()
    }

    /**
     * Gets the extension from the full FHIR phone number stored in the [focus] element.
     * @return a mutable list containing the extension if present
     */
    fun getPhoneNumberExtension(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        val part = PhoneUtilities.getPhoneNumberPart(primVal, PhonePart.Extension)
        return if (part != null) mutableListOf(IntegerType(part)) else mutableListOf()
    }

    /**
     * Determines if the [focus] passed in is a phone number with an extension.
     * @return a boolean indicating if the focus has an extension or not
     */
    fun hasPhoneNumberExtension(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        return mutableListOf(BooleanType(PhoneUtilities.hasPhoneNumberExtension(primVal)))
    }

    /**
     * Splits the [focus] into multiple strings using the delimeter provided in [parameters]
     * @returns list of strings
     */
    fun split(focus: MutableList<Base>, parameters: MutableList<MutableList<Base>>?): MutableList<Base> {
        return if (!parameters.isNullOrEmpty() &&
            parameters.size == 1 &&
            parameters.first().size == 1 &&
            focus.size == 1 &&
            focus.first() is StringType
        ) {
            val delimeter = (parameters.first().first()).primitiveValue()
            val stringToSplit = focus.first().primitiveValue()

            stringToSplit.split(delimeter).map { StringType(it) }.toMutableList()
        } else mutableListOf()
    }

    /**
     * Enum representing the CodingSystemMapping.
     * FHIR, urls [fhirURL] and their corresponding HL7 Ids [hl7ID]
     */
    enum class CodingSystemMapper(val fhirURL: String, val hl7ID: String) {
        ICD10("http://hl7.org/fhir/sid/icd-10-cm", "I10"),
        LOINC("http://loinc.org", "LN"),
        SNOMED_CLINICAL("http://snomed.info/sct", "SCT"),
        HL70189("http://terminology.hl7.org/CodeSystem/v2-0189", "HL70189"),
        HL70006("http://terminology.hl7.org/CodeSystem/v2-0006", "HL70006"),
        NONE("", "");

        companion object {
            /**
             * Get a coding system mapper by its [fhirURL]
             * @return an enum instance representing the appropriate mapping
             */
            fun getByFhirUrl(fhirURL: String): CodingSystemMapper {
                return CodingSystemMapper.values().find {
                    it.fhirURL == fhirURL
                } ?: NONE
            }
        }
    }

    /**
     * Gets the FHIR System URL stored in the [focus] element and maps it to the appropriate
     * HL7 v2.5.1 - 0396 - Coding system.
     * @return a mutable list containing the single character HL7 result status
     */
    fun getCodingSystemMapping(focus: MutableList<Base>): MutableList<Base> {
        return mutableListOf(StringType(CodingSystemMapper.getByFhirUrl(focus[0].primitiveValue()).hl7ID))
    }

    /**
     * Regex to identify OIDs.  Source: https://www.hl7.org/fhir/datatypes.html#oid
     */
    private val oidRegex = """^[0-2](.(0|[1-9]\d*))+$""".toRegex()

    /**
     * Regex to identify CLIAs.
     * CLIAs are 10-character strings
     * The first char is alphanumeric, the 3rd is always a letter (usually "D").
     * Every other char is numeric.
     */
    private val cliaRegex = """^[a-zA-Z\d]\d[a-zA-Z]\d{7}$""".toRegex()

    /**
     * Regex to extract type and ID from a URN. Note the Clia NID is not part of the mapping docs.
     */
    private val urnRegex = """^urn:(oid|uuid|dns|uri|clia|id):(.*)$""".toRegex()

    /**
     * Regex to extract type and ID from an unknown ID type
     */
    private val unknownId = """^(.*)-(.*):(.*)$""".toRegex()

    /**
     * Get the ID for the value in [focus].
     * @return a list with one ID value or an empty list
     */
    fun getId(focus: MutableList<Base>): MutableList<Base> {
        val type = when {
            // We can only process one value and primitives
            focus.isEmpty() || focus.size > 1 || !focus[0].isPrimitive || focus[0].primitiveValue().isNullOrBlank() ->
                null

            urnRegex.matches(focus[0].primitiveValue()) -> {
                val matches = urnRegex.matchEntire(focus[0].primitiveValue())
                matches?.groupValues?.get(2)
            }

            unknownId.matches(focus[0].primitiveValue()) -> {
                val matches = unknownId.matchEntire(focus[0].primitiveValue())
                matches?.groupValues?.get(3)
            }

            else -> focus[0].primitiveValue()
        }
        return if (type != null) mutableListOf(StringType(type)) else mutableListOf()
    }

    /**
     * Get the ID type for the value in [focus].
     * @return a list with one value denoting the ID type, or an empty list
     */
    fun getIdType(focus: MutableList<Base>): MutableList<Base> {
        val oidType = "ISO"
        val cliaType = "CLIA"
        val type = when {
            // We can only process one value and primitives
            focus.isEmpty() || focus.size > 1 || !focus[0].isPrimitive || focus[0].primitiveValue().isNullOrBlank() ->
                null

            urnRegex.matches(focus[0].primitiveValue()) -> {
                val matches = urnRegex.matchEntire(focus[0].primitiveValue())
                when (val type = matches?.groupValues?.get(1)?.uppercase()) {
                    "OID" -> oidType
                    "ID" -> null
                    else -> type
                }
            }

            unknownId.matches(focus[0].primitiveValue()) -> {
                val matches = unknownId.matchEntire(focus[0].primitiveValue())
                val universalId = matches?.groupValues?.get(3)
                when {
                    universalId == null -> null
                    universalId.matches(oidRegex) -> oidType
                    universalId.matches(cliaRegex) -> cliaType
                    // Return the stated type if any
                    else -> matches.groupValues[2]
                }
            }

            focus[0].primitiveValue().matches(cliaRegex) -> cliaType

            else -> null
        }
        return if (type != null) mutableListOf(StringType(type)) else mutableListOf()
    }

    /**
     * Get the LOINC Code from the LIVD table based on the device id, equipment model id, test kit name id, or the
     * element model name
     * @return a list with one value denoting the LOINC Code, or an empty list
     */
    fun livdTableLookup(
        focus: MutableList<Base>,
        parameters: MutableList<MutableList<Base>>?,
        metadata: Metadata = Metadata.getInstance()
    ): MutableList<Base> {
        val lookupTable = metadata.findLookupTable(name = LivdLookup.livdTableName)

        if (focus.size != 1) {
            throw SchemaException("Must call the livdTableLookup function on a single observation")
        }

        val observation = focus.first()
        if (observation !is Observation) {
            throw SchemaException("Must call the livdTableLookup function on an observation")
        }

        var result: String? = ""
        // Maps to OBX 17 CWE.1 Which is coding[1].code
        val testPerformedCode = (observation as Observation?)?.code?.coding?.firstOrNull()?.code
        val deviceId = (observation as Observation?)?.method?.coding?.firstOrNull()?.code
        if (!deviceId.isNullOrEmpty()) {
            result = LivdLookup.find(
                testPerformedCode = testPerformedCode,
                processingModeCode = null,
                deviceId = deviceId,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = null,
                tableRef = lookupTable,
                tableColumn = parameters!!.first().first().primitiveValue()
            )
        }

        // Maps to OBX 18 which is mapped to Device.identifier
        val equipmentModelId = (observation.device.resource as Device?)?.identifier?.firstOrNull()?.id
        if (!result.isNullOrBlank() && !equipmentModelId.isNullOrEmpty()) {
            result = LivdLookup.find(
                testPerformedCode = testPerformedCode,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = equipmentModelId,
                testKitNameId = null,
                equipmentModelName = null,
                tableRef = lookupTable,
                tableColumn = parameters!!.first().first().primitiveValue()
            )
        }

        val deviceName = (observation.device.resource as Device?)?.deviceName?.first()?.name
        if (!result.isNullOrBlank() && !deviceName.isNullOrBlank()) {
            result = LivdLookup.find(
                testPerformedCode = testPerformedCode,
                processingModeCode = null,
                deviceId = null,
                equipmentModelId = null,
                testKitNameId = null,
                equipmentModelName = deviceName,
                tableRef = lookupTable,
                tableColumn = parameters!!.first().first().primitiveValue()
            )
        }

        return if (result.isNullOrBlank()) {
            mutableListOf(StringType(null))
        } else {
            mutableListOf(StringType(result))
        }
    }
}