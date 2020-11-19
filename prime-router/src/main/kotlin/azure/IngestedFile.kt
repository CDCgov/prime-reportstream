package gov.cdc.prime.router.azure

import com.azure.core.exception.HttpResponseException
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.queue.QueueClient
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

/*
 * POJO that specifically represents a File sent to the Hub/Router.
 * @todo Merge this with Rick's Report.kt
 */
class IngestedFile {
    var filename: String // name of the file as passed in by the client.
    var topic: String // eg, covid-19
    var schema: String // eg, pdi-covid-19
    var action: String // eg, "route" - what to do with this file.  Probably should be an array.
    lateinit var blobURL: String // URL of the blob in Azure storage.

    @get:JsonIgnore
    var reportContent: String // The actual content of the csv (or other format) file.  @todo SHOULD NOT BE STRING.

    /* Create an object from an incoming http POST
     * This assumes the full body of the POST is the actual contents of the data file.
     * Does not currently work with binary data.
     *
     *
     *
     * @throws Exception on validation errors.  Detailed error msgs are in an array returned by e.getSuppressed().
     */
    constructor(request: HttpRequestMessage<String?>, context: ExecutionContext?) {
        val params = request.queryParameters
        filename = params.getOrDefault("filename", "")
        topic = params.getOrDefault("topic", "")  // ex.  'covid-19'
        schema = params.getOrDefault("schema", "")  // ex.  'pdi-covid-19'
        action = params.getOrDefault("action", "")
        // Note:  use of String will only work for text data
        // Does not handle multipart form data right now.
<<<<<<< HEAD
        reportContent = request.body ?:  ""
=======
        reportContent = request.body ?: ""

>>>>>>> master
        validate()
    }

    @Throws(Exception::class)
    fun validate() {
        val errors: MutableList<String> = ArrayList()
        if (filename.isEmpty()) {
            errors.add("Error:  Missing filename parameter")
        }
         if (schema.isEmpty()) {
            errors.add("Error:  Missing schemaName parameter")
        }
        // @todo Should we allow an empty report data file, or is that always an error?
        if (reportContent.isEmpty()) {
            errors.add("Error:  incoming data file is empty or missing")
        }
        if (!errors.isEmpty()) {
            val e = Exception()
            errors.forEach { e.addSuppressed(Exception(it)) }
            throw e
        }
    }

    /*
     * Queue this CVS file for further processing.  Store its metadata on the queue, and put the
     * blob in Azure Blob Storage.
     * @throws All kinds of unchecked exceptions if the Queue or Blob submission fails.
     */
    @Throws(JsonProcessingException::class)
    fun queueForProcessing(queue: QueueClient, blobContainerClient: BlobContainerClient, context: ExecutionContext) {
        // Create the blob first.
        val blobFilename = createInternalFilename(filename, schema)
        val blobClient = blobContainerClient.getBlobClient(blobFilename)
        val bytes = reportContent.toByteArray(Charset.forName("UTF-8"))
        val inputStream: InputStream = ByteArrayInputStream(bytes)
        context.logger.info("uploading ${bytes.size} byte file to blob storage...")
        blobClient.upload(inputStream, bytes.size.toLong(), true)
        context.logger.info("upload done: $blobFilename")
        blobURL = blobClient.blobUrl

        // Now create the message
        val metadataJson = toJson()
        try {
            context.logger.info("sending this message: $metadataJson")
            val result = queue.sendMessage(metadataJson)
            context.logger.info("message Id ${result.messageId} sent.")
        } catch (e: HttpResponseException) {
            // I can't for the life of me figure out how to fix this, so I'm temporarily ignoring it.
            // The sendMessage *works*, its just when its constructing the return value, it gives this very weird exception
            /* Unexpected first character (char code 0xEF), not valid in xml document: could be mangled UTF-8 BOM marker. Make sure that the Reader uses correct encoding or pass an InputStream instead
	    */
            context.logger.info("Caught this exception: " + e + e.message)
            e.printStackTrace()
            // @todo FIX THIS - IGNORING this exception.
        }
    }

    // @todo needs error checking.
    @Throws(JsonProcessingException::class)
    fun toJson(): String {
        val mapper = ObjectMapper()
        return mapper.writeValueAsString(this)
    }

    companion object {
        /*
         * Generate a filename that can be used to store a blob.  Filename must be guaranteed unique, hence the UUID.
         * I thought that was better than a timestamp, since that's not guaranteed unique if we're running high thruput.
         *
         * Trying to cover all possibilities here.  Even a completely empty filename will create
         * a filename that's just a UUID.
         *
         * @todo Rick will need a function that takes one of these filenames and generates one to represent a
         *   transformed file.
         *
         * Normal use case:    foo.csv in schema 'pdi-covid-19' becomes
         *     foo-pdi-covid-19-08ad635e-c801-43d0-8353-eb157193d065.csv
         */
        fun createInternalFilename(externalFilename: String?, schemaName: String?): String {
            val baseName = FilenameUtils.getBaseName(externalFilename)
            val basePart = if (StringUtils.isEmpty(baseName)) "" else "$baseName-"
            val extension = FilenameUtils.getExtension(externalFilename)
            val extPart = if (StringUtils.isEmpty(extension)) "" else ".$extension"
            val schemaPart = if (StringUtils.isEmpty(schema)) "" else "$schema-"
            return basePart + schemaPart + UUID.randomUUID() + extPart
        }
    }
}