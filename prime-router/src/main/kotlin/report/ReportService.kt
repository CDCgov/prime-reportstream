package gov.cdc.prime.router.report

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.history.db.ReportGraph

/**
 * Collection of report related operations
 */
class ReportService(
    private val reportGraph: ReportGraph = ReportGraph(),
) {

    /**
     * Gets the root report up the report_linage table
     *
     * @param childReportId child report ID
     * @throws IllegalStateException if a root report cannot be found for this report ID
     * @return ReportFile object of the root report
     */
    fun getRootReport(childReportId: ReportId): ReportFile {
        return reportGraph.getRootReport(childReportId)
            ?: error("No Root Report found for reportId=$childReportId")
    }

    /**
     * Gets the root report and concatenates sender fields
     *
     * @param childReportId child report ID
     * @throws IllegalStateException if a root report cannot be found for this report ID
     * @throws IllegalStateException if the root report sender fields are null
     * @return concatenates sender fields as a string
     */
    fun getSenderName(childReportId: ReportId): String {
        val rootReport = getRootReport(childReportId)
        return if (rootReport.sendingOrg != null && rootReport.sendingOrgClient != null) {
            "${rootReport.sendingOrg}.${rootReport.sendingOrgClient}"
        } else {
            error("Root report must contain a sending org and sending org client")
        }
    }
}