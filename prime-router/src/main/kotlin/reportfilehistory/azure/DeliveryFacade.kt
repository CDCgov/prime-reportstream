package gov.cdc.prime.router.azure

import gov.cdc.prime.router.DeliveryHistory
import gov.cdc.prime.router.common.JacksonMapperUtilities

/**
 * Deliveries API
 * Contains all business logic regarding deliveries and JSON serialization.
 */
class DeliveryFacade(
    private val dbDeliveryAccess: DeliveryAccess = DatabaseDeliveryAccess(),
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
        organizationName: String
    ): String {
        val result = findDeliveries(organizationName)
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)
    }

    /**
     * @param organizationName from JWT Claim.
     *
     * @return a List of Actions
     */
    private fun findDeliveries(
        organizationName: String,
    ): List<DeliveryHistory> {
        require(organizationName.isNotBlank()) {
            "Invalid organization."
        }

        return dbDeliveryAccess.fetchActions(
            organizationName,
            DeliveryHistory::class.java
        )
    }

    companion object {
        val instance: HistoryFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            HistoryFacade(DatabaseHistoryAccess())
        }
    }
}