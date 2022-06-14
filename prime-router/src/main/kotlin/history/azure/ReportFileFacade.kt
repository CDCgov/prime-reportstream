package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import org.apache.logging.log4j.kotlin.Logging
import java.util.UUID

/**
 * Submissions / history API
 * Contains all business logic regarding submissions and JSON serialization.
 */
abstract class ReportFileFacade(
    private val dbAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton
) : Logging {
    /**
     * @return a single Action associated with this [reportId]
     */
    fun fetchActionForReportId(reportId: UUID): Action? {
        return dbAccess.fetchActionForReportId(reportId)
    }

    /**
     * @return a single Action, given its key [actionId]
     */
    fun fetchAction(actionId: Long): Action? {
        return dbAccess.fetchAction(actionId)
    }

    /**
     * Check whether these [claims] allow access to this [action]
     * @return true if [claims] authorizes access to this 'receive' [action].  Return
     * false if the actionId is not a proper submission or if the claim does not give access.
     */
    fun checkSenderAccessAuthorization(
        action: Action,
        claims: AuthenticatedClaims,
    ): Boolean {
        return when {
            // Admins always get access
            claims.isPrimeAdmin -> true
            // User has an organization claim that matches the action's sendingOrg
            // No reason to also require that the org have (isSenderOrgClaim == true) because
            // if they belong to the org that sent it, that's sufficient to say they have the right to see it.
            (action.sendingOrg == claims.organizationNameClaim) &&
                (!claims.organizationNameClaim.isNullOrBlank()) -> true
            else -> {
                logger.error(
                    "User from org '${claims.organizationNameClaim}'" +
                        " denied access to action_id ${action.actionId}" +
                        " submitted by ${action.sendingOrg}"
                )
                false
            }
        }
    }
}