package gov.cdc.prime.router.azure

import gov.cdc.prime.router.DeliveryHistory
import gov.cdc.prime.router.common.JacksonMapperUtilities
import java.time.OffsetDateTime

/**
 * Deliveries API
 * Contains all business logic regarding deliveries and JSON serialization.
 */
class DeliveryFacade(
    // private val dbDeliveryAccess: ReportFileAccess = DatabaseDeliveryAccess(),
    dbAccess: DatabaseAccess = WorkflowEngine.databaseAccessSingleton
) : ReportFileFacade(
    dbAccess,
) {
    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    /**
     * Serializes a list of Actions into a String.
     *
     * @param organizationName from JWT Claim.
     *
     * @return a String representation of an array of actions.
     */
    fun findDeliveriesAsJson(
        organizationName: String,
        sortOrder: ReportFileAccess.SortOrder,
        sortColumn: ReportFileAccess.SortColumn,
        offset: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean
    ): String {
        val result = findDeliveries(organizationName, sortOrder, sortColumn, offset, toEnd, pageSize, showFailed)
        return mapper.writeValueAsString(result)
    }

    /**
     * @param organizationName from JWT Claim.
     *
     * @return a List of Actions
     */
    private fun findDeliveries(
        organizationName: String,
        sortOrder: ReportFileAccess.SortOrder,
        sortColumn: ReportFileAccess.SortColumn,
        offset: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean
    ): List<DeliveryHistory> {
        require(organizationName.isNotBlank()) {
            "Invalid organization."
        }
        require(pageSize > 0) {
            "pageSize must be a positive integer."
        }

        // return dbSubmissionAccess.fetchActions(
        //     organizationName,
        //     sortOrder,
        //     sortColumn,
        //     offset,
        //     toEnd,
        //     pageSize,
        //     DeliveryHistory::class.java
        // )

        return listOf(
            DeliveryHistory(
                922,
                OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
                "ca-dph",
                "elr-secondary",
                201,
                null,
                "b9f63105-bbed-4b41-b1ad-002a90f07e62",
                "covid-19",
                14,
                // "http://localhost:10000/devstoreaccount1/reports/receive%2Fsimple_report.default%2Fpdi-covid-19-b9f63105-bbed-4b41-b1ad-002a90f07e62-20220505140424.csv",
                ".../simple_report.default%2Fpdi-covid-19-b9f63105-bbed-4b41-b1ad-002a90f07e62-20220505140424.csv",
                "primedatainput/pdi-covid-19",
                "CSV"
            ),
            DeliveryHistory(
                284,
                OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
                "ca-dph",
                "elr-secondary",
                201,
                null,
                "c3c8e304-8eff-4882-9000-3645054a30b7",
                "covid-19",
                1,
                // "http://localhost:10000/devstoreaccount1/reports/ready%2Fca-dph.elr-secondary%2Fcovid-19-c3c8e304-8eff-4882-9000-3645054a30b7-20220412130611.hl7",
                ".../ca-dph.elr-secondary%2Fcovid-19-c3c8e304-8eff-4882-9000-3645054a30b7-20220412130611.hl7",
                "covid-19",
                "HL7_BATCH"
            )
        )
    }

    companion object {
        val instance: DeliveryFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DeliveryFacade(DatabaseDeliveryAccess())
        }
    }
}