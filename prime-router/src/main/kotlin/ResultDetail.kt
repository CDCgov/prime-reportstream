package gov.cdc.prime.router

/**
 * @property scope of the result detail
 * @property id of the result (depends on scope)
 * @property details of the result
 */
data class ResultDetail(val scope: DetailScope, val id: String, val message: ResponseMessage) {
    /**
     * @property REPORT scope for the detail
     * @property ITEM scope for the detail
     */
    enum class DetailScope { PARAMETER, REPORT, ITEM, TRANSLATION }

    override fun toString(): String {
        return "${scope.toString().lowercase()}${if (id.isBlank()) "" else " $id"}: ${message.detailMsg()}"
    }

    companion object {
        fun report(message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.REPORT, "", message)
        }

        fun report(message: String): ResultDetail {
            return ResultDetail(DetailScope.REPORT, "", GenericMessage(ResponseMsgType.NONE, message))
        }

        fun item(id: String, message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.ITEM, id, message)
        }

        fun item(id: String, message: String): ResultDetail {
            return ResultDetail(DetailScope.ITEM, id, GenericMessage(ResponseMsgType.NONE, message))
        }

        fun param(id: String, message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.PARAMETER, id, message)
        }

        fun param(id: String, message: String): ResultDetail {
            return ResultDetail(DetailScope.PARAMETER, id, GenericMessage(ResponseMsgType.NONE, message))
        }

        fun translation(id: String, message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.TRANSLATION, id, message)
        }

        fun translation(id: String, message: String): ResultDetail {
            return ResultDetail(DetailScope.TRANSLATION, id, GenericMessage(ResponseMsgType.NONE, message))
        }
    }

    enum class ResponseMsgType { NONE, PAYLOAD_SIZE, OPTION, ROUTE_TO, REPORT, ELEMENT, MISSING, UNEXPECTED, INVALID_DATE, INVALID_CODE, TRANSLATION }

    interface ResponseMessage {
        val type: ResponseMsgType
        fun detailMsg(): String
        fun groupingId(): String {
            return ""
        }
    }

    data class GenericMessage(
        override val type: ResponseMsgType = ResponseMsgType.ELEMENT,
        val message: String = "",
        val fieldMapping: String = ""
    ): ResponseMessage {
        override fun detailMsg(): String {
            return message;
        }

        override fun groupingId(): String {
            return fieldMapping
        }
    }

    data class PayloadMessage(
        override val type: ResponseMsgType = ResponseMsgType.REPORT,
        val reason: Reason = Reason.NONE,
        val contentLength: Long = 0,
        val bodyLength: Int = 0
    ) : ResponseMessage {
        enum class Reason {NONE, NO_HEADER, NAN, NEGATIVE, HDR_TOO_LARGE, BODY_TOO_LARGE}

        override fun detailMsg(): String {
            return when (reason) {
                Reason.NO_HEADER ->
                    "ERROR: No content-length header found. Refusing this request."
                Reason.NAN ->
                    "ERROR: content-length header is not a number"
                Reason.NEGATIVE ->
                    "ERROR: negative content-length $contentLength"
                Reason.HDR_TOO_LARGE ->
                    "ERROR: content-length $contentLength is larger than max $PAYLOAD_MAX_BYTES"
                Reason.BODY_TOO_LARGE ->
                    "ERROR: body size $bodyLength} is larger than max $PAYLOAD_MAX_BYTES " +
                        "(content-length header = $contentLength)"
                else -> ""
            }
        }

        companion object {
            fun noHeader(): PayloadMessage {
                return PayloadMessage(ResponseMsgType.PAYLOAD_SIZE, Reason.NO_HEADER)
            }

            fun nonNumeric(): PayloadMessage {
                return PayloadMessage(ResponseMsgType.PAYLOAD_SIZE, Reason.NAN)
            }

            fun negative(contentLength: Long): PayloadMessage {
                return PayloadMessage(ResponseMsgType.PAYLOAD_SIZE, Reason.NEGATIVE, contentLength)
            }

            fun headerTooLarge(contentLength: Long): PayloadMessage {
                return PayloadMessage(ResponseMsgType.PAYLOAD_SIZE, Reason.HDR_TOO_LARGE, contentLength)
            }

            fun bodyTooLarge(contentLength: Long, bodyLength: Int): PayloadMessage {
                return PayloadMessage(ResponseMsgType.PAYLOAD_SIZE, Reason.BODY_TOO_LARGE, contentLength, bodyLength)
            }
        }
    }

    data class InvalidDateMessage(
        override val type: ResponseMsgType = ResponseMsgType.INVALID_DATE,
        val formattedValue: String = "",
        val fieldMapping: String = ""
    ) : ResponseMessage {
        override fun detailMsg() : String {
            return "Invalid date: '$formattedValue' for element $fieldMapping"
        }

        override fun groupingId(): String {
            return fieldMapping
        }

        companion object {
            fun new(formattedValue: String, fieldMapping: String): InvalidDateMessage {
                return InvalidDateMessage(ResponseMsgType.INVALID_DATE, formattedValue, fieldMapping)
            }
        }
    }

    class InvalidCodeMessage(
        override val type: ResponseMsgType = ResponseMsgType.INVALID_CODE,
        val reason: Reason = Reason.NONE,
        val formattedValue: String = "",
        val fieldMapping: String = ""
    ) : ResponseMessage {
        enum class Reason {NONE, ALT_VALUES, SCHEMA, DISPLAY, CODE, NO_MATCH}

        override fun detailMsg(): String {
            return when (reason) {
                Reason.ALT_VALUES ->
                    "Invalid code: '$formattedValue' is not a display value in altValues set for $fieldMapping"
                Reason.SCHEMA ->
                    "Schema Error: missing value set for $fieldMapping"
                Reason.DISPLAY ->
                    "Invalid code: '$formattedValue' is not a display value for element $fieldMapping"
                Reason.CODE ->
                    "Invalid code: '$formattedValue' is not a code value for element $fieldMapping"
                Reason.NO_MATCH ->
                    "Invalid code: '$formattedValue' does not match any codes for $fieldMapping"
                else -> ""
            }
        }

        override fun groupingId(): String {
            return fieldMapping
        }

        companion object {
            fun altValues(formattedValue: String, fieldMapping: String): InvalidCodeMessage {
                return InvalidCodeMessage(ResponseMsgType.INVALID_CODE, Reason.ALT_VALUES, formattedValue, fieldMapping)
            }

            fun schema(fieldMapping: String): InvalidCodeMessage {
                return InvalidCodeMessage(ResponseMsgType.INVALID_CODE, Reason.SCHEMA, "", fieldMapping)
            }

            fun display(formattedValue: String, fieldMapping: String): InvalidCodeMessage {
                return InvalidCodeMessage(ResponseMsgType.INVALID_CODE, Reason.DISPLAY, formattedValue, fieldMapping)
            }

            fun code(formattedValue: String, fieldMapping: String): InvalidCodeMessage {
                return InvalidCodeMessage(ResponseMsgType.INVALID_CODE, Reason.CODE, formattedValue, fieldMapping)
            }

            fun noMatch(formattedValue: String, fieldMapping: String): InvalidCodeMessage {
                return InvalidCodeMessage(ResponseMsgType.INVALID_CODE, Reason.NO_MATCH, formattedValue, fieldMapping)
            }
        }
    }
}