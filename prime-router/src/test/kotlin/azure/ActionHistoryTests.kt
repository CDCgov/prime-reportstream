package gov.cdc.prime.router.azure

import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.azure.db.enums.TaskAction
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
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
        val actionHistory = ActionHistory("batch")
        assertEquals(actionHistory.action.actionName, TaskAction.batch)
    }

    @Test
    fun `test constructor with bad data`() {
        assertFails { ActionHistory("foobar") }
    }

    @Test
    fun `test trackActionParams`() {
        // todo need to figure out how to test this.
    }

    @Test
    fun `test trackActionResult`() {
        val actionHistory1 = ActionHistory("batch")
        actionHistory1.trackActionResult("foobar")
        assertEquals(actionHistory1.action.actionResult, "foobar")
        val giantStr = "x".repeat(3000)
        actionHistory1.trackActionResult(giantStr)
        assertTrue { actionHistory1.action.actionResult.length == 2048 }
    }

    @Test
    fun `test trackExternalIncomingReport`() {
        val one = Schema(name = "one", topic = "test", elements = listOf())
        val report1 = Report(one, listOf(), sources = listOf(ClientSource("myOrg", "myClient")))
        val incomingReport = ReportFunction.ValidatedRequest(
            ReportFunction.Options.CheckConnections, mapOf(),
            listOf<ResultDetail>(),
            listOf<ResultDetail>(), report1
        )
        val actionHistory1 = ActionHistory("receive")
        actionHistory1.trackExternalIncomingReport(incomingReport)
        assertNotNull(actionHistory1.reportsReceived[report1.id])
        val reportFile = actionHistory1.reportsReceived[report1.id] !!
        assertEquals(reportFile.schemaName, "one")
        assertEquals(reportFile.schemaTopic, "test")
        assertEquals(reportFile.sendingOrg, "myOrg")
        assertEquals(reportFile.sendingOrgClient, "myClient")
        assertNull(reportFile.receivingOrg)

        // not allowed to track the same report twice.
        assertFails { actionHistory1.trackExternalIncomingReport(incomingReport) }

        // must pass a valid report.   Here, its set to null.
        val incomingReport2 = ReportFunction.ValidatedRequest(
            ReportFunction.Options.CheckConnections, mapOf(),
            listOf<ResultDetail>(),
            listOf<ResultDetail>(), null
        )
        assertFails { actionHistory1.trackExternalIncomingReport(incomingReport2) }
    }

    @Test
    fun `test trackCreatedReport`() {
        val event1 = ReportEvent(Event.EventAction.TRANSLATE, UUID.randomUUID(), OffsetDateTime.now())
        val schema1 = Schema(name = "schema1", topic = "topic1", elements = listOf())
        val report1 = Report(schema1, listOf(), sources = listOf(ClientSource("myOrg", "myClient")))
        val valReq1 = ReportFunction.ValidatedRequest(
            ReportFunction.Options.CheckConnections, mapOf(),
            listOf<ResultDetail>(),
            listOf<ResultDetail>(), report1
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
        val actionHistory1 = ActionHistory("receive")

        actionHistory1.trackCreatedReport(event1, report1, orgSvc, valReq1)

        assertNotNull(actionHistory1.reportsOut[report1.id])
        val reportFile = actionHistory1.reportsOut[report1.id] !!
        assertEquals(reportFile.schemaName, "schema1")
        assertEquals(reportFile.schemaTopic, "topic1")
        assertEquals(reportFile.receivingOrg, "myOrg")
        assertEquals(reportFile.receivingOrgSvc, "myService")
        assertNull(reportFile.sendingOrg)
        assertEquals(reportFile.itemCount, 0)

        // not allowed to track the same report twice.
        assertFails { actionHistory1.trackCreatedReport(event1, report1, orgSvc, valReq1) }
    }

    /**
     * todo This is a very weak test.   What I'd really like to do is confirm that two sql inserts were generated,
     * one to insert into ACTION and one to insert into REPORT_FILE.
     */
    @Test
    fun `test saveToDb with an externally received report`() {
        val dataProvider = MockDataProvider { emptyArray<MockResult>() }
        val connection = MockConnection(dataProvider)
        val db = spyk(DatabaseAccess(connection))

        val one = Schema(name = "schema1", topic = "topic1", elements = listOf())
        val report1 = Report(one, listOf(), sources = listOf(ClientSource("myOrg", "myClient")))
        val incomingReport = ReportFunction.ValidatedRequest(
            ReportFunction.Options.None, mapOf(),
            listOf<ResultDetail>(),
            listOf<ResultDetail>(), report1
        )
        val actionHistory1 = ActionHistory("receive")
        actionHistory1.trackExternalIncomingReport(incomingReport)

        every { db.transact(block = any()) }.returns(Unit)

        actionHistory1.saveToDb(db)

        verify(exactly = 1) {
            db.transact(block = any())
        }
        confirmVerified(db)
    }
}