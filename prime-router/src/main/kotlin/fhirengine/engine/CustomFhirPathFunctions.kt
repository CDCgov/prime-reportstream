package fhirengine.engine

import fhirengine.translation.hl7.utils.FhirPathFunctions
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.metadata.LivdLookup
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.utils.FHIRPathUtilityClasses.FunctionDetails

/**
 * Custom FHIR functions created by report stream to help map from FHIR -> HL7
 * only used in cases when the same logic couldn't be accomplished using the FHIRPath
 */
class CustomFhirPathFunctions : FhirPathFunctions {

    /**
     * Custom FHIR Function names used to map from the string used in the FHIR path
     * to the function name in the CustomFHIRFunctions class
     */
    enum class CustomFhirPathFunctionNames {

        LivdTableLookup,
        FIPSCountyLookup,
        ;

        companion object {
            /**
             * Get from a [functionName].
             * @return the function name enum or null if not found
             */
            fun get(functionName: String?): CustomFhirPathFunctionNames? {
                return try {
                    functionName?.let { CustomFhirPathFunctionNames.valueOf(it.replaceFirstChar(Char::titlecase)) }
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
    override fun resolveFunction(
        functionName: String?,
        additionalFunctions: FhirPathFunctions?,
    ): FunctionDetails? {
        return when (CustomFhirPathFunctionNames.get(functionName)) {
            CustomFhirPathFunctionNames.LivdTableLookup -> {
                FunctionDetails(
                    "looks up data in the LIVD table that match the information provided",
                    1,
                    1
                )
            }
            CustomFhirPathFunctionNames.FIPSCountyLookup -> {
                FunctionDetails(
                    "looks up data in the fipps county table that match the information provided",
                    2,
                    2
                )
            }

            else -> null
        }
    }

    /**
     * Execute the function on a [focus] resource for a given [functionName] and [parameters].
     * @return the function result
     */
    override fun executeFunction(
        focus: MutableList<Base>?,
        functionName: String?,
        parameters: MutableList<MutableList<Base>>?,
        additionalFunctions: FhirPathFunctions?,
    ): MutableList<Base> {
        check(focus != null)
        return (
            when (CustomFhirPathFunctionNames.get(functionName)) {
                CustomFhirPathFunctionNames.LivdTableLookup -> {
                    livdTableLookup(focus, parameters)
                }
                CustomFhirPathFunctionNames.FIPSCountyLookup -> {
                    fipsCountyLookup(parameters)
                }
                else -> error(IllegalStateException("Tried to execute invalid FHIR Path function $functionName"))
            }
            )
    }

    /**
     * Get the LOINC Code from the LIVD table based on the device id, equipment model id, test kit name id, or the
     * element model name
     * @return a list with one value denoting the LOINC Code, or an empty list
     */
    fun livdTableLookup(
        focus: MutableList<Base>,
        parameters: MutableList<MutableList<Base>>?,
        metadata: Metadata = Metadata.getInstance(),
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
                deviceId = deviceId,
                tableRef = lookupTable,
                tableColumn = parameters!!.first().first().primitiveValue()
            )
        }

        // Maps to OBX 18 which is mapped to Device.identifier
        val equipmentModelId = (observation.device.resource as Device?)?.identifier?.firstOrNull()?.id
        if (result.isNullOrBlank() && !equipmentModelId.isNullOrEmpty()) {
            result = LivdLookup.find(
                testPerformedCode = testPerformedCode,
                equipmentModelId = equipmentModelId,
                tableRef = lookupTable,
                tableColumn = parameters!!.first().first().primitiveValue()
            )
        }

        val deviceName = (observation.device.resource as Device?)?.deviceName?.firstOrNull()?.name
        if (result.isNullOrBlank() && !deviceName.isNullOrBlank()) {
            result = LivdLookup.find(
                testPerformedCode = testPerformedCode,
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

    /**
     * Get the FIPS Code from the FIPS County table based on the county name and state
     * @return a String with the FIPS Code
     */
    fun fipsCountyLookup(
        parameters: MutableList<MutableList<Base>>?,
        metadata: Metadata = Metadata.getInstance(),
    ): MutableList<Base> {
        val lookupTable = metadata.findLookupTable(name = "fips-county")
        var filters = lookupTable?.FilterBuilder() ?: error("Could not find table fips-county")

        // it says it cannot be null, but it can
        if (parameters == null || parameters.first().isNullOrEmpty()) {
            return mutableListOf(StringType(""))
        }

        val county = parameters.first().first().primitiveValue() ?: return mutableListOf(StringType(""))
        filters = filters.equalsIgnoreCase("County", county)

        // it says it cannot be null, but it can
        if (parameters[1].isNullOrEmpty()) {
            return mutableListOf(StringType(county))
        }
        val state = parameters[1].first().primitiveValue() ?: return mutableListOf(StringType(county))
        filters = filters.equalsIgnoreCase("State", state.uppercase())
        val result = filters.findSingleResult("FIPS")
        return if (!result.isNullOrBlank()) {
            mutableListOf(StringType(result))
        } else {
            mutableListOf(StringType(parameters.first().first().primitiveValue()))
        }
    }
}