package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.HttpException
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.DeliveryFacility
import gov.cdc.prime.router.history.DeliveryHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import org.apache.logging.log4j.util.Lazy.lazy
import java.time.OffsetDateTime
import java.util.*

/**
 * Deliveries API
 * Contains all business logic regarding deliveries and JSON serialization.
 */
class DeliveryFacade(
    private val dbDeliveryAccess: DatabaseDeliveryAccess = DatabaseDeliveryAccess(),
    dbAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton,
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
            throw HttpException("Invalid format for report ID: $reportIdStr", HttpStatus.UNPROCESSABLE_ENTITY)
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
            DeliveryHistory::class.java,
            reportId,
            fileName
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
        return dbDeliveryAccess.fetchAction(
            deliveryId,
            orgName = null,
            DeliveryHistory::class.java
        )
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
    ): Boolean {
        return claims.authorizedForSendOrReceive(action.receivingOrg, null, request)
    }

    companion object {
        val instance: DeliveryFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DeliveryFacade()
        }
    }
}