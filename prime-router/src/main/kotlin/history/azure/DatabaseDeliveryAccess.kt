package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.COVID_RESULT_METADATA
import gov.cdc.prime.router.azure.db.Tables.ITEM_LINEAGE
import gov.cdc.prime.router.azure.db.Tables.REPORT_FACILITIES
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.DeliveryFacility
import org.jooq.Condition
import org.jooq.Field
import org.jooq.impl.DSL
import java.util.UUID

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseDeliveryAccess(
    db: DatabaseAccess = BaseEngine.databaseAccessSingleton
) : HistoryDatabaseAccess(db) {

    /**
     * Values that facilities can be sorted by
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
     * Creates a condition filter based on the given [organization] and [orgService].
     * This condition is for a list of reports ready for delivery.   These reports may or may not have been sent.
     *
     * @return Condition used to filter the organization involved in the requested history
     */
    override fun organizationFilter(
        organization: String,
        orgService: String?
    ): Condition {
        var filter = ACTION.ACTION_NAME.eq(TaskAction.batch)
            .and(REPORT_FILE.RECEIVING_ORG.eq(organization))

        if (orgService != null) {
            filter = filter.and(REPORT_FILE.RECEIVING_ORG_SVC.eq(orgService))
        }

        return filter
    }

    /**
     * Fetch a single (usually detailed) action of a specific type.
     *
     * @param actionId the action id attached to this submission.
     * @param orgName currently this is ignored.  Its only needed for submissions.
     * @param klass the class that the found data will be converted to.
     * @return the submission matching the given query parameters, or null.
     */
    override fun <T> fetchAction(
        actionId: Long,
        orgName: String?,
        klass: Class<T>
    ): T? {
        return db.transactReturning { txn ->
            DSL.using(txn)
                .select(
                    ACTION.ACTION_ID,
                    ACTION.CREATED_AT,
                    ACTION.SENDING_ORG,
                    REPORT_FILE.RECEIVING_ORG,
                    REPORT_FILE.RECEIVING_ORG_SVC,
                    ACTION.HTTP_STATUS,
                    ACTION.EXTERNAL_NAME,
                    REPORT_FILE.REPORT_ID,
                    REPORT_FILE.SCHEMA_TOPIC,
                    REPORT_FILE.ITEM_COUNT,
                    REPORT_FILE.BODY_URL,
                    REPORT_FILE.SCHEMA_NAME,
                    REPORT_FILE.BODY_FORMAT
                )
                .from(
                    ACTION.join(REPORT_FILE).on(
                        REPORT_FILE.ACTION_ID.eq(ACTION.ACTION_ID)
                    )
                )
                .where(
                    ACTION.ACTION_ID.eq(actionId)
                ).fetchOne()?.into(klass)
        }
    }

    override fun <T> fetchRelatedActions(reportId: UUID, klass: Class<T>): List<T> {
        TODO("Not yet implemented")
    }

    /**
     * Fetch a list of facilities for a single delivery.
     *
     * @param reportId ID of report whose details we want to see
     * @param sortDir sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column
     * @return a list of facilities
     */
    fun fetchFacilityListOld(
        reportId: ReportId,
        sortDir: SortDir,
        sortColumn: FacilitySortColumn
    ): List<DeliveryFacility> {
        val column = when (sortColumn) {
            /* Decides sort column by enum */
            FacilitySortColumn.NAME -> REPORT_FACILITIES.TESTING_LAB_NAME
            FacilitySortColumn.CITY -> REPORT_FACILITIES.TESTING_LAB_CITY
            FacilitySortColumn.STATE -> REPORT_FACILITIES.TESTING_LAB_STATE
            FacilitySortColumn.CLIA -> REPORT_FACILITIES.TESTING_LAB_CLIA
            FacilitySortColumn.POSITIVE -> REPORT_FACILITIES.POSITIVE
            FacilitySortColumn.TOTAL -> REPORT_FACILITIES.COUNT_RECORDS
        }

        val sortedColumn = when (sortDir) {
            /* Applies sort order by enum */
            SortDir.ASC -> column.asc()
            SortDir.DESC -> column.desc()
        }

        return db.transactReturning { txn ->
            val query = DSL.using(txn)
                .select(
                    REPORT_FACILITIES.TESTING_LAB_NAME,
                    REPORT_FACILITIES.TESTING_LAB_CITY,
                    REPORT_FACILITIES.TESTING_LAB_STATE,
                    REPORT_FACILITIES.TESTING_LAB_CLIA,
                    REPORT_FACILITIES.POSITIVE,
                    REPORT_FACILITIES.COUNT_RECORDS
                )
                .from(REPORT_FACILITIES(reportId))
                .orderBy(sortedColumn)

            query.fetchInto(DeliveryFacility::class.java)
        }
    }

    fun fetchFacilityList(
        reportId: ReportId,
        sortDir: SortDir,
        sortColumn: FacilitySortColumn
    ): List<DeliveryFacility> {
        val positive: Field<String> = DSL.field(
            "sum(CASE WHEN test_result = 'DETECTED' THEN 1 ELSE 0 END)",
            String::class.java
        ).`as`("positive")
        val total: Field<String> = DSL.field(
            "count(covid_results_metadata_id)",
            String::class.java
        ).`as`("count_records")

        val column = when (sortColumn) {
            /* Decides sort column by enum */
            FacilitySortColumn.NAME -> COVID_RESULT_METADATA.TESTING_LAB_NAME
            FacilitySortColumn.CITY -> COVID_RESULT_METADATA.TESTING_LAB_CITY
            FacilitySortColumn.STATE -> COVID_RESULT_METADATA.TESTING_LAB_STATE
            FacilitySortColumn.CLIA -> COVID_RESULT_METADATA.TESTING_LAB_CLIA
            FacilitySortColumn.POSITIVE -> positive
            FacilitySortColumn.TOTAL -> total
        }

        val sortedColumn = when (sortDir) {
            /* Applies sort order by enum */
            SortDir.ASC -> column.asc()
            SortDir.DESC -> column.desc()
        }

        return db.transactReturning { txn ->
            val query = DSL.using(txn)
                .select(
                    COVID_RESULT_METADATA.TESTING_LAB_NAME,
                    COVID_RESULT_METADATA.TESTING_LAB_CITY,
                    COVID_RESULT_METADATA.TESTING_LAB_STATE,
                    COVID_RESULT_METADATA.TESTING_LAB_CLIA,
                    positive,
                    total
                )
                .from(
                    COVID_RESULT_METADATA.join(ITEM_LINEAGE).on(
                        COVID_RESULT_METADATA.REPORT_ID.eq(ITEM_LINEAGE.INPUT_REPORT_ID),
                        COVID_RESULT_METADATA.REPORT_INDEX.eq(ITEM_LINEAGE.INPUT_REPORT_INDEX)
                    )
                ).where(
                    ITEM_LINEAGE.CHILD_REPORT_ID.eq(reportId)
                ).groupBy(
                    COVID_RESULT_METADATA.TESTING_LAB_NAME,
                    COVID_RESULT_METADATA.TESTING_LAB_CITY,
                    COVID_RESULT_METADATA.TESTING_LAB_STATE,
                    COVID_RESULT_METADATA.TESTING_LAB_CLIA
                )
                .orderBy(sortedColumn)

            query.fetchInto(DeliveryFacility::class.java)
        }
    }
}