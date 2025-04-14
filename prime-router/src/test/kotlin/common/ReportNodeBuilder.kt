package gov.cdc.prime.router.common

import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.unittest.UnitTestUtils
import java.time.OffsetDateTime
import java.util.UUID

class ReportGraphNode(val node: ReportFile) {
    val children: MutableList<ReportGraphNode> = mutableListOf()
}

class ReportGraphBuilder {
    private lateinit var theSubmission: ReportNodeBuilder
    private lateinit var theTopic: Topic
    private lateinit var theFormat: MimeFormat
    private lateinit var theSender: Sender
    private var theNextAction: TaskAction? = null
    private lateinit var theExternalName: String

    fun topic(topic: Topic) {
        this.theTopic = topic
    }

    fun format(format: MimeFormat) {
        this.theFormat = format
    }

    fun sender(sender: Sender) {
        this.theSender = sender
    }

    fun nextAction(nextAction: TaskAction) {
        this.theNextAction = nextAction
    }

    fun externalName(name: String) {
        this.theExternalName = name
    }

    fun submission(initializer: ReportNodeBuilder.() -> Unit) {
        this.theSubmission = ReportNodeBuilder().apply(initializer)
    }

    fun generate(dbAccess: DatabaseAccess): ReportGraphNode {
        if (!::theTopic.isInitialized) {
            throw IllegalStateException("Topic must be set")
        }
        if (!::theFormat.isInitialized) {
            throw IllegalStateException("Format must be set")
        }
        if (!::theSubmission.isInitialized) {
            throw IllegalStateException("Root must be set")
        }
        val graph = dbAccess.transactReturning { txn ->
            val report = Report(
                theFormat,
                emptyList(),
                theSubmission.theItemCount,
                metadata = UnitTestUtils.simpleMetadata,
                // TODO
                nextAction = TaskAction.convert,
                topic = theTopic
            )

            val action = Action().setActionName(theSubmission.theAction).setExternalName("")
            action.setSendingOrg(theSender.organizationName)
            action.setSendingOrgClient(theSender.name)
            action.setHttpStatus(201)
            val actionId = dbAccess.insertAction(txn, action)
            action.actionId = actionId
            val reportFile = ReportFile()
                .setSchemaTopic(theTopic)
                .setReportId(report.id)
                .setActionId(actionId)
                .setSchemaName("generated-report")
                .setSendingOrg(theSender.organizationName)
                .setSendingOrgClient(theSender.name)
                .setBodyFormat(theFormat.toString())
                .setItemCount(theSubmission.theItemCount)
                .setExternalName("test-external-name")
                .setBodyUrl(theSubmission.theReportBlobUrl)
                .setNextAction(theNextAction ?: theSubmission.reportGraphNodes.firstOrNull()?.theAction)
                .setCreatedAt(OffsetDateTime.now())
            if (action.actionName == TaskAction.send) {
                reportFile.setTransportParams("{Some Transport Params}")
            }
            dbAccess.insertReportFile(
                reportFile, txn, action
            )

            val graph = ReportGraphNode(reportFile)

            theSubmission.reportGraphNodes.foldIndexed(graph) { nodeIndex, acc, node ->
                acc.children.add(descend(node, dbAccess, txn, report, graph, nodeIndex))
                acc
            }
            graph
        }
        return graph
    }

    private fun descend(
        node: ReportNodeBuilder,
        dbAccess: DatabaseAccess,
        txn: DataAccessTransaction,
        report: Report,
        graph: ReportGraphNode,
        nodeIndex: Int,
    ): ReportGraphNode {
        val childReport = Report(
            theFormat,
            emptyList(),
            node.theItemCount,
            metadata = UnitTestUtils.simpleMetadata,
            nextAction = TaskAction.convert,
            topic = theTopic
        )
        val childAction = Action().setActionName(node.theAction).setExternalName("")

        if (node.receiver != null) {
            childAction.setReceivingOrg(node.receiver!!.organizationName)
            childAction.setReceivingOrgSvc(node.receiver!!.name)
        }
        val childActionId = dbAccess.insertAction(txn, childAction)
        childAction.actionId = childActionId
        val childReportFile = ReportFile()
            .setSchemaTopic(theTopic)
            .setReportId(childReport.id)
            .setActionId(childActionId)
            .setSchemaName("generated-report")
            .setBodyFormat(theFormat.toString())
            .setItemCount(node.theItemCount)
            .setExternalName("test-external-name")
            .setBodyUrl(node.theReportBlobUrl)
            .setTransportResult(node.theTransportResult)
            .setNextAction(node.theNextAction ?: node.reportGraphNodes.firstOrNull()?.theAction)
            .setCreatedAt(graph.node.createdAt.plusMinutes(1))
        if (childAction.actionName == TaskAction.send) {
            childReportFile.setTransportParams("{Some Transport Params}")
        }
        if (node.receiver != null) {
            childReportFile.setReceivingOrg(node.receiver!!.organizationName)
            childReportFile.setReceivingOrgSvc(node.receiver!!.name)
        }
        dbAccess.insertReportFile(
            childReportFile, txn, childAction
        )
        node.logs.forEach { log ->
            log.action = childAction
            log.reportId = childReport.id
            dbAccess.insertActionLog(log, txn)
        }

        dbAccess.insertReportLineage(
            ReportLineage(
                null,
                childActionId,
                graph.node.reportId,
                childReportFile.reportId,
                OffsetDateTime.now()
            ),
            txn
        )

        dbAccess.insertItemLineages(
            setOf(
                ItemLineage(
                    null,
                    graph.node.reportId,
                    1,
                    childReportFile.reportId,
                    nodeIndex + 1,
                    null,
                    null,
                    null,
                    ""
                )
            ),
            txn, childAction
        )
        val childGraph = ReportGraphNode(childReportFile)
        node.reportGraphNodes.foldIndexed(childGraph) { childNodeIndex, acc, descendant ->
            acc.children.add(descend(descendant, dbAccess, txn, report, childGraph, childNodeIndex))
            acc
        }
        return childGraph
    }
}

class ReportNodeBuilder {

    companion object {
        fun reportGraph(
            initializer: ReportGraphBuilder.() -> Unit,
        ): ReportGraphBuilder = ReportGraphBuilder().apply(initializer)
    }

    lateinit var theAction: TaskAction
    var theNextAction: TaskAction? = null
    var theReportBlobUrl: String = UUID.randomUUID().toString()
    var theItemCount: Int = 1
    val reportGraphNodes: MutableList<ReportNodeBuilder> = mutableListOf()
    var receiver: Receiver? = null
    var logs: MutableList<ActionLog> = mutableListOf()
    var theTransportResult: String? = null

    fun receiver(receiver: Receiver) {
        this.receiver = receiver
    }

    fun action(action: TaskAction) {
        this.theAction = action
    }

    fun nextAction(nextAction: TaskAction) {
        this.theNextAction = nextAction
    }

    fun reportBlobUrl(reportBlobUrl: String) {
        this.theReportBlobUrl = reportBlobUrl
    }

    fun itemCount(itemCount: Int) {
        this.theItemCount = itemCount
    }

    fun reportGraphNode(initializer: ReportNodeBuilder.() -> Unit) {
        this.reportGraphNodes.add(ReportNodeBuilder().apply(initializer))
    }

    fun log(actionLog: ActionLog) {
        this.logs.add(actionLog)
    }

    fun transportResult(transportResult: String) {
        this.theTransportResult = transportResult
    }
}