package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import gov.cdc.prime.router.common.PhonePart
import gov.cdc.prime.router.common.PhoneUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.exceptions.PathEngineException
import org.hl7.fhir.r4.model.Address.AddressUse
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus
import org.hl7.fhir.r4.model.Enumeration
import org.hl7.fhir.r4.model.HumanName.NameUse
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TypeDetails
import org.hl7.fhir.r4.model.ValueSet
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import org.hl7.fhir.r4.model.codesystems.V3ActCode
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.hl7.fhir.r4.utils.FHIRPathEngine.IEvaluationContext.FunctionDetails

/**
 * Context used for resolving [constants].
 */
data class CustomContext(
    val bundle: Bundle,
    var focusResource: Base,
    val constants: MutableMap<String, String> = mutableMapOf()
) {
    companion object {
        /**
         * Add [constants] to a context.
         * @return a new context with the [constants] added or the existing context of no new constants are specified
         */
        fun addConstants(constants: Map<String, String>, previousContext: CustomContext): CustomContext {
            return if (constants.isEmpty()) previousContext
            else {
                val newContext = CustomContext(
                    previousContext.bundle,
                    previousContext.focusResource,
                    previousContext.constants.toMap().toMutableMap() // This makes a copy of the map
                )
                constants.forEach { newContext.constants[it.key] = it.value }
                newContext
            }
        }

        /**
         * Add constant with [key] and [value] to a context.
         * @return a new context with the constant added or the existing context of no new constant is specified
         */
        fun addConstant(key: String, value: String, previousContext: CustomContext): CustomContext {
            return addConstants(mapOf(key to value), previousContext)
        }
    }
}

/**
 * String substitution for constants.
 */
class ConstantSubstitutor {
    /**
     * The resolver.  Uses %{} instead of the default to match the FHIR path use of %.
     */
    private val constantResolver = StringSubstitutor().setVariablePrefix("%{").setEscapeChar('%')

    /**
     * Replace the constants in a given [inputText] using the [context].
     * @return the resolved string
     */
    fun replace(inputText: String, context: CustomContext?): String {
        return constantResolver.setVariableResolver(StringCustomResolver(context)).replace(inputText)
    }

    /**
     * Custom resolver for the [ConstantSubstitutor] that uses the [context] to resolve the constants.
     */
    internal class StringCustomResolver(val context: CustomContext?) : StringLookup, Logging {
        override fun lookup(key: String?): String {
            require(!key.isNullOrBlank())
            when {
                context == null -> throw HL7ConversionException("No context available to resolve constant '$key'")

                !context.constants.contains(key) || context.constants[key] == null ->
                    throw HL7ConversionException("Constant '$key' was not found in the provided context")

                else -> return context.constants[key]!!
            }
        }
    }
}

/**
 * Custom FHIR Function names used to map from the string used in the FHIR path
 * to the function name in the CustomFHIRFunctions class
 */
enum class CustomFHIRFunctionNames {
    GetAddressUse,
    GetPhoneNumberCountryCode,
    GetPhoneNumberAreaCode,
    GetPhoneNumberLocalNumber,
    GetPhoneNumberExtension,
    GetTelecomUseCode,
    GetNameUseCode,
    GetPatientClass,
    GetAdministrativeGenderCode,
    GetYesNoValue,
    GetObservationResultsStatus,
    GetCodingSystemMapping,
    Split,
    GetId,
    GetIdType,
    HasExtension
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
 * Custom FHIR functions created by report stream to help map from FHIR -> HL7
 * only used in cases when the same logic couldn't be accomplished using the FHIRPath
 */
object CustomFHIRFunctions {

    /**
     * Converts a FHIR AddressUse enum the [focus] to the correct HL7 v2.5.1 - 0190 - Address type
     * @return a mutable list containing the appropriate HL7 Address type string
     */
    fun getAddressUse(focus: MutableList<Base>): MutableList<Base> {
        return when (AddressUse.fromCode(focus[0].primitiveValue())) {
            AddressUse.HOME -> mutableListOf(StringType("H"))
            AddressUse.WORK -> mutableListOf(StringType("B"))
            AddressUse.TEMP -> mutableListOf(StringType("C"))
            else -> mutableListOf()
        }
    }

    /**
     * Converts a FHIR ContactPointUse enum the [focus] to the correct
     * HL7 v2.5.1 - 0201 - Telecommunication use code
     * @return a mutable list containing the appropriate HL7 telecommunication use string
     */
    fun getTelecomUseCode(focus: MutableList<Base>): MutableList<Base> {
        return when (ContactPointUse.fromCode((focus[0] as Enumeration<*>).code)) {
            ContactPointUse.HOME -> mutableListOf(StringType("PRN"))
            ContactPointUse.WORK -> mutableListOf(StringType("WPN"))
            ContactPointUse.MOBILE -> mutableListOf(StringType("CP"))
            else -> mutableListOf()
        }
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
    fun hasExtension(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        return mutableListOf(BooleanType(PhoneUtilities.hasExtension(primVal)))
    }

    /**
     * Gets the FHIR name use code stored in the [focus] element and converts to HL7 v2.5.1 - 0200 - Name type.
     * @return a mutable list containing the single character HL7 name type
     */
    fun getNameUseCode(focus: MutableList<Base>): MutableList<Base> {
        return when (NameUse.fromCode((focus[0] as Enumeration<*>).code)) {
            NameUse.OFFICIAL -> mutableListOf(StringType("L"))
            NameUse.USUAL -> mutableListOf(StringType("D"))
            NameUse.MAIDEN -> mutableListOf(StringType("M"))
            NameUse.NICKNAME -> mutableListOf(StringType("N"))
            NameUse.ANONYMOUS -> mutableListOf(StringType("S"))
            else -> mutableListOf()
        }
    }

    /**
     * Gets the FHIR V3 act code stored in the [focus] element and converts to HL7 v2.5.1
     * Some segments such as PV1.2 use string versions of this field in HL7 v2
     * @return a mutable list containing the HL7 single character version of the code
     */
    fun getPatientClass(focus: MutableList<Base>): MutableList<Base> {
        return when (V3ActCode.fromCode((focus[0] as CodeType).code)) {
            V3ActCode.EMER -> mutableListOf(StringType("E"))
            V3ActCode.IMP -> mutableListOf(StringType("I"))
            V3ActCode.PRENC -> mutableListOf(StringType("P"))
            V3ActCode.AMB -> mutableListOf(StringType("O"))
            else -> mutableListOf()
        }
    }

    /**
     * Gets the FHIR gender stored in the [focus] element
     * and converts to HL7 v2.5.1 - 0001 - Administrative Sex
     * @return a mutable list containing the HL7 single character version of the code
     */
    fun getAdministrativeGenderCode(focus: MutableList<Base>): MutableList<Base> {
        return when (AdministrativeGender.fromCode((focus[0] as Enumeration<*>).code)) {
            AdministrativeGender.UNKNOWN -> mutableListOf(StringType("U"))
            AdministrativeGender.FEMALE -> mutableListOf(StringType("F"))
            AdministrativeGender.MALE -> mutableListOf(StringType("M"))
            AdministrativeGender.OTHER -> mutableListOf(StringType("O"))
            else -> mutableListOf()
        }
    }

    /**
     * Gets the FHIR DiagnosticResultStatus stored in the [focus] element and converts
     * to an HL7 v2.5.1 - 0123 - Result Status.
     * @return a mutable list containing the single character HL7 result status
     */
    fun getObservationResultsStatus(focus: MutableList<Base>): MutableList<Base> {
        return when (DiagnosticReportStatus.fromCode((focus[0] as Enumeration<*>).code)) {
            // Partial could be either A or R I've chosen A, but we should probably figure out better answer
            DiagnosticReportStatus.PARTIAL -> mutableListOf(StringType("A"))
            DiagnosticReportStatus.CORRECTED -> mutableListOf(StringType("C"))
            DiagnosticReportStatus.FINAL -> mutableListOf(StringType("F"))
            DiagnosticReportStatus.PRELIMINARY -> mutableListOf(StringType("P"))
            DiagnosticReportStatus.CANCELLED -> mutableListOf(StringType("X"))
            // Unknown could be multiple values. Will map to empty using the else clause
            else -> mutableListOf()
        }
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
        } else
            mutableListOf()
    }

    /**
     * Translate a boolean-type value into the single character Y/N value used by HL7.
     * @return a mutable list containing the HL7 single character version of the code
     */
    fun getYesNoValue(focus: MutableList<Base>): MutableList<Base> {
        return when (focus[0].primitiveValue()) {
            "true" -> mutableListOf(StringType("Y"))
            "false" -> mutableListOf(StringType("N"))
            else -> mutableListOf(StringType(""))
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
     * Regex to identify CLIAs.  CLIAs are just 10 alphanumeric characters.
     */
    private val cliaRegex = """^[a-zA-Z\d]{10}$""".toRegex()

    /**
     * Get the ID for the value in [focus].
     * @return a list with one ID value or an empty list
     */
    fun getId(focus: MutableList<Base>): MutableList<Base> {
        val type = when {
            // We can only process one value and primitives
            focus.isEmpty() || focus.size > 1 || !focus[0].isPrimitive || focus[0].primitiveValue().isNullOrBlank() ->
                null

            // OIDs start with "urn:oid:"
            focus[0].primitiveValue().startsWith("urn:oid:") ->
                focus[0].primitiveValue().replace("urn:oid:", "")

            // A generic ID that starts with "urn:id:" is just an ID
            focus[0].primitiveValue().startsWith("urn:id:") ->
                focus[0].primitiveValue().replace("urn:id:", "")

            // A plain string with an OID or CLIA.
            focus[0] is StringType &&
                (focus[0].primitiveValue().matches(oidRegex) || focus[0].primitiveValue().matches(cliaRegex)) ->
                focus[0].primitiveValue()

            else -> focus[0].primitiveValue()
        }
        return if (type != null) mutableListOf(StringType(type)) else mutableListOf()
    }

    /**
     * Get the ID type for the value in [focus].
     * @return a list with one value denoting the ID type, or an empty list
     */
    fun getIdType(focus: MutableList<Base>): MutableList<Base> {
        val type = when {
            // We can only process one value and primitives
            focus.isEmpty() || focus.size > 1 || !focus[0].isPrimitive || focus[0].primitiveValue().isNullOrBlank() ->
                null

            // A URL that starts with urn:oid: is ISO
            focus[0].primitiveValue().startsWith("urn:oid:") -> "ISO"

            focus[0] is StringType && focus[0].primitiveValue().startsWith("urn:id:") &&
                focus[0].primitiveValue().replace("urn:id:", "").matches(cliaRegex) -> "CLIA"

            // A string with an OID is ISO
            focus[0] is StringType && focus[0].primitiveValue().matches(oidRegex) -> "ISO"

            // A string with a CLIA is CLIA
            focus[0] is StringType && focus[0].primitiveValue().matches(cliaRegex) -> "CLIA"

            else -> null
        }
        return if (type != null) mutableListOf(StringType(type)) else mutableListOf()
    }
}

/**
 * Custom resolver for the FHIR path engine.
 */
class FhirPathCustomResolver : FHIRPathEngine.IEvaluationContext, Logging {
    override fun resolveConstant(appContext: Any?, name: String?, beforeContext: Boolean): Base? {
        // Name is always passed in from the FHIR path engine
        require(!name.isNullOrBlank())

        val constantValue = when {
            appContext == null || appContext !is CustomContext ->
                throw PathEngineException("No context available to resolve constant '$name'")

            // Support prefix constant replacement like for URLs similar to %ext in FHIRPathEngine
            // E.g., '%resource.source.extension(%`rsext-source-software-vendor-org`).value' where
            // rsext is the defined constant.
            name.startsWith("`") -> {
                val constantNameParts = name.trimStart('`').split("-", limit = 2)
                when (constantNameParts.size) {
                    // 2 parts means the constant is a prefix and there is a suffix
                    2 -> {
                        val constantValue = appContext.constants[constantNameParts[0]]
                        constantValue?.let {
                            "$constantValue + '${constantNameParts[1].trimEnd('`')}'"
                        }
                    }

                    // 1 part means the constant is surrounded by `, useful for separating other text from the constant name
                    1 -> {
                        appContext.constants[constantNameParts[0].trimEnd('`')]
                    }

                    else -> null
                }
            }

            // Just a straight constant replacement
            appContext.constants.contains(name) -> appContext.constants[name]

            // Must return null as the resolver is called by the FhirPathEngine to test for non-constants too
            else -> null
        }

        // Evaluate the constant before it is used.
        return if (constantValue.isNullOrBlank()) null
        else {
            val values = FhirPathUtils.evaluate(appContext, appContext.focusResource, appContext.bundle, constantValue)
            if (values.size != 1)
                throw SchemaException("Constant $name must resolve to one value, but had ${values.size}.")
            else {
                logger.trace("Evaluated FHIR Path constant $name to: ${values[0]}")
                // Convert string constants that are whole integers to Integer type to facilitate math operations
                if (values[0] is StringType && StringUtils.isNumeric(values[0].primitiveValue()))
                    IntegerType(values[0].primitiveValue())
                else values[0]
            }
        }
    }

    override fun resolveConstantType(appContext: Any?, name: String?): TypeDetails {
        throw NotImplementedError("Not implemented")
    }

    override fun log(argument: String?, focus: MutableList<Base>?): Boolean {
        throw NotImplementedError("Not implemented")
    }

    override fun resolveFunction(functionName: String?): FunctionDetails {
        check(!functionName.isNullOrBlank())
        return when (CustomFHIRFunctionNames.valueOf(functionName.replaceFirstChar(Char::titlecase))) {
            CustomFHIRFunctionNames.GetAddressUse -> {
                FunctionDetails("convert FHIR address type to HL7", 0, 0)
            }
            CustomFHIRFunctionNames.GetPhoneNumberCountryCode -> {
                FunctionDetails("extract country code from FHIR phone number", 0, 0)
            }
            CustomFHIRFunctionNames.GetPhoneNumberAreaCode -> {
                FunctionDetails("extract country code from FHIR phone number", 0, 0)
            }
            CustomFHIRFunctionNames.GetPhoneNumberLocalNumber -> {
                FunctionDetails("extract country code from FHIR phone number", 0, 0)
            }
            CustomFHIRFunctionNames.GetPhoneNumberExtension -> {
                FunctionDetails("extract extension from FHIR phone number", 0, 0)
            }
            CustomFHIRFunctionNames.HasExtension -> {
                FunctionDetails("see if extension exists in FHIR phone number", 0, 0)
            }
            CustomFHIRFunctionNames.GetTelecomUseCode -> {
                FunctionDetails("convert FHIR contact point use to HL7 telecom use code", 0, 0)
            }
            CustomFHIRFunctionNames.GetNameUseCode -> {
                FunctionDetails("convert FHIR name use code and coverts it to HL7 name type code", 0, 0)
            }
            CustomFHIRFunctionNames.GetPatientClass -> {
                FunctionDetails("convert FHIR class code and coverts it to HL7 v3 act code", 0, 0)
            }
            CustomFHIRFunctionNames.GetAdministrativeGenderCode -> {
                FunctionDetails("convert FHIR class code and coverts it to HL7 gender", 0, 0)
            }
            CustomFHIRFunctionNames.GetYesNoValue -> {
                FunctionDetails("convert FHIR class code and coverts it to Y/N", 0, 0)
            }
            CustomFHIRFunctionNames.GetObservationResultsStatus -> {
                FunctionDetails("convert FHIR DiagnosticReportStatus to HL7 0123 - Result Status", 0, 0)
            }
            CustomFHIRFunctionNames.GetCodingSystemMapping -> {
                FunctionDetails("convert FHIR coding system url to HL7 ID", 0, 0)
            }
            CustomFHIRFunctionNames.Split -> {
                FunctionDetails("splits a string by provided delimeter", 1, 1)
            }
            CustomFHIRFunctionNames.GetId -> {
                FunctionDetails("extracts an ID from a resource property", 0, 0)
            }
            CustomFHIRFunctionNames.GetIdType -> {
                FunctionDetails("determines the ID type from a resource property", 0, 0)
            }
        }
    }

    override fun checkFunction(
        appContext: Any?,
        functionName: String?,
        parameters: MutableList<TypeDetails>?
    ): TypeDetails {
        throw NotImplementedError("Not implemented")
    }

    override fun executeFunction(
        appContext: Any?,
        focus: MutableList<Base>?,
        functionName: String?,
        parameters: MutableList<MutableList<Base>>?
    ): MutableList<Base> {
        check(focus != null)
        check(!functionName.isNullOrBlank())
        return (
            when (CustomFHIRFunctionNames.valueOf(functionName.replaceFirstChar(Char::titlecase))) {
                CustomFHIRFunctionNames.GetAddressUse -> {
                    CustomFHIRFunctions.getAddressUse(focus)
                }
                CustomFHIRFunctionNames.GetPhoneNumberCountryCode -> {
                    CustomFHIRFunctions.getPhoneNumberCountryCode(focus)
                }
                CustomFHIRFunctionNames.GetPhoneNumberAreaCode -> {
                    CustomFHIRFunctions.getPhoneNumberAreaCode(focus)
                }
                CustomFHIRFunctionNames.GetPhoneNumberLocalNumber -> {
                    CustomFHIRFunctions.getPhoneNumberLocalNumber(focus)
                }
                CustomFHIRFunctionNames.GetPhoneNumberExtension -> {
                    CustomFHIRFunctions.getPhoneNumberExtension(focus)
                }
                CustomFHIRFunctionNames.HasExtension -> {
                    CustomFHIRFunctions.hasExtension(focus)
                }
                CustomFHIRFunctionNames.GetTelecomUseCode -> {
                    CustomFHIRFunctions.getTelecomUseCode(focus)
                }
                CustomFHIRFunctionNames.GetNameUseCode -> {
                    CustomFHIRFunctions.getNameUseCode(focus)
                }
                CustomFHIRFunctionNames.GetPatientClass -> {
                    CustomFHIRFunctions.getPatientClass(focus)
                }
                CustomFHIRFunctionNames.GetAdministrativeGenderCode -> {
                    CustomFHIRFunctions.getAdministrativeGenderCode(focus)
                }
                CustomFHIRFunctionNames.GetYesNoValue -> {
                    CustomFHIRFunctions.getYesNoValue(focus)
                }
                CustomFHIRFunctionNames.GetObservationResultsStatus -> {
                    CustomFHIRFunctions.getObservationResultsStatus(focus)
                }
                CustomFHIRFunctionNames.GetCodingSystemMapping -> {
                    CustomFHIRFunctions.getCodingSystemMapping(focus)
                }
                CustomFHIRFunctionNames.Split -> {
                    CustomFHIRFunctions.split(focus, parameters)
                }
                CustomFHIRFunctionNames.GetId -> {
                    CustomFHIRFunctions.getId(focus)
                }
                CustomFHIRFunctionNames.GetIdType -> {
                    CustomFHIRFunctions.getIdType(focus)
                }
            }
            )
    }

    override fun resolveReference(appContext: Any?, url: String?): Base? {
        // Name is always passed in from the FHIR path engine
        require(!url.isNullOrBlank())

        return when (appContext) {
            null, !is CustomContext -> throw PathEngineException("No context available to resolve constant '$url'")
            else -> appContext.bundle.entry.find { it.fullUrl == url }?.resource
        }
    }

    override fun conformsToProfile(appContext: Any?, item: Base?, url: String?): Boolean {
        throw NotImplementedError("Not implemented")
    }

    override fun resolveValueSet(appContext: Any?, url: String?): ValueSet {
        throw NotImplementedError("Not implemented")
    }
}