package gov.cdc.prime.router.azure

import com.azure.core.http.ContentType
import com.google.common.net.MediaType
import gov.cdc.prime.router.Metadata
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.BlobTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import com.microsoft.azure.functions.annotation.BindingName
import gov.cdc.prime.router.CsvConverter
import gov.cdc.prime.router.OrganizationService
import java.io.ByteArrayInputStream
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.util.*
import java.util.logging.Level


import gov.cdc.prime.router.transport.SftpTransport;


/**
 * Azure Functions with HTTP Trigger. Write to blob.
 */
class SendFunction {

    @FunctionName("Send")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @BlobTrigger(name = "content",
            path = "%PROCESSED_BLOB_CONTAINER%/{fileName}.csv",
            dataType = "binary") content: ByteArray,
        @BindingName("fileName") fileName: String,
        context: ExecutionContext,
    ) {
        val baseDir = System.getenv("AzureWebJobsScriptRoot")
        Metadata.loadAll("$baseDir/metadata")

        context.logger.info("Dispatch function processed a blob. Name: $fileName Size: ${content.size} bytes");

        try {
            //val mockServer = MockSftpServer( 9022 )
            //context.logger.info( "Writing to ${mockServer.getBaseDirectory().toString()}" )
            //val session = initSshClient()
            //val sendKlass = Class.forName("gov.cdc.prime.router.SftpSend").kotlin

            val transportMetadata: OrganizationService.Transport = lookupTransportMetadata()
            val transport = SftpTransport() // TODO:  look up the correct class to call based on the transport metadata


            transport.send(transportMetadata, content, fileName)
        } catch (t: Throwable) {
            error("Unable to process blob ${fileName}\n ${t.message}")
        }

    }

    private fun lookupTransportMetadata(): OrganizationService.Transport {
        return OrganizationService.Transport();  // TODO: actually lookup the Transport here - for now use the default
    }

}
