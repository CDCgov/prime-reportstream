package gov.cdc.prime.router.history

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import assertk.assertions.isTrue
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.FieldPrecisionMessage
import gov.cdc.prime.router.InvalidEquipmentMessage
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.InvalidTranslationMessage
import gov.cdc.prime.router.MissingFieldMessage
import gov.cdc.prime.router.ReportStreamFilterResult
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

class SubmissionHistoryTests {
    @Test
    fun `tests consolidation of logs`() {
        fun createLogs(logs: List<DetailedActionLog>): DetailedSubmissionHistory = DetailedSubmissionHistory(
                1, TaskAction.receive, OffsetDateTime.now(),
                null, mutableListOf(), logs
            )

        val messageM = "message 1"
        val messageA = "A message 2"
        val messageZ = "zz message"
        val itemLog1 = InvalidEquipmentMessage("mapping")
        val itemLog2 = FieldPrecisionMessage("mapping", messageZ)
        val itemLog3 = MissingFieldMessage("mapping")
        val logMessages = listOf(
            DetailedActionLog(
                ActionLogScope.report, null, null, null,
                ActionLogLevel.warning, InvalidReportMessage(messageM)
            ),
            DetailedActionLog(
                ActionLogScope.report, null, null, null,
                ActionLogLevel.error, InvalidReportMessage(messageA)
            ),
            DetailedActionLog(
                ActionLogScope.item, null, 1, null,
                ActionLogLevel.error, itemLog1
            ),
            DetailedActionLog(
                ActionLogScope.item, null, 3, null,
                ActionLogLevel.error, itemLog2
            ),
            DetailedActionLog(
                ActionLogScope.item, null, 2, null,
                ActionLogLevel.error, itemLog3
            ),
            DetailedActionLog(
                ActionLogScope.item, null, 2, "id2",
                ActionLogLevel.error, itemLog1
            ),
            DetailedActionLog(
                ActionLogScope.item, null, 3, "id3",
                ActionLogLevel.error, itemLog1
            ),
            DetailedActionLog(
                ActionLogScope.item, null, 4, null,
                ActionLogLevel.warning, itemLog1
            ),
            DetailedActionLog(
                ActionLogScope.item, null, 5, null,
                ActionLogLevel.warning, itemLog2
            ),
            DetailedActionLog(
                ActionLogScope.item, null, 9, "id9",
                ActionLogLevel.warning, itemLog3
            )
        )

        var rawLogs = emptyList<DetailedActionLog>()
        assertThat(createLogs(rawLogs).consolidateLogs()).isEmpty()

        // Test sorting by message
        rawLogs = mutableListOf(logMessages[0], logMessages[1])
        var result = createLogs(rawLogs).consolidateLogs()
        assertThat(result.size).isEqualTo(2)
        assertThat(result[0].message).isEqualTo(rawLogs[1].detail.message)
        assertThat(result[0].message).isEqualTo(messageA)
        result.forEach {
            assertThat(it.indices).isNull()
            assertThat(it.trackingIds).isNull()
        }

        // Test that item logs are at the end
        rawLogs.add(0, logMessages[2])
        result = createLogs(rawLogs).consolidateLogs()
        assertThat(result.size).isEqualTo(3)
        assertThat(result[2].indices).isNotNull()
        assertThat(result[2].indices!!.size).isEqualTo(1)
        assertThat(result[2].indices!![0]).isNotNull()
        assertThat(result[2].trackingIds).isNotNull()
        assertThat(result[2].trackingIds!!.size).isEqualTo(1)
        assertThat(result[2].trackingIds!![0]).isNull()

        // Now test multiple item logs
        rawLogs.add(logMessages[3])
        rawLogs.add(logMessages[4])
        result = createLogs(rawLogs).consolidateLogs()
        assertThat(result.size).isEqualTo(5)
        // Check the ordering of the item messages by index.  Note this is very specific to the data in this test
        assertThat(result[2].indices).isNotNull()
        assertThat(result[2].indices!!.size).isEqualTo(1)
        assertThat(result[2].indices!![0]).isEqualTo(1) // Index number
        assertThat(result[3].indices).isNotNull()
        assertThat(result[3].indices!!.size).isEqualTo(1)
        assertThat(result[3].indices!![0]).isEqualTo(2) // Index number
        assertThat(result[4].indices).isNotNull()
        assertThat(result[4].indices!!.size).isEqualTo(1)
        assertThat(result[4].indices!![0]).isEqualTo(3) // Index number

        // Now test consolidation of indices and tracking IDs
        rawLogs.add(logMessages[5])
        rawLogs.add(logMessages[6])
        assertThat(result[2].message).isEqualTo(itemLog1.message)
        assertThat(result[2].indices).isNotNull()
        assertThat(result[2].indices!!.size).isEqualTo(1)
        result = createLogs(rawLogs).consolidateLogs()
        assertThat(result.size).isEqualTo(5)
        assertThat(result[2].indices!!.size).isEqualTo(3)

        // Errors vs warnings
        rawLogs.add(logMessages[7])
        rawLogs.add(logMessages[8])
        val logs = createLogs(rawLogs)
        assertThat(logs.errors.size).isEqualTo(4)
        assertThat(logs.warnings.size).isEqualTo(3)
        // And add an item warning that looks the same as a previous error
        rawLogs.add(logMessages[9])
        assertThat(createLogs(rawLogs).warnings.size).isEqualTo(4)
    }

    @Test
    fun `test check for if log can be consolidated`() {
        // Same
        assertThat(
            ConsolidatedActionLog(
                DetailedActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailedActionLog(
                    ActionLogScope.item, null, 2, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            )
        ).isTrue()

        // Different log level
        assertThat(
            ConsolidatedActionLog(
                DetailedActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailedActionLog(
                    ActionLogScope.item, null, 2, null,
                    ActionLogLevel.warning, InvalidEquipmentMessage("mapping")
                )
            )
        ).isFalse()

        // Different message
        assertThat(
            ConsolidatedActionLog(
                DetailedActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailedActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, MissingFieldMessage("mapping")
                )
            )
        ).isFalse()

        // Different message and log level
        assertThat(
            ConsolidatedActionLog(
                DetailedActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailedActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.warning, MissingFieldMessage("mapping")
                )
            )
        ).isFalse()

        // Different scope
        assertThat(
            ConsolidatedActionLog(
                DetailedActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailedActionLog(
                    ActionLogScope.report, null, null, null,
                    ActionLogLevel.error, InvalidReportMessage("mapping")
                )
            )
        ).isFalse()

        // Different scope, log level and message
        assertThat(
            ConsolidatedActionLog(
                DetailedActionLog(
                    ActionLogScope.translation, null, null, null,
                    ActionLogLevel.error, InvalidTranslationMessage("some other message")
                )
            ).canBeConsolidatedWith(
                DetailedActionLog(
                    ActionLogScope.report, null, null, null,
                    ActionLogLevel.warning, InvalidReportMessage("mapping")
                )
            )
        ).isFalse()

        ConsolidatedActionLog(
            DetailedActionLog(
                ActionLogScope.item, null, 1, null,
                ActionLogLevel.error, InvalidEquipmentMessage("mapping")
            )
        ).run {
            assertThat(scope).isEqualTo(ActionLogScope.item)
            assertThat(type).isEqualTo(ActionLogLevel.error)
            assertThat(field).isNotNull()
        }
    }

    @Test
    fun `test SubmissionHistory init`() {
        SubmissionHistory(
            1,
            OffsetDateTime.now(),
            null,
            null,
            null,
            null,
            "",
            null,
            "",
            null,
            "",
            "",
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(sendingOrg).isEqualTo("")
            assertThat(httpStatus).isNull()
            assertThat(externalName).isEqualTo(null)
            assertThat(reportId).isNull()
            assertThat(topic).isNull()
            assertThat(reportItemCount).isNull()
            assertThat(bodyUrl).isNull()
            assertThat(schemaName).isEqualTo("")
            assertThat(bodyFormat).isEqualTo("")
        }
        SubmissionHistory(
            1,
            OffsetDateTime.now(),
            "",
            null,
            null,
            null,
            "simple_report",
            201,
            "",
            "http://anyblob.com",
            "test-schema",
            "CSV"
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(sendingOrg).isEqualTo("simple_report")
            assertThat(httpStatus).isEqualTo(201)
            assertThat(externalName).isEqualTo("")
            assertThat(reportId).isNull()
            assertThat(topic).isNull()
            assertThat(reportItemCount).isNull()
            assertThat(bodyUrl).isEqualTo("http://anyblob.com")
            assertThat(schemaName).isEqualTo("test-schema")
            assertThat(bodyFormat).isEqualTo("CSV")
        }
        SubmissionHistory(
            1,
            OffsetDateTime.now(),
            "testname.csv",
            "a2cf1c46-7689-4819-98de-520b5007e45f",
            Topic.COVID_19,
            3,
            "simple_report",
            201,
            "",
            "http://anyblob.com",
            "test-schema",
            "CSV"
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(sendingOrg).isEqualTo("simple_report")
            assertThat(httpStatus).isEqualTo(201)
            assertThat(externalName).isEqualTo("testname.csv")
            assertThat(reportId).isEqualTo("a2cf1c46-7689-4819-98de-520b5007e45f")
            assertThat(topic).isEqualTo(Topic.COVID_19)
            assertThat(reportItemCount).isEqualTo(3)
            assertThat(bodyUrl).isEqualTo("http://anyblob.com")
            assertThat(schemaName).isEqualTo("test-schema")
            assertThat(bodyFormat).isEqualTo("CSV")
        }

        SubmissionHistory(
            1,
            OffsetDateTime.now(),
            "testname.csv",
            "a2cf1c46-7689-4819-98de-520b5007e45f",
            Topic.COVID_19,
            3,
            "simple_report",
            201,
            "",
            null,
            "test-schema",
            "CSV"
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(sendingOrg).isEqualTo("simple_report")
            assertThat(httpStatus).isEqualTo(201)
            assertThat(externalName).isEqualTo("testname.csv")
            assertThat(reportId).isEqualTo("a2cf1c46-7689-4819-98de-520b5007e45f")
            assertThat(topic).isEqualTo(Topic.COVID_19)
            assertThat(reportItemCount).isEqualTo(3)
            assertThat(bodyUrl).isEqualTo(null)
            assertThat(schemaName).isEqualTo("test-schema")
            assertThat(bodyFormat).isEqualTo("CSV")
        }
    }

    @Test
    fun `test DetailedSubmissionHistory common properties init`() {
        DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            reports = mutableListOf(),
            logs = emptyList()
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(reportId).isNull()
            assertThat(httpStatus).isNull()
            assertThat(sender).isNullOrEmpty()
            assertThat(topic).isNull()
            assertThat(reportItemCount).isNull()
            assertThat(externalName).isEqualTo("")
            assertThat(destinations.size).isEqualTo(0)
            assertThat(destinationCount).isEqualTo(0)
            assertThat(reports.size).isEqualTo(0)
            assertThat(logs.size).isEqualTo(0)
        }
        DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            201,
            mutableListOf(),
            emptyList(),
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(reportId).isNull()
            assertThat(httpStatus).isEqualTo(201)
            assertThat(sender).isNullOrEmpty()
            assertThat(topic).isNull()
            assertThat(reportItemCount).isNull()
            assertThat(externalName).isEqualTo("")
            assertThat(destinations.size).isEqualTo(0)
            assertThat(destinationCount).isEqualTo(0)
            assertThat(reports).isEmpty()
            assertThat(logs.size).isEqualTo(0)
        }

        DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            null,
            mutableListOf(),
            emptyList(),
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(reportId).isNull()
            assertThat(httpStatus).isNull()
            assertThat(sender).isNullOrEmpty()
            assertThat(topic).isNull()
            assertThat(reportItemCount).isNull()
            assertThat(externalName).isEqualTo("")
            assertThat(destinations.size).isEqualTo(0)
            assertThat(destinationCount).isEqualTo(0)
            assertThat(logs.size).isEqualTo(0)
        }

        val inputReport = DetailedReport(
            UUID.randomUUID(),
            null,
            null,
            "org",
            "client",
            Topic.TEST,
            "externalName",
            null,
            null,
            5,
            7,
            false,
            null,
            null,
            null
        )

        val refUUID = UUID.randomUUID()

        val reports = listOf(
            inputReport,
            DetailedReport(
                refUUID, "recvOrg1",
                "recvSvc1",
                null,
                null,
                Topic.TEST,
                "otherExternalName1",
                null,
                null,
                1,
                1,
                true,
                null,
                null,
                null
            ),
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg2",
                "recvSvc2",
                null,
                null,
                Topic.TEST,
                "otherExternalName2",
                null,
                null,
                2,
                null,
                true,
                null,
                null,
                null
            ),
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg3",
                "recvSvc3",
                null,
                null, Topic.TEST,
                "no item count dest",
                null,
                null,
                0,
                null,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()

        DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            201,
            reports,
            emptyList(),
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(reportId).isEqualTo(inputReport.reportId.toString())
            assertThat(httpStatus).isEqualTo(201)
            assertThat(sender).isEqualTo(ClientSource(inputReport.sendingOrg!!, inputReport.sendingOrgClient!!).name)
            assertThat(topic).isEqualTo(inputReport.schemaTopic)
            assertThat(reportItemCount).isEqualTo(inputReport.itemCount)
            assertThat(externalName).isEqualTo(inputReport.externalName)
            assertThat(destinations.size).isEqualTo(3)
            assertThat(destinationCount).isEqualTo(2)
            // assertThat(destinations.first().organization).isNull()
            assertThat(destinations.first().itemCountBeforeQualFilter).isEqualTo(1)
        }

        val logs = listOf(
            DetailedActionLog(
                ActionLogScope.item,
                UUID.randomUUID(),
                1,
                null,
                ActionLogLevel.error,
                InvalidEquipmentMessage("")
            ),
            DetailedActionLog(
                ActionLogScope.translation,
                refUUID,
                2,
                null,
                ActionLogLevel.filter,
                ReportStreamFilterResult(
                    "ignore.QUALITY_PASS",
                    5,
                    "matches",
                    listOf(
                        "ordering_facility_county",
                        "QUALITY_PASS"
                    ),
                    "802798",
                    ReportStreamFilterType.QUALITY_FILTER.name
                )
            ),
        )

        DetailedSubmissionHistory(1, TaskAction.receive, OffsetDateTime.now(), null, reports, logs).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(reportId).isEqualTo(null)
            assertThat(sender).isEqualTo(ClientSource(inputReport.sendingOrg!!, inputReport.sendingOrgClient!!).name)
            assertThat(topic).isEqualTo(inputReport.schemaTopic)
            assertThat(reportItemCount).isEqualTo(inputReport.itemCount)
            assertThat(externalName).isEqualTo(inputReport.externalName)
            assertThat(destinations.size).isEqualTo(3)
            assertThat(destinationCount).isEqualTo(2)

            assertThat(logs).isNotNull()
        }
    }

    // Status calculation tests
    // Legacy and Universal pipeline have some differences in how they calculate overallStatus
    // The places where the logic is different should have separate test cases for each pipeline
    @Test
    fun `test DetailedSubmissionHistory overallStatus (error)`() {
        // error: general submission error
        val testError = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            reports = mutableListOf(),
            logs = emptyList()
        )
        testError.enrichWithSummary()
        testError.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.ERROR)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }
    }

    @Test
    fun `test DetailedSubmissionHistory UP overallStatus (received)`() {
        // received: freshly received, no routing yet
        val testReceived = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports = mutableListOf(),
            logs = emptyList()
        )
        testReceived.actionsPerformed = mutableSetOf(TaskAction.receive)
        testReceived.enrichWithSummary()
        testReceived.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }
        // received: no destinations have been calculated yet
        val noDestinationsCalculatedYet = emptyList<DetailedReport>().toMutableList()
        val testReceivedButNoDestinationsYet = DetailedSubmissionHistory(
            1, TaskAction.destination_filter, OffsetDateTime.now(),
            HttpStatus.OK.value(), noDestinationsCalculatedYet, logs = emptyList()
        )
        testReceivedButNoDestinationsYet.enrichWithSummary()
        testReceivedButNoDestinationsYet.run {
            assertThat(destinations.count()).isEqualTo(0)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }
        val inputReport = DetailedReport(
            UUID.randomUUID(),
            null,
            null,
            "org",
            "client",
            Topic.FULL_ELR,
            "externalName",
            null,
            null,
            5,
            null,
            false,
            null,
            null,
            null
        )
        // received: one of two destinations has been calculated, with all items for it filtered out
        val oneFilteredDestinationCalculated = listOf(
            inputReport,
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg4",
                "recvSvc4",
                null,
                null,
                Topic.FULL_ELR,
                "one item dest",
                null,
                OffsetDateTime.now().plusDays(1),
                0,
                5,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testReceivedOneFilteredDestination = DetailedSubmissionHistory(
            1, TaskAction.destination_filter, OffsetDateTime.now(),
            HttpStatus.OK.value(), oneFilteredDestinationCalculated, logs = emptyList()
        )
        testReceivedOneFilteredDestination.enrichWithSummary()
        testReceivedOneFilteredDestination.run {
            assertThat(destinations.count()).isEqualTo(1)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }
        // received: one of two destinations has been calculated, with no items filtered out
        val reports = listOf(
            DetailedReport(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                Topic.FULL_ELR,
                "no item count dest",
                null,
                null,
                0,
                null,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testReceivedNoDestination = DetailedSubmissionHistory(
            1,
            TaskAction.destination_filter,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports,
            emptyList()
        )
        testReceivedNoDestination.enrichWithSummary()
        testReceivedNoDestination.run {
            assertThat(destinationCount).isEqualTo(0)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }
    }

    @Test
    fun `test DetailedSubmissionHistory UP overallStatus (received) (legacy route step)`() {
        // received: freshly received, no routing yet
        val testReceived = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports = mutableListOf(),
            logs = emptyList()
        )
        testReceived.actionsPerformed = mutableSetOf(TaskAction.receive)
        testReceived.enrichWithSummary()
        testReceived.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }
        // received: no destinations have been calculated yet
        val noDestinationsCalculatedYet = emptyList<DetailedReport>().toMutableList()
        val testReceivedButNoDestinationsYet = DetailedSubmissionHistory(
            1, TaskAction.route, OffsetDateTime.now(),
            HttpStatus.OK.value(), noDestinationsCalculatedYet, logs = emptyList()
        )
        testReceivedButNoDestinationsYet.enrichWithSummary()
        testReceivedButNoDestinationsYet.run {
            assertThat(destinations.count()).isEqualTo(0)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }
        val inputReport = DetailedReport(
            UUID.randomUUID(),
            null,
            null,
            "org",
            "client",
            Topic.FULL_ELR,
            "externalName",
            null,
            null,
            5,
            null,
            false,
            null,
            null,
            null
        )
        // received: one of two destinations has been calculated, with all items for it filtered out
        val oneFilteredDestinationCalculated = listOf(
            inputReport,
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg4",
                "recvSvc4",
                null,
                null,
                Topic.FULL_ELR,
                "one item dest",
                null,
                OffsetDateTime.now().plusDays(1),
                0,
                5,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testReceivedOneFilteredDestination = DetailedSubmissionHistory(
            1, TaskAction.route, OffsetDateTime.now(),
            HttpStatus.OK.value(), oneFilteredDestinationCalculated, logs = emptyList()
        )
        testReceivedOneFilteredDestination.enrichWithSummary()
        testReceivedOneFilteredDestination.run {
            assertThat(destinations.count()).isEqualTo(1)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }
        // received: one of two destinations has been calculated, with no items filtered out
        val reports = listOf(
            DetailedReport(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                Topic.FULL_ELR,
                "no item count dest",
                null,
                null,
                0,
                null,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testReceivedNoDestination = DetailedSubmissionHistory(
            1,
            TaskAction.route,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports,
            emptyList()
        )
        testReceivedNoDestination.enrichWithSummary()
        testReceivedNoDestination.run {
            assertThat(destinationCount).isEqualTo(0)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }
    }

    @Test
    fun `test DetailedSubmissionHistory LEGACY overallStatus calculations (received)`() {
        val testReceived = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports = mutableListOf(),
            logs = emptyList()
        )
        testReceived.actionsPerformed = mutableSetOf(TaskAction.receive)
        testReceived.enrichWithSummary()
        testReceived.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }

        val reports = listOf(
            DetailedReport(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                Topic.TEST,
                "no item count dest",
                null,
                null,
                0,
                null,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testReceivedNoDestination = DetailedSubmissionHistory(
            1,
            TaskAction.destination_filter,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports,
            logs = emptyList()
        )
        testReceivedNoDestination.enrichWithSummary()
        testReceivedNoDestination.run {
            assertThat(destinationCount).isEqualTo(0)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }
    }

    @Test
    fun `test DetailedSubmissionHistory LEGACY overallStatus calculations (received) (legacy route step)`() {
        val testReceived = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports = mutableListOf(),
            logs = emptyList()
        )
        testReceived.actionsPerformed = mutableSetOf(TaskAction.receive)
        testReceived.enrichWithSummary()
        testReceived.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }

        val reports = listOf(
            DetailedReport(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                Topic.TEST,
                "no item count dest",
                null,
                null,
                0,
                null,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testReceivedNoDestination = DetailedSubmissionHistory(
            1,
            TaskAction.route,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports,
            logs = emptyList()
        )
        testReceivedNoDestination.enrichWithSummary()
        testReceivedNoDestination.run {
            assertThat(destinationCount).isEqualTo(0)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }
    }

    @Test
    fun `test DetailedSubmissionHistory overallStatus (waiting to deliver)`() {
        val inputReport = DetailedReport(
            UUID.randomUUID(),
            null,
            null,
            "org",
            "client",
            Topic.FULL_ELR,
            "externalName",
            null,
            null,
            5,
            null,
            false,
            null,
            null,
            null
        )
        val latestReport = DetailedReport(
            UUID.randomUUID(),
            "recvOrg2",
            "recvSvc2",
            null, null,
            Topic.FULL_ELR,
            "otherExternalName2",
            null,
            null,
            4,
            null,
            true,
            null,
            null,
            null
        )
        // waiting to deliver: one of two destinations has been calculated, with no items filtered out
        val oneUnfilteredDestinationCalculated = listOf(
            inputReport,
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg4",
                "recvSvc4",
                null,
                null,
                Topic.FULL_ELR,
                "one item dest",
                null,
                null,
                5,
                5,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testReceivedOneUnfilteredDestination = DetailedSubmissionHistory(
            1, TaskAction.destination_filter, OffsetDateTime.now(),
            HttpStatus.OK.value(), oneUnfilteredDestinationCalculated, logs = emptyList()
        )
        testReceivedOneUnfilteredDestination.enrichWithSummary()
        testReceivedOneUnfilteredDestination.run {
            assertThat(destinations.count()).isEqualTo(1)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
        }
        val reports = listOf(
            inputReport,
            latestReport,
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg1",
                "recvSvc1",
                null,
                null,
                Topic.FULL_ELR,
                "otherExternalName1",
                null,
                null,
                1,
                null,
                true,
                null,
                null,
                null
            ),
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg3",
                "recvSvc3",
                null,
                null,
                Topic.FULL_ELR,
                "no item count dest",
                null,
                null,
                0,
                null,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testWaitingToDeliver = DetailedSubmissionHistory(
            1, TaskAction.receive, OffsetDateTime.now(),
            HttpStatus.OK.value(), reports, logs = emptyList()
        )
        testWaitingToDeliver.enrichWithSummary()
        testWaitingToDeliver.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
        }
    }

    @Test
    fun `test DetailedSubmissionHistory overallStatus (waiting to deliver) (legacy route step)`() {
        val inputReport = DetailedReport(
            UUID.randomUUID(),
            null,
            null,
            "org",
            "client",
            Topic.FULL_ELR,
            "externalName",
            null,
            null,
            5,
            null,
            false,
            null,
            null,
            null
        )
        val latestReport = DetailedReport(
            UUID.randomUUID(),
            "recvOrg2",
            "recvSvc2",
            null, null,
            Topic.FULL_ELR,
            "otherExternalName2",
            null,
            null,
            4,
            null,
            true,
            null,
            null,
            null
        )
        // waiting to deliver: one of two destinations has been calculated, with no items filtered out
        val oneUnfilteredDestinationCalculated = listOf(
            inputReport,
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg4",
                "recvSvc4",
                null,
                null,
                Topic.FULL_ELR,
                "one item dest",
                null,
                null,
                5,
                5,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testReceivedOneUnfilteredDestination = DetailedSubmissionHistory(
            1, TaskAction.route, OffsetDateTime.now(),
            HttpStatus.OK.value(), oneUnfilteredDestinationCalculated, logs = emptyList()
        )
        testReceivedOneUnfilteredDestination.enrichWithSummary()
        testReceivedOneUnfilteredDestination.run {
            assertThat(destinations.count()).isEqualTo(1)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
        }
        val reports = listOf(
            inputReport,
            latestReport,
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg1",
                "recvSvc1",
                null,
                null,
                Topic.FULL_ELR,
                "otherExternalName1",
                null,
                null,
                1,
                null,
                true,
                null,
                null,
                null
            ),
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg3",
                "recvSvc3",
                null,
                null,
                Topic.FULL_ELR,
                "no item count dest",
                null,
                null,
                0,
                null,
                true,
                null,
                null,
                null
            ),
        ).toMutableList()
        val testWaitingToDeliver = DetailedSubmissionHistory(
            1, TaskAction.receive, OffsetDateTime.now(),
            HttpStatus.OK.value(), reports, logs = emptyList()
        )
        testWaitingToDeliver.enrichWithSummary()
        testWaitingToDeliver.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
        }
    }
}