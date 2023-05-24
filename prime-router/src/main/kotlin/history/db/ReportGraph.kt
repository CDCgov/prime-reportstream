package gov.cdc.prime.router.history.db

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables.COVID_RESULT_METADATA
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.tables.pojos.CovidResultMetadata
import gov.cdc.prime.router.common.BaseEngine
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.CommonTableExpression
import org.jooq.Record
import org.jooq.Record2
import org.jooq.impl.DSL
import org.jooq.impl.DSL.selectDistinct
import org.jooq.impl.SQLDataType
import java.util.UUID

private const val PARENT_REPORT_ID_FIELD = "parent_report_id"

private const val METADATA_CTE = "metadata"

private const val PATH_FIELD = "path"

/**
 * This class is responsible for generating graphs of either reports or items and then using that graph to link it
 * back to relevant tables (like the metadata table) in order to generate queries.  One example is fetching all
 * the covid metadata associated with a delivered report (see getMetadataForReports as an example of how to combine the
 * various CTEs)
 *
 * These queries are generated via composing CTEs (common table expressions) to ultimately fetch specific data; the
 * unique Postgres feature used here is a recursive CTE (see ancestorLineageExpression and descendantLineageExpression)
 * that walk either up or down the lineages.
 *
 *
 * @param db database access to run the generated queries against
 */
class ReportGraph(
    val db: DatabaseAccess = BaseEngine.databaseAccessSingleton
) : Logging {

    /**
     * Returns all the metadata for the items in the past in reports; will recursively walk up the report lineage
     * and then filter down to reports where the sender is set in order to find the metadata rows
     *
     * @param descendantReportIds the list of ids to start from
     *
     */
    fun getMetadataForReports(descendantReportIds: List<UUID>): List<CovidResultMetadata> {
        val lineage = ancestorLineageCommonTableExpression(descendantReportIds)

        val sourceReportIds =
            sourceReportsCommonTableExpression(lineage)

        val metadata = metadataCommonTableExpression(sourceReportIds)

        return db.transactReturning { txn ->
            DSL.using(txn)
                .withRecursive(lineage)
                .with(sourceReportIds)
                .with(metadata)
                .selectDistinct(metadata.asterisk())
                .from(
                    metadata,
                ).fetchInto(CovidResultMetadata::class.java)
        }
    }

    /**
     * Returns all the metadata rows associated with the passed in source report ids
     *
     * @param sourceReportIds report ids for sent reports that serve as the input for finding rows in metadata
     */
    private fun metadataCommonTableExpression(sourceReportIds: CommonTableExpression<Record>) =
        DSL.name(METADATA_CTE).`as`(
            selectDistinct(COVID_RESULT_METADATA.asterisk())
                .from(COVID_RESULT_METADATA)
                .where(
                    COVID_RESULT_METADATA.REPORT_ID.`in`(
                        selectDistinct(sourceReportIds.field(REPORT_FILE.REPORT_ID.name, SQLDataType.UUID)).from(
                            sourceReportIds
                        )
                    )
                )
        )

    /**
     * Accepts a walked graph of report ids and finds the corresponding report file filtering down to
     * reports where the sending org is not null
     *
     * @param lineage a CTE that represents a graph of reports
     */
    private fun sourceReportsCommonTableExpression(
        lineage: CommonTableExpression<Record2<UUID, String>>
    ): CommonTableExpression<Record> {
        val sourceCteName = "source"
        return DSL.name(sourceCteName).`as`(
            DSL.select(REPORT_FILE.asterisk()).from(REPORT_FILE).join(lineage.name).on(
                REPORT_FILE.REPORT_ID.eq(lineage.field(PARENT_REPORT_ID_FIELD, SQLDataType.UUID))
            ).where(REPORT_FILE.SENDING_ORG.isNotNull)
        )
    }

    private val lineageCteName = "lineage"
    /**
     * Accepts a list of ids and walks up the report lineage graph
     *
     * @param childReportIds the initial set of report ids to walk up from
     */
    private fun ancestorLineageCommonTableExpression(childReportIds: List<UUID>) =
        DSL.name(lineageCteName).fields(
            PARENT_REPORT_ID_FIELD,
            PATH_FIELD
        ).`as`(
            DSL.select(
                REPORT_LINEAGE.PARENT_REPORT_ID,
                REPORT_LINEAGE.CHILD_REPORT_ID.cast(SQLDataType.VARCHAR),
            ).from(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.CHILD_REPORT_ID.`in`(childReportIds))
                .unionAll(
                    DSL.select(
                        REPORT_LINEAGE.PARENT_REPORT_ID,
                        DSL.field("$lineageCteName.$PATH_FIELD", SQLDataType.VARCHAR)
                            .concat(REPORT_LINEAGE.PARENT_REPORT_ID)
                    )
                        .from(REPORT_LINEAGE)
                        .join(DSL.table(DSL.name(lineageCteName)))
                        .on(
                            DSL.field(DSL.name(lineageCteName, PARENT_REPORT_ID_FIELD), SQLDataType.UUID)
                                .eq(REPORT_LINEAGE.CHILD_REPORT_ID)
                        )

                )
        )

    /**
     * Accepts a list of ids and walks down the report lineage graph
     *
     * @param sourceReportIds the initial set of report ids to walk down from
     */
    private fun descendantLineageCommonTableExpression(sourceReportIds: List<UUID>) =
        DSL.name(lineageCteName).fields(
            PARENT_REPORT_ID_FIELD,
            PATH_FIELD
        ).`as`(
            DSL.select(
                REPORT_LINEAGE.CHILD_REPORT_ID,
                REPORT_LINEAGE.PARENT_REPORT_ID.cast(SQLDataType.VARCHAR),
            ).from(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.PARENT_REPORT_ID.`in`(sourceReportIds))
                .unionAll(
                    DSL.select(
                        REPORT_LINEAGE.CHILD_REPORT_ID,
                        DSL.field("$lineageCteName.$PATH_FIELD", SQLDataType.VARCHAR)
                            .concat(REPORT_LINEAGE.CHILD_REPORT_ID)
                    )
                        .from(REPORT_LINEAGE)
                        .join(DSL.table(DSL.name(lineageCteName)))
                        .on(
                            DSL.field(DSL.name(lineageCteName, PARENT_REPORT_ID_FIELD), SQLDataType.UUID)
                                .eq(REPORT_LINEAGE.CHILD_REPORT_ID)
                        )

                )
        )
}