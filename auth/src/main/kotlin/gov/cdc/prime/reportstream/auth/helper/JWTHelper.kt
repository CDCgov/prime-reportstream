package gov.cdc.prime.reportstream.auth.helper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.nimbusds.jwt.SignedJWT
import gov.cdc.prime.reportstream.auth.config.ApplicationConfig
import gov.cdc.prime.reportstream.auth.model.AuthenticationFailure
import gov.cdc.prime.reportstream.auth.model.InvalidClientId
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.text.ParseException

@Component
class JWTHelper @Autowired constructor(
    private val applicationConfig: ApplicationConfig,
) : Logging {

    fun getAndVerifyClientId(token: String): Either<AuthenticationFailure, String> {
        return try {
            val jwt = SignedJWT.parse(token)
            val clientId = jwt.jwtClaimsSet.getStringClaim("cid")
            // only allow configured client IDs
            val maybeValidClientId = applicationConfig.oktaConfig.validClientIds.find { it == clientId }
            if (maybeValidClientId != null) {
                maybeValidClientId.right()
            } else {
                logger.warn("No configured client ID matching value=$clientId")
                InvalidClientId.left()
            }
        } catch (ex: ParseException) {
            logger.warn("Error parsing JWT", ex)
            InvalidClientId.left()
        }
    }
}