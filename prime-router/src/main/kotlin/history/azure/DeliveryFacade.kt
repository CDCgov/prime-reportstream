package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DeliveryHistory
import java.time.OffsetDateTime

/**
 * Deliveries API
 * Contains all business logic regarding deliveries and JSON serialization.
 */
class DeliveryFacade(
    private val dbDeliveryAccess: ReportFileAccess = DatabaseDeliveryAccess(),
    dbAccess: DatabaseAccess = WorkflowEngine.databaseAccessSingleton
) : ReportFileFacade(
    dbAccess,
) {
    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    /**
     * Serializes a list of Actions into a String.
     *
     * @param organization from JWT Claim.
     * @param sortDir sort the table by date in ASC or DESC order.
     * @param sortColumn sort the table by a specific column; defaults to sorting by created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param since is the OffsetDateTime minimum date to get results for.
     * @param until is the OffsetDateTime maximum date to get results for.
     * @param pageSize Int of items to return per page.
     *
     * @return a String representation of an array of actions.
     */
    fun findDeliveriesAsJson(
        organization: String,
        sortDir: ReportFileAccess.SortDir,
        sortColumn: ReportFileAccess.SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int
    ): String {
        val result = findDeliveries(organization, sortDir, sortColumn, cursor, since, until, pageSize)
        return mapper.writeValueAsString(result)
    }

    /**
     * Find deliveries based on parameters.
     *
     * @param organization from JWT Claim.
     * @param sortDir sort the table by date in ASC or DESC order; defaults to DESC.
     * @param sortColumn sort the table by a specific column; defaults to sorting by CREATED_AT.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param since is the OffsetDateTime minimum date to get results for.
     * @param until is the OffsetDateTime maximum date to get results for.
     * @param pageSize Int of items to return per page.
     *
     * @return a List of Actions
     */
    fun findDeliveries(
        organization: String,
        sortDir: ReportFileAccess.SortDir,
        sortColumn: ReportFileAccess.SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int
    ): List<DeliveryHistory> {
        require(organization.isNotBlank()) {
            "Invalid organization."
        }
        require(pageSize > 0) {
            "pageSize must be a positive integer."
        }
        require(since == null || until == null || until > since) {
            "End date must be after start date."
        }

        val delivery1 = DeliveryHistory(
            284,
            OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
            "ca-dph",
            "elr-secondary",
            201,
            null,
            "c3c8e304-8eff-4882-9000-3645054a30b7",
            "covid-19",
            1,
            "",
            "covid-19",
            "HL7_BATCH"
        )

        val delivery2 = DeliveryHistory(
            922,
            OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
            "ca-dph",
            "elr-secondary",
            201,
            null,
            "b9f63105-bbed-4b41-b1ad-002a90f07e62",
            "covid-19",
            14,
            "",
            "primedatainput/pdi-covid-19",
            "CSV"
        )

        var list = mutableListOf<DeliveryHistory>()

        if (since == null || since.compareTo(OffsetDateTime.parse("2022-04-12T17:06:10.534Z")) <= 0) {
            if (until == null || until.compareTo(OffsetDateTime.parse("2022-04-12T17:06:10.534Z")) >= 0) {
                list.add(delivery1)
            }
        }

        if (until == null || until.compareTo(OffsetDateTime.parse("2022-04-19T17:06:10.534Z")) >= 0) {
            if (since == null || since.compareTo(OffsetDateTime.parse("2022-04-19T17:06:10.534Z")) <= 0) {
                list.add(delivery2)
            }
        }

        if (sortDir == ReportFileAccess.SortDir.DESC && sortColumn == ReportFileAccess.SortColumn.CREATED_AT) {
            list = list.reversed().toMutableList()
        }

        if (pageSize == 1) {
            list.removeLast()
        }

        println("$cursor")

        return list

        // return dbDeliveryAccess.fetchActions(
        //     organizationName,
        //     sortOrder,
        //     sortColumn,
        //     offset,
        //     toEnd,
        //     pageSize,
        //     false,
        //     DeliveryHistory::class.java
        // )
    }

    companion object {
        val instance: DeliveryFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DeliveryFacade()
        }
    }
}