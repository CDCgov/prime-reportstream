package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.Sender
import org.apache.logging.log4j.kotlin.Logging
import java.lang.IllegalArgumentException

/**
 * A set of helper functions related to validating scopes.
 *
 * In ReportStream, a scope is of the form senderOrReceiverFullname.detailedScope,
 * that is, orgName.senderName.detailedScope or orgName.receiverName.detailedScope.
 * Example:  strac.default.report
 */
class Scope {
    companion object : Logging {
        enum class DetailedScope {
            report, // ability to submit a report
        }

        fun isWellFormedScope(scope: String): Boolean {
            val splits = scope.split(".")
            if (splits.size != 3) {
                logger.warn("Scope should be org.sender.endpoint.  Instead got: $scope ")
                return false
            }
            return true
        }

        /**
         * Is this [scope] string well-formed and has a valid DetailedScope,
         * and the org.sender portion of it matches the [expectedSender]
         */
        fun isValidScope(scope: String, expectedSender: Sender): Boolean {
            if (!isValidScope(scope)) return false
            val splits = scope.split(".")
            if (splits[0] != expectedSender.organizationName) {
                logger.warn("Expected organization ${expectedSender.organizationName}. Instead got: ${splits[0]}")
                return false
            }
            if (splits[1] != expectedSender.name) {
                logger.warn("Expected sender ${expectedSender.name}. Instead got: ${splits[1]}")
                return false
            }
            return true
        }

        /**
         * Is this [scope] string well-formed and has a valid DetailedScope.
         */
        fun isValidScope(scope: String): Boolean {
            if (!isWellFormedScope(scope)) return false
            val splits = scope.split(".")
            try {
                DetailedScope.valueOf(splits[2])
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid DetailedScope ${splits[2]}")
                return false
            }
            return true
        }

        /**
         * Break apart a scope into its constituent parts.
         * @return null if it isn't well-formed.
         * Otherwise return the triple (organizationName, sender/receiverName, detailed scope)
         */
        fun parseScope(scope: String): Triple<String, String, DetailedScope>? {
            if (!isValidScope(scope)) return null
            val splits = scope.split(".")
            val orgName = splits[0]
            val senderOrReceiver = splits[1]
            val detailedScope = DetailedScope.valueOf(splits[2])
            return Triple(orgName, senderOrReceiver, detailedScope)
        }

        /**
         * Returns true if the [scopeList], presumably from the claims, contains the desired [requiredScope].
         * Otherwise false.
         */
        fun scopeListContainsScope(scopeList: String, requiredScope: String): Boolean {
            if (requiredScope.isBlank() || requiredScope.isEmpty()) return false
            // A scope is a set of strings separated by single spaces
            val scopesTrial: List<String> = scopeList.split(" ")
            return scopesTrial.contains(requiredScope)
        }
    }
}