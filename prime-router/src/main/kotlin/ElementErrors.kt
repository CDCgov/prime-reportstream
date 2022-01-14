package gov.cdc.prime.router

data class MissingFieldMessage(
    override val type: ActionEventDetailType = ActionEventDetailType.MISSING,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ActionEventDetail {
    override fun detailMsg(): String {
        return "Blank value for element $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping
    }

    companion object {
        fun new(fieldMapping: String): MissingFieldMessage {
            return MissingFieldMessage(ActionEventDetailType.MISSING, "", fieldMapping)
        }
    }
}

data class InvalidDateMessage(
    override val type: ActionEventDetailType = ActionEventDetailType.INVALID_DATE,
    val formattedValue: String = "",
    val fieldMapping: String = "",
    val format: String? = ""
) : ActionEventDetail {
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
            return InvalidDateMessage(ActionEventDetailType.INVALID_DATE, formattedValue, fieldMapping, format)
        }
    }
}

data class InvalidCodeMessage(
    override val type: ActionEventDetailType = ActionEventDetailType.INVALID_CODE,
    val formattedValue: String = "",
    val fieldMapping: String = "",
    val format: String? = ""
) : ActionEventDetail {
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
            return InvalidCodeMessage(ActionEventDetailType.INVALID_CODE, formattedValue, fieldMapping, format)
        }
    }
}

data class InvalidPhoneMessage(
    override val type: ActionEventDetailType = ActionEventDetailType.INVALID_PHONE,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ActionEventDetail {
    override fun detailMsg(): String {
        return "Invalid phone number '$formattedValue' for $fieldMapping. Reformat to a 10-digit phone number " +
            "(e.g. (555) - 555-5555)."
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
    }

    companion object {
        fun new(formattedValue: String, fieldMapping: String): InvalidPhoneMessage {
            return InvalidPhoneMessage(ActionEventDetailType.INVALID_PHONE, formattedValue, fieldMapping)
        }
    }
}

data class InvalidPostalMessage(
    override val type: ActionEventDetailType = ActionEventDetailType.INVALID_POSTAL,
    val formattedValue: String = "",
    val fieldMapping: String = "",
    val format: String? = ""
) : ActionEventDetail {
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
            return InvalidPostalMessage(ActionEventDetailType.INVALID_POSTAL, formattedValue, fieldMapping, format)
        }
    }
}

data class UnsupportedHDMessage(
    override val type: ActionEventDetailType = ActionEventDetailType.UNSUPPORTED_HD,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ActionEventDetail {
    override fun detailMsg(): String {
        return "Unsupported HD format for input: '$formattedValue' in $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
    }

    companion object {
        fun new(): UnsupportedHDMessage {
            return UnsupportedHDMessage(ActionEventDetailType.UNSUPPORTED_HD)
        }

        fun new(formattedValue: String, fieldMapping: String): UnsupportedHDMessage {
            return UnsupportedHDMessage(ActionEventDetailType.UNSUPPORTED_HD, formattedValue, fieldMapping)
        }
    }
}

data class UnsupportedEIMessage(
    override val type: ActionEventDetailType = ActionEventDetailType.UNSUPPORTED_EI,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ActionEventDetail {
    override fun detailMsg(): String {
        return "Unsupported EI format for input: '$formattedValue' in $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
    }

    companion object {
        fun new(): UnsupportedEIMessage {
            return UnsupportedEIMessage(ActionEventDetailType.UNSUPPORTED_EI)
        }

        fun new(formattedValue: String, fieldMapping: String): UnsupportedEIMessage {
            return UnsupportedEIMessage(ActionEventDetailType.UNSUPPORTED_EI, formattedValue, fieldMapping)
        }
    }
}

data class InvalidParamMessage(
    val httpParameter: String,
    val message: String,
    val detail: ActionEventDetail? = null,
    override val type: ActionEventDetailType = ActionEventDetailType.INVALID_PARAM,
) : ActionEventDetail {
    override fun detailMsg(): String {
        return message
    }

    override fun groupingId(): String {
        return message
    }
}

data class InvalidReportMessage(
    val message: String = "",
    override val type: ActionEventDetailType = ActionEventDetailType.REPORT,
) : ActionEventDetail {
    override fun detailMsg(): String {
        return message
    }

    override fun groupingId(): String {
        return message
    }

    companion object {
        fun new(message: String): InvalidReportMessage {
            return InvalidReportMessage(message)
        }
    }
}

data class InvalidTranslationMessage(
    override val type: ActionEventDetailType = ActionEventDetailType.TRANSLATION,
    val message: String = "",
) : ActionEventDetail {
    override fun detailMsg(): String {
        return message
    }

    override fun groupingId(): String {
        return message
    }

    companion object {
        fun new(message: String): InvalidTranslationMessage {
            return InvalidTranslationMessage(ActionEventDetailType.TRANSLATION, message)
        }
    }
}

data class InvalidHL7Message(
    override val type: ActionEventDetailType = ActionEventDetailType.INVALID_HL7,
    val message: String = "",
) : ActionEventDetail {
    override fun detailMsg(): String {
        return message
    }

    override fun groupingId(): String {
        return message
    }

    companion object {
        fun new(message: String): InvalidHL7Message {
            return InvalidHL7Message(ActionEventDetailType.INVALID_HL7, message)
        }
    }
}

/**
 * An message to denote that an equipment was not found in the LIVD table.
 */
data class InvalidEquipmentMessage(
    override val type: ActionEventDetailType = ActionEventDetailType.INVALID_EQUIPMENT,
    val elementName: String
) : ActionEventDetail {
    override fun detailMsg(): String {
        return "Invalid field $elementName; please refer to the Department of Health and Human Services’ (HHS) " +
            "LOINC Mapping spreadsheet for acceptable values."
    }

    override fun groupingId(): String {
        return detailMsg()
    }

    companion object {
        fun new(element: Element): InvalidEquipmentMessage {
            return InvalidEquipmentMessage(elementName = element.name)
        }
    }
}