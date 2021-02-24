package gov.cdc.prime.router.azure

import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import gov.cdc.prime.router.transport.RedoxTransport
import gov.cdc.prime.router.transport.RetryToken
import gov.cdc.prime.router.transport.SftpTransport
import org.jooq.Configuration
import java.io.ByteArrayInputStream

/**
 * Methods to add a new report to the workflow pipeline and to handle a step in the pipeline.
 * A new WorkflowEngine object should be created for every function call.
 *
 * @see gov.cdc.prime.router.Report
 * @see QueueAccess
 * @see DatabaseAccess.Header
 */
class WorkflowEngine(
    // Immutable objects can be shared between every function call
    val metadata: Metadata = WorkflowEngine.metadata,
    val hl7Serializer: Hl7Serializer = WorkflowEngine.hl7Serializer,
    val csvSerializer: CsvSerializer = WorkflowEngine.csvSerializer,
    val redoxSerializer: RedoxSerializer = WorkflowEngine.redoxSerializer,
    val translator: Translator = Translator(metadata),
    // New connection for every function
    val db: DatabaseAccess = DatabaseAccess(dataSource = DatabaseAccess.dataSource),
    val blob: BlobAccess = BlobAccess(csvSerializer, hl7Serializer, redoxSerializer),
    val queue: QueueAccess = QueueAccess(),
    val sftpTransport: SftpTransport = SftpTransport(),
    val redoxTransport: RedoxTransport = RedoxTransport(),
) {
    /**
     * Check the connections to Azure Storage and DB
     */
    fun checkConnections() {
        db.checkConnection()
        blob.checkConnection()
    }

    /**
     * Place a report into the workflow
     */
    fun receiveReport(
        validatedRequest: ReportFunction.ValidatedRequest,
        actionHistory: ActionHistory,
        txn: Configuration? = null
    ) {
        if (validatedRequest.report == null) error("Cannot receive a null report")
        val blobInfo = blob.uploadBody(validatedRequest.report)
        try {
            val receiveEvent = ReportEvent(Event.EventAction.RECEIVE, validatedRequest.report.id, null)
            db.insertHeader(validatedRequest.report, blobInfo.format.toString(), blobInfo.blobUrl, receiveEvent, txn)
            // todo bodyURL is no longer needed in report; its in 'blobInfo'
            validatedRequest.report.bodyURL = blobInfo.blobUrl
            actionHistory.trackExternalInputReport(validatedRequest, blobInfo)
        } catch (e: Exception) {
            // Clean up
            blob.deleteBlob(blobInfo.blobUrl)
            throw e
        }
    }

    /**
     * Place a report into the workflow (Note:  I moved queueing the message to after the Action is saved)
     */
    fun dispatchReport(
        nextAction: Event,
        report: Report,
        actionHistory: ActionHistory,
        receiver: OrganizationService,
        txn: Configuration? = null
    ) {
        val blobInfo = blob.uploadBody(report)
        try {
            db.insertHeader(report, blobInfo.format.toString(), blobInfo.blobUrl, nextAction, txn)
            // todo remove this; its now tracked in BlobInfo
            report.bodyURL = blobInfo.blobUrl
            actionHistory.trackCreatedReport(nextAction, report, receiver, blobInfo)
        } catch (e: Exception) {
            // Clean up
            blob.deleteBlob(blobInfo.blobUrl)
            throw e
        }
    }

    /**
     * Handle a single report event. Callback returns the next action for the report.
     *
     * @param messageEvent received from the message queue
     * @param updateBlock is wrapped in a DB transaction and called
     */
    fun handleReportEvent(
        messageEvent: ReportEvent,
        actionHistory: ActionHistory,
        updateBlock: (header: DatabaseAccess.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent,
    ) {
        lateinit var nextEvent: ReportEvent
        db.transact { txn ->
            val header = db.fetchAndLockHeader(messageEvent.reportId, txn)
            val currentEventAction = Event.EventAction.parseQueueMessage(header.task.nextAction.literal)
            // Ignore messages that are not consistent with the current header
            if (currentEventAction != messageEvent.eventAction) return@transact
            val retryToken = RetryToken.fromJSON(header.task.retryToken?.data())
            nextEvent = updateBlock(header, retryToken, txn)
            val retryJson = nextEvent.retryToken?.toJSON()
            db.updateHeader(
                header.task.reportId,
                currentEventAction,
                nextEvent.eventAction,
                nextEvent.at,
                retryJson,
                txn
            )
            recordAction(actionHistory, txn)
        }
        queue.sendMessage(nextEvent) // Avoid race condition by doing after txn completes.
    }

/**
     * Handle a receiver specific event. Fetch all pending tasks for the specified receiver and nextAction
     *
     * @param messageEvent that was received
     * @param maxCount of headers to process
     * @param updateBlock called with headers to process
     */
    fun handleReceiverEvent(
        messageEvent: ReceiverEvent,
        maxCount: Int,
        updateBlock: (headers: List<DatabaseAccess.Header>, txn: Configuration?) -> Unit,
    ) {
        val receiver = metadata.findService(messageEvent.receiverName)
            ?: error("Unable to find a receiving service called ${messageEvent.receiverName}")

        db.transact { txn ->
            val headers = db.fetchAndLockHeaders(
                messageEvent.eventAction.toTaskAction(),
                messageEvent.at,
                receiver,
                maxCount,
                txn
            )
            updateBlock(headers, txn)
            headers.forEach {
                val currentAction = Event.EventAction.parseQueueMessage(it.task.nextAction.literal)
                db.updateHeader(
                    it.task.reportId,
                    currentAction,
                    Event.EventAction.NONE,
                    nextActionAt = null,
                    retryToken = null,
                    txn
                )
            }
        }
    }

    /**
     * Create a report object from a header including loading the blob data associated with it
     */
    fun createReport(header: DatabaseAccess.Header): Report {
        // todo All of this info is already populated in the Header obj.
        val schema = metadata.findSchema(header.task.schemaName)
            ?: error("Invalid schema in queue: ${header.task.schemaName}")
        val destination = metadata.findService(header.task.receiverName)
        val bytes = blob.downloadBlob(header.task.bodyUrl)
        val sources = header.sources.map { DatabaseAccess.toSource(it) }
        return when (header.task.bodyFormat) {
            // TODO after the CSV internal format is flushed from the system, this code will be safe to remove
            "CSV" -> {
                val result = csvSerializer.readExternal(schema.name, ByteArrayInputStream(bytes), sources, destination)
                if (result.report == null || result.errors.isNotEmpty()) {
                    error("Internal Error: Could not read a saved CSV blob: ${header.task.bodyUrl}")
                }
                result.report
            }
            "INTERNAL" -> {
                csvSerializer.readInternal(
                    schema.name,
                    ByteArrayInputStream(bytes),
                    sources,
                    destination,
                    header.reportFile.reportId
                )
            }
            else -> error("Unsupported read format")
        }
    }

/**
     * Create a report object from a header including loading the blob data associated with it
     */
    fun readBody(header: DatabaseAccess.Header): ByteArray {
        return blob.downloadBlob(header.task.bodyUrl)
    }

    fun recordAction(actionHistory: ActionHistory, txn: Configuration? = null) {
        if (txn != null) {
            actionHistory.saveToDb(txn)
        } else {
            db.transact { innerTxn -> actionHistory.saveToDb(innerTxn) }
        }
    }

    companion object {
        /**
         * These are all potentially heavy weight objects that
         * should only be created once.
         */
        val metadata: Metadata by lazy {
            val baseDir = System.getenv("AzureWebJobsScriptRoot")
            val primeEnv = System.getenv("PRIME_ENVIRONMENT")
            val ext = primeEnv?.let { "-$it" } ?: ""
            Metadata("$baseDir/metadata", orgExt = ext)
        }

        val csvSerializer: CsvSerializer by lazy {
            CsvSerializer(metadata)
        }

        val hl7Serializer: Hl7Serializer by lazy {
            Hl7Serializer(metadata)
        }

        val redoxSerializer: RedoxSerializer by lazy {
            RedoxSerializer(metadata)
        }
    }
}