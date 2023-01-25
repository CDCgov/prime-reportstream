package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
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
     * @return a single Report associated with this [actionId]
     */
    fun fetchReportForActionId(actionId: Long): ReportFile? {
        return dbAccess.fetchReportForActionId(actionId)
    }

    /**
     * @return a single Action, given its key [actionId]
     */
    fun fetchAction(actionId: Long): Action? {
        return dbAccess.fetchAction(actionId)
    }

    /**
     * Check whether these [claims] allow access to this [orgName].
     * @return true if [claims] authorizes access to this [orgName].  Return
     * false if the [orgName] is empty or if the claim does not give access.
     */
    fun checkAccessAuthorizationForOrg(
        claims: AuthenticatedClaims,
        orgName: String? = null,
        senderOrReceiver: String? = null,
        request: HttpRequestMessage<String?>,
    ): Boolean {
        return claims.authorizedForSendOrReceive(orgName, senderOrReceiver, request)
    }

    /**
     * Check whether these [claims] from this [request]
     * allow access to the sender or receiver associated with this [action].
     * @return true if authorized, false otherwise.
     */
    abstract fun checkAccessAuthorizationForAction(
        claims: AuthenticatedClaims,
        action: Action,
        request: HttpRequestMessage<String?>,
    ): Boolean
}