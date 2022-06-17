package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import org.jooq.Condition

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess(
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
        var senderFilter = ACTION.ACTION_NAME.eq(TaskAction.receive)
            .and(ACTION.SENDING_ORG.eq(organization))

        if (orgService != null) {
            senderFilter = senderFilter.and(ACTION.SENDING_ORG_CLIENT.eq(orgService))
        }

        return senderFilter
    }
}