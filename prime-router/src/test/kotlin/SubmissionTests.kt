package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

class SubmissionTests {
    @Test
    fun `tests consolidation of logs`() {
        fun createLogs(logs: List<DetailActionLog>): DetailedSubmissionHistory {
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
            DetailActionLog(
                ActionLogScope.report, null, null, null,
                ActionLogLevel.warning, InvalidReportMessage(messageM)
            ),
            DetailActionLog(
                ActionLogScope.report, null, null, null,
                ActionLogLevel.error, InvalidReportMessage(messageA)
            ),
            DetailActionLog(
                ActionLogScope.item, null, 1, null,
                ActionLogLevel.error, itemLog1
            ),
            DetailActionLog(
                ActionLogScope.item, null, 3, null,
                ActionLogLevel.error, itemLog2
            ),
            DetailActionLog(
                ActionLogScope.item, null, 2, null,
                ActionLogLevel.error, itemLog3
            ),
            DetailActionLog(
                ActionLogScope.item, null, 2, "id2",
                ActionLogLevel.error, itemLog1
            ),
            DetailActionLog(
                ActionLogScope.item, null, 3, "id3",
                ActionLogLevel.error, itemLog1
            ),
            DetailActionLog(
                ActionLogScope.item, null, 4, null,
                ActionLogLevel.warning, itemLog1
            ),
            DetailActionLog(
                ActionLogScope.item, null, 5, null,
                ActionLogLevel.warning, itemLog2
            ),
            DetailActionLog(
                ActionLogScope.item, null, 9, "id9",
                ActionLogLevel.warning, itemLog3
            )
        )

        var rawLogs = emptyList<DetailActionLog>()
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
                DetailActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailActionLog(
                    ActionLogScope.item, null, 2, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            )
        ).isTrue()

        // Different log level
        assertThat(
            ConsolidatedActionLog(
                DetailActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailActionLog(
                    ActionLogScope.item, null, 2, null,
                    ActionLogLevel.warning, InvalidEquipmentMessage("mapping")
                )
            )
        ).isFalse()

        // Different message
        assertThat(
            ConsolidatedActionLog(
                DetailActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, MissingFieldMessage("mapping")
                )
            )
        ).isFalse()

        // Different message and log level
        assertThat(
            ConsolidatedActionLog(
                DetailActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.warning, MissingFieldMessage("mapping")
                )
            )
        ).isFalse()

        // Different scope
        assertThat(
            ConsolidatedActionLog(
                DetailActionLog(
                    ActionLogScope.item, null, 1, null,
                    ActionLogLevel.error, InvalidEquipmentMessage("mapping")
                )
            ).canBeConsolidatedWith(
                DetailActionLog(
                    ActionLogScope.report, null, null, null,
                    ActionLogLevel.error, InvalidReportMessage("mapping")
                )
            )
        ).isFalse()

        // Different scope, log level and message
        assertThat(
            ConsolidatedActionLog(
                DetailActionLog(
                    ActionLogScope.translation, null, null, null,
                    ActionLogLevel.error, InvalidTranslationMessage("some other message")
                )
            ).canBeConsolidatedWith(
                DetailActionLog(
                    ActionLogScope.report, null, null, null,
                    ActionLogLevel.warning, InvalidReportMessage("mapping")
                )
            )
        ).isFalse()
    }

    @Test
    fun `test DetailedSubmissionHistory common properties init`() {
        DetailedSubmissionHistory(1, TaskAction.receive, OffsetDateTime.now(), null, null, emptyList()).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(id).isNull()
            assertThat(sender).isNull()
            assertThat(topic).isNull()
            assertThat(reportItemCount).isNull()
            assertThat(externalName).isNull()
            assertThat(destinations.size).isEqualTo(0)
            assertThat(realDestinations.size).isEqualTo(0)
            assertThat(destinationCount).isEqualTo(0)
        }

        val inputReport = DetailReport(
            UUID.randomUUID(), null, null, "org",
            "client", "topic", "externalName", null, null, 5
        )

        var reports = listOf(
            inputReport,
            DetailReport(
                UUID.randomUUID(), "recvOrg1", "recvSvc1", null,
                null, "topic", "otherExternalName1", null, null, 1
            ),
            DetailReport(
                UUID.randomUUID(), "recvOrg2", "recvSvc2", null,
                null, "topic", "otherExternalName2", null, null, 2
            ),
            DetailReport(
                UUID.randomUUID(), "recvOrg3", "recvSvc3", null,
                null, "topic", "no item count dest", null, null, 0
            ),
        ).toMutableList()

        DetailedSubmissionHistory(1, TaskAction.receive, OffsetDateTime.now(), null, reports, emptyList()).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(id).isEqualTo(inputReport.reportId.toString())
            assertThat(sender).isEqualTo(ClientSource(inputReport.sendingOrg!!, inputReport.sendingOrgClient!!).name)
            assertThat(topic).isEqualTo(inputReport.schemaTopic)
            assertThat(reportItemCount).isEqualTo(inputReport.itemCount)
            assertThat(externalName).isEqualTo(inputReport.externalName)
            assertThat(destinations.size).isEqualTo(3)
            assertThat(realDestinations.size).isEqualTo(2)
            assertThat(destinationCount).isEqualTo(2)
        }

        val logs = listOf(
            DetailActionLog(
                ActionLogScope.item,
                UUID.randomUUID(),
                1,
                null,
                ActionLogLevel.error,
                InvalidEquipmentMessage("")
            )
        )

        DetailedSubmissionHistory(1, TaskAction.receive, OffsetDateTime.now(), null, reports, logs).run {
            assertThat(actionId).isEqualTo(1)
            assertThat(id).isEqualTo(null)
            assertThat(sender).isEqualTo(ClientSource(inputReport.sendingOrg!!, inputReport.sendingOrgClient!!).name)
            assertThat(topic).isEqualTo(inputReport.schemaTopic)
            assertThat(reportItemCount).isEqualTo(inputReport.itemCount)
            assertThat(externalName).isEqualTo(inputReport.externalName)
            assertThat(destinations.size).isEqualTo(3)
            assertThat(realDestinations.size).isEqualTo(2)
            assertThat(destinationCount).isEqualTo(2)
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

    @Test
    fun `test DetailedSubmissionHistory overallStatus and completionAt calculations`() {
        DetailedSubmissionHistory(
            1, TaskAction.receive, OffsetDateTime.now(), HttpStatus.BAD_REQUEST.value(), null
        ).run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.ERROR)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }

        DetailedSubmissionHistory(
            1, TaskAction.receive, OffsetDateTime.now(), HttpStatus.OK.value(), null
        ).run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }

        var reports = listOf(
            DetailReport(
                UUID.randomUUID(), "recvOrg3", "recvSvc3", null, null,
                "topic", "no item count dest", null, null, 0
            ),
        ).toMutableList()

        DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(), HttpStatus.OK.value(), reports
        ).run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING)
            assertThat(plannedCompletionAt).isNull()
            assertThat(actualCompletionAt).isNull()
        }

        val tomorrow = OffsetDateTime.now().plusDays(1)
        val today = OffsetDateTime.now()

        val inputReport = DetailReport(
            UUID.randomUUID(), null, null, "org", "client",
            "topic", "externalName", null, null, 3
        )

        val latestReport = DetailReport(
            UUID.randomUUID(), "recvOrg2", "recvSvc2", null, null,
            "topic", "otherExternalName2", null, tomorrow, 2
        )

        reports = listOf(
            inputReport,
            latestReport,
            DetailReport(
                UUID.randomUUID(), "recvOrg1", "recvSvc1", null, null,
                "topic", "otherExternalName1", null, today, 1
            ),
            DetailReport(
                UUID.randomUUID(), "recvOrg3", "recvSvc3", null, null,
                "topic", "no item count dest", null, null, 0
            ),
        ).toMutableList()

        DetailedSubmissionHistory(
            1, TaskAction.receive, OffsetDateTime.now(),
            HttpStatus.OK.value(), reports
        ).run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
            assertThat(plannedCompletionAt).isEqualTo(tomorrow)
            assertThat(actualCompletionAt).isNull()
        }

        val partiallyDelivered = listOf(
            DetailReport(
                UUID.randomUUID(), "recvOrg1", "recvSvc1", null, null,
                "topic", "extname", null, null, 3
            ),
        ).toMutableList()

        val testSent = DetailedSubmissionHistory(
            1, TaskAction.receive, OffsetDateTime.now(),
            HttpStatus.OK.value(), reports
        )
        testSent.enrichWithDescendant(
            DetailedSubmissionHistory(
                1, TaskAction.send, OffsetDateTime.now(),
                HttpStatus.OK.value(), partiallyDelivered
            )
        )
        testSent.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.PARTIALLY_DELIVERED)
            assertThat(plannedCompletionAt).isEqualTo(tomorrow)
            assertThat(actualCompletionAt).isNull()
        }

        val testDownload = DetailedSubmissionHistory(
            1, TaskAction.receive, OffsetDateTime.now(),
            HttpStatus.OK.value(), reports
        )
        testDownload.enrichWithDescendant(
            DetailedSubmissionHistory(
                1, TaskAction.download, OffsetDateTime.now(),
                HttpStatus.OK.value(), partiallyDelivered
            )
        )
        testDownload.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.PARTIALLY_DELIVERED)
            assertThat(plannedCompletionAt).isEqualTo(tomorrow)
            assertThat(actualCompletionAt).isNull()
        }

        val deliveredReports = listOf(
            DetailReport(
                UUID.randomUUID(), "recvOrg1", "recvSvc1", null, null,
                "topic", "otherExternalName1", null, null, 1
            ),
            DetailReport(
                UUID.randomUUID(), "recvOrg2", "recvSvc2", null, null,
                "topic", "otherExternalName2", null, null, 2
            ),
        ).toMutableList()

        testSent.enrichWithDescendant(
            DetailedSubmissionHistory(
                1, TaskAction.send, OffsetDateTime.now(),
                HttpStatus.OK.value(), deliveredReports
            )
        )
        testSent.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.DELIVERED)
            assertThat(plannedCompletionAt).isEqualTo(tomorrow)
            assertThat(actualCompletionAt).isEqualTo(latestReport.createdAt)
        }

        testDownload.enrichWithDescendant(
            DetailedSubmissionHistory(
                1, TaskAction.download, OffsetDateTime.now(),
                HttpStatus.OK.value(), deliveredReports
            )
        )
        testDownload.run {
            assertThat(overallStatus).isEqualTo(DetailedSubmissionHistory.Status.DELIVERED)
            assertThat(plannedCompletionAt).isEqualTo(tomorrow)
            assertThat(actualCompletionAt).isEqualTo(latestReport.createdAt)
        }
    }
}