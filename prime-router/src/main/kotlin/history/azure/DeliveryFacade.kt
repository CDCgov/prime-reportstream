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
    private val dbDeliveryAccess: DatabaseDeliveryAccess = DatabaseDeliveryAccess(),
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
    private fun findDeliveries(
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

        return dbDeliveryAccess.fetchActions(
            organizationName,
            sortOrder,
            sortColumn,
            offset,
            toEnd,
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