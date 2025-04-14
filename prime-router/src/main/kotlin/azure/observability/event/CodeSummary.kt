package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.azure.ConditionStamper.Companion.MEMBER_OID_EXTENSION_URL
import org.hl7.fhir.r4.model.Coding

data class CodeSummary(
    val system: String = EMPTY_STRING,
    val code: String = EMPTY_STRING,
    val display: String = EMPTY_STRING,
    val memberOid: String = EMPTY_STRING,
) {
    companion object {
        const val UNKNOWN = "Unknown"
        const val EMPTY_STRING = ""

        /**
         * Create an instance of [CodeSummary] from a [Coding]
         */
        fun fromCoding(coding: Coding): CodeSummary {
            val memberOid: String? = coding.extension
                .firstOrNull { it.url == MEMBER_OID_EXTENSION_URL }
                ?.value
                ?.primitiveValue()

            return CodeSummary(
                system = coding.system ?: UNKNOWN,
                code = coding.code ?: UNKNOWN,
                display = coding.display ?: UNKNOWN,
                memberOid = memberOid ?: UNKNOWN
            )
        }
    }
}