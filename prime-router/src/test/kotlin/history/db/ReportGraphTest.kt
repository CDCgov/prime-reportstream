package gov.cdc.prime.router.history.db

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class ReportGraphTest {

    @Nested
    @DisplayName("getRootReport")
    inner class GetRootReport {

        // seed database with sample report lineage
        val receiveAction = Action().setActionId(1).setActionName(TaskAction.receive)
        val receivedReportId = UUID.randomUUID()
        val receivedReportFile = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(receivedReportId)
            .setActionId(receiveAction.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("receive-name")
            .setBodyUrl("receive-url")
            .setSendingOrg("sendingOrg")
            .setSendingOrgClient("sendingClient")

        val convertAction = Action().setActionId(2)
        val convertReportFile = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(UUID.randomUUID())
            .setActionId(convertAction.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("convert-name")
            .setBodyUrl("convert-url")

        val routeAction = Action().setActionId(3)
        val routeReportId = UUID.randomUUID()
        val routeReportFile = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(routeReportId)
            .setActionId(routeAction.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("route-name")
            .setBodyUrl("route-url")

        val translateAction = Action().setActionId(4)
        val translateReportId = UUID.randomUUID()
        val translateReportFile = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(translateReportId)
            .setActionId(translateAction.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("translate-name")
            .setBodyUrl("translate-url")

        val batchAction = Action().setActionId(5)
        val batchReportId = UUID.randomUUID()
        val batchReportFile = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(batchReportId)
            .setActionId(batchAction.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("batch-name")
            .setBodyUrl("batch-url")

        val sendAction = Action().setActionId(6)
        val sendReportId = UUID.randomUUID()
        val sendReportFile = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(sendReportId)
            .setActionId(sendAction.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("send-name")
            .setBodyUrl("send-url")

        val reportGraph = ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        init {
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, receiveAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, convertAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, routeAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, translateAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, batchAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, sendAction)

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(receivedReportFile, txn, receiveAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(convertReportFile, txn, convertAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(routeReportFile, txn, routeAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(translateReportFile, txn, translateAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(batchReportFile, txn, batchAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(sendReportFile, txn, sendAction)

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            0,
                            receiveAction.actionId,
                            receivedReportFile.reportId,
                            convertReportFile.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            1,
                            convertAction.actionId,
                            convertReportFile.reportId,
                            routeReportFile.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            2,
                            routeAction.actionId,
                            routeReportFile.reportId,
                            translateReportFile.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            3,
                            translateAction.actionId,
                            translateReportFile.reportId,
                            batchReportFile.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            4,
                            batchAction.actionId,
                            batchReportFile.reportId,
                            sendReportFile.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )
            }
        }

        @Test
        fun `find root report from child report`() {
            val root = reportGraph.getRootReport(translateReportId)
            assertThat(root)
                .isNotNull()
                .transform { it.reportId }
                .isEqualTo(receivedReportFile.reportId)
        }

        @Test
        fun `find root report from report in the middle of graph`() {
            val root = reportGraph.getRootReport(routeReportId)
            assertThat(root)
                .isNotNull()
                .transform { it.reportId }
                .isEqualTo(receivedReportFile.reportId)
        }

        @Test
        fun `return null if a root is passed in`() {
            val root = reportGraph.getRootReport(receivedReportId)
            assertThat(root)
                .isNull()
        }

        @Test
        fun `find root reports from send child report`() {
            val root = reportGraph.getRootReports(sendReportId)
            assertThat(root)
                .isNotNull()
                .transform { it.first().reportId }
                .isEqualTo(receivedReportFile.reportId)
        }
    }
}