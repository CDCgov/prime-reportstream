enum class ResponseMsgType { NONE, PAYLOAD_SIZE, OPTION, ROUTE_TO, REPORT, MISSING, UNEXPECTED, INVALID_DATE, INVALID_CODE, TRANSLATION }

interface ResponseMessage {
    val type: ResponseMsgType
    fun detailMsg(): String
    fun groupingId(): String
}
