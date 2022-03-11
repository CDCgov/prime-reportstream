package gov.cdc.prime.router

data class MissingFieldMessage(
    override val type: ActionLogDetailType = ActionLogDetailType.MISSING,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ActionLogDetail {
    override fun detailMsg(): String {
        return "Blank value for element $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping
    }

    companion object {
        fun new(fieldMapping: String): MissingFieldMessage {
            return MissingFieldMessage(ActionLogDetailType.MISSING, "", fieldMapping)
        }
    }
}

data class InvalidDateMessage(
    override val type: ActionLogDetailType = ActionLogDetailType.INVALID_DATE,
    val formattedValue: String = "",
    val fieldMapping: String = "",
    val format: String? = ""
) : ActionLogDetail {
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
            return InvalidDateMessage(ActionLogDetailType.INVALID_DATE, formattedValue, fieldMapping, format)
        }
    }
}

data class InvalidCodeMessage(
    override val type: ActionLogDetailType = ActionLogDetailType.INVALID_CODE,
    val formattedValue: String = "",
    val fieldMapping: String = "",
    val format: String? = ""
) : ActionLogDetail {
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
            return InvalidCodeMessage(ActionLogDetailType.INVALID_CODE, formattedValue, fieldMapping, format)
        }
    }
}

data class InvalidPhoneMessage(
    override val type: ActionLogDetailType = ActionLogDetailType.INVALID_PHONE,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ActionLogDetail {
    override fun detailMsg(): String {
        return "Invalid phone number '$formattedValue' for $fieldMapping. Reformat to a 10-digit phone number " +
            "(e.g. (555) - 555-5555)."
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
    }

    companion object {
        fun new(formattedValue: String, fieldMapping: String): InvalidPhoneMessage {
            return InvalidPhoneMessage(ActionLogDetailType.INVALID_PHONE, formattedValue, fieldMapping)
        }
    }
}

data class InvalidPostalMessage(
    override val type: ActionLogDetailType = ActionLogDetailType.INVALID_POSTAL,
    val formattedValue: String = "",
    val fieldMapping: String = "",
    val format: String? = ""
) : ActionLogDetail {
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
            return InvalidPostalMessage(ActionLogDetailType.INVALID_POSTAL, formattedValue, fieldMapping, format)
        }
    }
}

data class UnsupportedHDMessage(
    override val type: ActionLogDetailType = ActionLogDetailType.UNSUPPORTED_HD,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ActionLogDetail {
    override fun detailMsg(): String {
        return "Unsupported HD format for input: '$formattedValue' in $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
    }

    companion object {
        fun new(): UnsupportedHDMessage {
            return UnsupportedHDMessage(ActionLogDetailType.UNSUPPORTED_HD)
        }

        fun new(formattedValue: String, fieldMapping: String): UnsupportedHDMessage {
            return UnsupportedHDMessage(ActionLogDetailType.UNSUPPORTED_HD, formattedValue, fieldMapping)
        }
    }
}

data class UnsupportedEIMessage(
    override val type: ActionLogDetailType = ActionLogDetailType.UNSUPPORTED_EI,
    val formattedValue: String = "",
    val fieldMapping: String = ""
) : ActionLogDetail {
    override fun detailMsg(): String {
        return "Unsupported EI format for input: '$formattedValue' in $fieldMapping"
    }

    override fun groupingId(): String {
        return fieldMapping + formattedValue
    }

    companion object {
        fun new(): UnsupportedEIMessage {
            return UnsupportedEIMessage(ActionLogDetailType.UNSUPPORTED_EI)
        }

        fun new(formattedValue: String, fieldMapping: String): UnsupportedEIMessage {
            return UnsupportedEIMessage(ActionLogDetailType.UNSUPPORTED_EI, formattedValue, fieldMapping)
        }
    }
}

data class InvalidParamMessage(
    val httpParameter: String,
    val message: String,
    val detail: ActionLogDetail? = null,
    override val type: ActionLogDetailType = ActionLogDetailType.INVALID_PARAM,
) : ActionLogDetail {
    override fun detailMsg(): String {
        return message
    }

    override fun groupingId(): String {
        return message
    }
}

data class InvalidReportMessage(
    val message: String = "",
    override val type: ActionLogDetailType = ActionLogDetailType.REPORT,
) : ActionLogDetail {
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
    override val type: ActionLogDetailType = ActionLogDetailType.TRANSLATION,
    val message: String = "",
) : ActionLogDetail {
    override fun detailMsg(): String {
        return message
    }

    override fun groupingId(): String {
        return message
    }

    companion object {
        fun new(message: String): InvalidTranslationMessage {
            return InvalidTranslationMessage(ActionLogDetailType.TRANSLATION, message)
        }
    }
}

data class InvalidHL7Message(
    override val type: ActionLogDetailType = ActionLogDetailType.INVALID_HL7,
    val message: String = "",
) : ActionLogDetail {
    override fun detailMsg(): String {
        return message
    }

    override fun groupingId(): String {
        return message
    }

    companion object {
        fun new(message: String): InvalidHL7Message {
            return InvalidHL7Message(ActionLogDetailType.INVALID_HL7, message)
        }
    }
}

/**
 * A message to denote that an equipment was not found in the LIVD table.
 */
data class InvalidEquipmentMessage(
    override val type: ActionLogDetailType = ActionLogDetailType.INVALID_EQUIPMENT,
    val elementName: String
) : ActionLogDetail {
    override fun detailMsg(): String {
        return "Invalid field $elementName; please refer to the Department of Health and Human Services' (HHS) " +
            "LOINC Mapping spreadsheet for acceptable values."
    }

    override fun groupingId(): String {
        return detailMsg()
    }

    companion object {
        fun new(element: Element): InvalidEquipmentMessage {
            return InvalidEquipmentMessage(elementName = element.fieldMapping)
        }
    }
}

/**
 * A message to denote a field precision issue.
 */
data class FieldPrecisionMessage(
    override val type: ActionLogDetailType = ActionLogDetailType.FIELD_PRECISION,
    val message: String
) : ActionLogDetail {
    override fun detailMsg(): String {
        return message
    }

    override fun groupingId(): String {
        return detailMsg()
    }

    companion object {
        fun new(message: String): FieldPrecisionMessage {
            return FieldPrecisionMessage(message = message)
        }
    }
}