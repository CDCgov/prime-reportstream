package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import org.jooq.DSLContext
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Disabled
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ActionHistoryTests {
    @Test
    fun `test constructor`() {
        val actionHistory = ActionHistory(TaskAction.batch)
        assertThat(actionHistory.action.actionName).isEqualTo(TaskAction.batch)
    }

    @Test
    fun `test trackActionResult`() {
        val actionHistory1 = ActionHistory(TaskAction.batch)
        actionHistory1.trackActionResult("foobar")
        assertThat(actionHistory1.action.actionResult).isEqualTo("foobar")
        val giantStr = "x".repeat(3000)
        actionHistory1.trackActionResult(giantStr)
        assertThat(actionHistory1.action.actionResult.length == 2048).isTrue()
    }

    @Test
    fun `test trackExternalInputReport`() {
        val one = Schema(name = "one", topic = "test", elements = listOf())
        val report1 = Report(
            one, listOf(),
            sources = listOf(ClientSource("myOrg", "myClient")),
            metadata = Metadata()
        )
        val actionHistory1 = ActionHistory(TaskAction.receive)
        val blobInfo1 = BlobAccess.BlobInfo(Report.Format.CSV, "myUrl", byteArrayOf(0x11, 0x22))
        actionHistory1.trackExternalInputReport(report1, blobInfo1)
        assertNotNull(actionHistory1.reportsReceived[report1.id])
        val reportFile = actionHistory1.reportsReceived[report1.id] !!
        assertThat(reportFile.schemaName).isEqualTo("one")
        assertThat(reportFile.schemaTopic).isEqualTo("test")
        assertThat(reportFile.sendingOrg).isEqualTo("myOrg")
        assertThat(reportFile.sendingOrgClient).isEqualTo("myClient")
        assertThat(reportFile.bodyUrl).isEqualTo("myUrl")
        assertThat(reportFile.blobDigest[1]).isEqualTo(34)
        assertThat(reportFile.receivingOrg).isNull()

        // not allowed to track the same report twice.
        assertThat { actionHistory1.trackExternalInputReport(report1, blobInfo1) }.isFailure()
    }

    @Test
    fun `test trackCreatedReport`() {
        val event1 = ReportEvent(Event.EventAction.TRANSLATE, UUID.randomUUID(), OffsetDateTime.now())
        val schema1 = Schema(name = "schema1", topic = "topic1", elements = listOf())
        val report1 = Report(
            schema1, listOf(), sources = listOf(ClientSource("myOrg", "myClient")),
            itemLineage = listOf(),
            metadata = Metadata()
        )
        val org =
            DeepOrganization(
                name = "myOrg",
                description = "blah blah",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                receivers = listOf(
                    Receiver("myService", "myOrg", "topic", CustomerStatus.INACTIVE, "schema")
                )
            )
        val orgReceiver = org.receivers[0]
        val actionHistory1 = ActionHistory(TaskAction.receive)
        val blobInfo1 = BlobAccess.BlobInfo(Report.Format.CSV, "myUrl", byteArrayOf(0x11, 0x22))
        actionHistory1.trackCreatedReport(event1, report1, orgReceiver, blobInfo1)

        assertThat(actionHistory1.reportsOut[report1.id]).isNotNull()
        val reportFile = actionHistory1.reportsOut[report1.id] !!
        assertThat(reportFile.schemaName).isEqualTo("schema1")
        assertThat(reportFile.schemaTopic).isEqualTo("topic1")
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
    fun `test trackExistingInputReport`() {
        val uuid = UUID.randomUUID()
        val actionHistory1 = ActionHistory(TaskAction.send)
        actionHistory1.trackExistingInputReport(uuid)
        assertThat(actionHistory1.reportsIn[uuid]).isNotNull()
        val reportFile = actionHistory1.reportsIn[uuid] !!
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
                        "myService", "myOrg", "topic1", CustomerStatus.INACTIVE, "schema1",
                        format = Report.Format.REDOX
                    )
                )
            )
        val orgReceiver = org.receivers[0]
        val actionHistory1 = ActionHistory(TaskAction.receive)
        actionHistory1.trackSentReport(orgReceiver, uuid, "filename1", "params1", "result1", 15)
        assertThat(actionHistory1.reportsOut[uuid]).isNotNull()
        val reportFile = actionHistory1.reportsOut[uuid] !!
        assertThat(reportFile.schemaName).isEqualTo("schema1")
        assertThat(reportFile.schemaTopic).isEqualTo("topic1")
        assertThat(reportFile.receivingOrg).isEqualTo("myOrg")
        assertThat(reportFile.externalName).isEqualTo("filename1")
        assertThat(reportFile.transportParams).isEqualTo("params1")
        assertThat(reportFile.transportResult).isEqualTo("result1")
        assertThat(reportFile.receivingOrgSvc).isEqualTo("myService")
        assertThat(reportFile.bodyFormat).isEqualTo("REDOX")
        assertThat(reportFile.sendingOrg).isNull()
        assertThat(reportFile.bodyUrl).isNull()
        assertThat(reportFile.blobDigest).isNull()
        assertThat(reportFile.itemCount).isEqualTo(15)
        // not allowed to track the same report twice.
        assertThat {
            actionHistory1.trackSentReport(
                orgReceiver, uuid, "filename1", "params1", "result1", 15
            )
        }.isFailure()
    }

    @Test
    fun `test trackDownloadedReport`() {
        val metadata = Metadata.getInstance()
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
                        "receiverX", "myOrg", "topic", CustomerStatus.INACTIVE, "schema",
                        format = Report.Format.HL7
                    )
                )
            )
        val schema = Schema("schema", "topic")
        val header = WorkflowEngine.Header(
            Task(), reportFile1, null, org, org.receivers[0], schema, "".toByteArray()
        )
        val actionHistory1 = ActionHistory(TaskAction.download)
        val uuid2 = UUID.randomUUID()
        actionHistory1.trackDownloadedReport(header, "filename1", uuid2, "bob")
        assertThat(actionHistory1.reportsOut[uuid2]).isNotNull()
        val reportFile2 = actionHistory1.reportsOut[uuid2] !!
        assertThat(reportFile2.receivingOrgSvc).isEqualTo("myRcvr")
        assertThat(reportFile2.receivingOrg).isEqualTo("myOrg")
        assertThat(reportFile2.externalName).isEqualTo("filename1")
        assertThat(reportFile2.downloadedBy).isEqualTo("bob")
        assertThat(reportFile2.sendingOrg).isNull()
        assertThat(reportFile2.bodyUrl).isNull()
        assertThat(reportFile2.blobDigest).isNull()
        // not allowed to track the same report twice.
        assertThat {
            actionHistory1.trackDownloadedReport(
                header, "filename1", uuid2, "bob"
            )
        }.isFailure()
    }

    /**
     * todo Figure out how to make this test work.
     * What I'd really like to do is confirm that two sql inserts were generated,
     * one to insert into ACTION and one to insert into REPORT_FILE.
     */
    @Test @Disabled
    fun `test saveToDb with an externally received report`() {
        val dataProvider = MockDataProvider { emptyArray<MockResult>() }
        val connection = MockConnection(dataProvider) as DSLContext // ? why won't this work?
        val mockDb = spyk(DatabaseAccess(connection))

        val one = Schema(name = "schema1", topic = "topic1", elements = listOf())
        val report1 = Report(
            one,
            listOf(),
            sources = listOf(ClientSource("myOrg", "myClient")),
            metadata = Metadata()
        )

        val actionHistory1 = ActionHistory(TaskAction.receive)
        val blobInfo1 = BlobAccess.BlobInfo(Report.Format.CSV, "myUrl", byteArrayOf(0x11, 0x22))
        actionHistory1.trackExternalInputReport(report1, blobInfo1)

        mockDb.transact { txn -> actionHistory1.saveToDb(txn) }
    }

    @Test
    fun `test prettyPrintDestinations`() {
        val org0 =
            DeepOrganization(
                name = "org0",
                description = "foo bar",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                receivers = listOf(
                    Receiver(
                        "service0", "org0", "topic", CustomerStatus.INACTIVE, "schema",
                        format = Report.Format.REDOX
                    )
                )
            )
        val org1 =
            DeepOrganization(
                name = "org1",
                description = "blah blah",
                jurisdiction = Organization.Jurisdiction.FEDERAL,
                receivers = listOf(
                    Receiver("service1", "org1", "topic", CustomerStatus.INACTIVE, "schema", format = Report.Format.HL7)
                )
            )
        val settings = FileSettings().loadOrganizationList(listOf(org0, org1))
        val actionHistory = ActionHistory(TaskAction.batch)
        val r0 = ReportFile()
        r0.reportId = UUID.randomUUID()
        r0.receivingOrg = org0.name
        r0.receivingOrgSvc = org0.receivers[0].name
        r0.itemCount = 17
        actionHistory.reportsOut[r0.reportId] = r0
        val r1 = ReportFile()
        r1.reportId = UUID.randomUUID()
        r1.receivingOrg = org1.name
        r1.receivingOrgSvc = org1.receivers[0].name
        r1.nextActionAt = OffsetDateTime.now()
        r1.itemCount = 1
        actionHistory.reportsOut[r1.reportId] = r1

        val factory = JsonFactory()
        var outStream = ByteArrayOutputStream()
        factory.createGenerator(outStream).use {
            it.writeStartObject()
            // Finally, we're ready to run the test:
            actionHistory.prettyPrintDestinationsJson(it, settings, ReportFunction.Options.None)
            it.writeEndObject()
        }

        // Expecting this json:
        // {"destinations":[
        //   {"organization":"foo bar","organization_id":"org0",
        //    "service":"service0","sending_at":"never","itemCount":17},
        //   {"organization":"blah blah","organization_id":"org1","service":"service1",
        //    "sending_at":"2021-02-13T17:30:33.847022-05:00","itemCount":1}],
        //   "destinationCount":2}

        val json = outStream.toString()
        assertTrue { json.isNotEmpty() }
        val tree: JsonNode? = jacksonObjectMapper().readTree(json)
        assertNotNull(tree)
        assertTrue(tree["destinationCount"].isInt)
        assertThat(tree["destinationCount"].intValue()).isEqualTo(2)
        val arr = tree["destinations"] as ArrayNode
        assertThat(arr.size()).isEqualTo(2)

        assertThat(arr[0]["organization"].textValue()).isEqualTo("foo bar")
        assertThat(arr[0]["sending_at"].textValue()).isEqualTo("immediately")
        assertThat(arr[0]["itemCount"].intValue()).isEqualTo(17)

        assertThat(arr[1]["organization_id"].textValue()).isEqualTo("org1")
        assertThat(arr[1]["service"].textValue()).isEqualTo("service1")
        assertThat(arr[1]["itemCount"].intValue()).isEqualTo(1)

        // Another test, this time add a 3rd ReportFile with same org as the one of the others.
        val r2 = ReportFile(r1)
        r2.reportId = UUID.randomUUID()
        actionHistory.reportsOut[r2.reportId] = r2
        outStream = ByteArrayOutputStream()
        factory.createGenerator(outStream).use {
            it.writeStartObject()
            actionHistory.prettyPrintDestinationsJson(it, settings, ReportFunction.Options.None)
            it.writeEndObject()
        }
        val json2 = outStream.toString()
        assertTrue { json2.isNotEmpty() }
        val tree2: JsonNode? = jacksonObjectMapper().readTree(json2)
        assertNotNull(tree2)
        assertThat(tree2["destinationCount"].intValue()).isEqualTo(2)
        val arr2 = tree2["destinations"] as ArrayNode
        assertThat(arr2.size()).isEqualTo(2) // still 2 destinations, even with 3 ReportFile
        assertThat(arr2[1]["itemCount"].intValue()).isEqualTo(2) // second destination now has 2 items instead of 1.

        // Another test, test report option SkipSend
        outStream = ByteArrayOutputStream()
        factory.createGenerator(outStream).use {
            it.writeStartObject()
            actionHistory.prettyPrintDestinationsJson(it, settings, ReportFunction.Options.SkipSend)
            it.writeEndObject()
        }
        val json3 = outStream.toString()
        val tree3: JsonNode? = jacksonObjectMapper().readTree(json3)
        val arr3 = tree3?.get("destinations") as ArrayNode?
        assertThat(arr3?.get(0)?.get("sending_at")?.textValue() ?: "").isEqualTo(
            "never - skipSend specified"
        )
    }
}