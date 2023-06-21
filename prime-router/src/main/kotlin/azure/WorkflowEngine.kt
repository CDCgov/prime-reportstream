package gov.cdc.prime.router.azure

import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.LegacyPipelineSender
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.ReadResult
import gov.cdc.prime.router.transport.AS2Transport
import gov.cdc.prime.router.transport.BlobStoreTransport
import gov.cdc.prime.router.transport.GAENTransport
import gov.cdc.prime.router.transport.RESTTransport
import gov.cdc.prime.router.transport.RetryItems
import gov.cdc.prime.router.transport.RetryToken
import gov.cdc.prime.router.transport.SftpTransport
import gov.cdc.prime.router.transport.SoapTransport
import org.jooq.Configuration
import org.jooq.Field
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime

/**
 * A top-level object that contains all the helpers and accessors to power the workflow.
 * Workflow objects are heavy-weight and should only be created once per function lifetime.
 *
 * @see gov.cdc.prime.router.Report
 * @see QueueAccess
 * @see DatabaseAccess.Header
 */
class WorkflowEngine(
    val metadata: Metadata = Metadata.getInstance(),
    val settings: SettingsProvider = settingsProviderSingleton,
    val hl7Serializer: Hl7Serializer = hl7SerializerSingleton,
    val csvSerializer: CsvSerializer = csvSerializerSingleton,
    val db: DatabaseAccess = databaseAccessSingleton,
    val blob: BlobAccess = BlobAccess(),
    queue: QueueAccess = QueueAccess,
    val translator: Translator = Translator(metadata, settings),
    val sftpTransport: SftpTransport = SftpTransport(),
    val as2Transport: AS2Transport = AS2Transport(),
    val soapTransport: SoapTransport = SoapTransport(),
    val gaenTransport: GAENTransport = GAENTransport(),
    val restTransport: RESTTransport = RESTTransport()
) : BaseEngine(queue) {

    /**
     * Custom builder for Workflow engine
     */
    data class Builder(
        var metadata: Metadata? = null,
        var settingsProvider: SettingsProvider? = null,
        var databaseAccess: DatabaseAccess? = null,
        var blobAccess: BlobAccess? = null,
        var queueAccess: QueueAccess? = null,
        var hl7Serializer: Hl7Serializer? = null,
        var csvSerializer: CsvSerializer? = null
    ) {
        /**
         * Set the metadata instance.
         * @return the modified workflow engine
         */
        fun metadata(metadata: Metadata) = apply { this.metadata = metadata }

        /**
         * Set the settings provider instance.
         * @return the modified workflow engine
         */
        fun settingsProvider(settingsProvider: SettingsProvider) = apply { this.settingsProvider = settingsProvider }

        /**
         * Set the database access instance.
         * @return the modified workflow engine
         */
        fun databaseAccess(databaseAccess: DatabaseAccess) = apply { this.databaseAccess = databaseAccess }

        /**
         * Set the blob access instance.
         * @return the modified workflow engine
         */
        fun blobAccess(blobAccess: BlobAccess) = apply { this.blobAccess = blobAccess }

        /**
         * Set the queue access instance.
         * @return the modified workflow engine
         */
        fun queueAccess(queueAccess: QueueAccess) = apply { this.queueAccess = queueAccess }

        /**
         * Set the HL7 serializer instance.
         * @return the modified workflow engine
         */
        fun hl7Serializer(hl7Serializer: Hl7Serializer) = apply { this.hl7Serializer = hl7Serializer }

        /**
         * Set the CSV serializer instance.
         * @return the modified workflow engine
         */
        fun csvSerializer(csvSerializer: CsvSerializer) = apply { this.csvSerializer = csvSerializer }

        /**
         * Build the workflow engine instance.
         * @return the workflow engine instance
         */
        fun build(): WorkflowEngine {
            if (metadata != null) {
                settingsProvider = settingsProvider ?: getSettingsProvider(metadata!!)
                hl7Serializer = hl7Serializer ?: Hl7Serializer(metadata!!, settingsProvider!!)
                csvSerializer = csvSerializer ?: CsvSerializer(metadata!!)
            } else {
                settingsProvider = settingsProvider ?: settingsProviderSingleton
                hl7Serializer = hl7Serializer ?: hl7SerializerSingleton
                csvSerializer = csvSerializer ?: csvSerializerSingleton
            }

            return WorkflowEngine(
                metadata ?: Metadata.getInstance(),
                settingsProvider!!,
                hl7Serializer!!,
                csvSerializer!!,
                databaseAccess ?: databaseAccessSingleton,
                blobAccess ?: BlobAccess(),
                queueAccess ?: QueueAccess
            )
        }
    }

    val blobStoreTransport: BlobStoreTransport = BlobStoreTransport()

    /**
     * Check the connections to Azure Storage and DB
     */
    fun checkConnections() {
        db.checkConnection()
        BlobAccess.checkConnection()
    }

    /**
     * Returns true if the [itemHash] passed in is already present in the database
     */
    fun isDuplicateItem(itemHash: String): Boolean {
        return db.isDuplicateItem(itemHash)
    }

    /**
     * Record a received [report] from a [sender] into the action history and save the original [rawBody]
     * of the received message. Return the blobUrl string to the calling function to save as part of the report
     */
    fun recordReceivedReport(
        report: Report,
        rawBody: ByteArray,
        sender: Sender,
        actionHistory: ActionHistory,
        payloadName: String? = null
    ): BlobAccess.BlobInfo {
        // Save a copy of the original report
        val reportFormat =
            if (sender.topic.isUniversalPipeline) report.bodyFormat
            else Report.Format.safeValueOf(sender.format.toString())

        val blobFilename = report.name.replace(report.bodyFormat.ext, reportFormat.ext)
        val blobInfo = BlobAccess.uploadBody(
            reportFormat,
            rawBody,
            blobFilename,
            sender.fullName,
            Event.EventAction.RECEIVE
        )

        actionHistory.trackExternalInputReport(report, blobInfo, payloadName)
        return blobInfo
    }

    fun insertProcessTask(
        report: Report,
        reportFormat: String,
        reportUrl: String,
        nextAction: Event
    ) {
        db.insertTask(report, reportFormat, reportUrl, nextAction, null)
    }

    /**
     * Place a report into the workflow (Note:  I moved queueing the message to after the Action is saved)
     */
    fun dispatchReport(
        nextAction: Event,
        report: Report,
        actionHistory: ActionHistory,
        receiver: Receiver,
        txn: Configuration? = null,
        isEmptyReport: Boolean = false
    ) {
        val receiverName = "${receiver.organizationName}.${receiver.name}"

        // these values come from the first item in a batched report, except for empty batches which have no items\
        //  in the generateBodyAndUploadReport function these values are only used if it is an HL7 batch
        // todo when generateBodyAndUploadReport is refactored, this can be changed
        val sendingApp: String? = if (isEmptyReport) "CDC PRIME - Atlanta" else null
        val receivingApp: String? = if (isEmptyReport && receiver.translation is Hl7Configuration) {
            receiver.translation.receivingApplicationName
        } else null
        val receivingFacility: String? = if (isEmptyReport && receiver.translation is Hl7Configuration) {
            receiver.translation.receivingFacilityName
        } else null

        val blobInfo = try {
            val bodyBytes = ReportWriter.getBodyBytes(
                report,
                sendingApp,
                receivingApp,
                receivingFacility
            )
            blob.uploadReport(report, bodyBytes, receiverName, nextAction.eventAction)
        } catch (ex: Exception) {
            logger.error(
                "Got exception while dispatching to schema ${report.schema.name}" +
                    ", and rcvr ${receiver.fullName}"
            )
            throw ex
        }
        logger.info("Saved dispatched report for receiver $receiverName to blob ${blobInfo.blobUrl}")
        try {
            db.insertTask(report, blobInfo.format.toString(), blobInfo.blobUrl, nextAction, txn)
            // todo remove this; its now tracked in BlobInfo
            report.bodyURL = blobInfo.blobUrl

            // if this is a newly generated empty report, track it as a 'new report'
            if (isEmptyReport) {
                actionHistory.trackGeneratedEmptyReport(nextAction, report, receiver, blobInfo)
            } else {
                actionHistory.trackCreatedReport(nextAction, report, receiver, blobInfo)
            }
        } catch (e: Exception) {
            // Clean up
            BlobAccess.deleteBlob(blobInfo.blobUrl)
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
        updateBlock: (header: Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
    ) {
        var nextEvent: ReportEvent? = null
        db.transact { txn ->
            val reportId = messageEvent.reportId
            val task = db.fetchAndLockTask(reportId, txn)
            val (organization, receiver) = findOrganizationAndReceiver(task.receiverName, txn)
            val reportFile = db.fetchReportFile(reportId, org = null, txn = txn)
            // todo remove this once things are permanently sane ;)
            ActionHistory.sanityCheckReport(task, reportFile, false)
            val itemLineages = db.fetchItemLineagesForReport(reportId, reportFile.itemCount, txn)
            val header = createHeader(task, reportFile, itemLineages, organization, receiver)
            val currentEventAction = Event.EventAction.parseQueueMessage(header.task.nextAction.literal)
            // Ignore messages that are not consistent with the current header
            if (currentEventAction != messageEvent.eventAction) {
                logger.warn(
                    "Weirdness for $reportId: queue event = ${messageEvent.eventAction.name}, " +
                        " but task.nextAction = ${currentEventAction.name} "
                )
                return@transact
            }
            val retryToken = RetryToken.fromJSON(header.task.retryToken?.data())

            nextEvent = updateBlock(header, retryToken, txn)
            logger.debug("Finished updateBlock for $reportId")
            val retryJson = nextEvent!!.retryToken?.toJSON()
            updateHeader(
                header.task.reportId,
                currentEventAction,
                nextEvent!!.eventAction,
                nextEvent!!.at,
                retryJson,
                txn
            )
        }
        if (nextEvent != null) queue.sendMessage(nextEvent!!) // Avoid race condition by doing after txn completes.
    }

    /**
     * Atomic read and set: sanity checks on the state of the report to be resent plus setting the state.
     */
    fun resendEvent(
        reportId: ReportId,
        receiver: Receiver,
        isTest: Boolean,
        msgs: MutableList<String>
    ) {
        // Send immediately.
        var doSendQueue = false // set to true if all the required actions complete
        val nextEvent = ReportEvent(Event.EventAction.SEND, reportId, at = null, isEmptyBatch = false)
        db.transact { txn ->
            db.fetchAndLockTask(reportId, txn) // Required, it creates lock.
            val organization = settings.findOrganization(receiver.organizationName)
                ?: throw Exception("No such organization ${receiver.organizationName}")
            val header = fetchHeader(reportId, organization) // exception if not found
            val reportFile = header.reportFile
            if (reportFile.nextAction != TaskAction.send) {
                throw Exception("Cannot send $reportId. Its next action is ${reportFile.nextAction}")
            }
            if (reportFile.bodyUrl.isEmpty()) {
                throw Exception("Cannot send $reportId.  Its not in the blob store.")
            }
            // The report might be old and the receiver has disappeared for some reason.
            if (receiver.fullName != "${reportFile.receivingOrg}.${reportFile.receivingOrgSvc}") {
                throw Exception("Cannot send $reportId. It is not associated with receiver ${receiver.fullName}")
            }
            if (header.task.nextActionAt != null && header.task.nextActionAt.isAfter(OffsetDateTime.now())) {
                msgs.add(
                    "Cannot send $reportId. Its already scheduled to ${header.task.nextAction}" +
                        " at ${header.task.nextActionAt}"
                )
                return@transact
            }

            val retryItems: RetryItems = RetryToken.allItems
            if (retryItems.isEmpty()) {
                msgs.add("All Items in $reportId successfully sent.  Nothing to resend. DONE")
                return@transact
            } else {
                if (retryItems == RetryToken.allItems) {
                    msgs.add("Will resend all (${reportFile.itemCount}) items in $reportId")
                } else {
                    msgs.add(
                        "Will resend these failed/not-sent items in $reportId: ${retryItems.joinToString(",")}"
                    )
                }
                if (!isTest) {
                    updateHeader(
                        reportId,
                        Event.EventAction.RESEND,
                        nextEvent.eventAction,
                        nextEvent.at,
                        RetryToken(0, retryItems).toJSON(), // retryCount=0 will start at [1]
                        txn
                    )
                    msgs.add("$reportId has been queued to resend immediately to ${receiver.fullName}\n")
                    doSendQueue = true
                } else {
                    msgs.add("Nothing sent.  This was just a test")
                }
            }
        }
        if (!isTest && doSendQueue) queue.sendMessage(nextEvent) // Avoid race condition by doing after txn completes.
    }

    /**
     * Creates an empty report to send to a [receiver] that is configured to receive empty batches.
     */
    fun generateEmptyReport(
        actionHistory: ActionHistory,
        receiver: Receiver
    ) {
        // generate empty report for receiver's specified foramt
        val toSchema = metadata.findSchema(receiver.schemaName)
            ?: error("${receiver.schemaName} schema is missing from catalog")
        val clientSource = ClientSource("ReportStream", "EmptyBatch")

        val emptyReport = Report(
            toSchema,
            emptyList(),
            listOf(clientSource),
            destination = receiver,
            bodyFormat = receiver.format,
            metadata = Metadata.getInstance(),
            itemLineage = emptyList()
        )

        // set the empty report to be sent to the receiver
        this.db.transact { txn ->
            val sendEvent = ReportEvent(Event.EventAction.SEND, emptyReport.id, true)
            this.dispatchReport(sendEvent, emptyReport, actionHistory, receiver, txn, true)
        }
    }

    // routeReport does all filtering and translating per receiver, generating one file per receiver to then be batched
    fun routeReport(
        report: Report,
        options: Options,
        defaults: Map<String, String>,
        routeTo: List<String>,
        actionHistory: ActionHistory
    ): List<ActionLog> {
        val (warnings, emptyReports, preparedReports) = translateAndRouteReport(report, defaults, routeTo)

        emptyReports.forEach { (filteredReport, receiver) ->
            if (!filteredReport.filteringResults.isEmpty()) {
                actionHistory.trackFilteredReport(report, filteredReport, receiver)
            }
        }

        this.db.transact { txn ->
            preparedReports.forEach { (report, receiver) ->
                sendToDestination(
                    report,
                    receiver,
                    options,
                    actionHistory,
                    txn
                )
            }
        }
        return warnings
    }

    /**
     * Do translation and routing as one atomic unit
     *
     * @param report The report file
     * @param defaults Optional map of translation defaults
     * @param routeTo Optional list of receivers to limit translation to
     * @return The resulting set of warnings (ActionLogs), empty and prepared reports (RoutedReports)
     */
    internal fun translateAndRouteReport(
        report: Report,
        defaults: Map<String, String>,
        routeTo: List<String>
    ): Triple<List<ActionLog>, List<Translator.RoutedReport>, List<Translator.RoutedReport>> {
        val (routedReports, warnings) = this.translator
            .filterAndTranslateByReceiver(
                report,
                defaults,
                routeTo
            )

        val (emptyReports, preparedReports) = routedReports.partition { (report, _) -> report.isEmpty() }
        return Triple(warnings, emptyReports, preparedReports)
    }

    // 1. create <event, report> pair or pairs depending on input
    // 2. dispatchReport
    // 3. log
    private fun sendToDestination(
        report: Report,
        receiver: Receiver,
        options: Options,
        actionHistory: ActionHistory,
        txn: DataAccessTransaction
    ) {
        val loggerMsg: String
        when {
            options == Options.SkipSend -> {
                // Note that SkipSend should really be called SkipBothTimingAndSend  ;)
                val event = ReportEvent(Event.EventAction.NONE, report.id, actionHistory.generatingEmptyReport)
                this.dispatchReport(event, report, actionHistory, receiver, txn)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
            receiver.timing != null && options != Options.SendImmediately -> {
                val time = receiver.timing.nextTime()
                // Always force a batched report to be saved in our INTERNAL format
                val batchReport = report.copy(bodyFormat = Report.Format.INTERNAL)
                val event = BatchEvent(Event.EventAction.BATCH, receiver.fullName, false, time)
                this.dispatchReport(event, batchReport, actionHistory, receiver, txn)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
            receiver.format.isSingleItemFormat -> {
                report.filteringResults.forEach {
                    val emptyReport = Report(
                        report.schema,
                        emptyList(),
                        emptyList(),
                        destination = report.destination,
                        bodyFormat = report.bodyFormat,
                        metadata = Metadata.getInstance()
                    )
                    emptyReport.filteringResults.add(it)
                    actionHistory.trackFilteredReport(report, emptyReport, receiver)
                }

                report
                    .split()
                    .forEach {
                        val event = ReportEvent(Event.EventAction.SEND, it.id, actionHistory.generatingEmptyReport)
                        this.dispatchReport(event, it, actionHistory, receiver, txn)
                    }
                loggerMsg = "Queued to send immediately: HL7 split into ${report.itemCount} individual reports"
            }
            else -> {
                val event = ReportEvent(Event.EventAction.SEND, report.id, actionHistory.generatingEmptyReport)
                this.dispatchReport(event, report, actionHistory, receiver, txn)
                loggerMsg = "Queued to send immediately: ${event.toQueueMessage()}"
            }
        }
        logger.info(loggerMsg)
    }

    /**
     * The process step has failed. Ensure the actionHistory gets a 'warning' if it is not yet the 5th attempt
     * at this record. If it is the 5th attempt, set it to process_error
     * Also, save all the action history to the database.
     */
    fun handleProcessFailure(
        numAttempts: Int,
        actionHistory: ActionHistory,
        message: String
    ) {
        // if there are already four process_warning records in the database for this reportId, this is the last try
        if (numAttempts >= 5) {
            actionHistory.setActionType(TaskAction.process_error)
            actionHistory.trackActionResult(
                "Failed to process $message $numAttempts times, setting status to process_error." +
                    " Mo intervention required."
            )
            logger.fatal(actionHistory.action.actionResult)
        } else {
            actionHistory.setActionType(TaskAction.process_warning)
            actionHistory.trackActionResult(
                "Failed to process message $message $numAttempts times," +
                    " setting status to process_warning."
            )
        }
        // save action record to db
        db.transact { txn ->
            db.saveActionHistoryToDb(actionHistory, txn)
        }
    }

    /**
     * Handle a receiver specific event. Fetch all pending tasks for the specified receiver and nextAction
     * @param messageEvent that was received
     * @param actionHistory action history being passed through for this message process
     */
    fun handleProcessEvent(
        messageEvent: ProcessEvent,
        actionHistory: ActionHistory
    ) {
        db.transact { txn ->
            val task = db.fetchAndLockTask(messageEvent.reportId, txn)

            val currentAction = Event.EventAction.parseQueueMessage(task.nextAction.literal)
            if (currentAction != Event.EventAction.PROCESS) {
                // As of this writing we are not sure why this bug occurs.  However, this at least prevents it from
                // causing trouble.
                logger.error(
                    "ProcessQueueFailure: The 'process' queue wants to " +
                        "process report ${messageEvent.reportId}, " +
                        "but the TASK table shows its next_action is $currentAction. " +
                        "Its likely this report was processed earlier, but not removed from the 'process' queue."
                )
                // don't throw an exception, as that'll just make this erroneous process action run again.
                // Also, no reason to update the TASK table:  its already marked as done.
                return@transact
            }

            val blobContent = BlobAccess.downloadBlob(task.bodyUrl)

            val report = csvSerializer.readInternal(
                task.schemaName,
                ByteArrayInputStream(blobContent),
                emptyList(),
                blobReportId = messageEvent.reportId
            )

            actionHistory.trackExistingInputReport(report.id)

            //  send to routeReport
            val warnings = routeReport(
                report,
                messageEvent.options,
                messageEvent.defaults,
                messageEvent.routeTo,
                actionHistory
            )

            actionHistory.trackLogs(warnings)

            // record action history records
            recordAction(actionHistory)

            // queue messages here after all task / action records are in
            actionHistory.queueMessages(this)

            updateHeader(
                messageEvent.reportId,
                currentAction,
                Event.EventAction.NONE,
                nextActionAt = null,
                retryToken = null,
                txn
            )
        }
    }

    /**
     * Handle a batch event for a receiver. Fetch all pending tasks for the specified receiver and nextAction
     *
     * @param messageEvent that was received
     * @param maxCount of headers to process
     * @param updateBlock called with headers to process
     */
    fun handleBatchEvent(
        messageEvent: BatchEvent,
        maxCount: Int,
        backstopTime: OffsetDateTime?,
        updateBlock: (headers: List<Header>, txn: Configuration?) -> Unit
    ) {
        db.transact { txn ->
            val tasks = db.fetchAndLockBatchTasksForOneReceiver(
                messageEvent.at,
                messageEvent.receiverName,
                maxCount,
                backstopTime,
                txn
            )
            val ids = tasks.map { it.reportId }
            val reportFiles = ids
                .mapNotNull {
                    try {
                        db.fetchReportFile(it, org = null, txn = txn)
                    } catch (e: Exception) {
                        println(e.printStackTrace())
                        // Call to sanityCheckReports further below will log the problem in better detail.
                        // note that we are logging but ignoring this error, so that it doesn't poison the entire batch
                        null // id not found. Can occur if errors in ReportFunction fail to write to REPORT_FILE
                    }
                }
                .map { (it.reportId as ReportId) to it }
                .toMap()
            val (organization, receiver) = findOrganizationAndReceiver(messageEvent.receiverName, txn)
            // This check is needed as long as TASK does not FK to REPORT_FILE.  @todo FK TASK to REPORT_FILE
            ActionHistory.sanityCheckReports(tasks, reportFiles, false)
            val headers = tasks.mapNotNull {
                if (reportFiles[it.reportId] != null) {
                    createHeader(it, reportFiles[it.reportId]!!, null, organization, receiver)
                } else {
                    null
                }
            }

            updateBlock(headers, txn)
            // Here we iterate through the original tasks, rather than headers.
            // So even TASK entries whose report_id is missing from REPORT_FILE are marked as done,
            // because missing report_id is an unrecoverable error. @todo  See #2185 for better solution.
            tasks.forEach {
                val currentAction = Event.EventAction.parseQueueMessage(it.nextAction.literal)
                updateHeader(
                    it.reportId,
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
    fun createReport(header: Header): Report {
        val bytes = BlobAccess.downloadBlob(header.task.bodyUrl)
        return when (header.task.bodyFormat) {
            // TODO after the CSV internal format is flushed from the system, this code will be safe to remove
            "CSV", "CSV_SINGLE" -> {
                val result = csvSerializer.readExternal(
                    header.task.schemaName,
                    ByteArrayInputStream(bytes),
                    emptyList(),
                    header.receiver
                )
                if (result.actionLogs.hasErrors()) {
                    error("Internal Error: Could not read a saved CSV blob: ${header.task.bodyUrl}")
                }
                result.report
            }
            "INTERNAL" -> {
                csvSerializer.readInternal(
                    header.task.schemaName,
                    ByteArrayInputStream(bytes),
                    emptyList(),
                    header.receiver,
                    header.reportFile.reportId
                )
            }
            else -> error("Unsupported read format")
        }
    }

    /**
     * Create a report object from a header including loading the blob data associated with it
     */
    fun readBody(header: Header): ByteArray {
        return BlobAccess.downloadBlob(header.task.bodyUrl)
    }

    fun recordAction(actionHistory: ActionHistory, txn: Configuration? = null) {
        if (txn != null) {
            db.saveActionHistoryToDb(actionHistory, txn)
        } else {
            db.transact { innerTxn -> db.saveActionHistoryToDb(actionHistory, innerTxn) }
        }
    }

    private fun findOrganizationAndReceiver(
        fullName: String,
        txn: DataAccessTransaction? = null
    ): Pair<Organization, Receiver> {
        return if (settings is SettingsFacade) {
            val (organization, receiver) = (settings).findOrganizationAndReceiver(fullName, txn)
                ?: error("Receiver not found in database: $fullName")
            Pair(organization, receiver)
        } else {
            val (organization, receiver) = settings.findOrganizationAndReceiver(fullName)
                ?: error("Invalid receiver name: $fullName")
            Pair(organization, receiver)
        }
    }

    fun fetchDownloadableReportFiles(
        since: OffsetDateTime?,
        organizationName: String
    ): List<ReportFile> {
        return db.fetchDownloadableReportFiles(since, organizationName)
    }

    /**
     * The header class provides the information needed to process a task.
     */
    data class Header(
        val task: Task,
        val reportFile: ReportFile,
        val itemLineages: List<ItemLineage>?, // ok to not have item-level lineage
        val organization: Organization?,
        val receiver: Receiver?,
        val schema: Schema?,
        val content: ByteArray?,
        // todo: until this can be refactored, we need a way for the calling functions to know
        //  if this header is expecting to have content to detect errors on download (IE, file does not exist
        //  see #3505
        val expectingContent: Boolean
    )

    private fun createHeader(
        task: Task,
        reportFile: ReportFile,
        itemLineages: List<ItemLineage>?,
        organization: Organization?,
        receiver: Receiver?,
        fetchBlobBody: Boolean = true
    ): Header {
        val schema = if (reportFile.schemaName != null) {
            metadata.findSchema(reportFile.schemaName)
        } else null

        val downloadContent = (reportFile.bodyUrl != null && fetchBlobBody)
        val content = if (downloadContent && BlobAccess.exists(reportFile.bodyUrl)) {
            BlobAccess.downloadBlob(reportFile.bodyUrl)
        } else null
        return Header(task, reportFile, itemLineages, organization, receiver, schema, content, downloadContent)
    }

    fun fetchHeader(
        reportId: ReportId,
        organization: Organization,
        fetchBlobBody: Boolean = true
    ): Header {
        val reportFile = db.fetchReportFile(reportId, organization)
        val task = db.fetchTask(reportId)
        val (org2, receiver) = findOrganizationAndReceiver(
            reportFile.receivingOrg + "." + reportFile.receivingOrgSvc
        )
        if (org2.name != organization.name) error("${org2.name} != ${organization.name}: Org Name Mismatch check fail")
        // todo remove this sanity check
        ActionHistory.sanityCheckReport(task, reportFile, false)
        val itemLineages = db.fetchItemLineagesForReport(reportId, reportFile.itemCount)
        return createHeader(task, reportFile, itemLineages, organization, receiver, fetchBlobBody)
    }

    /**
     * Update the header of a report with new values
     */
    private fun updateHeader(
        reportId: ReportId,
        currentEventAction: Event.EventAction,
        nextEventAction: Event.EventAction,
        nextActionAt: OffsetDateTime? = null,
        retryToken: String? = null,
        txn: DataAccessTransaction
    ) {
        fun finishedField(currentEventAction: Event.EventAction): Field<OffsetDateTime> {
            return when (currentEventAction) {
                Event.EventAction.RECEIVE -> Tables.TASK.TRANSLATED_AT
                Event.EventAction.PROCESS -> Tables.TASK.PROCESSED_AT
                // we don't really use these  *_AT columns for anything at this point, and 'convert' is another name
                //  for 'process' ... but 'process' is just too vague
                Event.EventAction.CONVERT -> Tables.TASK.PROCESSED_AT
                Event.EventAction.ROUTE -> Tables.TASK.ROUTED_AT
                Event.EventAction.TRANSLATE -> Tables.TASK.TRANSLATED_AT
                Event.EventAction.REBATCH -> Tables.TASK.TRANSLATED_AT // overwrites prior date
                Event.EventAction.BATCH -> Tables.TASK.BATCHED_AT
                Event.EventAction.RESEND -> Tables.TASK.BATCHED_AT // overwrites prior date
                Event.EventAction.SEND -> Tables.TASK.SENT_AT
                Event.EventAction.WIPE -> Tables.TASK.WIPED_AT

                Event.EventAction.BATCH_ERROR,
                Event.EventAction.SEND_ERROR,
                Event.EventAction.PROCESS_ERROR,
                Event.EventAction.PROCESS_WARNING,
                Event.EventAction.WIPE_ERROR -> Tables.TASK.ERRORED_AT

                Event.EventAction.NONE -> error("Internal Error: NONE currentAction")
            }
        }
        db.updateTask(
            reportId,
            nextEventAction.toTaskAction(),
            nextActionAt,
            retryToken,
            finishedField(currentEventAction),
            txn
        )
    }

    // 1. detect format and get serializer
    // 2. readExternal and return result / errors / warnings
    // TODO: This could be moved to a utility/reports.kt or something like that, as it is not really part of workflow
    /**
     * Reads in a received covid-19 message of HL7 or CSV format, generates an in-memory report instance
     * @param sender Sender information, pulled from database based on sender name
     * @param content Content of incoming message
     * @param defaults Default values that can be passed in as part of the request
     * @return Returns a generated report object, or null
     */
    fun parseTopicReport(
        sender: LegacyPipelineSender,
        content: String,
        defaults: Map<String, String>
    ): ReadResult {
        return when (sender.format) {
            Sender.Format.CSV -> {
                try {
                    this.csvSerializer.readExternal(
                        schemaName = sender.schemaName,
                        input = ByteArrayInputStream(content.toByteArray()),
                        sources = listOf(ClientSource(organization = sender.organizationName, client = sender.name)),
                        defaultValues = defaults,
                        sender = sender
                    )
                } catch (e: Exception) {
                    throw ActionError(
                        ActionLog(
                            InvalidReportMessage(
                                "An unexpected error occurred requiring additional help. Contact the ReportStream " +
                                    "team at reportstream@cdc.gov."
                            )
                        ),
                        e.message
                    )
                }
            }
            Sender.Format.HL7, Sender.Format.HL7_BATCH -> {
                try {
                    this.hl7Serializer.readExternal(
                        schemaName = sender.schemaName,
                        input = ByteArrayInputStream(content.toByteArray()),
                        ClientSource(organization = sender.organizationName, client = sender.name),
                        sender = sender
                    )
                } catch (e: Exception) {
                    throw ActionError(
                        ActionLog(
                            InvalidReportMessage(
                                "An unexpected error occurred requiring additional help. Contact the ReportStream " +
                                    "team at reportstream@cdc.gov."
                            )
                        ),
                        e.message
                    )
                }
            }
            else -> throw IllegalStateException("Sender format ${sender.format} is not supported")
        }
    }
}