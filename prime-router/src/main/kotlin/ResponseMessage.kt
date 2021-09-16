package gov.cdc.prime.router

enum class ResponseMsgType { 
    MISSING,
    INVALID_DATE,
    INVALID_CODE,
    INVALID_PHONE,
    INVALID_POSTAL,
    UNSUPPORTED_HD,
    UNSUPPORTED_EI,
}

interface ResponseMessage {
    val type: ResponseMsgType
    fun detailMsg(): String
    fun groupingId(): String
}
