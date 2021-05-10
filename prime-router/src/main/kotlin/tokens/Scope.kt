package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.Sender
import org.apache.logging.log4j.kotlin.Logging

/**
 * A set of helper functions related to validating scopes
 */
class Scope {
    companion object: Logging {
        fun isWellFormedScope(scope: String): Boolean {
            val splits = scope.split(".")
            if (splits.size != 3) {
                logger.warn("Scope should be org.sender.endpoint.  Instead got: $scope ")
                return false
            }
            return true
        }

        fun isValidScope(scope: String, expectedSender: Sender): Boolean {
            if (!isWellFormedScope(scope)) return false
            val splits = scope.split(".")
            if (splits[0] != expectedSender.organizationName) {
                logger.warn("Expected organization ${expectedSender.organizationName}. Instead got: ${splits[0]}")
                return false
            }
            if (splits[1] != expectedSender.name) {
                logger.warn("Expected sender ${expectedSender.name}. Instead got: ${splits[1]}")
                return false
            }
            return when (splits[2]) {
                "report" -> true
                else -> false
            }
        }

        fun generateValidScope(sender: Sender, endpoint: String): String {
            return "${sender.fullName}.$endpoint"
        }

        fun scopeListContainsScope(scopeList: String, desiredScope: String): Boolean {
            if (desiredScope.isBlank() || desiredScope.isEmpty()) return false
            // A scope is a set of strings separated by single spaces
            val scopesTrial: List<String> = scopeList.split(" ")
            return scopesTrial.contains(desiredScope)
        }


    }
}