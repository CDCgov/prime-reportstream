package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.DeliveryFacility
import gov.cdc.prime.router.history.DeliveryHistory
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import java.time.OffsetDateTime
import java.util.*

/**
 * Deliveries API
 * Contains all business logic regarding deliveries and JSON serialization.
 */
class DeliveryFacade(
    private val dbDeliveryAccess: DatabaseDeliveryAccess = DatabaseDeliveryAccess(),
    private val dbAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton,
    val reportService: ReportService = ReportService(),
) : ReportFileFacade(
    dbAccess
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
     * @param reportIdStr is the reportId to get results for.
     * @param fileName is the fileName to get results for.
     * @param receivingOrgSvcStatus is the customer status of the receiving organization's service.
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
        pageSize: Int,
        reportIdStr: String?,
        fileName: String?,
        receivingOrgSvcStatus: List<CustomerStatus>?,
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

        var reportId: UUID?
        try {
            reportId = if (reportIdStr != null) UUID.fromString(reportIdStr) else null
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid format for report ID: $reportIdStr")
        }

        return dbDeliveryAccess.fetchActionsForDeliveries(
            organization,
            receivingOrgSvc,
            sortDir,
            sortColumn,
            cursor,
            since,
            until,
            pageSize,
            true,
            DeliveryHistory::class.java,
            reportId,
            fileName,
            receivingOrgSvcStatus
        )
    }

    /**
     * Get expanded details for a single report
     *
     * @param id  either a report id (UUID) or action id (Long)
     * @param deliveryId id for the delivery being used
     * @return Report details
     */
    fun findDetailedDeliveryHistory(
        id: String,
        deliveryId: Long,
    ): DeliveryHistory? {
        // This functionality is handling the fact that the calling function supports loading the history either
        // by the action id or report id
        val reportFileId = try {
            UUID.fromString(id)
        } catch (ex: IllegalArgumentException) {
            null
        }

        val deliveryHistory = if (reportFileId != null) {
            val action = dbAccess.fetchAction(deliveryId)
            val reportFile = dbAccess.fetchReportFile(reportFileId)
             DeliveryHistory.createDeliveryHistoryFromReportAndAction(reportFile, action!!)
        } else {
            dbDeliveryAccess.fetchAction(
                deliveryId,
                orgName = null,
                DeliveryHistory::class.java
            )
        }

        val reportId = deliveryHistory?.reportId
        if (reportId != null) {
            val roots = reportService.getRootReports(UUID.fromString(reportId))
            deliveryHistory.originalIngestion = roots.map {
                mapOf(
                    "reportId" to it.reportId,
                    "ingestionTime" to it.createdAt,
                    "sendingOrg" to it.sendingOrg,
                )
            }
        }
        return deliveryHistory
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
    ): List<DeliveryFacility> = dbDeliveryAccess.fetchFacilityList(
            reportId,
            sortDir,
            sortColumn,
        )

    /**
     * Check whether these [claims] from this [request]
     * allow access to the receiver associated with this [action].
     * @return true if authorized, false otherwise.
     * Because this is a Delivery request, this checks the [Action.receivingOrg]
     */
    override fun checkAccessAuthorizationForAction(
        claims: AuthenticatedClaims,
        action: Action,
        request: HttpRequestMessage<String?>,
    ): Boolean = claims.authorizedForSendOrReceive(action.receivingOrg, null, request)

    companion object {
        val instance: DeliveryFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DeliveryFacade()
        }
    }
}