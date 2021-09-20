package gov.cdc.prime.router

enum class ResponseMsgType { 
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
}

interface ResponseMessage {
    val type: ResponseMsgType
    fun detailMsg(): String
    fun groupingId(): String
}
