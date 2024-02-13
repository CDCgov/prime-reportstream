package gov.cdc.prime.router.report

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.history.db.ReportGraph
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlin.test.Test

class ReportServiceTests {

    inner class Fixture {
        val reportGraphMock = mockk<ReportGraph>()
        val reportService = ReportService(reportGraphMock)

        val childReportId = UUID.randomUUID()
        val rootReport = ReportFile()
            .setReportId(UUID.randomUUID())
            .setSendingOrg("sendingOrg")
            .setSendingOrgClient("sendingOrgClient")
    }

    @Test
    fun `getRootReport success`() {
        val f = Fixture()

        every { f.reportGraphMock.getRootReport(f.childReportId) } returns f.rootReport

        val actual = f.reportService.getRootReport(f.childReportId)
        assertThat(actual).isEqualTo(f.rootReport)
    }

    @Test
    fun `getRootReport failure`() {
        val f = Fixture()

        every { f.reportGraphMock.getRootReport(f.childReportId) } returns null

        assertFailure {
            f.reportService.getRootReport(f.childReportId)
        }.isInstanceOf(IllegalStateException::class)
    }

    @Test
    fun `getSenderName success`() {
        val f = Fixture()

        every { f.reportGraphMock.getRootReport(f.childReportId) } returns f.rootReport

        val actual = f.reportService.getSenderName(f.childReportId)
        assertThat(actual).isEqualTo("sendingOrg.sendingOrgClient")
    }

    @Test
    fun `getSenderName failure`() {
        val f = Fixture()

        every { f.reportGraphMock.getRootReport(f.childReportId) } returns f.rootReport
            .setSendingOrg(null)
            .setSendingOrgClient(null)

        assertFailure {
            f.reportService.getSenderName(f.childReportId)
        }.isInstanceOf(IllegalStateException::class)
    }
}