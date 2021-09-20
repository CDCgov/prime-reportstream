package gov.cdc.prime.router

data class MissingFieldMessage(
    override val type: ResponseMsgType = ResponseMsgType.MISSING,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ResponseMessage {
    override fun detailMsg() : String {
        return "Blank value for element $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping
    }

    companion object {
        fun new(fieldMapping: String): MissingFieldMessage {
            return MissingFieldMessage(ResponseMsgType.MISSING, fieldMapping)
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

data class InvalidCodeMessage(
    override val type: ResponseMsgType = ResponseMsgType.INVALID_CODE,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ResponseMessage {
    override fun detailMsg() : String {
        return "Invalid code: '$formattedValue' is not a display value in altValues set for $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping
    }

    companion object {
        fun new(formattedValue: String, fieldMapping: String): InvalidCodeMessage {
            return InvalidCodeMessage(ResponseMsgType.INVALID_CODE, formattedValue, fieldMapping)
        }
    }
}

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

data class UnsupportedHDMessage(
    override val type: ResponseMsgType = ResponseMsgType.UNSUPPORTED_HD,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ResponseMessage {
    override fun detailMsg() : String {
        return "Unsupported HD format for input: '$formattedValue' in $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping
    }

    companion object {
        fun new(): UnsupportedHDMessage {
            return UnsupportedHDMessage(ResponseMsgType.UNSUPPORTED_HD)
        }

        fun new(formattedValue: String, fieldMapping: String): UnsupportedHDMessage {
            return UnsupportedHDMessage(ResponseMsgType.UNSUPPORTED_HD, formattedValue, fieldMapping)
        }
    }
}

data class UnsupportedEIMessage(
    override val type: ResponseMsgType = ResponseMsgType.UNSUPPORTED_EI,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ResponseMessage {
    override fun detailMsg() : String {
        return "Unsupported EI format for input: '$formattedValue' in $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping
    }

    companion object {
        fun new(): UnsupportedEIMessage {
            return UnsupportedEIMessage(ResponseMsgType.UNSUPPORTED_EI)
        }

        fun new(formattedValue: String, fieldMapping: String): UnsupportedEIMessage {
            return UnsupportedEIMessage(ResponseMsgType.UNSUPPORTED_EI, formattedValue, fieldMapping)
        }
    }
}

data class InvalidParamMessage(
    override val type: ResponseMsgType = ResponseMsgType.INVALID_PARAM,
    val message: String = "",
) : ResponseMessage {
    override fun detailMsg() : String {
        return message
    }

    override fun groupingId(): String {
        return message
    }

    companion object {
        fun new(message: String): InvalidParamMessage {
            return InvalidParamMessage(ResponseMsgType.INVALID_PARAM, message)
        }
    }
}

data class InvalidReportMessage(
    override val type: ResponseMsgType = ResponseMsgType.REPORT,
    val message: String = "",
) : ResponseMessage {
    override fun detailMsg() : String {
        return message
    }

    override fun groupingId(): String {
        return message
    }

    companion object {
        fun new(message: String): InvalidReportMessage {
            return InvalidReportMessage(ResponseMsgType.REPORT, message)
        }
    }
}

data class InvalidTranslationMessage(
    override val type: ResponseMsgType = ResponseMsgType.TRANSLATION,
    val message: String = "",
) : ResponseMessage {
    override fun detailMsg() : String {
        return message
    }

    override fun groupingId(): String {
        return message
    }

    companion object {
        fun new(message: String): InvalidTranslationMessage {
            return InvalidTranslationMessage(ResponseMsgType.TRANSLATION, message)
        }
    }
}
