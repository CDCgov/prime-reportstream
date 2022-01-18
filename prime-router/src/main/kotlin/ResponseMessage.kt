package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

enum class ResponseMsgType {
    // CSV Serializing errors
    INVALID_DATE,
    INVALID_CODE,
    INVALID_PARAM,
    INVALID_PHONE,
    INVALID_POSTAL,
    MISSING,
    REPORT,
    TRANSLATION,
    UNSUPPORTED_HD,
    UNSUPPORTED_EI,
    // HL7 Serializing errors
    INVALID_HL7,
    // Common errors
    INVALID_EQUIPMENT,
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(InvalidDateMessage::class, name = "INVALID_DATE"),
    JsonSubTypes.Type(InvalidCodeMessage::class, name = "INVALID_CODE"),
    JsonSubTypes.Type(InvalidParamMessage::class, name = "INVALID_PARAM"),
    JsonSubTypes.Type(InvalidPhoneMessage::class, name = "INVALID_PHONE"),
    JsonSubTypes.Type(InvalidPostalMessage::class, name = "INVALID_POSTAL"),
    JsonSubTypes.Type(MissingFieldMessage::class, name = "MISSING"),
    JsonSubTypes.Type(InvalidReportMessage::class, name = "REPORT"),
    JsonSubTypes.Type(InvalidTranslationMessage::class, name = "TRANSLATION"),
    JsonSubTypes.Type(UnsupportedHDMessage::class, name = "UNSUPPORTED_HD"),
    JsonSubTypes.Type(UnsupportedEIMessage::class, name = "UNSUPPORTED_EI"),
    JsonSubTypes.Type(InvalidHL7Message::class, name = "INVALID_HL7"),
    JsonSubTypes.Type(InvalidEquipmentMessage::class, name = "INVALID_EQUIPMENT"),
)
interface ResponseMessage {
    val type: ResponseMsgType
    fun detailMsg(): String
    fun groupingId(): String
}