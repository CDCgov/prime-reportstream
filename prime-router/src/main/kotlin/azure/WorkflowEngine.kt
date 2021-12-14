package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import gov.cdc.prime.router.transport.AS2Transport
import gov.cdc.prime.router.transport.BlobStoreTransport
import gov.cdc.prime.router.transport.FTPSTransport
import gov.cdc.prime.router.transport.GAENTransport
import gov.cdc.prime.router.transport.RedoxTransport
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
    val redoxSerializer: RedoxSerializer = redoxSerializerSingleton,
    val db: DatabaseAccess = databaseAccessSingleton,
    val blob: BlobAccess = BlobAccess(csvSerializer, hl7Serializer, redoxSerializer),
    val queue: QueueAccess = QueueAccess,
    val translator: Translator = Translator(metadata, settings),
    val sftpTransport: SftpTransport = SftpTransport(),
    val redoxTransport: RedoxTransport = RedoxTransport(),
    val as2Transport: AS2Transport = AS2Transport(),
    val ftpsTransport: FTPSTransport = FTPSTransport(),
    val soapTransport: SoapTransport = SoapTransport(),
    val gaenTransport: GAENTransport = GAENTransport()
) {

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
        var csvSerializer: CsvSerializer? = null,
        var redoxSerializer: RedoxSerializer? = null
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
         * Set the Redox serializer instance.
         * @return the modified workflow engine
         */
        fun redoxSerializer(redoxSerializer: RedoxSerializer) = apply { this.redoxSerializer = redoxSerializer }

        /**
         * Build the workflow engine instance.
         * @return the workflow engine instance
         */
        fun build(): WorkflowEngine {
            if (metadata != null) {
                settingsProvider = settingsProvider ?: getSettingsProvider(metadata!!)
                hl7Serializer = hl7Serializer ?: Hl7Serializer(metadata!!, settingsProvider!!)
                csvSerializer = csvSerializer ?: CsvSerializer(metadata!!)
                redoxSerializer = redoxSerializer ?: RedoxSerializer(metadata!!)
            } else {
                settingsProvider = settingsProvider ?: settingsProviderSingleton
                hl7Serializer = hl7Serializer ?: hl7SerializerSingleton
                csvSerializer = csvSerializer ?: csvSerializerSingleton
                redoxSerializer = redoxSerializer ?: redoxSerializerSingleton
            }

            return WorkflowEngine(
                metadata ?: Metadata.getInstance(),
                settingsProvider!!,
                hl7Serializer!!,
                csvSerializer!!,
                redoxSerializer!!,
                databaseAccess ?: databaseAccessSingleton,
                blobAccess ?: BlobAccess(csvSerializer!!, hl7Serializer!!, redoxSerializer!!),
                queueAccess ?: QueueAccess
            )
        }
    }

    val blobStoreTransport: BlobStoreTransport = BlobStoreTransport(this)

    /**
     * Check the connections to Azure Storage and DB
     */
    fun checkConnections() {
        db.checkConnection()
        blob.checkConnection()
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
        workflowEngine: WorkflowEngine,
        payloadName: String? = null,
    ): String {
        // Save a copy of the original report
        val senderReportFormat = Report.Format.safeValueOf(sender.format.toString())
        val blobFilename = report.name.replace(report.bodyFormat.ext, senderReportFormat.ext)
        val blobInfo = workflowEngine.blob.uploadBody(
            senderReportFormat, rawBody,
            blobFilename, sender.fullName, Event.EventAction.RECEIVE
        )

        actionHistory.trackExternalInputReport(report, blobInfo, payloadName)
        return blobInfo.blobUrl
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
        context: ExecutionContext? = null
    ) {
        val receiverName = "${receiver.organizationName}.${receiver.name}"
        val blobInfo = try {
            // formatting errors can occur down in here.
            blob.uploadBody(report, receiverName, nextAction.eventAction)
        } catch (ex: Exception) {
            context?.logger?.warning(
                "Got exception while dispatching to schema ${report.schema.name}" +
                    ", and rcvr ${receiver.fullName}"
            )
            throw ex
        }
        context?.logger?.fine(
            "Saved dispatched report for receiver $receiverName" +
                " to blob ${blobInfo.blobUrl}"
        )
        try {
            db.insertTask(report, blobInfo.format.toString(), blobInfo.blobUrl, nextAction, txn)
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
        context: ExecutionContext? = null,
        updateBlock: (header: Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent,
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
                context?.let {
                    context.logger.warning(
                        "Weirdness for $reportId: queue event = ${messageEvent.eventAction.name}, " +
                            " but task.nextAction = ${currentEventAction.name} "
                    )
                }
                return@transact
            }
            val retryToken = RetryToken.fromJSON(header.task.retryToken?.data())

            nextEvent = updateBlock(header, retryToken, txn)
            context?.let { context.logger.info("Finished updateBlock for $reportId") }
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
        sendFailedOnly: Boolean,
        isTest: Boolean,
        msgs: MutableList<String>,
    ) {
        // Send immediately.
        val nextEvent = ReportEvent(Event.EventAction.SEND, reportId, at = null)
        db.transact { txn ->
            val task = db.fetchAndLockTask(reportId, txn) // Required, it creates lock.
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
                throw Exception(
                    "Cannot send $reportId. Its already scheduled to ${header.task.nextAction}" +
                        " at ${header.task.nextActionAt}"
                )
            }
            val retryItems: RetryItems = if (sendFailedOnly) {
                val itemDispositions = fetchItemDispositions(reportFile)
                // Yet another delightful feature of kotlin: groupingBy
                val counts = itemDispositions.values.groupingBy { it }.eachCount()
                msgs.add(
                    "Of ${reportFile.itemCount} items, " +
                        "${counts[RedoxTransport.ResultStatus.SUCCESS] ?: 0 } were sent successfully, " +
                        "${counts[RedoxTransport.ResultStatus.FAILURE] ?: 0} were attempted but failed, " +
                        "${counts[RedoxTransport.ResultStatus.NEVER_ATTEMPTED] ?: 0} were never attempted."
                )
                itemDispositions.filter { (_, disp) ->
                    disp != RedoxTransport.ResultStatus.SUCCESS
                }.map { (key, _) -> key.toString() }
            } else {
                RetryToken.allItems
            }
            if (retryItems.isEmpty()) {
                msgs.add("All Items in $reportId successfully sent.  Nothing to resend. DONE")
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
                        RetryToken(1, retryItems).toJSON(),
                        txn
                    )
                    msgs.add("$reportId has been queued to resend immediately to ${receiver.fullName}\n")
                } else {
                    msgs.add("Nothing sent.  This was just a test")
                }
            }
        }
        if (!isTest) queue.sendMessage(nextEvent) // Avoid race condition by doing after txn completes.
    }

    /**
     * Given a batched report, look at its children 'sent' reports, and gather the disposition
     * of every item in the batched report.   If there are no children,
     * this will return a map with all item statuses == NEVER_ATTEMPTED
     */
    fun fetchItemDispositions(reportFile: ReportFile): Map<Int, RedoxTransport.ResultStatus> {
        if (reportFile.nextAction != TaskAction.send) {
            throw Exception("Cannot send ${reportFile.reportId}. Its next action is ${reportFile.nextAction}")
        }
        if (reportFile.bodyFormat != Report.Format.REDOX.name) {
            throw Exception("SendFailed option only applies to REDOX reports.  This report is ${reportFile.bodyFormat}")
        }
        // Track what happened to each item.
        val itemsDispositionMap = mutableMapOf<Int, RedoxTransport.ResultStatus>().apply {
            // Initialize all to assume nothing was every done
            for (i in 0 until reportFile.itemCount) this[i] = RedoxTransport.ResultStatus.NEVER_ATTEMPTED
        }
        val childReportIds = db.fetchChildReports(reportFile.reportId)
        childReportIds.forEach childIdFor@{ childId ->
            val lineages = db.fetchItemLineagesForReport(childId, reportFile.itemCount)
            lineages?.forEach lineageFor@{ lineage ->
                // Once a success, always a success
                if (itemsDispositionMap[lineage.parentIndex] == RedoxTransport.ResultStatus.SUCCESS)
                    return@lineageFor
                itemsDispositionMap[lineage.parentIndex] = when {
                    lineage.transportResult.startsWith(RedoxTransport.ResultStatus.FAILURE.name) ->
                        RedoxTransport.ResultStatus.FAILURE
                    lineage.transportResult.startsWith(RedoxTransport.ResultStatus.NOT_SENT.name) ->
                        RedoxTransport.ResultStatus.FAILURE
                    else -> RedoxTransport.ResultStatus.SUCCESS
                }
            }
        }
        return itemsDispositionMap
    }

    // routeReport does all filtering and translating per receiver, generating one file per receiver to then be batched
    fun routeReport(
        context: ExecutionContext,
        report: Report,
        options: Options,
        defaults: Map<String, String>,
        routeTo: List<String>,
        warnings: MutableList<ResultDetail>,
        actionHistory: ActionHistory,
    ) {
        this.db.transact { txn ->
            val (emptyReports, preparedReports) = this
                .translator
                .filterAndTranslateByReceiver(
                    report,
                    defaults,
                    routeTo,
                    warnings,
                ).partition { (report, _) -> report.isEmpty() }

            emptyReports.forEach { (report, receiver) ->
                if (!report.filteringResults.isEmpty()) {
                    actionHistory.trackFilteredReport(report, receiver)
                }
            }

            preparedReports.forEach { (report, receiver) ->
                sendToDestination(
                    report,
                    receiver,
                    context,
                    options,
                    actionHistory,
                    txn
                )
            }
        }
    }

    // 1. create <event, report> pair or pairs depending on input
    // 2. dispatchReport
    // 3. log
    private fun sendToDestination(
        report: Report,
        receiver: Receiver,
        context: ExecutionContext,
        options: Options,
        actionHistory: ActionHistory,
        txn: DataAccessTransaction
    ) {
        val loggerMsg: String
        when {
            options == Options.SkipSend -> {
                // Note that SkipSend should really be called SkipBothTimingAndSend  ;)
                val event = ReportEvent(Event.EventAction.NONE, report.id)
                this.dispatchReport(event, report, actionHistory, receiver, txn, context)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
            receiver.timing != null && options != Options.SendImmediately -> {
                val time = receiver.timing.nextTime()
                // Always force a batched report to be saved in our INTERNAL format
                val batchReport = report.copy(bodyFormat = Report.Format.INTERNAL)
                val event = ReceiverEvent(Event.EventAction.BATCH, receiver.fullName, time)
                this.dispatchReport(event, batchReport, actionHistory, receiver, txn, context)
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
                    actionHistory.trackFilteredReport(emptyReport, receiver)
                }

                report
                    .split()
                    .forEach {
                        val event = ReportEvent(Event.EventAction.SEND, it.id)
                        this.dispatchReport(event, it, actionHistory, receiver, txn, context)
                    }
                loggerMsg = "Queued to send immediately: HL7 split into ${report.itemCount} individual reports"
            }
            else -> {
                val event = ReportEvent(Event.EventAction.SEND, report.id)
                this.dispatchReport(event, report, actionHistory, receiver, txn, context)
                loggerMsg = "Queued to send immediately: ${event.toQueueMessage()}"
            }
        }
        context.logger.info(loggerMsg)
    }

    /**
     * The process step has failed. Ensure the actionHistory gets a 'warning' if it is not yet the 5th attempt
     *  at this record. If it is the 5th attempt, set it to process_error
     */
    fun handleProcessFailure(
        messageEvent: ProcessEvent,
        queueMessage: String,
        actionHistory: ActionHistory
    ) {
        // Get count of process_warning actions for this reportId
        val numAttempts = db.getActionCountForReport(queueMessage)

        // if there are already four process_warning records in the database for this reportId, this is the last try
        val actionStatus = if (numAttempts == 4) TaskAction.process_error else TaskAction.process_warning
        // if count is < 4, add a process_warning status to the task
        // if count is 5, add a process_error status to the task
        actionHistory.setActionType(actionStatus)
        actionHistory.trackActionResult(
            "Failed to process $numAttempts times, setting status to $actionStatus."
        )

        // save action record to db
        db.transact { txn ->
            actionHistory.saveToDb(txn)
        }
    }

    /**
     * Handle a receiver specific event. Fetch all pending tasks for the specified receiver and nextAction
     * @param messageEvent that was received
     * @param context execution context
     * @param actionHistory action history being passed through for this message process
     */
    fun handleProcessEvent(
        messageEvent: ProcessEvent,
        context: ExecutionContext,
        actionHistory: ActionHistory
    ) {
        val errors: MutableList<ResultDetail> = mutableListOf()
        val warnings: MutableList<ResultDetail> = mutableListOf()

        db.transact { txn ->
            val task = db.fetchAndLockTask(messageEvent.reportId, txn)

            val blobContent = blob.downloadBlob(task.bodyUrl)
            val currentAction = Event.EventAction.parseQueueMessage(task.nextAction.literal)

            val report = csvSerializer.readInternal(
                task.schemaName,
                ByteArrayInputStream(blobContent),
                emptyList(),
                blobReportId = messageEvent.reportId
            )

            //  send to routeReport
            routeReport(
                context,
                report,
                messageEvent.options,
                messageEvent.defaults,
                messageEvent.routeTo,
                warnings,
                actionHistory
            )

            // track response body
            val responseBody = actionHistory.createResponseBody(
                messageEvent.options,
                warnings,
                errors,
                true,
                report
            )
            actionHistory.trackActionResponse(responseBody)

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
     * Handle a receiver specific event. Fetch all pending tasks for the specified receiver and nextAction
     *
     * @param messageEvent that was received
     * @param maxCount of headers to process
     * @param updateBlock called with headers to process
     */
    fun handleReceiverEvent(
        messageEvent: ReceiverEvent,
        maxCount: Int,
        updateBlock: (headers: List<Header>, txn: Configuration?) -> Unit,
    ) {
        db.transact { txn ->
            val tasks = db.fetchAndLockTasksForOneReceiver(
                messageEvent.eventAction.toTaskAction(),
                messageEvent.at,
                messageEvent.receiverName,
                maxCount,
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
        // todo All of this info is already populated in the Header obj.
        val schema = metadata.findSchema(header.task.schemaName)
            ?: error("Invalid schema in queue: ${header.task.schemaName}")
        val bytes = blob.downloadBlob(header.task.bodyUrl)
        return when (header.task.bodyFormat) {
            // TODO after the CSV internal format is flushed from the system, this code will be safe to remove
            "CSV", "CSV_SINGLE" -> {
                val result = csvSerializer.readExternal(
                    schema.name,
                    ByteArrayInputStream(bytes),
                    emptyList(),
                    header.receiver
                )
                if (result.report == null || result.errors.isNotEmpty()) {
                    error("Internal Error: Could not read a saved CSV blob: ${header.task.bodyUrl}")
                }
                result.report
            }
            "INTERNAL" -> {
                csvSerializer.readInternal(
                    schema.name,
                    ByteArrayInputStream(bytes),
                    emptyList(),
                    header.receiver,
                    header.reportFile.reportId,
                    true
                )
            }
            else -> error("Unsupported read format")
        }
    }

    /**
     * Create a report object from a header including loading the blob data associated with it
     */
    fun readBody(header: Header): ByteArray {
        return blob.downloadBlob(header.task.bodyUrl)
    }

    fun recordAction(actionHistory: ActionHistory, txn: Configuration? = null) {
        if (txn != null) {
            actionHistory.saveToDb(txn)
        } else {
            db.transact { innerTxn -> actionHistory.saveToDb(innerTxn) }
        }
    }

    private fun findOrganizationAndReceiver(
        fullName: String,
        txn: DataAccessTransaction? = null
    ): Pair<Organization, Receiver> {
        return if (settings is SettingsFacade) {
            val (organization, receiver) = (settings as SettingsFacade).findOrganizationAndReceiver(fullName, txn)
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
        organizationName: String,
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
        val content: ByteArray?
    )

    private fun createHeader(
        task: Task,
        reportFile: ReportFile,
        itemLineages: List<ItemLineage>?,
        organization: Organization?,
        receiver: Receiver?,
        fetchBlobBody: Boolean = true
    ): Header {
        val schema = if (reportFile.schemaName != null)
            metadata.findSchema(reportFile.schemaName)
        else null

        val content = if (reportFile.bodyUrl != null && fetchBlobBody)
            blob.downloadBlob(reportFile.bodyUrl)
        else null
        return Header(task, reportFile, itemLineages, organization, receiver, schema, content)
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
        txn: DataAccessTransaction,
    ) {
        fun finishedField(currentEventAction: Event.EventAction): Field<OffsetDateTime> {
            return when (currentEventAction) {
                Event.EventAction.RECEIVE -> Tables.TASK.TRANSLATED_AT
                Event.EventAction.PROCESS -> Tables.TASK.PROCESSED_AT
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
            reportId, nextEventAction.toTaskAction(), nextActionAt, retryToken, finishedField(currentEventAction), txn
        )
    }

    companion object {
        /**
         * These are all potentially heavy weight objects that
         * should only be created once.
         */
        val databaseAccessSingleton: DatabaseAccess by lazy {
            DatabaseAccess()
        }

        val settingsProviderSingleton: SettingsProvider by lazy {
            getSettingsProvider(Metadata.getInstance())
        }

        private val csvSerializerSingleton: CsvSerializer by lazy {
            CsvSerializer(Metadata.getInstance())
        }

        private val hl7SerializerSingleton: Hl7Serializer by lazy {
            Hl7Serializer(Metadata.getInstance(), settingsProviderSingleton)
        }

        private val redoxSerializerSingleton: RedoxSerializer by lazy {
            RedoxSerializer(Metadata.getInstance())
        }

        /**
         * Get a settings provider for a given [metadata] instance.
         * @return a settings provider
         */
        private fun getSettingsProvider(metadata: Metadata): SettingsProvider {
            val baseDir = System.getenv("AzureWebJobsScriptRoot") ?: "."
            val settingsEnabled: String? = System.getenv("FEATURE_FLAG_SETTINGS_ENABLED")
            return if (settingsEnabled == null || settingsEnabled.equals("true", ignoreCase = true)) {
                SettingsFacade(metadata, databaseAccessSingleton)
            } else {
                val ext = "-${Environment.get().toString().lowercase()}"
                FileSettings("$baseDir/settings", orgExt = ext)
            }
        }
    }

    // 1. detect format and get serializer
    // 2. readExternal and return result / errors / warnings
    // TODO: This could be moved to a utility/reports.kt or something like that, as it is not really part of workflow
    /**
     * Reads in a received message of HL7 or CSV format, generates an in-memory report instance
     * @param sender Sender information, pulled from database based on sender name
     * @param content Content of incoming message
     * @param defaults Default values that can be passed in as part of the request
     * @param errors Transactional store of errors produced while processing this message
     * @param warnings Transaction store of warnings produced while processing this message
     * @return Returns a generated report object, or null
     */
    fun createReport(
        sender: Sender,
        content: String,
        defaults: Map<String, String>,
        // TODO: Tech debt, should not be getting errors and warnings as side effect work, should be returning something
        //  and building these in the response object from the top layer function
        errors: MutableList<ResultDetail>,
        warnings: MutableList<ResultDetail>
    ): Report? {
        return when (sender.format) {
            Sender.Format.CSV -> {
                try {
                    val readResult = this.csvSerializer.readExternal(
                        schemaName = sender.schemaName,
                        input = ByteArrayInputStream(content.toByteArray()),
                        sources = listOf(ClientSource(organization = sender.organizationName, client = sender.name)),
                        defaultValues = defaults
                    )
                    errors += readResult.errors
                    warnings += readResult.warnings
                    readResult.report
                } catch (e: Exception) {
                    errors.add(
                        ResultDetail.report(
                            InvalidReportMessage.new(
                                "An unexpected error occurred requiring additional help. Contact the ReportStream " +
                                    "team at reportstream@cdc.gov."
                            )
                        )
                    )
                    null
                }
            }
            Sender.Format.HL7 -> {
                try {
                    val readResult = this.hl7Serializer.readExternal(
                        schemaName = sender.schemaName,
                        input = ByteArrayInputStream(content.toByteArray()),
                        ClientSource(organization = sender.organizationName, client = sender.name)
                    )
                    errors += readResult.errors
                    warnings += readResult.warnings
                    readResult.report
                } catch (e: Exception) {
                    errors.add(
                        ResultDetail.report(
                            InvalidReportMessage.new(
                                "An unexpected error occurred requiring " +
                                    "additional help. Contact the ReportStream team at reportstream@cdc.gov."
                            )
                        )
                    )
                    null
                }
            }
        }
    }
}