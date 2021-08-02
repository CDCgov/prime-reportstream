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
    enum class ResponseMsgType {
        NONE,
        PAYLOAD_SIZE,
        OPTION,
        ROUTE_TO,
        MISSING_HDR,
        UNEXPECTED_HDR,
        EMPTY_VALUE,
        INVALID_DATE,
        INVALID_CODE,
        INVALID_PHONE,
        INVALID_POSTAL,
        INVALID_FORMAT,
        TRANSLATION
    }

    override fun toString(): String {
        return "${scope.toString().lowercase()}${if (id.isBlank()) "" else " $id"}: ${message.detailMsg()}"
    }

    companion object {
        // convenience methods for creating ResultDetail instances
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

    /**
     * Message summary data class with same scopes as ResultDetail but with an ids property
     * meant to contain the relevant ids to which all items the summary message applies.
     */
    data class ResultDetailSummary(
        val scope: DetailScope,
        val ids: List<String>,
        val message: String,
    ) {
        companion object {
            /**
             * Returns a ResultDetailSummary instance from a ResultDetail instance. This is
             * primarily used when all ResultDetail instances are displayed in verbose mode.
             * @param detail the ResultDetail instance to transform into ResultDetailSummary
             * @return the ResultDetailSummary instance
             */
            fun resultSummaryFromDetail(detail: ResultDetail): ResultDetailSummary {
                return ResultDetailSummary(detail.scope, listOf(detail.id), detail.message.detailMsg())
            }

            /**
             * Generates the summary details based on the message type and grouping. For example,
             * multiple date fields can exist in a report leading to multiple invalid dates across
             * those fields. The groupingId will be the field name, so a message type of invalid
             * date will have a list of all invalid date messages across all fields. This function
             * will split the messages into separate lists and summaries based on the grouping.
             * @param type the message type which determines the summary message copy
             * @param messages the list of ResultDetail messages to summarize
             */
            fun summaryMsg(
                type: ResponseMsgType,
                messages: List<ResultDetail>
            ): List<ResultDetailSummary> {
                return when (type) {
                    ResponseMsgType.MISSING_HDR ->
                        listOf(ResultDetailSummary(
                            DetailScope.REPORT, messages.map { it.message.groupingId() }.distinct(),
                            "Missing ${messages.size} header(s)."
                        ))
                    ResponseMsgType.UNEXPECTED_HDR ->
                        listOf(ResultDetailSummary(
                            DetailScope.REPORT, messages.map { it.message.groupingId() }.distinct(),
                            "${messages.size} unexpected header(s) will be ignored."
                        ))
                    // these message types can span multiple groupings so there is a need
                    // for further splitting the list based on the groupingId
                    ResponseMsgType.EMPTY_VALUE,
                    ResponseMsgType.INVALID_DATE,
                    ResponseMsgType.INVALID_CODE,
                    ResponseMsgType.INVALID_PHONE,
                    ResponseMsgType.INVALID_POSTAL -> {
                        val typeString: String = when (type) {
                            ResponseMsgType.EMPTY_VALUE -> "empty"
                            ResponseMsgType.INVALID_DATE -> "invalid date"
                            ResponseMsgType.INVALID_CODE -> "invalid code"
                            ResponseMsgType.INVALID_PHONE -> "invalid phone number"
                            ResponseMsgType.INVALID_POSTAL -> "invalid postal code"
                            else -> type.toString()
                        }
                        // splits the list into smaller lists by groupingId
                        val grouping = mutableMapOf<String, MutableList<ResultDetail>>()
                        messages.forEach {
                            grouping.getOrPut(it.message.groupingId()) { mutableListOf() }.add(it)
                        }
                        // creates the detail summary for each groupingId (i.e. by field)
                        val resultSummary = mutableListOf<ResultDetailSummary>()
                        grouping.forEach { (groupingId, details) ->
                            resultSummary.addAll(
                                listOf(
                                    ResultDetailSummary(
                                        DetailScope.ITEM,
                                        details.map { it.id },
                                        "${details.size} $typeString value(s) for element $groupingId"
                                    )
                                )
                            )
                        }
                        resultSummary
                    }
                    else -> {
                        messages.map { ResultDetailSummary(it.scope, listOf(it.id), it.message.detailMsg()) }
                    }
                }
            }
        }
    }

    /**
     * An interface for custom messages to implement. Intended for implementing and
     * controlling message copy for the individual message types.
     */
    interface ResponseMessage {
        val type: ResponseMsgType
        fun detailMsg(): String
        fun groupingId(): String {
            return ""
        }
    }

    /**
     * Generic message impelmentation to help with transitioning all messaging.
     */
    data class GenericMessage(
        override val type: ResponseMsgType = ResponseMsgType.NONE,
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

    /**
     * Message implementation and copy for Payload error/warning messages
     */
    data class PayloadMessage(
        override val type: ResponseMsgType = ResponseMsgType.PAYLOAD_SIZE,
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

    /**
     * Message implementation for invalid date messages.
     */
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

    /**
     * Message implementation for invalid code/value messages.
     */
    data class InvalidCodeMessage(
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

    /**
     * Message implementation for invalid telephone messages.
     */
    data class InvalidPhoneMessage(
        override val type: ResponseMsgType = ResponseMsgType.INVALID_PHONE,
        val formattedValue: String = "",
        val fieldMapping: String = ""
    ) : ResponseMessage {
        override fun detailMsg() : String {
            return "Invalid phone number '$formattedValue' for $fieldMapping"
        }

        override fun groupingId(): String {
            return fieldMapping
        }

        companion object {
            fun new(formattedValue: String, fieldMapping: String): InvalidPhoneMessage {
                return InvalidPhoneMessage(ResponseMsgType.INVALID_PHONE, formattedValue, fieldMapping)
            }
        }
    }

    /**
     * Message implementation for invalid postal code messages.
     */
    data class InvalidPostalMessage(
        override val type: ResponseMsgType = ResponseMsgType.INVALID_POSTAL,
        val formattedValue: String = "",
        val fieldMapping: String = ""
    ) : ResponseMessage {
        override fun detailMsg() : String {
            return "Invalid postal code '$formattedValue' for $fieldMapping"
        }

        override fun groupingId(): String {
            return fieldMapping
        }

        companion object {
            fun new(formattedValue: String, fieldMapping: String): InvalidPostalMessage {
                return InvalidPostalMessage(ResponseMsgType.INVALID_POSTAL, formattedValue, fieldMapping)
            }
        }
    }

    /**
     * Message implementation for invalid format messages.
     */
    data class InvalidFormatMessage(
        override val type: ResponseMsgType = ResponseMsgType.INVALID_FORMAT,
        val reason: Reason = Reason.NONE,
        val fieldMapping: String = "",
        val format: String = ""
    ) : ResponseMessage {
        enum class Reason {NONE, INVALID_HD, UNSUPPORTED_HD, INVALID_EI, UNSUPPORTED_EI}

        override fun detailMsg(): String {
            return when (reason) {
                Reason.INVALID_HD ->
                    "Invalid HD format for $fieldMapping"
                Reason.UNSUPPORTED_HD ->
                    "Unsupported HD format for input: '$format' in $fieldMapping"
                Reason.INVALID_EI ->
                    "Invalid EI format for $fieldMapping"
                Reason.UNSUPPORTED_EI ->
                    "Unsupported EI format for input: '$format' in $fieldMapping"
                else -> ""
            }
        }

        override fun groupingId(): String {
            return fieldMapping
        }

        companion object {
            fun invalidHD(fieldMapping: String): InvalidFormatMessage {
                return InvalidFormatMessage(ResponseMsgType.INVALID_FORMAT, Reason.INVALID_HD, fieldMapping)
            }
            fun unsupportedHD(fieldMapping: String, format: String): InvalidFormatMessage {
                return InvalidFormatMessage(ResponseMsgType.INVALID_FORMAT, Reason.UNSUPPORTED_HD, fieldMapping, format)
            }
            fun invalidEI(fieldMapping: String): InvalidFormatMessage {
                return InvalidFormatMessage(ResponseMsgType.INVALID_FORMAT, Reason.INVALID_EI, fieldMapping)
            }
            fun unsupportedEI(fieldMapping: String, format: String): InvalidFormatMessage {
                return InvalidFormatMessage(ResponseMsgType.INVALID_FORMAT, Reason.UNSUPPORTED_EI, fieldMapping, format)
            }
        }
    }
}