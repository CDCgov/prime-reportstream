package gov.cdc.prime.router

data class MissingFieldMessage(
    override val type: ResponseMsgType = ResponseMsgType.MISSING,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ResponseMessage {
    override fun detailMsg(): String {
        return "Blank value for element $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping
    }

    companion object {
        fun new(fieldMapping: String): MissingFieldMessage {
            return MissingFieldMessage(ResponseMsgType.MISSING, "", fieldMapping)
        }
    }
}

data class InvalidDateMessage(
    override val type: ResponseMsgType = ResponseMsgType.INVALID_DATE,
    val formattedValue: String = "",
    val fieldMapping: String = "",
    val format: String? = ""
) : ResponseMessage {
    override fun detailMsg(): String {
        var msg = "Invalid date: '$formattedValue' for element $fieldMapping."
        if (format !== null) {
            msg += " Reformat to $format."
        }
        return msg
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
    }

    companion object {
        fun new(formattedValue: String, fieldMapping: String, format: String?): InvalidDateMessage {
            return InvalidDateMessage(ResponseMsgType.INVALID_DATE, formattedValue, fieldMapping, format)
        }
    }
}

data class InvalidCodeMessage(
    override val type: ResponseMsgType = ResponseMsgType.INVALID_CODE,
    val formattedValue: String = "",
    val fieldMapping: String = "",
    val format: String? = ""
) : ResponseMessage {
    override fun detailMsg(): String {
        var msg = "Invalid code: '$formattedValue' is not a display value in altValues set for $fieldMapping."
        if (format !== null) {
            msg += " Reformat to $format."
        }
        return msg
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
    }

    companion object {
        fun new(formattedValue: String, fieldMapping: String, format: String?): InvalidCodeMessage {
            return InvalidCodeMessage(ResponseMsgType.INVALID_CODE, formattedValue, fieldMapping, format)
        }
    }
}

data class InvalidPhoneMessage(
    override val type: ResponseMsgType = ResponseMsgType.INVALID_PHONE,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ResponseMessage {
    override fun detailMsg(): String {
        return "Invalid phone number '$formattedValue' for $fieldMapping. Reformat to a 10-digit phone number " +
            "(e.g. (555) - 555-5555)."
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
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
    val fieldMapping: String = "",
    val format: String? = ""
) : ResponseMessage {
    override fun detailMsg(): String {
        var msg = "Invalid postal code '$formattedValue' for $fieldMapping."
        if (format !== null) {
            msg += " Reformat to $format."
        }
        return msg
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
    }

    companion object {
        fun new(formattedValue: String, fieldMapping: String, format: String?): InvalidPostalMessage {
            return InvalidPostalMessage(ResponseMsgType.INVALID_POSTAL, formattedValue, fieldMapping, format)
        }
    }
}

data class UnsupportedHDMessage(
    override val type: ResponseMsgType = ResponseMsgType.UNSUPPORTED_HD,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ResponseMessage {
    override fun detailMsg(): String {
        return "Unsupported HD format for input: '$formattedValue' in $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
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
    override fun detailMsg(): String {
        return "Unsupported EI format for input: '$formattedValue' in $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
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
    override fun detailMsg(): String {
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
    override fun detailMsg(): String {
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
    override fun detailMsg(): String {
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

data class InvalidHL7Message(
    override val type: ResponseMsgType = ResponseMsgType.INVALID_HL7,
    val message: String = "",
) : ResponseMessage {
    override fun detailMsg(): String {
        return message
    }

    override fun groupingId(): String {
        return message
    }

    companion object {
        fun new(message: String): InvalidHL7Message {
            return InvalidHL7Message(ResponseMsgType.INVALID_HL7, message)
        }
    }
}

/**
 * An message to denote that an equipment was not found in the LIVD table.
 */
data class InvalidEquipmentMessage(
    override val type: ResponseMsgType = ResponseMsgType.INVALID_EQUIPMENT,
) : ResponseMessage {
    override fun detailMsg(): String {
        return "Unable to validate testing equipment information."
    }

    override fun groupingId(): String {
        return detailMsg()
    }

    companion object {
        fun new(): InvalidEquipmentMessage {
            return InvalidEquipmentMessage()
        }
    }
}