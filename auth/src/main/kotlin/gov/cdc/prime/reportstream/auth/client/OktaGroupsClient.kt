package gov.cdc.prime.reportstream.auth.client

import com.okta.sdk.resource.api.ApplicationGroupsApi
import com.okta.sdk.resource.api.ApplicationApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service

@Service
class OktaGroupsClient(private val applicationGroupsApi: ApplicationGroupsApi,
    private val applicationApi: ApplicationApi) : Logging {

    /**
     * Get all application groups from the Okta Admin API
     *
     * Group names are found at json path "_embedded.group.profile.name"
     *
     * @see https://developer.okta.com/docs/api/openapi/okta-management/management/tag/ApplicationGroups/#tag/ApplicationGroups/operation/listApplicationGroupAssignments
     */
    suspend fun getApplicationGroups(appId: String): List<String> = withContext(Dispatchers.IO) {
            try {
                val groups = applicationGroupsApi
                    .listApplicationGroupAssignments(appId, null, null, null, "group")
                    .map { it.embedded?.get("group") as Map<*, *> }
                    .map { it["profile"] as Map<*, *> }
                    .map { it["name"] as String }
                logger.info("$appId is a member of ${groups.joinToString()}")
                val app = applicationApi.getApplication(appId, null)
                app.putprofileItem("groups", groups)
                applicationApi.replaceApplication(appId, app)

                groups
            } catch (ex: Exception) {
                logger.error("Error retrieving application groups from Okta API", ex)
                throw ex
            }
        }
}