package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.Organization
import org.apache.logging.log4j.kotlin.Logging

/**
 * A set of helper functions related to validating scopes.
 *
 * In ReportStream, a scope is of the form senderOrReceiverFullname.detailedScope,
 * that is, orgName.senderName.detailedScope or orgName.receiverName.detailedScope.
 * Example:  strac.default.report
 */
class Scope {
    companion object : Logging {

        /**
         * Scope that represents prime admin access.
         * Pattern is <organization>.<receiver-or-sender>.<DetailedScope>
         * The first value must always be an organization name, or wildcard
         * The idea is to leave the last field loose; it could be a role or a permission
         * Examples:
         * md-phd.*.user -- user level access
         * md-phd.default.report -- able to submit reports only
         * md-phd.*.admin
         * *.*.primeadmin
         */
        val primeAdminScope = "*.*.primeadmin"

        /**
         * Constraint set of allowed values for the last part of a scope string.
         * [strRep] is the string representation of the DetailedScope.
         */
        enum class DetailedScope(val strRep: String) {
            Report("report"), // ability to submit a report
            Admin("admin"), // local org administrator role
            PrimeAdmin("primeadmin"), // overall administrator role
            User("user"); // user role

            companion object {
                /**
                 * Convert a string representation of a DetailedScope to a DetailedScope obj.
                 * @return the matching DetailedScope, or null if no such DetailedScope exists.
                 */
                fun toDetailedScope(strRep: String): DetailedScope? {
                    return values().find { it.strRep == strRep }
                }
            }
        }

        /**
         * Return true if scope is the PrimeAdmin scope
         */
        fun isPrimeAdmin(scope: String?): Boolean {
            return scope == primeAdminScope
        }

        /**
         * @return true if this [scope] syntax is correct.  Otherwise false.
         * Syntax check only.  Does not check if the org, sender-or-recevier, or role/perimission are valid.
         */
        fun isWellFormedScope(scope: String): Boolean {
            if (scope.contains("\\s".toRegex())) {
                logger.warn("Error: Scope [$scope] must not contain whitespace")
                return false
            }
            val splits = scope.split(".")
            if (splits.size != 3) {
                logger.warn("Scope must be org.sender-or-receiver.role-or-permission.  Instead got: $scope ")
                return false
            }
            if (splits[0].isEmpty() || splits[1].isEmpty() || splits[2].isEmpty()) {
                logger.warn("Error: Scope [$scope] must not have empty strings")
                return false
            }
            return true
        }

        /**
         * @return true if this [scope] string is well-formed and has a valid DetailedScope,
         * and the org portion of it matches [expectedOrganization].
         * Also returns true if scope is [primeAdminScope].
         * Otherwise, false.
         */
        fun isValidScope(scope: String, expectedOrganization: Organization): Boolean {
            if (!isValidScope(scope)) return false
            val splits = scope.split(".")
            if (splits[0] != expectedOrganization.name && scope != primeAdminScope) {
                logger.warn("Expected organization ${expectedOrganization.name}. Instead got: ${splits[0]}")
                return false
            }
            return true
        }

        /**
         * @return true if this [scope] string is well-formed and has a valid DetailedScope.
         */
        fun isValidScope(scope: String): Boolean {
            if (!isWellFormedScope(scope)) return false
            val splits = scope.split(".")
            if (DetailedScope.toDetailedScope(splits[2]) == null) {
                logger.warn("Invalid DetailedScope ${splits[2]}")
                return false
            }
            return true
        }

        /**
         * Break apart a scope into its constituent parts
         * @return null if it isn't well-formed.
         * Otherwise return the triple (organizationName, sender/receiverName, detailed scope)
         */
        fun parseScope(scope: String): Triple<String, String, DetailedScope>? {
            if (!isValidScope(scope)) return null
            val splits = scope.split(".")
            val orgName = splits[0]
            val senderOrReceiver = splits[1]
            val detailedScope: DetailedScope = DetailedScope.toDetailedScope(splits[2])!! // checked in isValidScope
            return Triple(orgName, senderOrReceiver, detailedScope)
        }

        /**
         * Finds the set of scopes that the user is authorized to have.
         * Returns the matches between the set of scopes the user claims ([userScopeList])
         * and the set of scopes acceptable to access a resource ([requiredScopeList])
         *
         * @return the (possibly empty) intersection of the two sets.  Empty Set == not authorized for anything.
         */
        internal fun authorizedScopes(userScopes: Set<String>, requiredScopes: Set<String>): Set<String> {
            return userScopes.filter { it.isNotBlank() }.intersect(requiredScopes.filter { it.isNotBlank() }.toSet())
        }

        /**
         * @return true if there is at least one match between the set of scopes the user claims ([userScopeList])
         * and the set of scopes acceptable to access a resource ([requiredScopeList])
         */
        internal fun authorized(userScopeList: Set<String>, requiredScopeList: Set<String>): Boolean {
            val intersection = authorizedScopes(userScopeList, requiredScopeList)
            return if (intersection.isEmpty()) {
                logger.warn(
                    "User not authorized. User has scope: (${userScopeList.joinToString(",")});" +
                        " required scope is: (${requiredScopeList.joinToString(",")})"
                )
                false
            } else {
                logger.info("User authorized for scopes (${intersection.joinToString(",")})")
                true
            }
        }

        /**
         * Note that [scopeList] should be a space-separated list of scopes. Eg "a.b.c d.e.f", which is a
         * canonical way of tracking scopes as a single string.
         *
         * @return true if the [scopeList], a space-separated list of scopes, presumably from the claims,
         * contains the desired [requiredScope].  Otherwise false.
         */
        fun scopeListContainsScope(scopeList: String, requiredScope: String): Boolean {
            if (requiredScope.isBlank() || requiredScope.isEmpty()) return false
            // A scope is a set of strings separated by single spaces
            val scopesTrial: Set<String> = scopeList.split(" ").toSet()
            return scopesTrial.contains(requiredScope)
        }

        /**
         * Iterate through the Okta group [memberships] and convert them to our scopes format.
         * @return the set of scopes associated with these [memberships].  Returns an empty set if no valid
         * Okta groups are in [memberships].  Note this completely ignores the Sender_ stuff.
         *
         * Example mappings from Okta to scopes:
         * DHPrimeAdmins         *.*.primeadmin
         * DHxyzAdmins           xyz.*.admin
         * DHSender_xyzAdmins    xyz.*.admin
         * DHSender_xyz          xyz.*.user
         * DHxyz                 xyz.*.user
         */
        fun mapOktaGroupsToScopes(memberships: Collection<String>): Set<String> {
            val tmpScopes = mutableSetOf<String>()
            memberships.forEach {
                when {
                    (it == oktaSystemAdminGroup) -> tmpScopes.add(Scope.primeAdminScope)
                    (it.startsWith(oktaSenderGroupPrefix) && it.endsWith(oktaAdminGroupSuffix)) -> {
                        val org = it.removePrefix(oktaSenderGroupPrefix).removeSuffix(oktaAdminGroupSuffix)
                        if (org.isBlank()) return@forEach
                        tmpScopes.add("$org.*.${Scope.Companion.DetailedScope.Admin.strRep}") // eg, md-phd.*.admin
                    }
                    (it.startsWith(oktaGroupPrefix) && it.endsWith(oktaAdminGroupSuffix)) -> {
                        val org = it.removePrefix(oktaGroupPrefix).removeSuffix(oktaAdminGroupSuffix)
                        if (org.isBlank()) return@forEach
                        tmpScopes.add("$org.*.${Scope.Companion.DetailedScope.Admin.strRep}") // eg, md-phd.*.admin
                    }
                    (it.startsWith(oktaSenderGroupPrefix)) -> {
                        val org = it.removePrefix(oktaSenderGroupPrefix)
                        if (org.isBlank()) return@forEach
                        tmpScopes.add("$org.*.${Scope.Companion.DetailedScope.User.strRep}") // eg, md-phd.*.user
                    }
                    (it.startsWith(oktaGroupPrefix)) -> {
                        val org = it.removePrefix(oktaGroupPrefix)
                        if (org.isBlank()) return@forEach
                        tmpScopes.add("$org.*.${Scope.Companion.DetailedScope.User.strRep}") // eg, md-phd.*.user
                    }
                }
            }
            return tmpScopes // Might be empty if no valid memberships are found following our patterns
        }
    }
}