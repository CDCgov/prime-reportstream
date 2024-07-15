package gov.cdc.prime.router.azure

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.extracting
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.microsoft.azure.functions.HttpMethod
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.InvalidHL7Message
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotEquals
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
    fun `test filterParameters`() {
        val actionHistory = ActionHistory(TaskAction.receive)
        val parameters = mapOf(
            "code" to "code1",
            "test" to "test1"
        )
        val headers = mapOf(
            "key1" to "key1",
            "cookie" to "cookie1",
            "auth-test" to "auth1",
            "client" to "sender1"
        )
        val httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.httpHeaders += headers
        httpRequestMessage.parameters += parameters

        val actionParams = actionHistory.filterParameters(httpRequestMessage)

        val testActionParams = """
            {"method":"GET","url":"http://localhost/","headers":{"client":"sender1"},"queryParameters":{"test":"test1"}} 
        """.trim()

        assertThat(JacksonMapperUtilities.objectMapper.writeValueAsString(actionParams)).isEqualTo(
            testActionParams
        )
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
        val blobInfo1 = BlobAccess.BlobInfo(MimeFormat.CSV, "myUrl", byteArrayOf(0x11, 0x22))
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
        assertFailure { actionHistory1.trackExternalInputReport(report1, blobInfo1) }
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
        val blobInfo1 = BlobAccess.BlobInfo(MimeFormat.CSV, "myUrl", byteArrayOf(0x11, 0x22))
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
        val blobInfo1 = BlobAccess.BlobInfo(MimeFormat.CSV, "myUrl", byteArrayOf(0x11, 0x22))
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
        assertFailure { actionHistory1.trackCreatedReport(event1, report1, orgReceiver, blobInfo1) }
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
            MimeFormat.CSV,
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
        val blobInfo1 = BlobAccess.BlobInfo(MimeFormat.CSV, "myUrl", byteArrayOf(0x11, 0x22))
        actionHistory1.trackCreatedReport(event1, report1, blobInfo = blobInfo1)

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
        assertFailure { actionHistory1.trackCreatedReport(event1, report1, blobInfo = blobInfo1) }
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
        actionHistory1.trackCreatedReport(event1, report1)

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
        assertFailure { actionHistory1.trackCreatedReport(event1, report1) }
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
        assertFailure { actionHistory1.trackExistingInputReport(uuid) }
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
                        format = MimeFormat.CSV
                    )
                )
            )
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.uploadBody(any(), any(), any(), any(), Event.EventAction.NONE) } returns BlobAccess.BlobInfo(
            MimeFormat.HL7,
            "http://blobUrl",
            "".toByteArray()
        )
        val header = mockk<WorkflowEngine.Header>()
        val inReportFile = mockk<ReportFile>()
        every { header.reportFile } returns inReportFile
        every { header.content } returns "".toByteArray()
        every { inReportFile.itemCount } returns 15
        every { inReportFile.reportId } returns uuid
        val orgReceiver = org.receivers[0]
        val actionHistory1 = ActionHistory(TaskAction.receive)
        actionHistory1.action
        actionHistory1.trackSentReport(orgReceiver, uuid, "filename1", "params1", "result1", header)
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
        assertThat(reportFile.bodyUrl).isEqualTo("http://blobUrl")
        assertThat(reportFile.blobDigest).isEqualTo("".toByteArray())
        assertThat(reportFile.itemCount).isEqualTo(15)
        assertThat(actionHistory1.action.externalName).isEqualTo("filename1")
        // not allowed to track the same report twice.
        assertFailure {
            actionHistory1.trackSentReport(
                orgReceiver, uuid, "filename1", "params1", "result1", header
            )
        }
    }

    @Test
    fun `test trackSentReport very long schema name`() {
        val uuid = UUID.randomUUID()
        val longNameWithClasspath =
            "classpath:/metadata/hl7_mapping/receivers/STLTs/REALLY_LONG_STATE_NAME/REALLY_LONG_STATE_NAME.yml"
        val longNameWithoutClasspath =
            "metadata/hl7_mapping/receivers/NESTED/NESTED/STLTs/REALLY_LONG_STATE_NAME/REALLY_LONG_STATE_NAME"
        val org =
            DeepOrganization(
                name = "myOrg",
                description = "blah blah",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                receivers = listOf(
                    Receiver(
                        "myService", "myOrg", Topic.TEST, CustomerStatus.INACTIVE,
                        longNameWithClasspath,
                        format = MimeFormat.CSV
                    ),
                    Receiver(
                        "myServiceToo", "myOrg", Topic.TEST, CustomerStatus.INACTIVE,
                        longNameWithoutClasspath,
                        format = MimeFormat.CSV
                    )
                )
            )
        mockkObject(BlobAccess.Companion)
        val blobUrls = mutableListOf<String>()
        every { BlobAccess.uploadBlob(capture(blobUrls), any()) } returns "http://blobUrl"
        every { BlobAccess.sha256Digest(any()) } returns byteArrayOf()
        every { BlobAccess.uploadBody(any(), any(), any(), any(), Event.EventAction.NONE) } answers { callOriginal() }
        val header = mockk<WorkflowEngine.Header>()
        val inReportFile = mockk<ReportFile>()
        every { header.reportFile } returns inReportFile
        every { header.content } returns "".toByteArray()
        every { inReportFile.itemCount } returns 15
        every { inReportFile.reportId } returns uuid
        val actionHistory1 = ActionHistory(TaskAction.receive)

        actionHistory1.trackSentReport(org.receivers[0], uuid, "filename1", "params1", "result1", header)
        assertThat(actionHistory1.reportsOut[uuid]).isNotNull()
        assertThat(actionHistory1.reportsOut[uuid]?.schemaName)
            .isEqualTo("g/receivers/STLTs/REALLY_LONG_STATE_NAME/REALLY_LONG_STATE_NAME")

        val actionHistory2 = ActionHistory(TaskAction.receive)

        actionHistory2.trackSentReport(org.receivers[1], uuid, "filename1", "params1", "result1", header)
        assertThat(actionHistory2.reportsOut[uuid]).isNotNull()
        assertThat(actionHistory2.reportsOut[uuid]?.schemaName)
            .isEqualTo("STED/NESTED/STLTs/REALLY_LONG_STATE_NAME/REALLY_LONG_STATE_NAME")
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
        reportFile1.externalName = "externalName1"
        val actionHistory1 = ActionHistory(TaskAction.download)
        val uuid2 = UUID.randomUUID()
        actionHistory1.trackDownloadedReport(reportFile1, uuid2, "bob")
        assertThat(actionHistory1.reportsOut[uuid2]).isNotNull()
        val reportFile2 = actionHistory1.reportsOut[uuid2]!!
        assertThat(reportFile2.receivingOrgSvc).isEqualTo("myRcvr")
        assertThat(reportFile2.receivingOrg).isEqualTo("myOrg")
        assertThat(reportFile2.externalName).isEqualTo("externalName1")
        assertThat(reportFile2.downloadedBy).isEqualTo("bob")
        assertThat(reportFile2.sendingOrg).isNull()
        assertThat(reportFile2.bodyUrl).isNull()
        assertThat(reportFile2.blobDigest).isNull()
        assertThat(actionHistory1.action.externalName).isEqualTo("externalName1")
        // not allowed to track the same report twice.
        assertFailure {
            actionHistory1.trackDownloadedReport(
                reportFile1, uuid2, "bob"
            )
        }
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

        assertThat(actionHistory.action.actionId).isEqualTo(12345)
        assertThat(actionHistory.reportsReceived.values).extracting { it.actionId }.containsOnly(12345L)
        assertThat(actionHistory.reportsOut.values).extracting { it.actionId }.containsOnly(12345L)
        assertThat(actionHistory.filteredOutReports.values).extracting { it.actionId }.containsOnly(12345L)
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

        assertThat(actionHistory.reportLineages.size).isEqualTo(1)
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

        assertThat(actionHistory.actionLogs).extracting { it.reportId }.containsOnly(null)
    }

    @Test
    fun `test trackSentReportSameFilenameDifferentReceiver`() {
        val uuid = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val org =
            DeepOrganization(
                name = "myOrg",
                description = "blah blah",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                receivers = listOf(
                    Receiver(
                        "myService", "myOrg", Topic.TEST, CustomerStatus.INACTIVE, "schema1",
                        format = MimeFormat.CSV
                    ),
                    Receiver(
                        "myServiceToo", "myOrg", Topic.TEST, CustomerStatus.INACTIVE, "schema1",
                        format = MimeFormat.CSV
                    )
                )
            )
        mockkObject(BlobAccess.Companion)
        val blobUrls = mutableListOf<String>()
        every { BlobAccess.uploadBlob(capture(blobUrls), any()) } returns "http://blobUrl"
        every { BlobAccess.sha256Digest(any()) } returns byteArrayOf()
        every { BlobAccess.uploadBody(any(), any(), any(), any(), Event.EventAction.NONE) } answers { callOriginal() }
        val header = mockk<WorkflowEngine.Header>()
        val inReportFile = mockk<ReportFile>()
        every { header.reportFile } returns inReportFile
        every { header.content } returns "".toByteArray()
        every { inReportFile.itemCount } returns 15
        every { inReportFile.reportId } returns uuid
        val actionHistory1 = ActionHistory(TaskAction.receive)
        actionHistory1.action
        actionHistory1.trackSentReport(org.receivers[0], uuid, "filename1", "params1", "result1", header)
        assertThat(actionHistory1.reportsOut[uuid]).isNotNull()
        val actionHistory2 = ActionHistory(TaskAction.receive)
        actionHistory2.action
        actionHistory2.trackSentReport(org.receivers[1], uuid2, "filename1", "params1", "result1", header)
        assertThat(actionHistory2.reportsOut[uuid2]).isNotNull()
        assertNotEquals(blobUrls[0], blobUrls[1])
        assertContains(blobUrls[0], org.receivers[0].fullName)
        assertContains(blobUrls[1], org.receivers[1].fullName)
    }

    @Test
    fun `test trimSchemaNameToMaxLength malformed URI`() {
        val longMalformedURI = ":very_very:_long_name//with a badly formed URI that causes a parse exception"
        val trimmed = ActionHistory.trimSchemaNameToMaxLength((longMalformedURI))
        assertThat(trimmed).isEqualTo("ong_name//with a badly formed URI that causes a parse exception")
    }

    @Test
    fun `test trackActionParams with invalid ip`() {
        val actionHistory = ActionHistory(TaskAction.receive)

        val mockHttpRequestMessage = MockHttpRequestMessage()
        mockHttpRequestMessage.httpHeaders["x-azure-clientip"] = "I'm invalid"

        actionHistory.trackActionParams(mockHttpRequestMessage)
        assertThat(actionHistory.action.senderIp).isNull()
    }

    @Test
    fun `test trackActionParams uses the first ip in forwarded ips`() {
        val actionHistory = ActionHistory(TaskAction.receive)

        val mockHttpRequestMessage = MockHttpRequestMessage()
        mockHttpRequestMessage.httpHeaders["x-azure-clientip"] = "127.0.0.3"
        mockHttpRequestMessage.httpHeaders["x-forwarded-for"] = "127.0.0.1,127.0.0.2"

        actionHistory.trackActionParams(mockHttpRequestMessage)
        assertThat(actionHistory.action.senderIp).isEqualTo("127.0.0.1")
    }

    @Test
    fun `trackActionParams the azure client ip`() {
        val actionHistory = ActionHistory(TaskAction.receive)

        val mockHttpRequestMessage = MockHttpRequestMessage()
        mockHttpRequestMessage.httpHeaders["x-azure-clientip"] = "127.0.0.3"

        actionHistory.trackActionParams(mockHttpRequestMessage)
        assertThat(actionHistory.action.senderIp).isEqualTo("127.0.0.3")
    }

    @Test
    fun `trackActionParams correctly filters headers and query params`() {
        val actionHistory = ActionHistory(TaskAction.receive)

        val mockHttpRequestMessage = MockHttpRequestMessage(method = HttpMethod.POST)
        mockHttpRequestMessage.httpHeaders["connection"] = "keep-alive"
        mockHttpRequestMessage.httpHeaders["cookie"] = "cookie"
        mockHttpRequestMessage.httpHeaders["key"] = "key"
        mockHttpRequestMessage.httpHeaders["auth"] = "auth"
        mockHttpRequestMessage.queryParameters["code"] = "code"
        mockHttpRequestMessage.queryParameters["processing"] = "async"
        mockHttpRequestMessage.httpHeaders["content-length"] = "825489"

        actionHistory.trackActionParams(mockHttpRequestMessage)
        assertThat(actionHistory.action.actionParams).isEqualTo(
            JacksonMapperUtilities.objectMapper.writeValueAsString(
                ActionHistory.ReceivedReportSenderParameters(
                    HttpMethod.POST,
                    "http://localhost/",
                    mapOf("connection" to "keep-alive", "content-length" to "825489"),
                    mapOf("processing" to "async")
                )
            )
        )
    }
}