package gov.cdc.prime.router.azure

import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.azure.db.tables.pojos.TaskSource
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import org.jooq.DSLContext
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionHistoryTests {
    @Test
    fun `test constructor`() {
        val actionHistory = ActionHistory(TaskAction.batch)
        assertEquals(actionHistory.action.actionName, TaskAction.batch)
    }

    @Test
    fun `test trackActionResult`() {
        val actionHistory1 = ActionHistory(TaskAction.batch)
        actionHistory1.trackActionResult("foobar")
        assertEquals(actionHistory1.action.actionResult, "foobar")
        val giantStr = "x".repeat(3000)
        actionHistory1.trackActionResult(giantStr)
        assertTrue { actionHistory1.action.actionResult.length == 2048 }
    }

    @Test
    fun `test trackExternalInputReport`() {
        val one = Schema(name = "one", topic = "test", elements = listOf())
        val report1 = Report(one, listOf(), sources = listOf(ClientSource("myOrg", "myClient")))
        val incomingReport = ReportFunction.ValidatedRequest(
            ReportFunction.Options.CheckConnections, mapOf(),
            listOf<ResultDetail>(),
            listOf<ResultDetail>(), report1
        )
        val actionHistory1 = ActionHistory(TaskAction.receive)
        actionHistory1.trackExternalInputReport(incomingReport)
        assertNotNull(actionHistory1.reportsReceived[report1.id])
        val reportFile = actionHistory1.reportsReceived[report1.id] !!
        assertEquals(reportFile.schemaName, "one")
        assertEquals(reportFile.schemaTopic, "test")
        assertEquals(reportFile.sendingOrg, "myOrg")
        assertEquals(reportFile.sendingOrgClient, "myClient")
        assertNull(reportFile.receivingOrg)

        // not allowed to track the same report twice.
        assertFails { actionHistory1.trackExternalInputReport(incomingReport) }

        // must pass a valid report.   Here, its set to null.
        val incomingReport2 = ReportFunction.ValidatedRequest(
            ReportFunction.Options.CheckConnections, mapOf(),
            listOf<ResultDetail>(),
            listOf<ResultDetail>(), null
        )
        assertFails { actionHistory1.trackExternalInputReport(incomingReport2) }
    }

    @Test
    fun `test trackCreatedReport`() {
        val event1 = ReportEvent(Event.EventAction.TRANSLATE, UUID.randomUUID(), OffsetDateTime.now())
        val schema1 = Schema(name = "schema1", topic = "topic1", elements = listOf())
        val report1 = Report(
            schema1, listOf(), sources = listOf(ClientSource("myOrg", "myClient")),
            itemLineage = listOf<ItemLineage>()
        )
        val org =
            Organization(
                name = "myOrg",
                description = "blah blah",
                clients = listOf(),
                services = listOf(
                    OrganizationService("myService", "topic", "schema")
                )
            )
        val orgSvc = org.services[0]
        val actionHistory1 = ActionHistory(TaskAction.receive)

        actionHistory1.trackCreatedReport(event1, report1, orgSvc)

        assertNotNull(actionHistory1.reportsOut[report1.id])
        val reportFile = actionHistory1.reportsOut[report1.id] !!
        assertEquals(reportFile.schemaName, "schema1")
        assertEquals(reportFile.schemaTopic, "topic1")
        assertEquals(reportFile.receivingOrg, "myOrg")
        assertEquals(reportFile.receivingOrgSvc, "myService")
        assertNull(reportFile.sendingOrg)
        assertEquals(reportFile.itemCount, 0)

        // not allowed to track the same report twice.
        assertFails { actionHistory1.trackCreatedReport(event1, report1, orgSvc) }
    }

    @Test
    fun `test trackExistingInputReport`() {
        val uuid = UUID.randomUUID()
        val actionHistory1 = ActionHistory(TaskAction.send)
        actionHistory1.trackExistingInputReport(uuid)
        assertNotNull(actionHistory1.reportsIn[uuid])
        val reportFile = actionHistory1.reportsIn[uuid] !!
        assertNull(reportFile.schemaName)
        assertNull(reportFile.schemaTopic)
        assertNull(reportFile.receivingOrg)
        assertNull(reportFile.receivingOrgSvc)
        assertNull(reportFile.sendingOrg)
        assertEquals(null, reportFile.itemCount)
        // not allowed to track the same report twice.
        assertFails { actionHistory1.trackExistingInputReport(uuid) }
    }

    @Test
    fun `test trackSentReport`() {
        val uuid = UUID.randomUUID()
        val org =
            Organization(
                name = "myOrg",
                description = "blah blah",
                clients = listOf(),
                services = listOf(
                    OrganizationService("myService", "topic1", "schema1", format = Report.Format.REDOX)
                )
            )
        val orgSvc = org.services[0]
        val actionHistory1 = ActionHistory(TaskAction.receive)
        actionHistory1.trackSentReport(orgSvc, uuid, "filename1", "params1", "result1", 15)
        assertNotNull(actionHistory1.reportsOut[uuid])
        val reportFile = actionHistory1.reportsOut[uuid] !!
        assertEquals("schema1", reportFile.schemaName)
        assertEquals("topic1", reportFile.schemaTopic)
        assertEquals("myOrg", reportFile.receivingOrg)
        assertEquals("filename1", reportFile.externalName)
        assertEquals("params1", reportFile.transportParams)
        assertEquals("result1", reportFile.transportResult)
        assertEquals("myService", reportFile.receivingOrgSvc)
        assertEquals("REDOX", reportFile.bodyFormat)
        assertNull(reportFile.sendingOrg)
        assertNull(reportFile.bodyUrl)
        assertEquals(15, reportFile.itemCount)
        // not allowed to track the same report twice.
        assertFails { actionHistory1.trackSentReport(orgSvc, uuid, "filename1", "params1", "result1", 15) }
    }

    @Test
    fun `test trackDownloadedReport`() {
        val metadata = Metadata("./metadata", "-local")
        val workflowEngine = mockkClass(WorkflowEngine::class)
        every { workflowEngine.metadata }.returns(metadata)
        val uuid = UUID.randomUUID()
        val reportFile1 = ReportFile()
        reportFile1.reportId = uuid
        val header = DatabaseAccess.Header(Task(), listOf<TaskSource>(), reportFile1, workflowEngine)
        val org =
            Organization(
                name = "myOrg",
                description = "blah blah",
                clients = listOf(),
                services = listOf(
                    OrganizationService("myService", "topic", "schema", format = Report.Format.HL7)
                )
            )
        val actionHistory1 = ActionHistory(TaskAction.download)
        val uuid2 = UUID.randomUUID()
        actionHistory1.trackDownloadedReport(header, "filename1", uuid, uuid2, "bob", org)
        assertNotNull(actionHistory1.reportsOut[uuid2])
        val reportFile2 = actionHistory1.reportsOut[uuid2] !!
        assertEquals("myOrg", reportFile2.receivingOrg)
        assertEquals("filename1", reportFile2.externalName)
        assertEquals("bob", reportFile2.downloadedBy)
        assertNull(reportFile2.sendingOrg)
        assertNull(reportFile2.bodyUrl)
        // not allowed to track the same report twice.
        assertFails { actionHistory1.trackDownloadedReport(header, "filename1", uuid, uuid2, "bob", org) }
    }

    /**
     * todo Figure out how to make this test work.
     * What I'd really like to do is confirm that two sql inserts were generated,
     * one to insert into ACTION and one to insert into REPORT_FILE.
     */
//    @Test
    fun `test saveToDb with an externally received report`() {
        val dataProvider = MockDataProvider { emptyArray<MockResult>() }
        val connection = MockConnection(dataProvider) as DSLContext // ? why won't this work?
        val mockDb = spyk(DatabaseAccess(connection))

        val one = Schema(name = "schema1", topic = "topic1", elements = listOf())
        val report1 = Report(one, listOf(), sources = listOf(ClientSource("myOrg", "myClient")))
        val incomingReport = ReportFunction.ValidatedRequest(
            ReportFunction.Options.None, mapOf(),
            listOf<ResultDetail>(),
            listOf<ResultDetail>(), report1
        )
        val actionHistory1 = ActionHistory(TaskAction.receive)
        actionHistory1.trackExternalInputReport(incomingReport)

        // Not sure how to get a transaction obj, to pass to saveToDb. ?
//        every { connection.transaction(any()) }.returns(Unit)

        mockDb.transact { txn -> actionHistory1.saveToDb(txn) }

        verify(exactly = 1) {
//            connection.transaction(block = any() as TransactionalRunnable)
        }
//        confirmVerified(connection)
    }
}