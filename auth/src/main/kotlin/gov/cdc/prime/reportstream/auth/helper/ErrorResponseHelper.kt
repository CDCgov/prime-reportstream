package gov.cdc.prime.reportstream.auth.helper

import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.prime.reportstream.auth.model.AuthenticationFailure
import gov.cdc.prime.reportstream.auth.model.BadRequestResponse
import gov.cdc.prime.reportstream.auth.model.ErrorResponse
import gov.cdc.prime.reportstream.auth.model.InactiveToken
import gov.cdc.prime.reportstream.auth.model.InvalidAuthHeader
import gov.cdc.prime.reportstream.auth.model.InvalidClientId
import gov.cdc.prime.reportstream.auth.model.OktaFailure
import gov.cdc.prime.reportstream.auth.model.UnauthorizedResponse
import gov.cdc.prime.reportstream.auth.model.UnexpectedError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class ErrorResponseHelper @Autowired constructor(
    private val objectMapper: ObjectMapper,
) {

    fun authenticationErrorToResponseEntity(authenticationFailure: AuthenticationFailure): ResponseEntity<ByteArray> {
        val errorResponse: ErrorResponse = when (authenticationFailure) {
            is InvalidAuthHeader -> {
                UnauthorizedResponse("Malformed authorization header")
            }
            is OktaFailure -> {
                UnauthorizedResponse("Error introspecting response with Okta")
            }
            is InactiveToken -> {
                UnauthorizedResponse("Token is inactive")
            }
            is InvalidClientId -> {
                BadRequestResponse("Invalid client id")
            }
        }

        val status = when (errorResponse) {
            is BadRequestResponse -> HttpStatus.BAD_REQUEST
            is UnauthorizedResponse -> HttpStatus.UNAUTHORIZED
            is UnexpectedError -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        val body = objectMapper.writeValueAsBytes(errorResponse)
        return ResponseEntity(body, status)
    }
}