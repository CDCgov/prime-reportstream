package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DeliveryHistory
import java.time.OffsetDateTime

/**
 * Deliveries API
 * Contains all business logic regarding deliveries and JSON serialization.
 */
class DeliveryFacade(
    private val dbDeliveryAccess: ReportFileAccess = DatabaseDeliveryAccess(),
    dbAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton
) : ReportFileFacade(
    dbAccess,
) {
    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    /**
     * Serializes a list of Actions into a String.
     *
     * @param organization from JWT Claim.
     * @param receivingOrgSvc is a specifier for the receiving organization's service.
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
        receivingOrgSvc: String?,
        sortDir: ReportFileAccess.SortDir,
        sortColumn: ReportFileAccess.SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int
    ): String {
        val result = findDeliveries(organization, receivingOrgSvc, sortDir, sortColumn, cursor, since, until, pageSize)
        return mapper.writeValueAsString(result)
    }

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

        return dbDeliveryAccess.fetchActions(
            organization,
            receivingOrgSvc,
            sortDir,
            sortColumn,
            cursor,
            since,
            until,
            pageSize,
            false,
            DeliveryHistory::class.java
        )
    }

    companion object {
        val instance: DeliveryFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DeliveryFacade()
        }
    }
}