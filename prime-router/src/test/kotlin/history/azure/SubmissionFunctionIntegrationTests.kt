package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
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

    // TODO sub class step into a Sender or Receiver step
    class Step(val action: TaskAction, val reportBlobUrl: String, val itemCount: Int)
    class PipelineHistoryBuilder {
        private val steps = mutableListOf<Step>()
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

        fun step(action: TaskAction) {
            // TODO drop hardcoding
            // TODO integrate with azurite?
            this.steps.add(Step(action, UUID.randomUUID().toString(), 1))
        }

        fun generate(dbAccess: DatabaseAccess): ReportFile {
            if (!::theTopic.isInitialized) {
                throw IllegalStateException("Topic must be set")
            }
            if (!::theFormat.isInitialized) {
                throw IllegalStateException("Format must be set")
            }
            if (!::theSender.isInitialized) {
                throw IllegalStateException("Sender must be set")
            }
            if (steps.isEmpty()) {
                throw IllegalStateException("Must generate some steps")
            }
            val createdReports = dbAccess.transactReturning { txn ->

                steps.foldIndexed(emptyList<ReportFile>()) { index, acc, step ->
                    val report = Report(
                        theFormat,
                        emptyList(),
                        step.itemCount,

                        metadata = UnitTestUtils.simpleMetadata,
                        nextAction = TaskAction.convert,
                        topic = theTopic
                    )
                    val action = Action().setActionName(step.action).setExternalName("")
                    if (index == 0) {
                        action.setSendingOrg(theSender.organizationName)
                        action.setSendingOrgClient(theSender.name)
                        action.setHttpStatus(201)
                    }
                    val actionId = dbAccess.insertAction(txn, action)
                    val reportFile = ReportFile()
                        .setSchemaTopic(theTopic)
                        .setReportId(report.id)
                        .setActionId(actionId)
                        .setSchemaName("")
                        .setBodyFormat(theFormat.toString())
                        .setItemCount(step.itemCount)
                        .setExternalName("test-external-name")
                        .setBodyUrl(step.reportBlobUrl)
                    if (index == 0) {
                        reportFile.setSendingOrg(theSender.organizationName)
                        reportFile.setSendingOrgClient(theSender.name)
                    }
                    dbAccess.insertReportFile(
                        reportFile, txn, action
                    )
                    if (acc.lastOrNull() != null) {
                        dbAccess.insertReportLineage(
                            ReportLineage(
                                null,
                                actionId,
                                acc.last().reportId,
                                report.id,
                                OffsetDateTime.now()
                            ),
                            txn
                        )
                        // TODO item lineage
                    }
                    acc + listOf(reportFile)
                }
            }
            return createdReports.first()
        }
    }

    fun history(initializer: PipelineHistoryBuilder.() -> Unit): PipelineHistoryBuilder {
        return PipelineHistoryBuilder().apply(initializer)
    }

    @Test
    fun `it should return history of a submission`() {
        val submittedReport = history {
            topic(Topic.FULL_ELR)
            format(Report.Format.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)
            step(TaskAction.receive)
            step(TaskAction.convert)
            step(TaskAction.route)
            step(TaskAction.translate)
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
            .getReportDetailedHistory(httpRequestMessage, submittedReport.reportId.toString())
        assertThat(history).isNotNull()
        val parsedHistory = JacksonMapperUtilities.defaultMapper.readValue<DetailedSubmissionHistory>(
            history.body as String,
            DetailedSubmissionHistory::class.java
        )
        assertThat(parsedHistory.overallStatus).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING)
    }
}