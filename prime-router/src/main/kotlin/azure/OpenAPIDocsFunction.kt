package gov.cdc.prime.router.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL

const val defaultConnEnvVar = "AzureWebJobsStorage"
const val PARAM_RESOURCE_NAME = "resourceName"
const val textJS = "text/javascript"
const val textCSS = "text/css"
const val textYaml = "text/yaml"

/**
 * Class for serving swagger UI page
 */
class OpenAPIDocsFunction : Logging {

    @FunctionName("getApiDocs")
    fun getAPIDocs(
        @HttpTrigger(
            name = "getApiDocs",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "docs"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        val swaggerPage = getResourceAsText("/swagger-ui-5.1.0-dist/index.html")
        return HttpUtilities.httpResponseHTML(request, swaggerPage ?: "swagger ui index.html not found.", HttpStatus.OK)
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
        if (resourceName.endsWith(".png")) {
            return HttpUtilities.httpResponseImage(request, downloadBlob(resourcePath), HttpStatus.OK)
        } else {
            var rsType = textJS
            val resourceClob = getResourceAsText(resourcePath)
            if (resourceName.endsWith(".css")) {
                rsType = textCSS
            } else if (resourceName.endsWith(".yaml")) {
                rsType = textYaml
            } else if (!resourceName.endsWith(".js")) {
                throw Exception("Unexpected resource request: resource name = $resourceName")
            }
            return HttpUtilities.httpResponseText(
                request,
                resourceClob
                    ?: "Resource $resourceName not found.",
                HttpStatus.OK, rsType
            )
        }
    }

    fun getResourceAsByteArray(path: String): ByteArray {
        val ostream = ByteArrayOutputStream()
        val url: URL = this::class.java.getResource(path)
        val istream: InputStream = url.openStream()
        ostream.write(istream.readAllBytes())
        return ostream.toByteArray()
    }

    fun getResourceAsText(path: String): String? =
        this::class.java.getResource(path)?.readText()

    fun downloadBlob(blobUrl: String): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.use { getBlobClient(blobUrl).downloadStream(it) }
        return stream.toByteArray()
    }

    private fun getBlobClient(blobUrl: String, blobConnEnvVar: String = defaultConnEnvVar): BlobClient {
        val blobConnection = System.getenv(blobConnEnvVar)
        return BlobClientBuilder().connectionString(blobConnection).endpoint(blobUrl).buildClient()
    }
}