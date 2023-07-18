package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import io.ktor.http.ContentType
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL

const val defaultConnEnvVar = "AzureWebJobsStorage"
const val PARAM_RESOURCE_NAME = "resourceName"
val RESOURCES_NAMES = listOf<String>(
    "api.yaml",
    "index.css",
    "swagger-ui.css",
    "swagger-initializer.js",
    "swagger-ui-bundle.js",
    "swagger-ui-standalone-preset.js",
    "favicon-16x16.png",
    "favicon-32x32.png",
)
/**
 * Class for serving swagger UI page
 */
class OpenAPIDocsFunction : Logging {

    @FunctionName("getApiDocs")
    fun getApiDocs(
        @HttpTrigger(
            name = "getApiDocs",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "docs"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val swaggerPage = getResourceAsText("/swagger-ui-5.1.0-dist/index.html")
        return HttpUtilities.httpResponseText(
            request,
            swaggerPage ?: "swagger ui index.html not found.",
            HttpStatus.OK,
            ContentType.Text.Html
        )
    }

    @FunctionName("getSwaggerResources")
    fun getSwaggerResources(
        @HttpTrigger(
            name = "getSwaggerResources",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "docs/{resourceName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName(PARAM_RESOURCE_NAME) resourceName: String
    ): HttpResponseMessage {
        val resourcePath = "/swagger-ui-5.1.0-dist/$resourceName"
        return if (!RESOURCES_NAMES.contains(resourceName)) {
            // better to response with 404 with minimum info
            HttpUtilities.notFoundResponse(request, "Not Found")
        } else if (resourceName.endsWith(".png")) {
            return HttpUtilities.httpResponseImage(
                request,
                getResourceAsStream(resourcePath),
                HttpStatus.OK,
                ContentType.Image.PNG
            )
        } else if (resourceName.endsWith(".css")) {
            return HttpUtilities.httpResponseText(
                request,
                getResourceAsText(resourcePath) ?: "Resource $resourceName not found.",
                HttpStatus.OK,
                ContentType.Text.CSS
            )
        } else if (resourceName.endsWith(".yaml")) {
            return HttpUtilities.httpResponseText(
                request,
                getResourceAsText(resourcePath) ?: "Resource $resourceName not found.",
                HttpStatus.OK,
                ContentType.Text.Any
            )
        } else if (resourceName.endsWith(".js")) {
            return HttpUtilities.httpResponseText(
                request,
                getResourceAsText(resourcePath) ?: "Resource $resourceName not found.",
                HttpStatus.OK,
                ContentType.Text.JavaScript
            )
        } else {
            throw Exception("Unexpected resource request: resource name = $resourceName")
        }
    }

    fun getResourceAsStream(path: String): ByteArrayOutputStream {
        val ostream = ByteArrayOutputStream()
        val url: URL = this::class.java.getResource(path)
        val istream: InputStream = url.openStream()
        ostream.write(istream.readAllBytes())
        return ostream
    }

    fun getResourceAsText(path: String): String? =
        this::class.java.getResource(path)?.readText()
}