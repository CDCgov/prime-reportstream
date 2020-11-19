package gov.cdc.prime.router

/**
 * Organization represents a partner organization of the hub (eg. a sender or a receiver).
 */
data class Organization(
    val name: String,
    val description: String? = null,
    val clients: List<OrganizationClient> = emptyList(),
    val services: List<OrganizationService> = emptyList(),
) {
    init {
        // init back-references
        services.forEach { it.organization = this }
        clients.forEach { it.organization = this }
    }
}