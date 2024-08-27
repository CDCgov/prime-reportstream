package gov.cdc.prime.reportstream.auth.model

enum class ErrorResponseType {
    UNAUTHORIZED,
    UNEXPECTED_ERROR,
    BAD_REQUEST,
}

sealed interface ErrorResponse {
    val type: ErrorResponseType
    val message: String
}

data class BadRequestResponse(
    override val message: String,
) : ErrorResponse {
    override val type: ErrorResponseType = ErrorResponseType.BAD_REQUEST
}

data class UnauthorizedResponse(
    override val message: String,
) : ErrorResponse {
    override val type: ErrorResponseType = ErrorResponseType.UNAUTHORIZED
}

data class UnexpectedError(
    override val message: String,
) : ErrorResponse {
    override val type: ErrorResponseType = ErrorResponseType.UNEXPECTED_ERROR
}