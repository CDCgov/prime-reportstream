package gov.cdc.prime.router.azure

interface DeliveryAccess {
    fun <T> fetchActions(
        receivingOrg: String,
        klass: Class<T>
    ): List<T>
}
/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseDeliveryAccess() : DeliveryAccess {

    /**
     * @param receivingOrg is the Organization Name returned from the Okta JWT Claim.
     * @return a list of results matching the SQL Query.
     */
    override fun <T> fetchActions(
        receivingOrg: String,
        klass: Class<T>
    ): List<T> {
        // @todo return mock data here
        return []
    }
}