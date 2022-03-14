package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime
import kotlin.test.Test

class SubmissionTests {
    @Test
    fun `tests consolidation of logs`() {
        fun createLogs(logs: List<DetailActionLog>?): DetailedSubmissionHistory {
            return DetailedSubmissionHistory(
                1, TaskAction.receive, OffsetDateTime.now(), null,
                null, null, null, null, logs
            )
        }

        val messageM = "message 1"
        val messageA = "A message 2"
        val messageZ = "zz message"
        var rawLogs = emptyList<DetailActionLog>()
        assertThat(createLogs(rawLogs).consolidateLogs()).isEmpty()

        // Test sorting by message
        rawLogs = mutableListOf(
            DetailActionLog(
                ActionLogScope.report, null, null, null,
                ActionLogLevel.warning, InvalidReportMessage(messageM)
            ),
            DetailActionLog(
                ActionLogScope.report, null, null, null,
                ActionLogLevel.error, InvalidReportMessage(messageA)
            )
        )
        var result = createLogs(rawLogs).consolidateLogs()
        assertThat(result.size).isEqualTo(2)
        assertThat(result[0].message).isEqualTo(rawLogs[1].detail.message)
        assertThat(result[0].message).isEqualTo(messageA)
        result.forEach {
            assertThat(it.indices).isNull()
            assertThat(it.trackingIds).isNull()
        }

        val itemLog1 = InvalidEquipmentMessage("mapping")
        val itemLog2 = FieldPrecisionMessage("mapping", messageZ)
        val itemLog3 = MissingFieldMessage("mapping")

        // Test that item logs are at the end
        rawLogs.add(
            0,
            DetailActionLog(
                ActionLogScope.item, null, 1, null,
                ActionLogLevel.error, itemLog1
            )
        )
        result = createLogs(rawLogs).consolidateLogs()
        assertThat(result.size).isEqualTo(3)
        assertThat(result[2].indices).isNotNull()
        assertThat(result[2].indices!!.size).isEqualTo(1)
        assertThat(result[2].indices!![0]).isNotNull()
        assertThat(result[2].trackingIds).isNotNull()
        assertThat(result[2].trackingIds!!.size).isEqualTo(1)
        assertThat(result[2].trackingIds!![0]).isNull()

        // Now test multiple item logs
        rawLogs.add(
            DetailActionLog(
                ActionLogScope.item, null, 3, null,
                ActionLogLevel.error, itemLog2
            )
        )
        rawLogs.add(
            DetailActionLog(
                ActionLogScope.item, null, 2, null,
                ActionLogLevel.error, itemLog3
            )
        )
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
        rawLogs.add(
            DetailActionLog(
                ActionLogScope.item, null, 2, "id2",
                ActionLogLevel.error, itemLog1
            )
        )
        rawLogs.add(
            DetailActionLog(
                ActionLogScope.item, null, 3, "id3",
                ActionLogLevel.error, itemLog1
            )
        )
        assertThat(result[2].message).isEqualTo(itemLog1.message)
        assertThat(result[2].indices).isNotNull()
        assertThat(result[2].indices!!.size).isEqualTo(1)
        result = createLogs(rawLogs).consolidateLogs()
        assertThat(result.size).isEqualTo(5)
        assertThat(result[2].indices!!.size).isEqualTo(3)

        // Errors vs warnings
        rawLogs.add(
            DetailActionLog(
                ActionLogScope.item, null, 4, null,
                ActionLogLevel.warning, itemLog1
            )
        )
        rawLogs.add(
            DetailActionLog(
                ActionLogScope.item, null, 5, null,
                ActionLogLevel.warning, itemLog2
            )
        )
        val logs = createLogs(rawLogs)
        assertThat(logs.errors.size).isEqualTo(4)
        assertThat(logs.warnings.size).isEqualTo(3)
    }
}