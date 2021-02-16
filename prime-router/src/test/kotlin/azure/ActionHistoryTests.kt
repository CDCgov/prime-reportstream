package gov.cdc.prime.router.azure

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import io.mockk.spyk
import io.mockk.verify
import org.jooq.DSLContext
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import java.io.ByteArrayOutputStream
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
    fun `test trackActionParams`() {
        // todo need to figure out how to test this.
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
    fun `test trackExternalIncomingReport`() {
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

    @Test
    fun `test prettyPrintDestinations`() {
        val org0 =
            Organization(
                name = "org0",
                description = "foo bar",
                clients = listOf(),
                services = listOf(
                    OrganizationService("service0", "topic", "schema", format = Report.Format.REDOX)
                )
            )
        val org1 =
            Organization(
                name = "org1",
                description = "blah blah",
                clients = listOf(),
                services = listOf(
                    OrganizationService("service1", "topic", "schema", format = Report.Format.HL7)
                )
            )
        val metadata = Metadata().loadOrganizationList(listOf(org0, org1))
        val actionHistory = ActionHistory(TaskAction.batch)
        val r0 = ReportFile()
        r0.reportId = UUID.randomUUID()
        r0.receivingOrg = org0.name
        r0.receivingOrgSvc = org0.services[0].name
        r0.itemCount = 17
        actionHistory.reportsOut[r0.reportId] = r0
        val r1 = ReportFile()
        r1.reportId = UUID.randomUUID()
        r1.receivingOrg = org1.name
        r1.receivingOrgSvc = org1.services[0].name
        r1.nextActionAt = OffsetDateTime.now()
        r1.itemCount = 1
        actionHistory.reportsOut[r1.reportId] = r1

        val factory = JsonFactory()
        var outStream = ByteArrayOutputStream()
        factory.createGenerator(outStream).use {
            it.writeStartObject()
            // Finally, we're ready to run the test:
            actionHistory.prettyPrintDestinationsJson(it, metadata)
            it.writeEndObject()
        }

        // Expecting this json:
        // {"destinations":[
        //   {"organization":"foo bar","organization_id":"org0",
        //    "service":"service0","sending_at":"immediately","itemCount":17},
        //   {"organization":"blah blah","organization_id":"org1","service":"service1",
        //    "sending_at":"2021-02-13T17:30:33.847022-05:00","itemCount":1}],
        //   "destinationCount":2}

        val json = outStream.toString()
        assertTrue { json.isNotEmpty() }
        var tree: JsonNode? = jacksonObjectMapper().readTree(json)
        assertNotNull(tree)
        assertTrue(tree["destinationCount"].isInt)
        assertEquals(2, tree["destinationCount"].intValue())
        val arr = tree["destinations"] as ArrayNode
        assertEquals(2, arr.size())

        assertEquals("foo bar", arr[0]["organization"].textValue())
        assertEquals("immediately", arr[0]["sending_at"].textValue())
        assertEquals(17, arr[0]["itemCount"].intValue())

        assertEquals("org1", arr[1]["organization_id"].textValue())
        assertEquals("service1", arr[1]["service"].textValue())
        assertEquals(1, arr[1]["itemCount"].intValue())

        // Another test, this time add a 3rd ReportFile with same org as the one of the others.
        val r2 = ReportFile(r1)
        r2.reportId = UUID.randomUUID()
        actionHistory.reportsOut[r2.reportId] = r2
        outStream = ByteArrayOutputStream()
        factory.createGenerator(outStream).use {
            it.writeStartObject()
            actionHistory.prettyPrintDestinationsJson(it, metadata)
            it.writeEndObject()
        }
        val json2 = outStream.toString()
        assertTrue { json2.isNotEmpty() }
        var tree2: JsonNode? = jacksonObjectMapper().readTree(json2)
        assertNotNull(tree2)
        assertEquals(2, tree2["destinationCount"].intValue()) // still 2 destinations, even with 3 ReportFile
        val arr2 = tree2["destinations"] as ArrayNode
        assertEquals(2, arr2.size()) // still 2 destinations, even with 3 ReportFile
        assertEquals(2, arr2[1]["itemCount"].intValue()) // second destination now has 2 items instead of 1.
    }
}