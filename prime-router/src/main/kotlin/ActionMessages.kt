package gov.cdc.prime.router

/**
 * Possible error codes when parsing/processing messages. Includes Hl7, CSV, FHIR, et al.
 */
enum class ErrorCode {
    INVALID_MSG_PARSE_GENERAL,
    INVALID_MSG_PARSE_TEXT,
    INVALID_MSG_PARSE_TEXT_OR_BLANK,
    INVALID_MSG_PARSE_NUMBER,
    INVALID_MSG_PARSE_DATE,
    INVALID_MSG_PARSE_DATETIME,
    INVALID_MSG_PARSE_DURATION,
    INVALID_MSG_PARSE_CODE,
    INVALID_MSG_PARSE_CODE_ALT_VALUES,
    INVALID_MSG_PARSE_CODE_VALUES,
    INVALID_MSG_PARSE_ELEMENT_CODE,
    INVALID_MSG_PARSE_TABLE,
    INVALID_MSG_PARSE_TABLE_OR_BLANK,
    INVALID_MSG_PARSE_EI,
    INVALID_MSG_PARSE_HD,
    INVALID_MSG_PARSE_ID,
    INVALID_MSG_PARSE_ID_CLIA,
    INVALID_MSG_PARSE_ID_DLN,
    INVALID_MSG_PARSE_ID_SSN,
    INVALID_MSG_PARSE_ID_NPI,
    INVALID_MSG_PARSE_STREET,
    INVALID_MSG_PARSE_STREET_OR_BLANK,
    INVALID_MSG_PARSE_CITY,
    INVALID_MSG_PARSE_POSTAL_CODE,
    INVALID_MSG_PARSE_PERSON_NAME,
    INVALID_MSG_PARSE_TELEPHONE,
    INVALID_MSG_PARSE_EMAIL,
    INVALID_MSG_PARSE_BLANK,
    INVALID_MSG_PARSE_UNKNOWN,
    INVALID_MSG_MISSING_FIELD,
    INVALID_MSG_EQUIPMENT_MAPPING,
    INVALID_HL7_MSG_VALIDATION,
    INVALID_HL7_MSG_TYPE_MISSING,
    INVALID_HL7_MSG_TYPE_UNSUPPORTED,
    INVALID_HL7_MSG_FORMAT_INVALID,
    UNKNOWN
}

/**
 * Action details for item logs for specific [fieldMapping] and [errorCode].
 */
abstract class ItemActionLogDetail(
    val fieldMapping: String = ""
) : ActionLogDetail {
    override val scope = ActionLogScope.item
    override val errorCode = ErrorCode.UNKNOWN
}

/**
 * Generic detail log that uses a given [message], [scope] and [errorCode].
 */
abstract class GenericActionLogDetail(
    override val message: String,
    override val scope: ActionLogScope,
    override val errorCode: ErrorCode = ErrorCode.UNKNOWN
) :
    ActionLogDetail

/**
 * Message for a missing field of [fieldMapping].
 */
class MissingFieldMessage(fieldMapping: String) : ItemActionLogDetail(fieldMapping) {
    override val message = "Blank value for element $fieldMapping. " +
        "Please refer to the ReportStream Programmer's Guide for required fields."
    override val errorCode = ErrorCode.INVALID_MSG_MISSING_FIELD
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
    override val errorCode = ErrorCode.INVALID_MSG_PARSE_DATE
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
        var msg = "The code '$formattedValue' for field $fieldMapping is invalid. Reformat to HL7 specification."
        if (format !== null) {
            msg += " Reformat to $format."
        }
        return msg
    }
    override val errorCode = ErrorCode.INVALID_MSG_PARSE_CODE
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
    override val errorCode = ErrorCode.INVALID_MSG_PARSE_TELEPHONE
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
    override val errorCode = ErrorCode.INVALID_MSG_PARSE_POSTAL_CODE
}

/**
 * Message for an invalid HD for a given [fieldMapping] and [formattedValue].
 */
class UnsupportedHDMessage(
    formattedValue: String = "",
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "Unsupported HD format for input: '$formattedValue' in $fieldMapping"
    override val errorCode = ErrorCode.INVALID_MSG_PARSE_HD
}

/**
 * Message for an invalid EI for a given [fieldMapping] and [formattedValue].
 */
class UnsupportedEIMessage(
    formattedValue: String = "",
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "Unsupported EI format for input: '$formattedValue' in $fieldMapping"
    override val errorCode = ErrorCode.INVALID_MSG_PARSE_EI
}

/**
 * A message to denote that equipment was not found in the LIVD table for a given [fieldMapping].
 */
class InvalidEquipmentMessage(
    fieldMapping: String
) : ItemActionLogDetail(fieldMapping) {
    override val message = "No match found for $fieldMapping; please refer to the " +
        "CDC LIVD table LOINC Mapping spreadsheet for acceptable values."
    override val errorCode = ErrorCode.INVALID_MSG_EQUIPMENT_MAPPING
}

/**
 * A [message] to denote a field precision issue for a given [fieldMapping].
 */
class FieldPrecisionMessage(
    fieldMapping: String = "", // Default to empty for backwards compatibility
    override val message: String,
    override val errorCode: ErrorCode = ErrorCode.UNKNOWN
) : ItemActionLogDetail(fieldMapping)

/**
 * A [message] and [errorCode] to denote a field processing issue for a given [fieldMapping].
 */
class FieldProcessingMessage(
    fieldMapping: String = "", // Default to empty for backwards compatibility
    override val message: String,
    override val errorCode: ErrorCode = ErrorCode.UNKNOWN
) : ItemActionLogDetail(fieldMapping)

/**
 * A [message] for invalid HL7 message.  Note field mapping is not available from the HAPI errors.
 * An optional [errorCode] for the error. Used to display a friendly error.
 */
class InvalidHL7Message(
    override val message: String,
    override val errorCode: ErrorCode = ErrorCode.UNKNOWN
) : ItemActionLogDetail("")

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
    override val errorCode = ErrorCode.UNKNOWN
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
    override val errorCode = ErrorCode.UNKNOWN
    override val message = "Item is a duplicate."
}

/**
 * A [message] for non-error details.
 */
class FhirActionLogDetail(
    override val message: String
) : GenericActionLogDetail(message, ActionLogScope.report, ErrorCode.UNKNOWN)

/**
 * A return message for invalid processing type
 */
class UnsupportedProcessingTypeMessage() : ActionLogDetail {
    override val scope = ActionLogScope.report
    override val errorCode = ErrorCode.UNKNOWN
    override val message = "Full ELR senders must be configured for async processing."
}

/**
 * A [message] for when a filter has an invalid expression
 */
class EvaluateFilterConditionErrorMessage(message: String?) : ActionLogDetail {
    override val scope = ActionLogScope.internal
    override val message = message ?: "An unknown filter condition error occurred."
    override val errorCode = ErrorCode.UNKNOWN
}