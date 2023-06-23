package gov.cdc.prime.router.history

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
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
        fun createLogs(logs: List<DetailedActionLog>): DetailedSubmissionHistory {
            return DetailedSubmissionHistory(
                1, TaskAction.receive, OffsetDateTime.now(),
                null, null, logs
            )
        }

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
            OffsetDateTime.now()
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(sendingOrg).isEqualTo("")
            assertThat(httpStatus).isNull()
            assertThat(externalName).isEqualTo("")
            assertThat(reportId).isNull()
            assertThat(topic).isNull()
            assertThat(reportItemCount).isNull()
        }
        SubmissionHistory(
            1,
            OffsetDateTime.now(),
            "",
            null,
            null,
            null,
            "simple_report",
            201
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(sendingOrg).isEqualTo("simple_report")
            assertThat(httpStatus).isEqualTo(201)
            assertThat(externalName).isEqualTo("")
            assertThat(reportId).isNull()
            assertThat(topic).isNull()
            assertThat(reportItemCount).isNull()
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
        ).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(createdAt).isNotNull()
            assertThat(sendingOrg).isEqualTo("simple_report")
            assertThat(httpStatus).isEqualTo(201)
            assertThat(externalName).isEqualTo("testname.csv")
            assertThat(reportId).isEqualTo("a2cf1c46-7689-4819-98de-520b5007e45f")
            assertThat(topic).isEqualTo(Topic.COVID_19)
            assertThat(reportItemCount).isEqualTo(3)
        }
    }

    @Test
    fun `test DetailedSubmissionHistory common properties init`() {
        DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
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
            assertThat(reports?.size).isEqualTo(0)
            assertThat(logs.size).isEqualTo(0)
        }
        DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            201,
            null,
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
            assertThat(reports).isNull()
            assertThat(logs.size).isEqualTo(0)
        }

        DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            null,
            null,
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
            false
        )

        val refUUID = UUID.randomUUID()

        var reports = listOf(
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
                true
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
                true
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
                true
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
                    ReportStreamFilterType.QUALITY_FILTER
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

        reports = listOf(inputReport, inputReport).toMutableList()

        assertThat {
            DetailedSubmissionHistory(
                1,
                TaskAction.receive,
                OffsetDateTime.now(),
                null,
                reports,
                emptyList()
            )
        }.isFailure()
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
            HttpStatus.OK.value(), noDestinationsCalculatedYet
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
            false
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
                true
            ),
        ).toMutableList()
        val testReceivedOneFilteredDestination = DetailedSubmissionHistory(
            1, TaskAction.route, OffsetDateTime.now(),
            HttpStatus.OK.value(), oneFilteredDestinationCalculated
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
                true
            ),
        ).toMutableList()
        val testReceivedNoDestination = DetailedSubmissionHistory(
            1,
            TaskAction.route,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports,
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
            null,
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
                true
            ),
        ).toMutableList()
        val testReceivedNoDestination = DetailedSubmissionHistory(
            1,
            TaskAction.route,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports,
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
            false
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
            true
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
                true
            ),
        ).toMutableList()
        val testReceivedOneUnfilteredDestination = DetailedSubmissionHistory(
            1, TaskAction.route, OffsetDateTime.now(),
            HttpStatus.OK.value(), oneUnfilteredDestinationCalculated
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
                true
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
                true
            ),
        ).toMutableList()
        val testWaitingToDeliver = DetailedSubmissionHistory(
            1, TaskAction.receive, OffsetDateTime.now(),
            HttpStatus.OK.value(), reports
        )
        testWaitingToDeliver.enrichWithSummary()
        testWaitingToDeliver.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
        }
    }

    @Test
    fun `test DetailedSubmissionHistory overallStatus (partially delivered)`() {
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
            false
        )
        // use cases found while investigating issue #9378
        // partially delivered: one destination with an item, the other got all filtered out
        val twoDestinationsOneItem = listOf(
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
                1,
                3,
                true
            ),
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg3",
                "recvSvc3",
                null,
                null,
                Topic.FULL_ELR,
                "all items filtered out",
                null,
                null,
                0,
                3,
                true
            ),
        ).toMutableList()
        val testPartiallyDeliveredTwoDestinations = DetailedSubmissionHistory(
            1, TaskAction.route, OffsetDateTime.now(),
            HttpStatus.OK.value(), twoDestinationsOneItem
        )
        testPartiallyDeliveredTwoDestinations.enrichWithDescendants(
            listOf(
                DetailedSubmissionHistory(
                    1, TaskAction.send, OffsetDateTime.now(),
                    HttpStatus.OK.value(), twoDestinationsOneItem
                ),
            )
        )
        testPartiallyDeliveredTwoDestinations.enrichWithSummary()
        testPartiallyDeliveredTwoDestinations.run {
            assertThat(destinations.count()).isEqualTo(2)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.PARTIALLY_DELIVERED)
        }
    }

    @Test
    fun `test DetailedSubmissionHistory overallStatus (delivered)`() {
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
            false
        )
        // delivered: one destination with an item, the other got SOME items filtered out
        val twoDestinationsSomeItems = listOf(
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
                1,
                1,
                true
            ),
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg3",
                "recvSvc3",
                null,
                null,
                Topic.FULL_ELR,
                "all items filtered out",
                null,
                null,
                1,
                4,
                true
            ),
        ).toMutableList()
        val testDeliveredTwoDestinationsSomeItems = DetailedSubmissionHistory(
            1, TaskAction.route, OffsetDateTime.now(),
            HttpStatus.OK.value(), twoDestinationsSomeItems
        )
        testDeliveredTwoDestinationsSomeItems.enrichWithDescendants(
            listOf(
                DetailedSubmissionHistory(
                    1, TaskAction.send, OffsetDateTime.now(),
                    HttpStatus.OK.value(), twoDestinationsSomeItems
                ),
            )
        )
        testDeliveredTwoDestinationsSomeItems.enrichWithSummary()
        testDeliveredTwoDestinationsSomeItems.run {
            assertThat(destinations.count()).isEqualTo(2)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.DELIVERED)
        }
        // delivered: all destinations received all items
        val everyDestinationGetsAllItems = listOf(
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
                4,
                4,
                true
            ),
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg3",
                "recvSvc3",
                null,
                null,
                Topic.FULL_ELR,
                "all items filtered out",
                null,
                null,
                3,
                3,
                true
            ),
        ).toMutableList()
        val testDeliveredToAllDestinations = DetailedSubmissionHistory(
            1, TaskAction.route, OffsetDateTime.now(),
            HttpStatus.OK.value(), everyDestinationGetsAllItems
        )
        testDeliveredToAllDestinations.enrichWithDescendants(
            listOf(
                DetailedSubmissionHistory(
                    1, TaskAction.send, OffsetDateTime.now(),
                    HttpStatus.OK.value(), everyDestinationGetsAllItems
                ),
                DetailedSubmissionHistory(
                    2, TaskAction.send, OffsetDateTime.now(),
                    HttpStatus.OK.value(), everyDestinationGetsAllItems
                ),
            )
        )
        testDeliveredToAllDestinations.enrichWithSummary()
        testDeliveredToAllDestinations.run {
            assertThat(destinations.count()).isEqualTo(2)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.DELIVERED)
        }
    }

    @Test
    fun `test DetailedSubmissionHistory UP overallStatus (not delivering)`() {
        // not delivering: one destination, all items filtered out
        val reportsAllItemsFilteredOut = listOf(
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
                5,
                true
            ),
        ).toMutableList()
        val testNotDelivering = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reportsAllItemsFilteredOut,
        )
        testNotDelivering.enrichWithSummary()
        testNotDelivering.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING)
        }
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
                true
            ),
        ).toMutableList()
        val testNotDeliveringNoDestination = DetailedSubmissionHistory(
            1,
            TaskAction.route,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports,
        )
        testNotDeliveringNoDestination.actionsPerformed = mutableSetOf(TaskAction.route)
        testNotDeliveringNoDestination.enrichWithSummary()
        testNotDeliveringNoDestination.run {
            assertThat(destinationCount).isEqualTo(0)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING)
        }
    }

    @Test
    fun `test DetailedSubmissionHistory LEGACY overallStatus calculations (not delivering)`() {
        var reports = listOf(
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg3",
                "recvSvc3",
                null,
                null,
                Topic.TEST,
                "no item count dest",
                null,
                null,
                0,
                null,
                true
            ),
        ).toMutableList()

        val testNotDelivering = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports,
        )
        testNotDelivering.enrichWithSummary()
        testNotDelivering.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }

        reports = listOf(
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
                true
            ),
        ).toMutableList()
        val testNotDeliveringNoDestination = DetailedSubmissionHistory(
            1,
            TaskAction.route,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports,
        )
        testNotDeliveringNoDestination.actionsPerformed = mutableSetOf(TaskAction.route)
        testNotDeliveringNoDestination.enrichWithSummary()
        testNotDeliveringNoDestination.run {
            assertThat(destinationCount).isEqualTo(0)
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }
    }

    @Test
    fun `test Destination nextActionTime`() {
        val inputReport = DetailedReport(
            UUID.randomUUID(),
            null,
            null,
            "org",
            "client",
            Topic.TEST,
            "externalName",
            null,
            OffsetDateTime.now(),
            3,
            null,
            false
        )
        val refUUID = UUID.randomUUID()
        val now = OffsetDateTime.now()
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
                now,
                1,
                1,
                true
            ),
            DetailedReport(
                UUID.randomUUID(),
                "recvOrg3",
                "recvSvc3",
                null,
                null, Topic.TEST,
                "no item count dest",
                null,
                now,
                0,
                null,
                false
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
            // First destination has a transport set therefore sendingAt
            assertThat(destinations.first().sendingAt).isEqualTo(now)
            assertThat(destinations.last().sendingAt).isNull()
        }
    }

    @Test
    fun `test Status enum toString`() {
        assertThat(DetailedSubmissionHistory.Status.RECEIVED.toString()).isEqualTo("Received")
    }

    @Test
    fun `test UP enrichWithDescendants stopped at route`() {
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
            7,
            false
        )

        val refUUID = UUID.randomUUID()

        val reports = listOf(
            inputReport,
            DetailedReport(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                Topic.FULL_ELR,
                "otherExternalName1",
                null,
                null,
                1,
                1,
                true
            ),
        ).toMutableList()

        val logs = listOf(
            DetailedActionLog(
                ActionLogScope.translation,
                refUUID,
                null,
                "802798",
                ActionLogLevel.filter,
                ReportStreamFilterResult(
                    "recvOrg1.recvSvc1",
                    1,
                    "matches",
                    listOf(
                        "ordering_facility_county",
                        "QUALITY_PASS"
                    ),
                    "802798",
                    ReportStreamFilterType.QUALITY_FILTER
                )
            ),
        )

        val testEnrich = DetailedSubmissionHistory(
            2,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports
        )
        assertThat(testEnrich.destinations.count()).isEqualTo(0)
        testEnrich.enrichWithDescendants(
            listOf(
                DetailedSubmissionHistory(
                    1,
                    TaskAction.route,
                    OffsetDateTime.now(),
                    HttpStatus.OK.value(),
                    null,
                    logs
                ),
            )
        )

        testEnrich.run {
            assertThat(destinations.count()).isEqualTo(1)
            assertThat(destinations.first().organizationId).isEqualTo("recvOrg1")
            assertThat(destinations.first().service).isEqualTo("recvSvc1")
        }
    }

    @Test
    fun `test UP enrichWithDescendants reached translate`() {
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
            7,
            false
        )

        val reports = listOf(
            inputReport,
            DetailedReport(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                Topic.FULL_ELR,
                "otherExternalName1",
                null,
                null,
                1,
                1,
                true
            ),
        ).toMutableList()

        val testEnrich = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports
        )
        assertThat(testEnrich.destinations.count()).isEqualTo(0)
        testEnrich.enrichWithDescendants(
            listOf(
                DetailedSubmissionHistory(
                    2,
                    TaskAction.route,
                    OffsetDateTime.now(),
                    HttpStatus.OK.value(),
                ),
                DetailedSubmissionHistory(
                    3,
                    TaskAction.translate,
                    OffsetDateTime.now(),
                    HttpStatus.OK.value(),
                    mutableListOf(
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
                            1,
                            true
                        )
                    )
                ),
            )
        )

        testEnrich.run {
            assertThat(destinations.count()).isEqualTo(1)
            assertThat(destinations.first().organizationId).isEqualTo("recvOrg1")
            assertThat(destinations.first().service).isEqualTo("recvSvc1")
        }
    }

    @Test
    fun `test UP enrichWithDescendants reached translate multiple report same receiver`() {
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
            7,
            false
        )

        val reports = listOf(
            inputReport,
            DetailedReport(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                Topic.FULL_ELR,
                "otherExternalName1",
                null,
                null,
                1,
                1,
                true
            ),
        ).toMutableList()

        val testEnrich = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            reports
        )
        assertThat(testEnrich.destinations.count()).isEqualTo(0)
        testEnrich.enrichWithDescendants(
            listOf(
                DetailedSubmissionHistory(
                    2,
                    TaskAction.route,
                    OffsetDateTime.now(),
                    HttpStatus.OK.value(),
                ),
                DetailedSubmissionHistory(
                    3,
                    TaskAction.translate,
                    OffsetDateTime.now(),
                    HttpStatus.OK.value(),
                    mutableListOf(
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
                            true
                        ),
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
                            1,
                            true
                        ),
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
                            true
                        )
                    )
                ),
            )
        )

        testEnrich.run {
            assertThat(destinations.count()).isEqualTo(1)
            assertThat(destinations.first().organizationId).isEqualTo("recvOrg1")
            assertThat(destinations.first().service).isEqualTo("recvSvc1")
            assertThat(destinations.first().itemCount).isEqualTo(3)
            assertThat(destinations.first().itemCountBeforeQualFilter).isEqualTo(1)
        }
    }

    @Test
    fun `test UP enrichWithDescendants reached translate multiple reports different receivers`() {
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
            7,
            false
        )

        val testEnrich = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            mutableListOf(inputReport)
        )
        assertThat(testEnrich.destinations.count()).isEqualTo(0)
        testEnrich.enrichWithDescendants(
            listOf(
                DetailedSubmissionHistory(
                    2,
                    TaskAction.route,
                    OffsetDateTime.now(),
                    HttpStatus.OK.value(),
                ),
                DetailedSubmissionHistory(
                    3,
                    TaskAction.translate,
                    OffsetDateTime.now(),
                    HttpStatus.OK.value(),
                    mutableListOf(
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
                            1,
                            true
                        ),
                        DetailedReport(
                            UUID.randomUUID(),
                            "recvOrg2",
                            "recvSvc2",
                            null,
                            null,
                            Topic.FULL_ELR,
                            "otherExternalName1",
                            null,
                            null,
                            1,
                            1,
                            true
                        )
                    )
                ),
            )
        )

        testEnrich.run {
            assertThat(destinations.count()).isEqualTo(2)
            assertThat(destinations.first().organizationId).isEqualTo("recvOrg1")
            assertThat(destinations.first().service).isEqualTo("recvSvc1")
        }
    }

    @Test
    fun `test UP enrichWithDescendants filterLogs populate for the corresponding receiver`() {
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
            false
        )

        val testEnrich = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            HttpStatus.OK.value(),
            mutableListOf(inputReport)
        )

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
                UUID.randomUUID(),
                2,
                null,
                ActionLogLevel.filter,
                ReportStreamFilterResult(
                    "recvOrg1.recvSvc1",
                    5,
                    "matches",
                    listOf(
                        "ordering_facility_county",
                        "QUALITY_PASS"
                    ),
                    "802798",
                    ReportStreamFilterType.QUALITY_FILTER
                )
            ),
            DetailedActionLog(
                ActionLogScope.translation,
                UUID.randomUUID(),
                2,
                null,
                ActionLogLevel.filter,
                ReportStreamFilterResult(
                    "recvOrg2.recvSvc2",
                    5,
                    "matches",
                    listOf(
                        "ordering_facility_county",
                        "QUALITY_PASS"
                    ),
                    "802798",
                    ReportStreamFilterType.QUALITY_FILTER
                )
            ),
            DetailedActionLog(
                ActionLogScope.translation,
                UUID.randomUUID(),
                2,
                null,
                ActionLogLevel.filter,
                ReportStreamFilterResult(
                    "recvOrg1.recvSvc1",
                    5,
                    "matches",
                    listOf(
                        "ordering_facility_county",
                        "QUALITY_PASS"
                    ),
                    "802798",
                    ReportStreamFilterType.QUALITY_FILTER
                )
            ),
        )

        assertThat(testEnrich.destinations.count()).isEqualTo(0)
        testEnrich.enrichWithDescendants(
            listOf(
                DetailedSubmissionHistory(
                    2,
                    TaskAction.route,
                    OffsetDateTime.now(),
                    HttpStatus.OK.value(),
                    null,
                    logs
                ),
                DetailedSubmissionHistory(
                    3,
                    TaskAction.translate,
                    OffsetDateTime.now(),
                    HttpStatus.OK.value(),
                    mutableListOf(
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
                            true
                        ),
                    )
                )
            )
        )

        testEnrich.run {
            assertThat(destinations.count()).isEqualTo(2)
            assertThat(destinations.first().organizationId).isEqualTo("recvOrg1")
            assertThat(destinations.first().filteredReportItems?.count()).isEqualTo(2)
            assertThat(destinations.first().service).isEqualTo("recvSvc1")
            assertThat(destinations[1].service).isEqualTo("recvSvc2")
            assertThat(destinations[1].filteredReportItems?.count()).isEqualTo(1)
        }
    }
}