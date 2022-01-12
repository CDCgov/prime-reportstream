package gov.cdc.prime.router

enum class ResponseMsgType {
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
    // Common errors
    INVALID_EQUIPMENT,
}

interface ResponseMessage {
    val type: ResponseMsgType
    fun detailMsg(): String
    fun groupingId(): String
}