package gov.cdc.prime.router

abstract class ItemActionLogDetail(val fieldMapping: String = "") : ActionLogDetail {
    override val scope = ActionLogScope.item
    override val groupingId = "${this.javaClass.simpleName}-$fieldMapping"
}

abstract class GenericActionLogDetail(message: String, override val scope: ActionLogScope) : ActionLogDetail {
    override val groupingId = message
}

class MissingFieldMessage(fieldMapping: String) : ItemActionLogDetail(fieldMapping) {
    override val message = "Blank value for element $fieldMapping"
}

class InvalidDateMessage(
    private val formattedValue: String,
    fieldMapping: String,
    private val format: String? = null
) : ItemActionLogDetail(fieldMapping) {
    override val scope = ActionLogScope.item
    override val message: String get() {
        var msg = "Invalid date: '$formattedValue' for element $fieldMapping."
        if (format !== null) {
            msg += " Reformat to $format."
        }
        return msg
    }
}

class InvalidCodeMessage(
    private val formattedValue: String,
    fieldMapping: String,
    private val format: String? = null
) : ItemActionLogDetail(fieldMapping) {
    override val message: String get() {
        var msg = "Invalid code: '$formattedValue' is not a display value in altValues set for $fieldMapping."
        if (format !== null) {
            msg += " Reformat to $format."
        }
        return msg
    }
}

class InvalidPhoneMessage(
    formattedValue: String,
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "Invalid phone number '$formattedValue' for $fieldMapping. Reformat to a 10-digit phone " +
        "number (e.g. (555) - 555-5555)."
    override val groupingId = fieldMapping + formattedValue
}

class InvalidPostalMessage(
    private val formattedValue: String,
    fieldMapping: String,
    private val format: String? = null
) : ItemActionLogDetail(fieldMapping) {
    override val message: String get() {
        var msg = "Invalid postal code '$formattedValue' for $fieldMapping."
        if (format !== null) {
            msg += " Reformat to $format."
        }
        return msg
    }
}

class UnsupportedHDMessage(
    formattedValue: String,
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "Unsupported HD format for input: '$formattedValue' in $fieldMapping"
}

class UnsupportedEIMessage(
    formattedValue: String,
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "Unsupported EI format for input: '$formattedValue' in $fieldMapping"
}

/**
 * A message to denote that an equipment was not found in the LIVD table.
 */
class InvalidEquipmentMessage(
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "Invalid field $fieldMapping; please refer to the Department of Health and Human " +
        "Services' (HHS) LOINC Mapping spreadsheet for acceptable values."
}

/**
 * A message to denote a field precision issue.
 */
class FieldPrecisionMessage(
    fieldMapping: String = "", // Default to empty for backwards compatibility
    override val message: String
) : ItemActionLogDetail(fieldMapping) {
    // Handle if field is empty for backwards compatibility
    override val groupingId = "${this.javaClass.simpleName}-${fieldMapping.ifBlank { message }}"
}

class InvalidParamMessage(override val message: String) : GenericActionLogDetail(message, ActionLogScope.parameter)

class InvalidReportMessage(override val message: String) : GenericActionLogDetail(message, ActionLogScope.report)

class InvalidHL7Message(override val message: String) : GenericActionLogDetail(message, ActionLogScope.report)

class InvalidTranslationMessage(override val message: String) :
    GenericActionLogDetail(message, ActionLogScope.translation)