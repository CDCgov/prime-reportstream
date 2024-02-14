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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
class ReportGraphTest {

    @Nested
    @DisplayName("getRootReport")
    @Testcontainers(parallel = true)
    inner class GetRootReport {

        @Container
        val dbContainer = ReportStreamTestDatabaseContainer()

        // seed database with sample report lineage
        val receiveAction = Action().setActionId(1).setActionName(TaskAction.receive)
        val receivedReportId = UUID.randomUUID()
        val receivedReportFile = ReportFile().setSchemaTopic(Topic.ELR_ELIMS).setReportId(receivedReportId)
            .setActionId(receiveAction.actionId).setSchemaName("").setBodyFormat("HL7").setItemCount(1)
            .setExternalName("receive-name").setBodyUrl("receive-url").setSendingOrg("sendingOrg")
            .setSendingOrgClient("sendingClient")

        val convertAction = Action().setActionId(2)
        val convertReportFile = ReportFile().setSchemaTopic(Topic.ELR_ELIMS).setReportId(UUID.randomUUID())
            .setActionId(convertAction.actionId).setSchemaName("").setBodyFormat("HL7").setItemCount(1)
            .setExternalName("convert-name").setBodyUrl("convert-url")

        val routeAction = Action().setActionId(3)
        val routeReportId = UUID.randomUUID()
        val routeReportFile =
            ReportFile().setSchemaTopic(Topic.ELR_ELIMS).setReportId(routeReportId).setActionId(routeAction.actionId)
                .setSchemaName("").setBodyFormat("HL7").setItemCount(1).setExternalName("route-name")
                .setBodyUrl("route-url")

        val translateAction = Action().setActionId(4)
        val translateReportId = UUID.randomUUID()
        val translateReportFile = ReportFile().setSchemaTopic(Topic.ELR_ELIMS).setReportId(translateReportId)
            .setActionId(translateAction.actionId).setSchemaName("").setBodyFormat("HL7").setItemCount(1)
            .setExternalName("translate-name").setBodyUrl("translate-url")

        @BeforeEach
        fun beforeEach() {
            val db = ReportStreamTestDatabaseContainer.getDataSourceFromContainer(dbContainer)
            db.transact { txn ->
                db.insertAction(txn, receiveAction)
                db.insertAction(txn, convertAction)
                db.insertAction(txn, routeAction)
                db.insertAction(txn, translateAction)

                db.insertReportFile(receivedReportFile, txn, receiveAction)
                db.insertReportFile(convertReportFile, txn, convertAction)
                db.insertReportFile(routeReportFile, txn, routeAction)
                db.insertReportFile(translateReportFile, txn, translateAction)

                db.insertReportLineage(
                    ReportLineage(
                        0,
                        receiveAction.actionId,
                        receivedReportFile.reportId,
                        convertReportFile.reportId,
                        OffsetDateTime.now()
                    ),
                    txn
                )
                db.insertReportLineage(
                    ReportLineage(
                        1,
                        convertAction.actionId,
                        convertReportFile.reportId,
                        routeReportFile.reportId,
                        OffsetDateTime.now()
                    ),
                    txn
                )
                db.insertReportLineage(
                    ReportLineage(
                        2,
                        routeAction.actionId,
                        routeReportFile.reportId,
                        translateReportFile.reportId,
                        OffsetDateTime.now()
                    ),
                    txn
                )
            }
        }

        @Test
        fun `find root report from child report`() {
            val db = ReportStreamTestDatabaseContainer.getDataSourceFromContainer(dbContainer)
            val reportGraph = ReportGraph(db)
            val root = reportGraph.getRootReport(translateReportId)
            assertThat(root).isNotNull().transform { it.reportId }.isEqualTo(receivedReportFile.reportId)
        }

        @Test
        fun `find root report from report in the middle of graph`() {
            val db = ReportStreamTestDatabaseContainer.getDataSourceFromContainer(dbContainer)
            val reportGraph = ReportGraph(db)
            val root = reportGraph.getRootReport(routeReportId)
            assertThat(root).isNotNull().transform { it.reportId }.isEqualTo(receivedReportFile.reportId)
        }

        @Test
        fun `return null if a root is passed in`() {
            val db = ReportStreamTestDatabaseContainer.getDataSourceFromContainer(dbContainer)
            val reportGraph = ReportGraph(db)
            val root = reportGraph.getRootReport(receivedReportId)
            assertThat(root).isNull()
        }
    }
}