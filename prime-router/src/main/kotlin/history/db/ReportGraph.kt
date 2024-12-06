package gov.cdc.prime.router.history.db

import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.COVID_RESULT_METADATA
import gov.cdc.prime.router.azure.db.Tables.ITEM_LINEAGE
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.Action
import gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE
import gov.cdc.prime.router.azure.db.tables.pojos.CovidResultMetadata
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.records.CovidResultMetadataRecord
import gov.cdc.prime.router.common.BaseEngine
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.CommonTableExpression
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.SelectConditionStep
import org.jooq.SelectOnConditionStep
import org.jooq.impl.CustomRecord
import org.jooq.impl.CustomTable
import org.jooq.impl.DSL
import org.jooq.impl.DSL.selectDistinct
import org.jooq.impl.SQLDataType
import java.util.UUID

private const val PARENT_REPORT_ID_FIELD = "parent_report_id"

private const val CHILD_REPORT_ID_FIELD = "child_report_id"

private const val PARENT_INDEX_FIELD = "parent_index"

private const val CHILD_INDEX_FIELD = "child_index"

private const val METADATA_CTE = "metadata"

private const val PATH_FIELD = "path"

private const val STARTING_REPORT_ID_FIELD = "starting_report_id"

class ItemGraphTable : CustomTable<ItemGraphRecord>(DSL.name("item_graph")) {

    val PARENT_REPORT_ID = createField(DSL.name(PARENT_REPORT_ID_FIELD), SQLDataType.UUID)
    val CHILD_REPORT_ID = createField(DSL.name(CHILD_REPORT_ID_FIELD), SQLDataType.UUID)
    val PARENT_INDEX = createField(DSL.name(PARENT_INDEX_FIELD), SQLDataType.INTEGER)
    val CHILD_INDEX = createField(DSL.name(CHILD_INDEX_FIELD), SQLDataType.INTEGER)
    val STARTING_REPORT_ID = createField(DSL.name(STARTING_REPORT_ID_FIELD), SQLDataType.UUID)

    companion object {
        val ITEM_GRAPH = ItemGraphTable()
    }

    override fun getRecordType(): Class<out ItemGraphRecord> {
        return ItemGraphRecord::class.java
    }
}

class ItemGraphRecord : CustomRecord<ItemGraphRecord>(ItemGraphTable.ITEM_GRAPH)

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
    val db: DatabaseAccess = BaseEngine.databaseAccessSingleton,
) : Logging {

    /**
     *
     * @param receiver the Receiver to load report ids for
     * @param taskAction the task to filter reports by
     * @param dslContext the jOOQ DSL context to execute the query against
     */
    fun fetchReportIdsForReceiverAndTask(
        receiver: Receiver,
        taskAction: TaskAction,
        dslContext: DSLContext,
    ): List<UUID> {
        return dslContext
            .select(REPORT_FILE.REPORT_ID)
            .from(REPORT_FILE)
            .join(Action.ACTION).on(Action.ACTION.ACTION_ID.eq(REPORT_FILE.ACTION_ID))
            .where(Action.ACTION.RECEIVING_ORG.eq(receiver.organizationName))
            .and(Action.ACTION.RECEIVING_ORG_SVC.eq(receiver.name))
            .and(Action.ACTION.ACTION_NAME.eq(taskAction))
            .fetchInto(UUID::class.java)
    }

    /**
     * Returns all the metadata for the items in the past in reports; will recursively walk up the report lineage
     * and then filter down to reports where the sender is set in order to find the metadata rows
     *
     * @param descendantReportIds the list of ids to start from
     *
     */
    fun getMetadataForReports(descendantReportIds: List<UUID>): List<CovidResultMetadata> {
        val itemGraph = itemAncestorGraphWithPathCommonTableExpression(descendantReportIds)

        val metadata = metadataCommonTableExpression(itemGraph)

        return db.transactReturning { txn ->
            DSL.using(txn)
                .withRecursive(itemGraph)
                .with(metadata)
                .selectDistinct(metadata.asterisk())
                .from(
                    metadata,
                ).fetchInto(CovidResultMetadata::class.java)
        }
    }

    /**
     * Recursively goes up the report_lineage table from any report until it reaches
     * a report that does not appear in report_lineage as a child report (the root report)
     *
     * This will return null if the root is passed in
     */
    fun getRootReport(childReportId: UUID): ReportFile? {
        return db.transactReturning { txn ->
            val cte = reportAncestorGraphCommonTableExpression(listOf(childReportId))
            rootReportRecords(txn, cte).fetchOneInto(ReportFile::class.java)
        }
    }

    /**
     * This data class captures the rough details that corresponds to an "item" which currently is not directly captured
     * in the database.
     *
     * More concretely, an item refers to specific piece of health data in a submitted report i.e. a test result in a
     * batch of several results.
     *
     */
    data class Item(
        val parentReportId: UUID,
        val parentIndex: Int,
        val childReportId: UUID,
        val childIndex: Int,
    )

    /**
     * Retrieves the root "item" by recursing up the item lineage
     *
     * @param childReportId the id of the report
     * @param childIndex the index of the item in the report
     * @param txn an optional transaction to use
     * @return [Item]
     */
    fun getRootItem(childReportId: UUID, childIndex: Int, txn: DataAccessTransaction): Item? {
        val cte = itemAncestorGraphCommonTableExpression(
            childReportId,
            childIndex
        )
        val rootItem = DSL.using(txn)
            .withRecursive(cte)
            .select(
                ItemGraphTable.ITEM_GRAPH.asterisk()
            )
            .from(cte)
            .join(REPORT_FILE)
            .on(REPORT_FILE.REPORT_ID.eq(ItemGraphTable.ITEM_GRAPH.PARENT_REPORT_ID))
            .leftJoin(REPORT_LINEAGE)
            .on(REPORT_FILE.REPORT_ID.eq(REPORT_LINEAGE.CHILD_REPORT_ID))
            .where(REPORT_LINEAGE.PARENT_REPORT_ID.isNull())
            .orderBy(REPORT_FILE.REPORT_ID.asc())
            .fetchOneInto(Item::class.java)
        return rootItem
    }

    /**
     * Recursively goes up the report_lineage table from any report until it reaches
     * all reports that do not appear in report_lineage as a child report (the root report)
     *
     * This will return null if the root is passed in
     *
     * If the passed in report ID has multiple root reports, they will all be returned
     */
    fun getRootReports(childReportId: UUID): List<ReportFile> {
        return db.transactReturning { txn ->
            val cte = reportAncestorGraphCommonTableExpression(listOf(childReportId))
            rootReportRecords(txn, cte).fetchInto(ReportFile::class.java)
        }
    }

    /**
     * Recursively goes down the report_lineage table from any report until it reaches
     * all descendant reports with the specified action type(s)
     *
     * This will return an empty list if no report with the specified action type is present or if
     * the ID of the final descendant is passed in
     *
     * If the passed in report ID has multiple descendant reports, they will all be returned
     */
    fun getDescendantReports(
        txn: DataAccessTransaction,
        parentReportId: UUID,
        searchedForTaskActions: Set<TaskAction>? = null,
    ): List<ReportFile> {
        val cte = reportDescendantGraphCommonTableExpression(listOf(parentReportId))
        return descendantReportRecords(txn, cte, searchedForTaskActions).fetchInto(ReportFile::class.java)
    }

    /**
     * Retrieves ancestor report from a [TaskAction] for a particular item.
     *
     * @param txn the transaction to run the DB access under
     * @param childReportId the reportId to search for ancestors of
     * @param childIndex the index of the child
     * @param searchedForTaskAction the task action associated with the desired ancestor report
     * @return The ancestor report for that particular action
     */
    fun getAncestorReport(
        txn: DataAccessTransaction,
        childReportId: UUID,
        childIndex: Int,
        searchedForTaskAction: TaskAction,
    ): ReportFile? {
        val cte = itemAncestorGraphCommonTableExpression(childReportId, childIndex)
        return ancestorReportRecords(txn, cte, searchedForTaskAction).fetchOneInto(ReportFile::class.java)
    }

    /**
     * Returns all the metadata rows associated with the passed in [ItemGraphRecord]
     *
     * @param itemGraphRecords the item graph records that should be used to find the metadata
     */
    fun metadataCommonTableExpression(
        itemGraphRecords: CommonTableExpression<ItemGraphRecord>,
    ): CommonTableExpression<CovidResultMetadataRecord> {
        return DSL.name(METADATA_CTE).`as`(
            selectDistinct(COVID_RESULT_METADATA.asterisk())
                .from(COVID_RESULT_METADATA)
                .where(
                    COVID_RESULT_METADATA.REPORT_ID.`in`(
                        selectDistinct(
                            DSL.field(
                                ItemGraphTable.ITEM_GRAPH.PARENT_REPORT_ID.unqualifiedName,
                                SQLDataType.UUID
                            )
                        ).from(
                            itemGraphRecords
                        )
                    )
                ).and(
                    COVID_RESULT_METADATA.REPORT_INDEX.`in`(
                        selectDistinct(
                            DSL.field(ItemGraphTable.ITEM_GRAPH.PARENT_INDEX.unqualifiedName, SQLDataType.INTEGER)
                        ).from(itemGraphRecords)
                    )
                ).coerce(COVID_RESULT_METADATA)
        )
    }

    /**
     * Accepts a list of report ids and then finds all the items associated with that report
     * to then recursively walk the item lineage to produce an [ItemGraphRecord]
     *
     * Results will look like:
     * 784f82f6-75f7-4ccc-aad1-1ab24ad8b595, 1, 784f82f6-75f7-4ccc-aad1-1ab24ad8b595
     * 81492979-40cd-45e7-a7ce-038269fd81aa, 1, 784f82f6-75f7-4ccc-aad1-1ab24ad8b595
     * 5a52b273-c79e-46fa-9c5a-65fa725d4daa, 1, 784f82f6-75f7-4ccc-aad1-1ab24ad8b595
     * 5a52b273-c79e-46fa-9c5a-65fa725d4daa, 2, 784f82f6-75f7-4ccc-aad1-1ab24ad8b595
     *
     * @param reportIds the set of reports to start from
     */
    fun itemAncestorGraphCommonTableExpression(receiver: Receiver, taskAction: TaskAction) =
        DSL
            .name(ItemGraphTable.ITEM_GRAPH.name)
            .`as`(
                DSL.select(
                    ITEM_LINEAGE.PARENT_REPORT_ID,
                    ITEM_LINEAGE.PARENT_INDEX,
                    ITEM_LINEAGE.CHILD_REPORT_ID.`as`(STARTING_REPORT_ID_FIELD)
                )
                    .from(ITEM_LINEAGE)
                    .where(
                        ITEM_LINEAGE.CHILD_REPORT_ID.`in`(
                            DSL.select(REPORT_FILE.REPORT_ID)
                                .from(REPORT_FILE)
                                .join(Action.ACTION).on(Action.ACTION.ACTION_ID.eq(REPORT_FILE.ACTION_ID))
                                .where(Action.ACTION.RECEIVING_ORG.eq(receiver.organizationName))
                                .and(Action.ACTION.RECEIVING_ORG_SVC.eq(receiver.name))
                                .and(Action.ACTION.ACTION_NAME.eq(taskAction))
                        )
                    )
                    .unionAll(
                        DSL.select(
                            ITEM_LINEAGE.PARENT_REPORT_ID,
                            ITEM_LINEAGE.PARENT_INDEX,
                            DSL.field("${ItemGraphTable.ITEM_GRAPH.name}.$STARTING_REPORT_ID_FIELD", SQLDataType.UUID)
                        )
                            .from(ITEM_LINEAGE)
                            .join(DSL.table(DSL.name(ItemGraphTable.ITEM_GRAPH.name)))
                            .on(
                                DSL.field(
                                    DSL.name(ItemGraphTable.ITEM_GRAPH.name, PARENT_REPORT_ID_FIELD), SQLDataType.UUID
                                )
                                    .eq(
                                        ITEM_LINEAGE.CHILD_REPORT_ID
                                    ),
                                DSL.field(
                                    DSL.name(ItemGraphTable.ITEM_GRAPH.name, PARENT_INDEX_FIELD), SQLDataType.INTEGER
                                ).eq(
                                    ITEM_LINEAGE.CHILD_INDEX
                                )
                            )
                    )
                    .coerce(ItemGraphTable.ITEM_GRAPH)
            )

    fun itemAncestorGraphCommonTableExpression(childId: UUID, childIndex: Int): CommonTableExpression<ItemGraphRecord> {
        val baseCase = DSL.select(
            ITEM_LINEAGE.PARENT_REPORT_ID,
            ITEM_LINEAGE.CHILD_REPORT_ID,
            ITEM_LINEAGE.PARENT_INDEX,
            ITEM_LINEAGE.CHILD_INDEX,
            ITEM_LINEAGE.CHILD_REPORT_ID.`as`(STARTING_REPORT_ID_FIELD)
        )
            .from(ITEM_LINEAGE)
            .where(
                ITEM_LINEAGE.CHILD_REPORT_ID.eq(childId)
            ).and(ITEM_LINEAGE.CHILD_INDEX.eq(childIndex))
        return DSL
            .name(ItemGraphTable.ITEM_GRAPH.name)
            .`as`(
                baseCase
                    .unionAll(
                        DSL.select(
                            ITEM_LINEAGE.PARENT_REPORT_ID,
                            ITEM_LINEAGE.CHILD_REPORT_ID,
                            ITEM_LINEAGE.PARENT_INDEX,
                            ITEM_LINEAGE.CHILD_INDEX,
                            DSL.field("${ItemGraphTable.ITEM_GRAPH.name}.$STARTING_REPORT_ID_FIELD", SQLDataType.UUID)
                        )
                            .from(ITEM_LINEAGE)
                            .join(DSL.table(DSL.name(ItemGraphTable.ITEM_GRAPH.name)))
                            .on(
                                DSL.field(
                                    DSL.name(ItemGraphTable.ITEM_GRAPH.name, PARENT_REPORT_ID_FIELD), SQLDataType.UUID
                                )
                                    .eq(
                                        ITEM_LINEAGE.CHILD_REPORT_ID
                                    ),
                                DSL.field(
                                    DSL.name(ItemGraphTable.ITEM_GRAPH.name, PARENT_INDEX_FIELD), SQLDataType.INTEGER
                                ).eq(
                                    ITEM_LINEAGE.CHILD_INDEX
                                )
                            )
                    )
                    .coerce(ItemGraphTable.ITEM_GRAPH)
            )
    }

    /**
     * Accepts a list of report ids and then finds all the items associated with that report
     * to then recursively walk the item lineage to produce an [ItemGraphRecord].  This is intended to be used
     * for debugging purposes to trace the lineage of a report.
     *
     * Results will look like:
     * 784f82f6-75f7-4ccc-aad1-1ab24ad8b595, 1, (784f82f6-75f7-4ccc-aad1-1ab24ad8b595,1), 784f82f6-75f7-4ccc-aad1-1ab24ad8b595
     * 81492979-40cd-45e7-a7ce-038269fd81aa, 1, (81492979-40cd-45e7-a7ce-038269fd81aa,1), 784f82f6-75f7-4ccc-aad1-1ab24ad8b595
     * 5a52b273-c79e-46fa-9c5a-65fa725d4daa, 1, (784f82f6-75f7-4ccc-aad1-1ab24ad8b595,1)->(5a52b273-c79e-46fa-9c5a-65fa725d4daa,1),
     * 5a52b273-c79e-46fa-9c5a-65fa725d4daa, 2, (81492979-40cd-45e7-a7ce-038269fd81aa,1)->(5a52b273-c79e-46fa-9c5a-65fa725d4daa,2), 784f82f6-75f7-4ccc-aad1-1ab24ad8b595
     *
     * @param reportIds the set of reports to start from
     */
    fun itemAncestorGraphWithPathCommonTableExpression(reportIds: List<UUID>) =
        DSL
            .name(ItemGraphTable.ITEM_GRAPH.name)
            .`as`(
                DSL.select(
                    ITEM_LINEAGE.PARENT_REPORT_ID,
                    ITEM_LINEAGE.PARENT_INDEX,
                    DSL.value("(")
                        .concat(ITEM_LINEAGE.CHILD_REPORT_ID)
                        .concat(",")
                        .concat(ITEM_LINEAGE.CHILD_INDEX)
                        .concat(")")
                        .concat("->(")
                        .concat(ITEM_LINEAGE.PARENT_REPORT_ID.cast(SQLDataType.VARCHAR))
                        .concat(",")
                        .concat(ITEM_LINEAGE.PARENT_INDEX)
                        .concat(")").`as`(PATH_FIELD),
                    ITEM_LINEAGE.CHILD_REPORT_ID.`as`(STARTING_REPORT_ID_FIELD)
                )
                    .from(ITEM_LINEAGE)
                    .where(ITEM_LINEAGE.CHILD_REPORT_ID.`in`(reportIds))
                    .unionAll(
                        DSL.select(
                            ITEM_LINEAGE.PARENT_REPORT_ID,
                            ITEM_LINEAGE.PARENT_INDEX,
                            DSL.field("${ItemGraphTable.ITEM_GRAPH.name}.$PATH_FIELD", SQLDataType.VARCHAR)
                                .concat("->").concat(
                                    DSL.value("(")
                                        .concat(ITEM_LINEAGE.PARENT_REPORT_ID.cast(SQLDataType.VARCHAR))
                                        .concat(",")
                                        .concat(ITEM_LINEAGE.PARENT_INDEX)
                                        .concat(")")
                                ),
                            DSL.field("${ItemGraphTable.ITEM_GRAPH.name}.$STARTING_REPORT_ID_FIELD", SQLDataType.UUID)
                        )
                            .from(ITEM_LINEAGE)
                            .join(DSL.table(DSL.name(ItemGraphTable.ITEM_GRAPH.name)))
                            .on(
                                DSL.field(
                                    DSL.name(ItemGraphTable.ITEM_GRAPH.name, PARENT_REPORT_ID_FIELD), SQLDataType.UUID
                                )
                                    .eq(
                                        ITEM_LINEAGE.CHILD_REPORT_ID
                                    ),
                                DSL.field(
                                    DSL.name(ItemGraphTable.ITEM_GRAPH.name, PARENT_INDEX_FIELD), SQLDataType.INTEGER
                                ).eq(
                                    ITEM_LINEAGE.CHILD_INDEX
                                )
                            )
                    ).coerce(ItemGraphTable.ITEM_GRAPH)
            )

    private val lineageCteName = "lineage"

    /**
     * Accepts a list of ids and walks up the report lineage graph
     *
     * @param childReportIds the initial set of report ids to walk up from
     */
    fun reportAncestorGraphCommonTableExpression(childReportIds: List<UUID>) =
        DSL.name(lineageCteName).fields(
            PARENT_REPORT_ID_FIELD
        ).`as`(
            DSL.select(
                REPORT_LINEAGE.PARENT_REPORT_ID
            ).from(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.CHILD_REPORT_ID.`in`(childReportIds))
                .unionAll(
                    DSL.select(
                        REPORT_LINEAGE.PARENT_REPORT_ID
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
     * Fetches all root report records in a recursive manner.
     *
     * @param txn the data access transaction
     * @param cte the common table expression for report lineage
     * @return the root report records
     */
    private fun rootReportRecords(
        txn: DataAccessTransaction,
        cte: CommonTableExpression<Record1<UUID>>,
    ) = DSL.using(txn)
        .withRecursive(cte)
        .select(REPORT_FILE.asterisk())
        .from(cte)
        .join(REPORT_FILE)
        .on(REPORT_FILE.REPORT_ID.eq(cte.field(0, UUID::class.java)))
        .leftJoin(REPORT_LINEAGE)
        .on(REPORT_FILE.REPORT_ID.eq(REPORT_LINEAGE.CHILD_REPORT_ID))
        .where(REPORT_LINEAGE.PARENT_REPORT_ID.isNull())

    /**
     * Accepts a list of ids and walks down the report lineage graph
     *
     * @param sourceReportIds the initial set of report ids to walk down from
     */
    private fun reportDescendantGraphCommonTableExpression(sourceReportIds: List<UUID>) =
        DSL.name(lineageCteName).fields(
            PARENT_REPORT_ID_FIELD,
        ).`as`(
            DSL.select(
                REPORT_LINEAGE.PARENT_REPORT_ID
            ).from(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.PARENT_REPORT_ID.`in`(sourceReportIds))
                .unionAll(
                    DSL.select(
                        REPORT_LINEAGE.CHILD_REPORT_ID,
                    )
                        .from(REPORT_LINEAGE)
                        .join(DSL.table(DSL.name(lineageCteName)))
                        .on(
                            DSL.field(DSL.name(lineageCteName, PARENT_REPORT_ID_FIELD), SQLDataType.UUID)
                                .eq(REPORT_LINEAGE.PARENT_REPORT_ID)
                        )

                )
        )

    /**
     * Fetches all descendant report records in a recursive manner.
     *
     * @param txn the data access transaction
     * @param cte the common table expression for report lineage
     * @return the descendant report records
     */
    private fun descendantReportRecords(
        txn: DataAccessTransaction,
        cte: CommonTableExpression<Record1<UUID>>,
        searchedForTaskActions: Set<TaskAction>?,
    ): SelectOnConditionStep<Record> {
        val select = DSL.using(txn)
            .withRecursive(cte)
            .select(REPORT_FILE.asterisk())
            .distinctOn(REPORT_FILE.REPORT_ID)
            .from(cte)
            .join(REPORT_FILE)
            .on(REPORT_FILE.REPORT_ID.eq(cte.field(0, UUID::class.java)))
            .join(ACTION)
            .on(ACTION.ACTION_ID.eq(REPORT_FILE.ACTION_ID))

        if (searchedForTaskActions != null) {
            select.where(ACTION.ACTION_NAME.`in`(searchedForTaskActions))
        }

        return select
    }

    /**
     * Fetches all ancestor report records in a recursive manner.
     *
     * @param txn the data access transaction
     * @param cte the common table expression for report lineage
     * @return the descendant report records
     */
    private fun ancestorReportRecords(
        txn: DataAccessTransaction,
        cte: CommonTableExpression<ItemGraphRecord>,
        searchedForTaskAction: TaskAction,
    ): SelectConditionStep<Record> {
        val select = DSL.using(txn)
            .withRecursive(cte)
            .select(REPORT_FILE.asterisk())
            .distinctOn(REPORT_FILE.REPORT_ID)
            .from(cte)
            .join(REPORT_FILE)
            .on(REPORT_FILE.REPORT_ID.eq(ItemGraphTable.ITEM_GRAPH.PARENT_REPORT_ID))
            .join(ACTION)
            .on(ACTION.ACTION_ID.eq(REPORT_FILE.ACTION_ID))
            .where(ACTION.ACTION_NAME.eq(searchedForTaskAction))

        return select
    }
}