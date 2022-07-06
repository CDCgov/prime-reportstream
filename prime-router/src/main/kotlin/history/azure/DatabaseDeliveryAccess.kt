package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.DeliveryFacility
import org.jooq.Condition
import org.jooq.impl.DSL
import java.time.OffsetDateTime

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseDeliveryAccess(
    db: DatabaseAccess = BaseEngine.databaseAccessSingleton,
) : HistoryDatabaseAccess(db) {

    /**
     * TODO
     *
     */
    enum class FacilitySortColumn {
        NAME,
        CITY,
        STATE,
        CLIA,
        POSITIVE,
        TOTAL,
    }

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
        var filter = ACTION.ACTION_NAME.eq(TaskAction.receive)
            .and(REPORT_FILE.RECEIVING_ORG.eq(organization))

        if (orgService != null) {
            filter = filter.and(ACTION.SENDING_ORG_CLIENT.eq(orgService))
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

    fun fetchFacilityList(
        reportId: ReportId,
        sortDir: SortDir,
        sortColumn: FacilitySortColumn,
        cursor: OffsetDateTime?,
        pageSize: Int,
    ): List<DeliveryFacility> {
        val column = when (sortColumn) {
            /* Decides sort column by enum */
            FacilitySortColumn.NAME -> Tables.REPORT_FACILITIES.TESTING_LAB_NAME
            FacilitySortColumn.CITY -> Tables.REPORT_FACILITIES.TESTING_LAB_CITY
            FacilitySortColumn.STATE -> Tables.REPORT_FACILITIES.TESTING_LAB_STATE
            FacilitySortColumn.CLIA -> Tables.REPORT_FACILITIES.TESTING_LAB_CLIA
            FacilitySortColumn.POSITIVE -> Tables.REPORT_FACILITIES.POSITIVE
            FacilitySortColumn.TOTAL -> Tables.REPORT_FACILITIES.COUNT_RECORDS
        }

        val sortedColumn = when (sortDir) {
            /* Applies sort order by enum */
            SortDir.ASC -> column.asc()
            SortDir.DESC -> column.desc()
        }

//        var filter: Condition?

        return db.transactReturning { txn ->
            val query = DSL.using(txn)
                // Note the report file and action tables have columns with the same name, so we must specify what we need.
                .select(
                    Tables.REPORT_FACILITIES.TESTING_LAB_NAME,
                    Tables.REPORT_FACILITIES.TESTING_LAB_CITY,
                    Tables.REPORT_FACILITIES.TESTING_LAB_STATE,
                    Tables.REPORT_FACILITIES.TESTING_LAB_CLIA,
                    Tables.REPORT_FACILITIES.POSITIVE,
                    Tables.REPORT_FACILITIES.COUNT_RECORDS
                )
                .from(Tables.REPORT_FACILITIES(reportId))
                .orderBy(sortedColumn)

            if (cursor != null) {
//                query.seek(cursor)
//                query.seek()
            }

            query.limit(pageSize)
                .fetchInto(DeliveryFacility::class.java)
        }
    }
}