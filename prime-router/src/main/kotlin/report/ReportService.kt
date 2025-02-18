package gov.cdc.prime.router.report

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.db.ReportGraph
import java.util.UUID

/**
 * Collection of report related operations
 */
class ReportService(
    private val reportGraph: ReportGraph = ReportGraph(),
    val db: DatabaseAccess = BaseEngine.databaseAccessSingleton,
) {

    /**
     * Gets the root report up the report_linage table
     *
     * @param childReportId child report ID
     * @return ReportFile object of the root report -- of the child report itself if it has no parents
     */
    fun getRootReport(childReportId: ReportId): ReportFile = reportGraph.getRootReport(childReportId)
            ?: reportGraph.db.fetchReportFile(childReportId)

    /**
     * Gets the index of the item in the submitted report by recursing the item lineage
     *
     * @param childReportId the child report id
     * @param childIndex the index of the item in the child report
     * @return the index of the item in the submitted report
     */
    fun getRootItemIndex(childReportId: UUID, childIndex: Int): Int? {
        val rootItem = db.transactReturning { txn ->
            reportGraph.getRootItem(childReportId, childIndex, txn)
        }
        return rootItem?.parentIndex
    }

    /**
     * Gets all root reports up the report_linage table
     *
     * @param childReportId child report ID
     * @return List of ReportFile objects of the root reports
     */
    fun getRootReports(
        childReportId: ReportId,
    ): List<ReportFile> = reportGraph.getRootReports(childReportId).distinctBy { it.reportId }

    /**
     * Accepts a descendant item (report id and index) and finds the ancestor report associated with the
     * passed [TaskAction]
     *
     * @param childReportId the descendant child report
     * @param childIndex the index of the item
     * @param task the particular task to find the ancestor report for
     *
     * @return the [ReportFile] ancestor at the passed [TaskAction]
     */
    fun getReportForItemAtTask(
        childReportId: ReportId,
        childIndex: Int,
        task: TaskAction,
    ): ReportFile? = db.transactReturning { txn ->
             reportGraph.getAncestorReport(txn, childReportId, childIndex, task)
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