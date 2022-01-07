package gov.cdc.prime.router
import com.fasterxml.jackson.annotation.JsonTypeInfo

enum class ActionEventDetailType {
    // CSV Serializing errors
    INVALID_DATE,
    INVALID_CODE,
    INVALID_PARAM,
    INVALID_PHONE,
    INVALID_POSTAL,
    MISSING,
    REPORT,
    TRANSLATION,
    UNSUPPORTED_HD,
    UNSUPPORTED_EI,
    // HL7 Serializing errors
    INVALID_HL7,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
interface ActionEventDetail {
    val type: ActionEventDetailType
    fun detailMsg(): String
    fun groupingId(): String
}