package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.IdType

fun convertFhirType(value: Base, sourceType: String, targetType: String): Base {
    return if (sourceType == "id" && targetType == "string") {
        var idValue = IdType()
        idValue.value = value.primitiveValue()
        idValue
    } else if (sourceType == targetType) {
        value
    } else {
        println("Conversion between $sourceType and $targetType not yet implemented.")
        value
    }
}