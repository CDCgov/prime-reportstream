package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.azure.DatabaseAccess
import org.apache.logging.log4j.kotlin.Logging

class AuthenticationStrategy() : Logging {
    companion object {

        // Returns an OktaAuthentication strategy if the authenticationType is "okta"
        fun authStrategy(
            authenticationType: String?,
            principalLevel: PrincipalLevel,
            db: DatabaseAccess,
        ): Any {
            // Clients using Okta will send "authentication-type": "okta" in the request header
            if (authenticationType == "okta") {
                return OktaAuthentication(principalLevel)
            }

            // default is TokenAuthentication
            return TokenAuthentication(DatabaseJtiCache(db))
        }
    }
}