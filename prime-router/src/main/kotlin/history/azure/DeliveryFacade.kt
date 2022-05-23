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
     * @param organizationName from JWT Claim.
     *
     * @return a String representation of an array of actions.
     */
    fun findDeliveriesAsJson(
        organizationName: String,
        sortOrder: ReportFileAccess.SortOrder,
        sortColumn: ReportFileAccess.SortColumn,
        offset: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        pageSize: Int
    ): String {
        val result = findDeliveries(organizationName, sortOrder, sortColumn, offset, toEnd, pageSize)
        return mapper.writeValueAsString(result)
    }

    /**
     * @param organizationName from JWT Claim.
     *
     * @return a List of Actions
     */
    fun findDeliveries(
        organizationName: String,
        sortOrder: ReportFileAccess.SortOrder,
        sortColumn: ReportFileAccess.SortColumn,
        offset: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        pageSize: Int
    ): List<DeliveryHistory> {
        require(organizationName.isNotBlank()) {
            "Invalid organization."
        }
        require(pageSize > 0) {
            "pageSize must be a positive integer."
        }
        require(offset == null || toEnd == null || toEnd > offset) {
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

        if (offset == null || offset.compareTo(OffsetDateTime.parse("2022-04-12T17:06:10.534Z")) <= 0) {
            if (toEnd == null || toEnd.compareTo(OffsetDateTime.parse("2022-04-12T17:06:10.534Z")) >= 0) {
                list.add(delivery1)
            }
        }

        if (toEnd == null || toEnd.compareTo(OffsetDateTime.parse("2022-04-19T17:06:10.534Z")) >= 0) {
            if (offset == null || offset.compareTo(OffsetDateTime.parse("2022-04-19T17:06:10.534Z")) <= 0) {
                list.add(delivery2)
            }
        }

        if (sortOrder == ReportFileAccess.SortOrder.DESC && sortColumn == ReportFileAccess.SortColumn.CREATED_AT) {
            list = list.reversed().toMutableList()
        }

        if (pageSize == 1) {
            list.removeLast()
        }

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