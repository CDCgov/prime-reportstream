package gov.cdc.prime.reportstream.auth.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import gov.cdc.prime.reportstream.auth.client.OktaClient
import gov.cdc.prime.reportstream.auth.helper.JWTHelper
import gov.cdc.prime.reportstream.auth.model.Authenticated
import gov.cdc.prime.reportstream.auth.model.AuthenticationFailure
import gov.cdc.prime.reportstream.auth.model.InactiveToken
import gov.cdc.prime.reportstream.auth.model.InvalidAuthHeader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthenticationService @Autowired constructor(
    private val oktaClient: OktaClient,
    private val jwtHelper: JWTHelper,
) {

    suspend fun authenticate(authHeader: String?): Either<AuthenticationFailure, Authenticated> {
        return either {
            val token = extractToken(authHeader).bind()
            oktaClient.introspect(token).flatMap { introspectResponse ->
                if (introspectResponse.active) {
                    Authenticated.right()
                } else {
                    InactiveToken.left()
                }
            }.bind()
        }
    }

    private fun extractToken(authHeader: String?): Either<AuthenticationFailure, String> {
        return if (authHeader != null &&
            authHeader.startsWith("Bearer ") &&
            authHeader.split(" ").size == 2
        ) {
            authHeader
                .substringAfter("Bearer ")
                .trim()
                .right()
        } else {
            InvalidAuthHeader.left()
        }
    }
}