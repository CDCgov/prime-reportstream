package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.MimeFormat
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
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.UUID

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class SubmissionFunctionIntegrationTests {

    class ReportNodeBuilder {
        lateinit var theAction: TaskAction
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

    class ReportGraphNode(val node: ReportFile) {
        val children: MutableList<ReportGraphNode> = mutableListOf()
    }

    class ReportGraphBuilder {
        private lateinit var theSubmission: ReportNodeBuilder
        private lateinit var theTopic: Topic
        private lateinit var theFormat: MimeFormat
        private lateinit var theSender: Sender

        fun topic(topic: Topic) {
            this.theTopic = topic
        }

        fun format(format: MimeFormat) {
            this.theFormat = format
        }

        fun sender(sender: Sender) {
            this.theSender = sender
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
                    .setSchemaName("")
                    .setSendingOrg(theSender.organizationName)
                    .setSendingOrgClient(theSender.name)
                    .setBodyFormat(theFormat.toString())
                    .setItemCount(theSubmission.theItemCount)
                    .setExternalName("test-external-name")
                    .setBodyUrl(theSubmission.theReportBlobUrl)
                    .setNextAction(theSubmission.reportGraphNodes.firstOrNull()?.theAction)
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
                .setSchemaName("")
                .setBodyFormat(theFormat.toString())
                .setItemCount(node.theItemCount)
                .setExternalName("test-external-name")
                .setBodyUrl(node.theReportBlobUrl)
                .setTransportResult(node.theTransportResult)
                .setNextAction(node.reportGraphNodes.firstOrNull()?.theAction)

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
            node.reportGraphNodes.foldIndexed(graph) { childNodeIndex, acc, descendant ->
                descend(descendant, dbAccess, txn, report, childGraph, childNodeIndex)
                acc
            }
            return childGraph
        }
    }

    fun reportGraph(initializer: ReportGraphBuilder.() -> Unit): ReportGraphBuilder {
        return ReportGraphBuilder().apply(initializer)
    }

    @Test
    fun `it should return a history for partially delivered submission`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            itemCount(0)
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            itemCount(0)
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.PARTIALLY_DELIVERED.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    // TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
    @Test
    fun `it should return a history for partially delivered submission (for legacy route step)`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.route)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            itemCount(0)
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.route)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            itemCount(0)
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.PARTIALLY_DELIVERED.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    @Test
    fun `it should return a history that a submission has been received`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(0)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(0)
    }

    @Test
    fun `it should return a history that indicates the report is not going to be delivered`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(0)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    // TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
    @Test
    fun `it should return a history that indicates the report is not going to be delivered (legacy route step)`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.route)
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(0)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    @Test
    fun `it should return a history that indicates waiting to deliver`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.error))
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(1)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    // TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
    @Test
    fun `it should return a history that indicates waiting to deliver (legacy route step)`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.route)
                        log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.error))
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.route)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(1)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    @Test
    fun `it should return history of a submission that is delivered`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.error))
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            }
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            reportGraphNode {
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

        val func = setupSubmissionFunction()

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
        assertThat(historyNode.get("sender").asText()).isEqualTo("phd.elr-hl7-sender")
        assertThat(historyNode.get("actualCompletionAt").asText()).isNotNull()
    }

    // TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
    @Test
    fun `it should return history of a submission that is delivered (legacy route step)`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.route)
                        log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.error))
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            }
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.route)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            reportGraphNode {
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

        val func = setupSubmissionFunction()

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
        assertThat(historyNode.get("sender").asText()).isEqualTo("phd.elr-hl7-sender")
        assertThat(historyNode.get("actualCompletionAt").asText()).isNotNull()
    }

    @BeforeEach
    fun setupAuth() {
        val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.authenticate(any()) } returns claims
    }

    private fun setupSubmissionFunction(): SubmissionFunction {
        val workflowEngine = WorkflowEngine
            .Builder()
            .metadata(UnitTestUtils.simpleMetadata)
            .settingsProvider(
                FileSettings().loadOrganizations(UniversalPipelineTestUtils.universalPipelineOrganization)
            )
            .databaseAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess)
            .build()
        return SubmissionFunction(
            SubmissionsFacade(
                DatabaseSubmissionsAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            ),
            workflowEngine,

            )
    }
}