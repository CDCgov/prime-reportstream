package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import org.apache.commons.lang3.math.NumberUtils
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.exceptions.PathEngineException
import org.hl7.fhir.r4.model.Address.AddressUse
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse
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
data class CustomContext(val bundle: Bundle, val constants: MutableMap<String, String> = mutableMapOf()) {
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
object ConstantSubstitutor {
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
            if (context == null) throw HL7ConversionException("No context available to resolve constant '$key'")
            else if (!context.constants.contains(key)) {
                throw HL7ConversionException("'$key' was not found in the provided context")
            }
            return context.constants[key] ?: ""
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
    GetTelecomUseCode,
    GetNameUseCode,
    GetPatientClass,
    GetAdministrativeGenderCode,
    GetYesNoValue,
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
        return when (AddressUse.fromCode(focus[0].toString())) {
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
     * Regex representing the FHIR phone format generated by the linux4health library
     */
    private val regex = """([+](\d{1,3}|1-\d{3}) (\d{3})|\((\d{3})\)) (\d{3}) (\d{4,6})""".toRegex()

    /**
     * Gets the phone number country code from the full FHIR phone number stored in
     * the [focus] element.
     * @return a mutable list containing the country code
     */
    fun getPhoneNumberCountryCode(focus: MutableList<Base>): MutableList<Base> {
        val matchResult = regex.find(focus[0].toString())
        val countryCode = matchResult?.groups?.get(2)?.value?.toInt()
        return if (countryCode != null) {
            mutableListOf(IntegerType(countryCode))
        } else mutableListOf()
    }

    /**
     * Gets the phone number area code from the full FHIR phone number stored in
     * the [focus] element.
     * @return a mutable list containing the phone number area code
     */
    fun getPhoneNumberAreaCode(focus: MutableList<Base>): MutableList<Base> {
        val matchResult = regex.find(focus[0].toString())
        val areaCode = matchResult?.groups?.get(3)?.value?.toInt() ?: matchResult?.groups?.get(4)?.value?.toInt()
        return if (areaCode != null) {
            mutableListOf(IntegerType(areaCode))
        } else mutableListOf()
    }

    /**
     * Gets the local phone number from the full FHIR phone number stored in
     * the [focus] element.
     * @return a mutable list containing the local number
     */
    fun getPhoneNumberLocalNumber(focus: MutableList<Base>): MutableList<Base> {
        val matchResult = regex.find(focus[0].toString())
        val localNumber = "${matchResult?.groups?.get(5)?.value}${matchResult?.groups?.get(6)?.value}"
        return mutableListOf(IntegerType(localNumber))
    }

    /**
     * Gets the FHIR name use code stored in the [focus] element and converts to HL7 v2.5 - 0200 - Name type.
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
}

/**
 * Custom resolver for the FHIR path engine.
 */
class FhirPathCustomResolver : FHIRPathEngine.IEvaluationContext {
    override fun resolveConstant(appContext: Any?, name: String?, beforeContext: Boolean): Base? {
        // Name is always passed in from the FHIR path engine
        require(!name.isNullOrBlank())

        return when {
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
                            StringType(constantValue + constantNameParts[1].trimEnd('`'))
                        }
                    }

                    // 1 part means the constant is surrounded by `, useful for separating other text from the constant name
                    1 -> {
                        val constantValue = appContext.constants[constantNameParts[0].trimEnd('`')]
                        constantValue?.let {
                            StringType(constantValue)
                        }
                    }

                    else -> null
                }
            }

            // Just a straight constant replacement
            appContext.constants.contains(name) -> {
                // Return the type depending on the contents of the variable
                if (NumberUtils.isDigits(appContext.constants[name])) {
                    IntegerType(NumberUtils.createInteger(appContext.constants[name]))
                } else StringType(appContext.constants[name])
            }
            // Must return null as the resolver is called by the FhirPathEngine to test for non-constants too
            else -> null
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