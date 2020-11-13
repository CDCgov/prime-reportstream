package gov.cdc.prime.router.azure;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.queue.QueueClient;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.azure.storage.blob.*;

/*
 * POJO that specifically represents a CSV sent to the Hub/Router.
 */
public class Csv {
    private String filename;  // name of the file as passed in by the client.
    private String topic;  // eg, covid-19
    private String csvContent;  // The actual content of the csv file.  @todo SHOULD NOT BE STRING.
    private String schemaName; // eg, pdi-covid-19
    private String action;  // eg, "route" - what to do with this file.  Probably should be an array.
    private String blobURL;  // URL of the blob in Azure storage.

    public Csv() {
    }

    /* Create a CSV from an incoming http POST
     * This assumes the full body of the POST is the contencts of the csv.
     * Does not currently work with binary data.
     *
     *
     *
     * @throws Exception on validation errors.  Detailed error msgs are in an array returned by e.getSuppressed().
     */
    public Csv(HttpRequestMessage<String> request, ExecutionContext context) throws Exception {
        Map<String,String> params = request.getQueryParameters();
        this.filename = params.get("filename");
        this.topic = params.get("topic");  // ex.  'covid-19'
        this.schemaName = params.get("schema-name");  // ex.  'pdi-covid-19'
        // Note:  use of String will only work for text data
        // Does not handle multipart form data right now.
        this.csvContent = request.getBody();
        validate();
    }

    public void validate() throws Exception {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isEmpty(filename)) {
            errors.add("Error:  Missing filename parameter");
        }
        if (StringUtils.isEmpty(topic)) {
            errors.add("Error:  Missing topic parameter");
        }
        if (StringUtils.isEmpty(schemaName)) {
            errors.add("Error:  Missing schemaName parameter");
        }
        // @todo Should we allow an empty csv file, or is that always an error?
        if (StringUtils.isEmpty(csvContent)) {
            errors.add("Error:  CSV File is empty or missing");
        }

        if (!errors.isEmpty()) {
            Exception e = new Exception();
            for (String error: errors) {
                e.addSuppressed(new Exception(error));
            }
            throw e;
        }
    }

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
     * Normal use case:    foo.cvs in schema 'pdi-covid-19' becomes
     *     foo-pdi-covid-19-08ad635e-c801-43d0-8353-eb157193d065.csv
     */
    public static String createInternalFilename(String externalFilename, String schemaName) {
        String baseName = FilenameUtils.getBaseName(externalFilename);
        String basePart = StringUtils.isEmpty(baseName) ? "" : baseName + "-";
        String extension = FilenameUtils.getExtension(externalFilename);
        String extPart = StringUtils.isEmpty(extension) ? "" : "." + extension;
        String schemaPart = StringUtils.isEmpty(schemaName) ? "" : schemaName + "-";
        return  basePart + schemaPart + java.util.UUID.randomUUID() + extPart;
    }
    /*
     * Queue this CVS file for further processing.  Store its metadata on the queue, and put the
     * blob in Azure Blob Storage.
     * @throws All kinds of unchecked exceptions if the Queue or Blob submission fails.
     */
    public void queueForProcessing(QueueClient queue, BlobContainerClient blobContainerClient, Logger logger) throws JsonProcessingException {
        // Create a BlobClient
        String blobFilename = createInternalFilename(this.filename, this.schemaName);
        BlobClient blobClient = blobContainerClient.getBlobClient(blobFilename);
        byte[] bytes = csvContent.getBytes(Charset.forName("UTF-8"));
        InputStream inputStream = new ByteArrayInputStream(bytes);
        logger.info("uploading " + bytes.length + " byte file to blob storage...");
        blobClient.upload(inputStream, bytes.length, true);
        logger.info("upload done: " +  blobFilename);
        this.blobURL = blobClient.getBlobUrl();

        String metadataJson = this.toJson();
        logger.info("sending this message: " + metadataJson);
        queue.sendMessage(metadataJson);
        logger.info("message sent.");
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @JsonIgnore
    public String getCsvContent() {
        return csvContent;
    }

    public void setCsvContent(String csvContent) {
        this.csvContent = csvContent;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getBlobURL() {
        return blobURL;
    }

    public void setBlobURL(String blobURL) {
        this.blobURL = blobURL;
    }

    // @todo needs error checking.
    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }


}
