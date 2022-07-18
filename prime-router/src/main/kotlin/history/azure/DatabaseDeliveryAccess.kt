package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import org.jooq.Condition

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseDeliveryAccess(
    db: DatabaseAccess = BaseEngine.databaseAccessSingleton,
) : HistoryDatabaseAccess(db) {

    /**
     * Creates a condition filter based on the given organization parameters.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param orgService is a specifier for an organization, such as the client or service used to send/receive
     * @return Condition used to filter the organization involved in the requested history
     */
    override fun organizationFilter(
        organization: String,
        orgService: String?,
    ): Condition {
        var filter = ACTION.ACTION_NAME.eq(TaskAction.send)
            .and(REPORT_FILE.RECEIVING_ORG.eq(organization))

        if (orgService != null) {
            filter = filter.and(REPORT_FILE.RECEIVING_ORG_SVC.eq(orgService))
        }

        return filter
    }

    /**
     * Fetch the details of an action's relations (descendants).
     * This is done through a recursive query on the report_lineage table.
     *
     * @param actionId the action id attached to the action to find relations for.
     * @param klass the class that the found data will be converted to.
     * @return a list of descendants for the given action id.
     */
    override fun <T> fetchRelatedActions(actionId: Long, klass: Class<T>): List<T> {
        TODO("Not yet implemented")
    }
}