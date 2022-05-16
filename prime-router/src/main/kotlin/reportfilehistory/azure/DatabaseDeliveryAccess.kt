package gov.cdc.prime.router.azure

import gov.cdc.prime.router.DeliveryHistory
import java.time.OffsetDateTime

// interface DeliveryAccess {
//     fun <T> fetchActions(
//         receivingOrg: String,
//         klass: Class<T>
//     ): List<T>
// }
/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseDeliveryAccess() { // : DeliveryAccess
    /**
     * @param receivingOrg is the Organization Name returned from the Okta JWT Claim.
     * @return a list of results matching the SQL Query.
     */
    // override fun <T> fetchActions(
    fun <T> fetchActions(
        // receivingOrg: String,
        // klass: Class<T>
        // ): List<T> {
    ): List<DeliveryHistory> {
        return listOf(
            DeliveryHistory(
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
            ),
        )
    }
}