package gov.cdc.prime.router

/**
 * Action details for item logs for specific [fieldMapping] and [errorCode].
 */
abstract class ItemActionLogDetail(
    val fieldMapping: String = ""
) : ActionLogDetail {
    override val scope = ActionLogScope.item
    override val errorCode: String = ""
}

/**
 * Generic detail log that uses a given [message], [scope] and [errorCode].
 */
abstract class GenericActionLogDetail(
    override val message: String,
    override val scope: ActionLogScope,
    override val errorCode: String = ""
) :
    ActionLogDetail

/**
 * Message for a missing field of [fieldMapping].
 */
class MissingFieldMessage(fieldMapping: String) : ItemActionLogDetail(fieldMapping) {
    override val message = "Blank value for element $fieldMapping. " +
        "Please refer to the ReportStream Programmer's Guide for required fields."
}

/**
 * Message for an invalid date for a given [fieldMapping], [formattedValue] and [format].
 */
class InvalidDateMessage(
    private val formattedValue: String = "",
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

/**
 * Message for an invalid CODE for a given [fieldMapping], [formattedValue] and [format].
 */
class InvalidCodeMessage(
    private val formattedValue: String = "",
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

/**
 * Message for an invalid phone number for a given [fieldMapping] and [formattedValue].
 */
class InvalidPhoneMessage(
    formattedValue: String = "",
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "Invalid phone number '$formattedValue' for $fieldMapping. Reformat to a 10-digit phone " +
        "number (e.g. (555) - 555-5555)."
}

/**
 * Message for an invalid postal code for a given [fieldMapping], [formattedValue] and [format].
 */
class InvalidPostalMessage(
    private val formattedValue: String = "",
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

/**
 * Message for an invalid HD for a given [fieldMapping] and [formattedValue].
 */
class UnsupportedHDMessage(
    formattedValue: String = "",
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "Unsupported HD format for input: '$formattedValue' in $fieldMapping"
}

/**
 * Message for an invalid EI for a given [fieldMapping] and [formattedValue].
 */
class UnsupportedEIMessage(
    formattedValue: String = "",
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "Unsupported EI format for input: '$formattedValue' in $fieldMapping"
}

/**
 * A message to denote that equipment was not found in the LIVD table for a given [fieldMapping].
 */
class InvalidEquipmentMessage(
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "No match found for $fieldMapping; please refer to the " +
        "CDC LIVD table LOINC Mapping spreadsheet for acceptable values."
}

/**
 * A [message] to denote a field precision issue for a given [fieldMapping].
 */
class FieldPrecisionMessage(
    fieldMapping: String = "", // Default to empty for backwards compatibility
    override val message: String,
    override val errorCode: String = ""
) : ItemActionLogDetail(fieldMapping)

/**
 * A [message] for invalid HL7 message.  Note field mapping is not available from the HAPI errors.
 * An optional [errorCode] for the error. Used to display a friendly error.
 */
class InvalidHL7Message(override val message: String, override val errorCode: String = "") : ItemActionLogDetail(
    ""
)

/**
 * A [message] for invalid request parameter.
 */
class InvalidParamMessage(override val message: String) : GenericActionLogDetail(message, ActionLogScope.parameter)

/**
 * A [message] for an invalid report.
 */
class InvalidReportMessage(override val message: String) : GenericActionLogDetail(message, ActionLogScope.report)

/**
 * A [message] for invalid translation operation.
 */
class InvalidTranslationMessage(override val message: String) :
    GenericActionLogDetail(message, ActionLogScope.translation)

/**
 * A [message] for entire duplicate report submission
 */
class DuplicateSubmissionMessage(val payloadName: String?) : ActionLogDetail {
    override val scope = ActionLogScope.report
    override val errorCode = ""
    override val message: String get() {
        var msg = "All items in this submission are duplicates."
        if (!payloadName.isNullOrEmpty()) {
            msg += " Payload name: $payloadName"
        }
        return msg
    }
}

/**
 * A [message] for a duplicate item within a submission
 */
class DuplicateItemMessage() : ActionLogDetail {
    override val scope = ActionLogScope.item
    override val errorCode = ""
    override val message = "Item is a duplicate."
}

/**
 * A [message] for non-error details.
 */
class FhirActionLogDetail(
    override val message: String
) : GenericActionLogDetail(message, ActionLogScope.report, "")

/**
 * A return message for invalid processing type
 */
class UnsupportedProcessingTypeMessage() : ActionLogDetail {
    override val scope = ActionLogScope.report
    override val errorCode = ""
    override val message = "Full ELR senders must be configured for async processing."
}