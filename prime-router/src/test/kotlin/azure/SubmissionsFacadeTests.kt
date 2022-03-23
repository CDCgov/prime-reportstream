package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import gov.cdc.prime.router.DetailActionLog
import gov.cdc.prime.router.DetailReport
import gov.cdc.prime.router.DetailedSubmissionHistory
import gov.cdc.prime.router.azure.db.enums.TaskAction
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

class SubmissionsFacadeTests {
    @Test
    fun `test find report`() {
        val goodUuid = UUID.fromString("662202ba-e3e5-4810-8cb8-161b75c63bc1")
        val mockSubmissionAccess = mockk<SubmissionAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = SubmissionsFacade(mockSubmissionAccess, mockDbAccess)

        // Report not found
        every { mockDbAccess.fetchActionIdForReport(any(), any()) } returns null
        assertThat(facade.findReport("org", goodUuid)).isNull()

        // Report was found, but no history
        every { mockDbAccess.fetchActionIdForReport(any(), any()) } returns 550
        every {
            mockSubmissionAccess.fetchAction(
                any(), any(), DetailedSubmissionHistory::class.java,
                DetailReport::class.java,
                DetailActionLog::class.java
            )
        } returns null
        assertThat(facade.findReport("org", goodUuid)).isNull()

        // Good return
        val goodReturn = DetailedSubmissionHistory(
            550, TaskAction.receive, OffsetDateTime.now(),
            null, null, null
        )
        every {
            mockSubmissionAccess.fetchAction(
                any(), any(), DetailedSubmissionHistory::class.java,
                DetailReport::class.java,
                DetailActionLog::class.java
            )
        } returns goodReturn
        every {
            mockSubmissionAccess.fetchRelatedActions(
                550, DetailedSubmissionHistory::class.java,
                DetailReport::class.java,
                DetailActionLog::class.java
            )
        } returns emptyList()
        assertThat(facade.findReport("org", goodUuid)).isEqualTo(goodReturn)
    }
}