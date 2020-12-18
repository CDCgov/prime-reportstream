package gov.cdc.prime.router.azure

import gov.cdc.prime.router.CsvConverter
import gov.cdc.prime.router.Hl7Converter
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Translator
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
    val hl7Converter: Hl7Converter = WorkflowEngine.hl7Converter,
    val csvConverter: CsvConverter = WorkflowEngine.csvConverter,
    val translator: Translator = Translator(metadata),
    // New connection for every function
    val db: DatabaseAccess = DatabaseAccess(connection = DatabaseAccess.getConnection()),
    val blob: BlobAccess = BlobAccess(csvConverter, hl7Converter),
    val queue: QueueAccess = QueueAccess(),
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
    fun dispatchReport(nextAction: Event, report: Report, txn: Configuration? = null) {
        val (bodyFormat, bodyUrl) = blob.uploadBody(report)
        try {
            db.insertHeader(report, bodyFormat, bodyUrl, nextAction, txn)
            queue.sendMessage(nextAction)
        } catch (e: Exception) {
            // Clean up
            blob.deleteBlob(bodyUrl)
            throw e
        }
    }

    /**
     * Handle a single report event. Callback returns the next action for the report.
     */
    fun handleReportEvent(
        event: ReportEvent,
        updateBlock: (header: DatabaseAccess.Header, txn: Configuration) -> ReportEvent,
    ) {
        db.transact { txn ->
            val header = db.fetchAndLockHeader(event.reportId, txn)
            val currentAction = Event.Action.parse(header.task.nextAction.literal)
            val nextAction = updateBlock(header, txn)
            db.updateHeader(header.task.reportId, currentAction, nextAction.action, nextAction.at, txn)
            queue.sendMessage(nextAction)
        }
    }

    /**
     * Handle a receiver specific event. Fetch all pending tasks for the specified receiver.
     * The next action for the tasks are assumed to be NONE.
     */
    fun handleReceiverEvent(
        event: ReceiverEvent,
        maxCount: Int,
        updateBlock: (headers: List<DatabaseAccess.Header>, txn: Configuration) -> Unit,
    ) {
        db.transact { txn ->
            val headers = db.fetchAndLockHeaders(
                event.action.toTaskAction(),
                event.at,
                event.receiverName,
                maxCount,
                txn
            )
            updateBlock(headers, txn)
            headers.forEach {
                val currentAction = Event.Action.parse(it.task.nextAction.literal)
                db.updateHeader(
                    it.task.reportId,
                    currentAction,
                    Event.Action.NONE,
                    nextActionAt = null,
                    txn
                )
            }
        }
    }

    /**
     * Create a report object from a header including loading the blob data associated with it
     */
    fun createReport(header: DatabaseAccess.Header): Report {
        val schema = metadata.findSchema(header.task.schemaName)
            ?: error("Invalid schema in queue: ${header.task.schemaName}")
        val destination = metadata.findService(header.task.receiverName)
        val bytes = blob.downloadBlob(header.task.bodyUrl)
        val sources = header.sources.map { DatabaseAccess.toSource(it) }
        return when (header.task.bodyFormat) {
            "CSV" -> {
                val result = csvConverter.read(schema.name, ByteArrayInputStream(bytes), sources, destination)
                if (result.errors.isNotEmpty()) {
                    error("Internal Error: Could not read a saved CSV blob: ${header.task.bodyUrl}")
                }
                result.report
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

        val csvConverter: CsvConverter by lazy {
            CsvConverter(metadata)
        }

        val hl7Converter: Hl7Converter by lazy {
            Hl7Converter(metadata)
        }
    }
}