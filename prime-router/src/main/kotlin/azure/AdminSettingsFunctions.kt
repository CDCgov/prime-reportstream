package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount

/**
 * These functions support the settings that only accessible by admins
 */
class AdminSettingsFunctions {
    class ListOrganizations {
        /**
         * @see docs/openapi.yml
         */
        @FunctionName("listOrganizations")
        @StorageAccount("AzureWebJobsStorage")
        fun run(
            @HttpTrigger(
                name = "req",
                methods = [HttpMethod.GET],
                authLevel = AuthorizationLevel.FUNCTION,
                route = "settings/organizations"
            ) request: HttpRequestMessage<String?>,
            context: ExecutionContext,
        ): HttpResponseMessage {
            return request.createResponseBuilder(HttpStatus.FOUND).build()
        }
    }

    class CreateOrganization {
        /**
         * @see docs/openapi.yml
         */
        @FunctionName("createOrganization")
        @StorageAccount("AzureWebJobsStorage")
        fun run(
            @HttpTrigger(
                name = "req",
                methods = [HttpMethod.POST],
                authLevel = AuthorizationLevel.FUNCTION,
                route = "settings/organizations"
            ) request: HttpRequestMessage<String?>,
            context: ExecutionContext,
        ): HttpResponseMessage {
            return request.createResponseBuilder(HttpStatus.FOUND).build()
        }
    }

    class UpdateOrganization {
        /**
         * @see docs/openapi.yml
         */
        @FunctionName("updateOrganization")
        @StorageAccount("AzureWebJobsStorage")
        fun run(
            @HttpTrigger(
                name = "req",
                methods = [HttpMethod.PUT],
                authLevel = AuthorizationLevel.FUNCTION,
                route = "settings/organizations/{organizationName:alpha}"
            ) request: HttpRequestMessage<String?>,
            @BindingName("organizationName") organizationName: String,
            context: ExecutionContext,
        ): HttpResponseMessage {
            return request.createResponseBuilder(HttpStatus.FOUND).build()
        }
    }
}