package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import gov.cdc.prime.router.metadata.LivdLookup.find
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.utils.FHIRPathEngine

object CustomFHIRFunctions {
    val lookupTable = gov.cdc.prime.router.Metadata.getInstance().findLookupTable(name = "LIVD-SARS-CoV-2")

    enum class CustomFHIRFunctionNames {
        LookupLivdTableLoincCodes;

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

    fun resolveFunction(functionName: String?): FHIRPathEngine.IEvaluationContext.FunctionDetails? {
        return when (CustomFHIRFunctionNames.get(functionName)) {
            CustomFHIRFunctionNames.LookupLivdTableLoincCodes -> {
                FHIRPathEngine.IEvaluationContext.FunctionDetails(
                    "looks up the LOINC codes in the LIVD table that match the information provided",
                    0,
                    1
                )
            }
            else -> null
        }
    }

    fun executeFunction(
        focus: MutableList<Base>?,
        functionName: String?,
        parameters: MutableList<MutableList<Base>>?
    ): MutableList<Base> {
        check(focus != null)
        return (
            when (CustomFHIRFunctionNames.get(functionName)) {
                CustomFHIRFunctionNames.LookupLivdTableLoincCodes -> {
                    lookupLivdTableLoincCodes(focus, parameters)
                }

                else -> throw IllegalStateException("Tried to execute invalid FHIR Path function $functionName")
            }
            )
    }

    /**
     * Get the LOINC Code from the LIVD table based on the device id, equipment model id, test kit name id, or the
     * element model name
     * @return a list with one value denoting the LOINC Code, or an empty list
     */
    fun lookupLivdTableLoincCodes(
        focus: MutableList<Base>,
        parameters: MutableList<MutableList<Base>>?
    ): MutableList<Base> {
        val observation = focus.first()
        if (observation is Observation) {
            val deviceName = (observation.device.resource as Device).deviceName.first().name
            if (!deviceName.isNullOrBlank()) {
                return mutableListOf(
                    StringType(
                        find(
                            testPerformedCode = null,
                            processingModeCode = null,
                            deviceId = null,
                            equipmentModelId = null,
                            testKitNameId = null,
                            equipmentModelName = deviceName,
                            tableRef = lookupTable,
                            tableColumn = parameters!!.first().first().primitiveValue()
                        )
                    )
                )
            }
        }

        return mutableListOf()
    }
}