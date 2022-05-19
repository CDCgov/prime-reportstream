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
     * @param order sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column; default created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param toEnd is the OffsetDateTime that dictates how far back returned results date.
     * @param limit is an Integer used for setting the number of results per page.
     * @return a list of results matching the SQL Query.
     */
    override fun <T> fetchActions(
        organization: String,
        order: ReportFileAccess.SortOrder,
        sortColumn: ReportFileAccess.SortColumn,
        cursor: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        limit: Int,
        showFailed: Boolean,
        klass: Class<T>
    ): List<T> {
        println("$organization $order $sortColumn $cursor $toEnd $limit $klass")
        return emptyList()
    }

    /**
     * fetch the details of a single action
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
     * Fetch the details of an actions relations (descendents)
     *
     * This is done through a recursive query on the report_lineage table
     */
    override fun <T> fetchRelatedActions(
        actionId: Long,
        klass: Class<T>
    ): List<T> {
        println("$actionId $klass")
        return emptyList()
    }
}