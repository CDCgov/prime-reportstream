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

class ReportGraph(
    val db: DatabaseAccess = BaseEngine.databaseAccessSingleton
) : Logging {

    /**
     * Returns all the metadata for the items in the past in reports; will recursively walk up the report lineage
     * and then filter down to reports where the sender is set in order to find the metadata rows
     *
     * @param childReportIds the list of ids to start from
     *
     */
    fun getMetadataForReports(childReportIds: List<UUID>): List<CovidResultMetadata> {
        val lineage = lineageCte(childReportIds)

        val sourceReportIds =
            sourceReportsCte(lineage)

        val metadata = metadataExpression(sourceReportIds)

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

    private fun metadataExpression(sourceReportIds: CommonTableExpression<Record>) =
        DSL.name("metadata").`as`(
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
     */
    private fun sourceReportsCte(lineage: CommonTableExpression<Record2<UUID, String>>) =
        DSL.name("source").`as`(
            DSL.select(REPORT_FILE.asterisk()).from(REPORT_FILE).join(lineage.name).on(
                REPORT_FILE.REPORT_ID.eq(lineage.field("parent_report_id", SQLDataType.UUID))
            ).where(REPORT_FILE.SENDING_ORG.isNotNull)
        )

    /**
     * Accepts a list of ids and walks up the report lineage graph
     */
    private fun lineageCte(childReportIds: List<UUID>) =
        DSL.name("lineage").fields(
            "parent_report_id",
            "path"
        ).`as`(
            DSL.select(
                REPORT_LINEAGE.PARENT_REPORT_ID,
                REPORT_LINEAGE.CHILD_REPORT_ID.cast(SQLDataType.VARCHAR),
            ).from(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.CHILD_REPORT_ID.`in`(childReportIds))
                .unionAll(
                    DSL.select(
                        REPORT_LINEAGE.PARENT_REPORT_ID,
                        DSL.field("lineage.path", SQLDataType.VARCHAR)
                    )
                        .from(REPORT_LINEAGE)
                        .join(DSL.table(DSL.name("lineage")))
                        .on(
                            DSL.field(DSL.name("lineage", "parent_report_id"), SQLDataType.UUID)
                                .eq(REPORT_LINEAGE.CHILD_REPORT_ID)
                        )

                )
        )
}