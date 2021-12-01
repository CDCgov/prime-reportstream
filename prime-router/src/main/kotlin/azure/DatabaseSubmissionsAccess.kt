package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.jooq.impl.DSL
import java.time.OffsetDateTime

interface SubmissionAccess {
    fun <T> fetchActions(
        sendingOrg: String,
        orderAscending: Boolean = false,
        resultsAfterDate: OffsetDateTime? = null,
        limit: Int = 10,
        klass: Class<T>
    ): List<T>
}
/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess(private val db: DatabaseAccess = DatabaseAccess()) : SubmissionAccess {

    /**
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param sortOrder sort the table by date in ASC or DESC order.
     * @param resultsAfterDate is the Action Id of the last result in the previous list.
     * @param limit is an Integer used for setting the number of results per page.
     * @return a list of results matching the SQL Query.
     */
    override fun <T> fetchActions(
        sendingOrg: String,
        orderAscending: Boolean,
        resultsAfterDate: OffsetDateTime?,
        limit: Int,
        klass: Class<T>
    ): List<T> {
        var results: List<T> = emptyList()

        val sorted = if (orderAscending) ACTION.CREATED_AT.asc() else ACTION.CREATED_AT.desc()

        db.transact { txn ->
            val query = DSL.using(txn)
                .selectFrom(ACTION)
                .where(ACTION.ACTION_NAME.eq(TaskAction.receive).and(ACTION.SENDING_ORG.eq(sendingOrg)))
                .orderBy(sorted)
            if (resultsAfterDate != null) {
                query.seek(resultsAfterDate)
            }
            results = query.limit(limit)
                .fetchInto(klass)
        }

        return results
    }
}