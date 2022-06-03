package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import java.time.OffsetDateTime

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseDeliveryAccess(private val db: DatabaseAccess = WorkflowEngine.databaseAccessSingleton) :
    ReportFileAccess {

    /**
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param sortDir sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column; default created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param since is the OffsetDateTime that dictates how far back returned results date.
     * @param until is the OffsetDateTime that dictates how recently returned results date.
     * @param pageSize is an Integer used for setting the number of results per page.
     * @param showFailed whether to include actions that failed to be sent.
     * @param klass the class that the found data will be converted to.
     * @return a list of results matching the SQL Query.
     */
    override fun <T> fetchActions(
        organization: String,
        sortDir: ReportFileAccess.SortDir,
        sortColumn: ReportFileAccess.SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean,
        klass: Class<T>
    ): List<T> {
        println("$organization $sortDir $sortColumn $cursor $since $until $pageSize $showFailed $klass")
        return emptyList()
    }

    /**
     * Fetch the details of a single delivery.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param actionId the action id attached to this delivery.
     * @param klass the class that the found data will be converted to.
     * @return the submission matching the given query parameters, or null.
     */
    override fun <T> fetchAction(
        organization: String,
        actionId: Long,
        klass: Class<T>
    ): T? {
        println("$organization $actionId $klass")
        return null
    }

    /**
     * Fetch the details of an action's relations (descendants).
     * This is done through a recursive query on the report_lineage table.
     *
     * @param actionId the action id attached to the action to find relations for.
     * @param klass the class that the found data will be converted to.
     * @return a list of descendants for the given action id.
     */
    override fun <T> fetchRelatedActions(
        actionId: Long,
        klass: Class<T>
    ): List<T> {
        println("$actionId $klass")
        return emptyList()
    }
}