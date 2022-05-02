package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.CustomerStatus
import org.apache.logging.log4j.kotlin.Logging

/*
 * Frontend API
 */

class FrontendFunctions(
    private val settingsFacade: SettingsFacade = SettingsFacade.common,
) : Logging {

    private fun parseCustomerStatus(customerStatusParam: String?): List<CustomerStatus> {
        if (customerStatusParam == null) {
            return listOf(CustomerStatus.ACTIVE)
        }
        val customerStatuses = mutableListOf<CustomerStatus>()
        customerStatusParam.split(',').forEach {
            val status = it.trim().uppercase()
            try {
                val customerStatus = enumValueOf<CustomerStatus>(status)
                customerStatuses.add(customerStatus)
            } catch (ex: IllegalArgumentException) {
                logger.warn("Invalid customer status value: $status: $ex")
            }
        }
        return customerStatuses
    }

    /**
     * TK
     */
    @FunctionName("activeOrganizations")
    fun getActiveOrganizations(
        @HttpTrigger(
            name = "activeOrganizations",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "frontend/active-organizations"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val customerStatuses = parseCustomerStatus(request.queryParameters["customerStatus"])
        val outputBody = settingsFacade.findOrganizationsByReceiverStatusAsJson(customerStatuses)
        return HttpUtilities.okResponse(request, outputBody)
    }
}