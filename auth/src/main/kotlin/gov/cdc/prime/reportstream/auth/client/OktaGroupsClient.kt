package gov.cdc.prime.reportstream.auth.client

import com.okta.sdk.resource.api.ApplicationGroupsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service

@Service
class OktaGroupsClient(
    private val applicationGroupsApi: ApplicationGroupsApi,
) : Logging {

    /**
     * Get all application groups from the Okta Admin API
     *
     * Group names are found at json path "_embedded.group.profile.name"
     *
     * @see https://developer.okta.com/docs/api/openapi/okta-management/management/tag/ApplicationGroups/#tag/ApplicationGroups/operation/listApplicationGroupAssignments
     */
    suspend fun getApplicationGroups(appId: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                applicationGroupsApi
                    .listApplicationGroupAssignments(appId, null, null, null, "group")
                    .map { it.embedded?.get("group") as Map<*, *> }
                    .map { it["profile"] as Map<*, *> }
                    .map { it["name"] as String }
            } catch (ex: Exception) {
                logger.error("Error retrieving application groups from Okta API", ex)
                throw ex
            }
        }
    }
}