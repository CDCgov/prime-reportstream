package gov.cdc.prime.router.azure.observability.event

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.version.Version
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.*

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class ReportEventServiceTest {

    @Test
    fun `test getReportEventData`() {
        setupSingleReport()

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getReportEventData(
            sendReportId,
            "",
            batchReportId,
            TaskAction.send,
            Topic.FULL_ELR
        )

        assertThat(data).isEqualToIgnoringGivenProperties(
            ReportEventData(
                sendReportId,
                batchReportId,
                Topic.FULL_ELR,
                "",
                TaskAction.send,
                OffsetDateTime.now(),
                Version.commitId,
                ""
            ),
            ReportEventData::timestamp
        )
    }

    @Test
    fun `test getReportEventData no parent`() {
        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getReportEventData(
            receivedReportId,
            "",
            null,
            TaskAction.send,
            Topic.FULL_ELR
        )

        assertThat(data).isEqualToIgnoringGivenProperties(
            ReportEventData(
                receivedReportId,
                null,
                Topic.FULL_ELR,
                "",
                TaskAction.send,
                OffsetDateTime.now(),
                Version.commitId,
                ""
            ),
            ReportEventData::timestamp
        )
    }

    @Test
    fun `test getItemEventData`() {
        setupSingleReport()

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getItemEventData(
            1,
            translateReportId,
            1,
            ""
        )

        assertThat(data).isEqualTo(
            ItemEventData(
                1,
                1,
                1,
                ""
            )
        )
    }

    @Test
    fun `test getSubmissionEventData for report event with no parentReportId`() {
        setupSingleReport()

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getSubmissionEventData(1, null)

        assertThat(data).isEqualTo(
            SubmissionEventData(
                listOf(),
                "[]"
            )
        )
    }

    @Test
    fun `test getSubmissionEventData for report event when there is one submitted report`() {
        setupSingleReport()

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getSubmissionEventData(1, sendReportId)

        assertThat(data).isEqualTo(
            SubmissionEventData(
                listOf(receivedReportId),
                "[\"${receivedReportFile.sendingOrg}.${receivedReportFile.sendingOrgClient}\"]"
            )
        )
    }

    @Test
    fun `test getSubmissionEventData for report event when there are multiple submitted reports`() {
        setupBatchedReports()

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getSubmissionEventData(1, sendReportId)

        assertThat(data).isEqualTo(
            SubmissionEventData(
                listOf(receivedReportId, receivedReportId2),
                "[\"${receivedReportFile.sendingOrg}.${receivedReportFile.sendingOrgClient}\"," +
                    " \"${receivedReportFile2.sendingOrg}.${receivedReportFile2.sendingOrgClient}\"]"
            )
        )
    }

    @Test
    fun `test getSubmissionEventData for item event with no parentReportId`() {
        setupSingleReport()

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getSubmissionEventData(1, null, true)

        assertThat(data).isEqualTo(
            SubmissionEventData(
                listOf(),
                "[]"
            )
        )
    }

    @Test
    fun `test getSubmissionEventData for item event when there is one submitted report`() {
        setupSingleReport()

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getSubmissionEventData(1, sendReportId, true)

        assertThat(data).isEqualTo(
            SubmissionEventData(
                listOf(receivedReportId),
                "[\"${receivedReportFile.sendingOrg}.${receivedReportFile.sendingOrgClient}\"]"
            )
        )
    }

    @Test
    fun `test getSubmissionEventData for item event when there are multiple submitted reports`() {
        setupBatchedReports()

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getSubmissionEventData(1, sendReportId, true)

        assertThat(data).isEqualTo(
            SubmissionEventData(
                listOf(receivedReportId),
                "[\"${receivedReportFile.sendingOrg}.${receivedReportFile.sendingOrgClient}\"]"
            )
        )
    }

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

    private fun setupBatchedReports() {
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->

            setupFirstItem(txn)
            setupSecondItem(txn)

            // Lineages for Item 1
            createReportLineages(0, convertAction, receivedReportFile, convertReportFile, txn)
            createItemLineage(0, convertAction, receivedReportFile, convertReportFile, txn)
            createReportLineages(1, routeAction, convertReportFile, routeReportFile, txn)
            createItemLineage(1, routeAction, convertReportFile, routeReportFile, txn)
            createReportLineages(2, translateAction, routeReportFile, translateReportFile, txn)
            createItemLineage(2, translateAction, routeReportFile, translateReportFile, txn)

            // Lineages for Item 2
            createReportLineages(5, convertAction2, receivedReportFile2, convertReportFile2, txn)
            createItemLineage(5, convertAction2, receivedReportFile2, convertReportFile2, txn)
            createReportLineages(6, routeAction2, convertReportFile2, routeReportFile2, txn)
            createItemLineage(6, routeAction2, convertReportFile2, routeReportFile2, txn)
            createReportLineages(7, translateAction2, routeReportFile2, translateReportFile2, txn)
            createItemLineage(7, translateAction2, routeReportFile2, translateReportFile2, txn)

            // Batch and Send steps require different Item Lineage connections
            createReportLineages(3, batchAction, translateReportFile, batchReportFile, txn)
            createItemLineage(3, batchAction, translateReportFile, batchReportFile, txn, 1, 1)

            createReportLineages(8, batchAction, translateReportFile2, batchReportFile, txn)
            createItemLineage(8, batchAction, translateReportFile2, batchReportFile, txn, 1, 2)

            createReportLineages(4, sendAction, batchReportFile, sendReportFile, txn)
            createItemLineage(4, sendAction, batchReportFile, sendReportFile, txn, 1, 1)
            createItemLineage(9, sendAction, batchReportFile, sendReportFile, txn, 1, 2)
        }
    }

    private fun setupSingleReport() {
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->

            setupFirstItem(txn)

            // Lineages for Item 1
            createReportLineages(0, convertAction, receivedReportFile, convertReportFile, txn)
            createItemLineage(0, convertAction, receivedReportFile, convertReportFile, txn)
            createReportLineages(1, routeAction, convertReportFile, routeReportFile, txn)
            createItemLineage(1, routeAction, convertReportFile, routeReportFile, txn)
            createReportLineages(2, translateAction, routeReportFile, translateReportFile, txn)
            createItemLineage(2, translateAction, routeReportFile, translateReportFile, txn)

            // Batch and Send steps require different Item Lineage connections
            createReportLineages(3, batchAction, translateReportFile, batchReportFile, txn)
            createItemLineage(3, batchAction, translateReportFile, batchReportFile, txn, 1, 1)

            createReportLineages(4, sendAction, batchReportFile, sendReportFile, txn)
            createItemLineage(4, sendAction, batchReportFile, sendReportFile, txn, 1, 1)
        }
    }

    private fun setupFirstItem(txn: DataAccessTransaction) {
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
    }

    private fun setupSecondItem(txn: DataAccessTransaction) {
        // insert the actions
        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertAction(txn, receiveAction2)
        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertAction(txn, convertAction2)
        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertAction(txn, routeAction2)
        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertAction(txn, translateAction2)

        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertReportFile(receivedReportFile2, txn, receiveAction2)
        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertReportFile(convertReportFile2, txn, convertAction2)
        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertReportFile(routeReportFile2, txn, routeAction2)
        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertReportFile(translateReportFile2, txn, translateAction2)
    }

    private fun createReportLineages(
        lineageId: Long,
        action: Action,
        parentReportFile: ReportFile,
        childReportFile: ReportFile,
        txn: DataAccessTransaction,
    ) {
        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertReportLineage(
                ReportLineage(
                    lineageId,
                    action.actionId,
                    parentReportFile.reportId,
                    childReportFile.reportId,
                    OffsetDateTime.now()
                ),
                txn
            )
    }

    private fun createItemLineage(
        lineageId: Long,
        action: Action,
        parentReportFile: ReportFile,
        childReportFile: ReportFile,
        txn: DataAccessTransaction,
        parentIndex: Int = 1,
        childIndex: Int = 1,
    ) {
        ReportStreamTestDatabaseContainer.testDatabaseAccess
            .insertItemLineages(
                setOf(
                    ItemLineage(
                        lineageId,
                        parentReportFile.reportId,
                        parentIndex,
                        childReportFile.reportId,
                        childIndex,
                        null,
                        null,
                        OffsetDateTime.now(),
                        "null"
                    )
                ),
                txn, action
            )
    }
}