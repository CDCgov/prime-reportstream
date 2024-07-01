package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.UUID

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class SubmissionFunctionIntegrationTests {

    class NodeBuilder {
        lateinit var theAction: TaskAction
        var theReportBlobUrl: String = UUID.randomUUID().toString()
        var theItemCount: Int = 1
        val nodes: MutableList<NodeBuilder> = mutableListOf()
        var receiver: Receiver? = null
        var logs: MutableList<ActionLog> = mutableListOf()
        var theTransportResult: String? = null

        fun receiver(receiver: Receiver) {
            this.receiver = receiver
        }

        fun action(action: TaskAction) {
            this.theAction = action
        }

        fun reportBlobUrl(reportBlobUrl: String) {
            this.theReportBlobUrl = reportBlobUrl
        }

        fun itemCount(itemCount: Int) {
            this.theItemCount = itemCount
        }

        fun node(initializer: NodeBuilder.() -> Unit) {
            this.nodes.add(NodeBuilder().apply(initializer))
        }

        fun log(actionLog: ActionLog) {
            this.logs.add(actionLog)
        }

        fun transportResult(transportResult: String) {
            this.theTransportResult = transportResult
        }
    }

    class PipelineGraphNode(val node: ReportFile) {
        val children: MutableList<PipelineGraphNode> = mutableListOf()
    }

    class PipelineGraphBuilder {
        private lateinit var theRoot: NodeBuilder
        private lateinit var theTopic: Topic
        private lateinit var theFormat: Report.Format
        private lateinit var theSender: Sender

        fun topic(topic: Topic) {
            this.theTopic = topic
        }

        fun format(format: Report.Format) {
            this.theFormat = format
        }

        fun sender(sender: Sender) {
            this.theSender = sender
        }

        fun root(initializer: NodeBuilder.() -> Unit) {
            this.theRoot = NodeBuilder().apply(initializer)
        }

        fun generate(dbAccess: DatabaseAccess): PipelineGraphNode {
            if (!::theTopic.isInitialized) {
                throw IllegalStateException("Topic must be set")
            }
            if (!::theFormat.isInitialized) {
                throw IllegalStateException("Format must be set")
            }
            if (!::theRoot.isInitialized) {
                throw IllegalStateException("Root must be set")
            }
            val graph = dbAccess.transactReturning { txn ->
                val report = Report(
                    theFormat,
                    emptyList(),
                    theRoot.theItemCount,
                    metadata = UnitTestUtils.simpleMetadata,
                    // TODO
                    nextAction = TaskAction.convert,
                    topic = theTopic
                )

                val action = Action().setActionName(theRoot.theAction).setExternalName("")
                action.setSendingOrg(theSender.organizationName)
                action.setSendingOrgClient(theSender.name)
                action.setHttpStatus(201)
                val actionId = dbAccess.insertAction(txn, action)
                action.actionId = actionId
                val reportFile = ReportFile()
                    .setSchemaTopic(theTopic)
                    .setReportId(report.id)
                    .setActionId(actionId)
                    .setSchemaName("")
                    .setSendingOrg(theSender.organizationName)
                    .setSendingOrgClient(theSender.name)
                    .setBodyFormat(theFormat.toString())
                    .setItemCount(theRoot.theItemCount)
                    .setExternalName("test-external-name")
                    .setBodyUrl(theRoot.theReportBlobUrl)
                dbAccess.insertReportFile(
                    reportFile, txn, action
                )

                val graph = PipelineGraphNode(reportFile)

                theRoot.nodes.fold(graph) { acc, node ->
                    acc.children.add(descend(node, dbAccess, txn, report, graph))
                    acc
                }
                graph
            }
            return graph
        }

        private fun descend(
            node: NodeBuilder,
            dbAccess: DatabaseAccess,
            txn: DataAccessTransaction,
            report: Report,
            graph: PipelineGraphNode,
        ): PipelineGraphNode {
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
                .setSchemaName("")
                .setBodyFormat(theFormat.toString())
                .setItemCount(node.theItemCount)
                .setExternalName("test-external-name")
                .setBodyUrl(node.theReportBlobUrl)
                .setTransportResult(node.theTransportResult)

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
            // TODO item lineage
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

            val childGraph = PipelineGraphNode(childReportFile)
            node.nodes.fold(graph) { acc, descendant ->
                descend(descendant, dbAccess, txn, report, childGraph)
                acc
            }
            return childGraph
        }
    }

    fun history(initializer: PipelineGraphBuilder.() -> Unit): PipelineGraphBuilder {
        return PipelineGraphBuilder().apply(initializer)
    }

    @Test
    fun `it should return history of a submission that is delivered`() {
        val submittedReport = history {
            topic(Topic.FULL_ELR)
            format(Report.Format.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            root {
                action(TaskAction.receive)
                node {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    node {
                        action(TaskAction.route)
                        log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.error))
                        node {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            node {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        node {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            node {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            }
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.authenticate(any()) } returns claims
        val workflowEngine = WorkflowEngine
            .Builder()
            .metadata(UnitTestUtils.simpleMetadata)
            .settingsProvider(
                FileSettings().loadOrganizations(UniversalPipelineTestUtils.universalPipelineOrganization)
            )
            .databaseAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess)
            .build()

        val func = SubmissionFunction(
            SubmissionsFacade(
                DatabaseSubmissionsAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            ),
            workflowEngine,
        )

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.DELIVERED.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(1)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }
}