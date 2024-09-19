package gov.cdc.prime.router.history.db

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.reportstream.shared.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import org.jooq.exception.TooManyRowsException
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
        // there are two lineages that are joined together at the batch step
        // the lineage will only have one root up through the translate step
        // the batch step and on can have more than one root
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

        val receiveAction2 = Action().setActionId(7).setActionName(TaskAction.receive)
        val receivedReportId2 = UUID.randomUUID()
        val receivedReportFile2 = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(receivedReportId2)
            .setActionId(receiveAction2.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("receive2-name")
            .setBodyUrl("receive2-url")
            .setSendingOrg("sendingOrg")
            .setSendingOrgClient("sendingClient")

        val convertAction2 = Action().setActionId(8)
        val convertReportFile2 = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(UUID.randomUUID())
            .setActionId(convertAction2.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("convert2-name")
            .setBodyUrl("convert2-url")

        val routeAction2 = Action().setActionId(9)
        val routeReportId2 = UUID.randomUUID()
        val routeReportFile2 = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(routeReportId2)
            .setActionId(routeAction2.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("route2-name")
            .setBodyUrl("route2-url")

        val translateAction2 = Action().setActionId(10)
        val translateReportId2 = UUID.randomUUID()
        val translateReportFile2 = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(translateReportId2)
            .setActionId(translateAction2.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(1)
            .setExternalName("translate2-name")
            .setBodyUrl("translate2-url")

        val batchAction = Action().setActionId(5)
        val batchReportId = UUID.randomUUID()
        val batchReportFile = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(batchReportId)
            .setActionId(batchAction.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(2)
            .setExternalName("batch-name")
            .setBodyUrl("batch-url")

        val sendAction = Action().setActionId(6).setActionName(TaskAction.send)
        val sendReportId = UUID.randomUUID()
        val sendReportFile = ReportFile()
            .setSchemaTopic(Topic.ELR_ELIMS)
            .setReportId(sendReportId)
            .setActionId(sendAction.actionId)
            .setSchemaName("")
            .setBodyFormat("HL7")
            .setItemCount(2)
            .setExternalName("send-name")
            .setBodyUrl("send-url")

        val reportGraph = ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        init {
            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->

                // insert the actions
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, receiveAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, convertAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, routeAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, translateAction)

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, receiveAction2)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, convertAction2)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, routeAction2)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertAction(txn, translateAction2)

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
                    .insertReportFile(receivedReportFile2, txn, receiveAction2)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(convertReportFile2, txn, convertAction2)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(routeReportFile2, txn, routeAction2)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(translateReportFile2, txn, translateAction2)

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(batchReportFile, txn, batchAction)
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportFile(sendReportFile, txn, sendAction)

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            0,
                            convertAction.actionId,
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
                            routeAction.actionId,
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
                            translateAction.actionId,
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
                            batchAction.actionId,
                            translateReportFile.reportId,
                            batchReportFile.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            5,
                            convertAction2.actionId,
                            receivedReportFile2.reportId,
                            convertReportFile2.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            6,
                            routeAction2.actionId,
                            convertReportFile2.reportId,
                            routeReportFile2.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            7,
                            translateAction2.actionId,
                            routeReportFile2.reportId,
                            translateReportFile2.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            8,
                            batchAction.actionId,
                            translateReportFile2.reportId,
                            batchReportFile.reportId,
                            OffsetDateTime.now()
                        ),
                        txn
                    )

                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            4,
                            sendAction.actionId,
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
        fun `find root report throws exception when passing in a multi-root ID`() {
            assertFailure {
                reportGraph.getRootReport(sendReportId)
            }.isInstanceOf(TooManyRowsException::class)
        }

        @Test
        fun `find root reports from send child report`() {
            val roots = reportGraph.getRootReports(sendReportId)
            assertThat(roots)
                .isNotNull()
                .hasSize(2)
            assertThat(roots[0].reportId).isEqualTo(receivedReportId)
            assertThat(roots[1].reportId).isEqualTo(receivedReportId2)
        }

        @Test
        fun `find descendant reports from receive parent report`() {
            var descendants: List<ReportFile> = emptyList()

            ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
                descendants = reportGraph.getDescendantReports(txn, receivedReportId, setOf(TaskAction.send))
            }

            assertThat(descendants)
                .isNotNull()
                .hasSize(1)
            assertThat(descendants[0].reportId).isEqualTo(sendReportId)
        }
    }
}