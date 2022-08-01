package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.DeliveryFacility
import gov.cdc.prime.router.history.DeliveryHistory
import java.time.OffsetDateTime

/**
 * Deliveries API
 * Contains all business logic regarding deliveries and JSON serialization.
 */
class DeliveryFacade(
    private val dbDeliveryAccess: DatabaseDeliveryAccess = DatabaseDeliveryAccess(),
    dbAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton
) : ReportFileFacade(
    dbAccess,
) {
    /**
     * Find deliveries based on parameters.
     *
     * @param organization from JWT Claim.
     * @param receivingOrgSvc is a specifier for the receiving organization's service.
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
        receivingOrgSvc: String?,
        sortDir: HistoryDatabaseAccess.SortDir,
        sortColumn: HistoryDatabaseAccess.SortColumn,
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

        return dbDeliveryAccess.fetchActions(
            organization,
            receivingOrgSvc,
            sortDir,
            sortColumn,
            cursor,
            since,
            until,
            pageSize,
            true,
            DeliveryHistory::class.java
        )
    }

    /**
     * Get expanded details for a single report
     *
     * @param deliveryId id for the delivery being used
     * @return Report details
     */
    fun findDetailedDeliveryHistory(
        deliveryId: Long,
    ): DeliveryHistory? {
        return DeliveryHistory(
            deliveryId,
            OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
            null,
            "c3c8e304-8eff-4882-9000-3645054a30b7",
            "covid-19",
            1,
            "ak-phd",
            "elr-secondary",
            null,
            "covid-19",
            "HL7_BATCH",
        )
//        return dbDeliveryAccess.fetchAction(
//            organizationName,
//            deliveryId,
//            DeliveryHistory::class.java
//        )
    }

    /**
     * Get facilities for a single delivery.
     *
     * @param reportId ID of report whose details we want to see
     * @param sortDir sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column
     * @return a list of facilities
     */
    fun findDeliveryFacilities(
        reportId: ReportId,
        sortDir: HistoryDatabaseAccess.SortDir,
        sortColumn: DatabaseDeliveryAccess.FacilitySortColumn,
    ): List<DeliveryFacility> {
        return dbDeliveryAccess.fetchFacilityList(
            reportId,
            sortDir,
            sortColumn,
        )
    }

    companion object {
        val instance: DeliveryFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DeliveryFacade()
        }
    }
}