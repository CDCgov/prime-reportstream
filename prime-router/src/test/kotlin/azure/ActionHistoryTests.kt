package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.InvalidHL7Message
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertNotNull

class ActionHistoryTests {
    @Test
    fun `test trackActionReceiverInfo`() {
        val actionHistory = ActionHistory(TaskAction.translate)
        val org = "org-name"
        val receiver = "receiver-name"
        actionHistory.trackActionReceiverInfo(org, receiver)
        assertThat(actionHistory.action.receivingOrg).isEqualTo(org)
        assertThat(actionHistory.action.receivingOrgSvc).isEqualTo(receiver)
    }

    @Test
    fun `test constructor`() {
        val actionHistory = ActionHistory(TaskAction.batch)
        assertThat(actionHistory.generatingEmptyReport).isEqualTo(false)
        assertThat(actionHistory.action.actionName).isEqualTo(TaskAction.batch)
    }

    @Test
    fun `test constructor with empty`() {
        val actionHistory = ActionHistory(TaskAction.batch, generatingEmptyReport = true)
        assertThat(actionHistory.generatingEmptyReport).isEqualTo(true)
    }

    @Test
    fun `test trackActionParams`() {
        val actionHistory = ActionHistory(TaskAction.process)

        actionHistory.trackActionParams("")
        assertThat(actionHistory.action.actionParams).isNull()

        actionHistory.trackActionParams("foo")
        assertThat(actionHistory.action.actionParams).isEqualTo("foo")

        actionHistory.trackActionParams("bar")
        assertThat(actionHistory.action.actionParams).isEqualTo("foo, bar")
    }

    @Test
    fun `test trackActionResult`() {
        val actionHistory1 = ActionHistory(TaskAction.batch)
        actionHistory1.trackActionResult("foobar")
        assertThat(actionHistory1.action.actionResult).isEqualTo("foobar")
        val giantStr = "x".repeat(3000)
        actionHistory1.trackActionResult(giantStr)
        // now with bigger strings! since we append, it should be at least 3000 chars
        assertThat(actionHistory1.action.actionResult.length >= 3000).isTrue()
    }

    @Test
    fun `test trackExternalInputReport`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf())
        val report1 = Report(
            one, listOf(),
            sources = listOf(ClientSource("myOrg", "myClient")),
            metadata = UnitTestUtils.simpleMetadata
        )
        val actionHistory1 = ActionHistory(TaskAction.receive)
        val blobInfo1 = BlobAccess.BlobInfo(Report.Format.CSV, "myUrl", byteArrayOf(0x11, 0x22))
        val payloadName = "quux"
        actionHistory1.trackExternalInputReport(report1, blobInfo1, payloadName)
        assertNotNull(actionHistory1.reportsReceived[report1.id])
        val reportFile = actionHistory1.reportsReceived[report1.id]!!
        assertThat(reportFile.schemaName).isEqualTo("one")
        assertThat(reportFile.schemaTopic).isEqualTo(Topic.TEST)
        assertThat(reportFile.sendingOrg).isEqualTo("myOrg")
        assertThat(reportFile.sendingOrgClient).isEqualTo("myClient")
        assertThat(reportFile.bodyUrl).isEqualTo("myUrl")
        assertThat(reportFile.blobDigest[1]).isEqualTo(34)
        assertThat(reportFile.receivingOrg).isNull()
        assertThat(reportFile.externalName).isEqualTo(payloadName)
        assertThat(actionHistory1.action.externalName).isEqualTo(payloadName)

        // not allowed to track the same report twice.
        assertThat { actionHistory1.trackExternalInputReport(report1, blobInfo1) }.isFailure()
    }

    @Test
    fun `test trackGeneratedEmptyReport`() {
        val event1 = ReportEvent(Event.EventAction.TRANSLATE, UUID.randomUUID(), false, OffsetDateTime.now())
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf())
        val report1 = Report(
            one, listOf(),
            sources = listOf(ClientSource("myOrg", "myClient")),
            metadata = UnitTestUtils.simpleMetadata
        )
        val org =
            DeepOrganization(
                name = "myOrg",
                description = "blah blah",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                receivers = listOf(
                    Receiver("myService", "myOrg", Topic.TEST, CustomerStatus.INACTIVE, "schema")
                )
            )
        val orgReceiver = org.receivers[0]
        val actionHistory1 = ActionHistory(TaskAction.send)
        val blobInfo1 = BlobAccess.BlobInfo(Report.Format.CSV, "myUrl", byteArrayOf(0x11, 0x22))
        actionHistory1.trackGeneratedEmptyReport(event1, report1, orgReceiver, blobInfo1)
        assertNotNull(actionHistory1.reportsReceived[report1.id])
        val reportFile = actionHistory1.reportsReceived[report1.id]!!
        assertThat(reportFile.schemaName).isEqualTo("one")
        assertThat(reportFile.schemaTopic).isEqualTo(Topic.TEST)
        assertThat(reportFile.bodyUrl).isEqualTo("myUrl")
        assertThat(reportFile.blobDigest[1]).isEqualTo(34)
        assertThat(reportFile.receivingOrg).isEqualTo("myOrg")
        assertThat(reportFile.receivingOrgSvc).isEqualTo("myService")
        assertThat(reportFile.bodyFormat).isEqualTo("CSV")
        assertThat(reportFile.itemCount).isEqualTo(0)
    }

    @Test
    fun `test trackCreatedReport`() {
        val event1 = ReportEvent(Event.EventAction.TRANSLATE, UUID.randomUUID(), false, OffsetDateTime.now())
        val schema1 = Schema(name = "schema1", topic = Topic.TEST, elements = listOf())
        val report1 = Report(
            schema1, listOf(), sources = listOf(ClientSource("myOrg", "myClient")),
            itemLineage = listOf(),
            metadata = UnitTestUtils.simpleMetadata
        )
        val org =
            DeepOrganization(
                name = "myOrg",
                description = "blah blah",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                receivers = listOf(
                    Receiver("myService", "myOrg", Topic.TEST, CustomerStatus.INACTIVE, "schema")
                )
            )
        val orgReceiver = org.receivers[0]
        val actionHistory1 = ActionHistory(TaskAction.receive)
        val blobInfo1 = BlobAccess.BlobInfo(Report.Format.CSV, "myUrl", byteArrayOf(0x11, 0x22))
        actionHistory1.trackCreatedReport(event1, report1, orgReceiver, blobInfo1)

        assertThat(actionHistory1.reportsOut[report1.id]).isNotNull()
        val reportFile = actionHistory1.reportsOut[report1.id]!!
        assertThat(reportFile.schemaName).isEqualTo("schema1")
        assertThat(reportFile.schemaTopic).isEqualTo(Topic.TEST)
        assertThat(reportFile.receivingOrg).isEqualTo("myOrg")
        assertThat(reportFile.receivingOrgSvc).isEqualTo("myService")
        assertThat(reportFile.bodyUrl).isEqualTo("myUrl")
        assertThat(reportFile.blobDigest[1]).isEqualTo(34)
        assertThat(reportFile.sendingOrg).isNull()
        assertThat(reportFile.itemCount).isEqualTo(0)

        // not allowed to track the same report twice.
        assertThat { actionHistory1.trackCreatedReport(event1, report1, orgReceiver, blobInfo1) }.isFailure()
    }

    @Test
    fun `test trackCreatedReport (no receiver parameter)`() {
        val event1 = ReportEvent(Event.EventAction.TRANSLATE, UUID.randomUUID(), false, OffsetDateTime.now())
        val schema1 = Schema(name = "schema1", topic = Topic.TEST, elements = listOf())
        val receiver = Receiver(
            "myService",
            "myOrg",
            Topic.TEST,
            CustomerStatus.INACTIVE,
            "CO",
            Report.Format.CSV,
            null,
            null,
            null
        )
        val report1 = Report(
            schema1, listOf(), sources = listOf(ClientSource("myOrg", "myClient")),
            destination = receiver,
            itemLineage = listOf(),
            metadata = UnitTestUtils.simpleMetadata
        )
        val actionHistory1 = ActionHistory(TaskAction.receive)
        val blobInfo1 = BlobAccess.BlobInfo(Report.Format.CSV, "myUrl", byteArrayOf(0x11, 0x22))
        actionHistory1.trackCreatedReport(event1, report1, blobInfo1)

        assertThat(actionHistory1.reportsOut[report1.id]).isNotNull()
        val reportFile = actionHistory1.reportsOut[report1.id]!!
        assertThat(reportFile.schemaName).isEqualTo("schema1")
        assertThat(reportFile.schemaTopic).isEqualTo(Topic.TEST)
        assertThat(reportFile.receivingOrg).isEqualTo("myOrg")
        assertThat(reportFile.receivingOrgSvc).isEqualTo("myService")
        assertThat(reportFile.bodyUrl).isEqualTo("myUrl")
        assertThat(reportFile.blobDigest[1]).isEqualTo(34)
        assertThat(reportFile.sendingOrg).isNull()
        assertThat(reportFile.itemCount).isEqualTo(0)

        // not allowed to track the same report twice.
        assertThat { actionHistory1.trackCreatedReport(event1, report1, blobInfo1) }.isFailure()
    }

    @Test
    fun `test trackCreatedReport (no receiver parameter, null receiver and blob)`() {
        val event1 = ReportEvent(Event.EventAction.TRANSLATE, UUID.randomUUID(), false, OffsetDateTime.now())
        val schema1 = Schema(name = "schema1", topic = Topic.TEST, elements = listOf())
        val report1 = Report(
            schema1, listOf(), sources = listOf(ClientSource("myOrg", "myClient")),
            destination = null,
            itemLineage = listOf(),
            metadata = UnitTestUtils.simpleMetadata
        )
        val actionHistory1 = ActionHistory(TaskAction.receive)
        actionHistory1.trackCreatedReport(event1, report1, null)

        assertThat(actionHistory1.reportsOut[report1.id]).isNotNull()
        val reportFile = actionHistory1.reportsOut[report1.id]!!
        assertThat(reportFile.schemaName).isEqualTo("schema1")
        assertThat(reportFile.schemaTopic).isEqualTo(Topic.TEST)
        assertThat(reportFile.receivingOrg).isNull()
        assertThat(reportFile.receivingOrgSvc).isNull()
        assertThat(reportFile.bodyUrl).isNull()
        assertThat(reportFile.blobDigest).isNull()
        assertThat(reportFile.sendingOrg).isNull()
        assertThat(reportFile.itemCount).isEqualTo(0)

        // not allowed to track the same report twice.
        assertThat { actionHistory1.trackCreatedReport(event1, report1, null) }.isFailure()
    }

    @Test
    fun `test trackExistingInputReport`() {
        val uuid = UUID.randomUUID()
        val actionHistory1 = ActionHistory(TaskAction.send)
        actionHistory1.trackExistingInputReport(uuid)
        assertThat(actionHistory1.reportsIn[uuid]).isNotNull()
        val reportFile = actionHistory1.reportsIn[uuid]!!
        assertThat(reportFile.schemaName).isNull()
        assertThat(reportFile.schemaTopic).isNull()
        assertThat(reportFile.receivingOrg).isNull()
        assertThat(reportFile.receivingOrgSvc).isNull()
        assertThat(reportFile.sendingOrg).isNull()
        assertThat(null).isEqualTo(reportFile.itemCount)
        // not allowed to track the same report twice.
        assertThat { actionHistory1.trackExistingInputReport(uuid) }.isFailure()
    }

    @Test
    fun `test trackSentReport`() {
        val uuid = UUID.randomUUID()
        val org =
            DeepOrganization(
                name = "myOrg",
                description = "blah blah",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                receivers = listOf(
                    Receiver(
                        "myService", "myOrg", Topic.TEST, CustomerStatus.INACTIVE, "schema1",
                        format = Report.Format.CSV
                    )
                )
            )
        val orgReceiver = org.receivers[0]
        val actionHistory1 = ActionHistory(TaskAction.receive)
        actionHistory1.trackSentReport(orgReceiver, uuid, "filename1", "params1", "result1", 15)
        assertThat(actionHistory1.reportsOut[uuid]).isNotNull()
        val reportFile = actionHistory1.reportsOut[uuid]!!
        assertThat(reportFile.schemaName).isEqualTo("schema1")
        assertThat(reportFile.schemaTopic).isEqualTo(Topic.TEST)
        assertThat(reportFile.receivingOrg).isEqualTo("myOrg")
        assertThat(reportFile.externalName).isEqualTo("filename1")
        assertThat(reportFile.transportParams).isEqualTo("params1")
        assertThat(reportFile.transportResult).isEqualTo("result1")
        assertThat(reportFile.receivingOrgSvc).isEqualTo("myService")
        assertThat(reportFile.bodyFormat).isEqualTo("CSV")
        assertThat(reportFile.sendingOrg).isNull()
        assertThat(reportFile.bodyUrl).isNull()
        assertThat(reportFile.blobDigest).isNull()
        assertThat(reportFile.itemCount).isEqualTo(15)
        assertThat(actionHistory1.action.externalName).isEqualTo("filename1")
        // not allowed to track the same report twice.
        assertThat {
            actionHistory1.trackSentReport(
                orgReceiver, uuid, "filename1", "params1", "result1", 15
            )
        }.isFailure()
    }

    @Test
    fun `test trackDownloadedReport`() {
        val metadata = UnitTestUtils.simpleMetadata
        val workflowEngine = mockkClass(WorkflowEngine::class)
        every { workflowEngine.metadata }.returns(metadata)
        val uuid = UUID.randomUUID()
        val reportFile1 = ReportFile()
        reportFile1.reportId = uuid
        reportFile1.receivingOrg = "myOrg"
        reportFile1.receivingOrgSvc = "myRcvr"
        // As of this writing, lineage is taken from the parent report obj, not the org/receiver obj.
        val org =
            DeepOrganization(
                name = "orgX",
                description = "blah blah",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                receivers = listOf(
                    Receiver(
                        "receiverX", "myOrg", Topic.TEST, CustomerStatus.INACTIVE, "schema",
                        format = Report.Format.HL7
                    )
                )
            )
        val schema = Schema("schema", Topic.TEST)
        val header = WorkflowEngine.Header(
            Task(), reportFile1, null, org, org.receivers[0], schema, "".toByteArray(), true
        )
        val actionHistory1 = ActionHistory(TaskAction.download)
        val uuid2 = UUID.randomUUID()
        actionHistory1.trackDownloadedReport(header, "filename1", uuid2, "bob")
        assertThat(actionHistory1.reportsOut[uuid2]).isNotNull()
        val reportFile2 = actionHistory1.reportsOut[uuid2]!!
        assertThat(reportFile2.receivingOrgSvc).isEqualTo("myRcvr")
        assertThat(reportFile2.receivingOrg).isEqualTo("myOrg")
        assertThat(reportFile2.externalName).isEqualTo("filename1")
        assertThat(reportFile2.downloadedBy).isEqualTo("bob")
        assertThat(reportFile2.sendingOrg).isNull()
        assertThat(reportFile2.bodyUrl).isNull()
        assertThat(reportFile2.blobDigest).isNull()
        assertThat(actionHistory1.action.externalName).isEqualTo("filename1")
        // not allowed to track the same report twice.
        assertThat {
            actionHistory1.trackDownloadedReport(
                header, "filename1", uuid2, "bob"
            )
        }.isFailure()
    }

    @Test
    fun `test setActionId`() {
        val metadata = UnitTestUtils.simpleMetadata
        val workflowEngine = mockkClass(WorkflowEngine::class)
        every { workflowEngine.metadata }.returns(metadata)
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val uuid3 = UUID.randomUUID()
        val reportFile1 = ReportFile()
        reportFile1.reportId = uuid1
        reportFile1.receivingOrg = "myOrg"
        reportFile1.receivingOrgSvc = "myRcvr"

        val reportFile2 = ReportFile()
        reportFile2.reportId = uuid2
        reportFile2.receivingOrg = "myOrg"
        reportFile2.receivingOrgSvc = "myRcvr"

        val reportFile3 = ReportFile()
        reportFile3.reportId = uuid3
        reportFile3.receivingOrg = "myOrg"
        reportFile3.receivingOrgSvc = "myRcvr"

        val actionHistory = ActionHistory(TaskAction.process)
        actionHistory.reportsReceived[uuid1] = reportFile1
        actionHistory.reportsOut[uuid2] = reportFile2
        actionHistory.filteredOutReports[uuid3] = reportFile3

        actionHistory.setActionId(12345)

        assertThat { actionHistory.action.actionId.equals(12345) }
        assertThat { actionHistory.reportsReceived.values.all { it.actionId.equals(12345) } }
        assertThat { actionHistory.reportsOut.values.all { it.actionId.equals(12345) } }
        assertThat { actionHistory.filteredOutReports.values.all { it.actionId.equals(12345) } }
    }

    @Test
    fun `test generateLineages - generateEmptyReport`() {
        val metadata = UnitTestUtils.simpleMetadata
        val workflowEngine = mockkClass(WorkflowEngine::class)
        every { workflowEngine.metadata }.returns(metadata)
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val reportFile1 = ReportFile()
        reportFile1.reportId = uuid1
        reportFile1.receivingOrg = "myOrg"
        reportFile1.receivingOrgSvc = "myRcvr"

        val reportFile2 = ReportFile()
        reportFile2.reportId = uuid2
        reportFile2.receivingOrg = "myOrg"
        reportFile2.receivingOrgSvc = "myRcvr"

        val actionHistory = ActionHistory(TaskAction.process, true)
        actionHistory.reportsIn[uuid1] = reportFile1
        actionHistory.reportsOut[uuid2] = reportFile2

        actionHistory.generateLineages()

        assertThat { actionHistory.reportLineages.size == 1 }
    }

    @Test
    fun `test generateLineages - no generateEmptyReport`() {
        val metadata = UnitTestUtils.simpleMetadata
        val workflowEngine = mockkClass(WorkflowEngine::class)
        every { workflowEngine.metadata }.returns(metadata)
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val reportFile1 = ReportFile()
        reportFile1.reportId = uuid1
        reportFile1.receivingOrg = "myOrg"
        reportFile1.receivingOrgSvc = "myRcvr"

        val reportFile2 = ReportFile()
        reportFile2.reportId = uuid2
        reportFile2.receivingOrg = "myOrg"
        reportFile2.receivingOrgSvc = "myRcvr"

        val actionHistory = spyk(ActionHistory(TaskAction.process))
        actionHistory.reportsReceived[uuid1] = reportFile1
        actionHistory.reportsOut[uuid2] = reportFile2
        actionHistory.setActionId(12345)

        every { actionHistory.generateReportLineagesUsingItemLineage(any()) } returns Unit

        actionHistory.generateLineages()

        verify(exactly = 1) { actionHistory.generateReportLineagesUsingItemLineage(any()) }
    }

    @Test
    fun `test nullifyReportIds`() {
        val metadata = UnitTestUtils.simpleMetadata
        val workflowEngine = mockkClass(WorkflowEngine::class)
        every { workflowEngine.metadata }.returns(metadata)
        val uuid1 = UUID.randomUUID()

        val actionLogDetail = InvalidHL7Message("Test Message")

        val actionLog = ActionLog(
            actionLogDetail,
            null,
            null,
            reportId = uuid1,
            action = null,
            type = ActionLogLevel.filter,
        )

        val actionHistory = spyk(ActionHistory(TaskAction.process))
        actionHistory.actionLogs.add(actionLog)

        actionHistory.nullifyReportIdsForNonTrackedReports()

        assertThat { actionHistory.actionLogs.all { it.reportId == null } }
    }
}