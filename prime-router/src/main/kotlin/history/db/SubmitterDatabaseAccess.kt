package gov.cdc.prime.router.history.db

import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.azure.ApiFilter
import gov.cdc.prime.router.azure.ApiFilterNames
import gov.cdc.prime.router.azure.ApiFilters
import gov.cdc.prime.router.azure.ApiSearch
import gov.cdc.prime.router.azure.ApiSearchParser
import gov.cdc.prime.router.azure.ApiSearchResult
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.RawApiSearch
import gov.cdc.prime.router.azure.SortDirection
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.Action
import gov.cdc.prime.router.azure.db.tables.CovidResultMetadata
import gov.cdc.prime.router.azure.db.tables.ReportFile
import gov.cdc.prime.router.common.BaseEngine
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.Condition
import org.jooq.Field
import org.jooq.TableField
import org.jooq.impl.CustomRecord
import org.jooq.impl.CustomTable
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Enum containing the list of submitter types
 */
enum class SubmitterType {
    SUBMITTER,
    FACILITY,
    PROVIDER
}

/**
 * A custom view of the submitters after selecting them out of the metadata table
 */
class SubmitterTable : CustomTable<SubmitterRecord>(DSL.name("submitter")) {

    val ID = createField(DSL.name("id"), SQLDataType.VARCHAR)
    val NAME = createField(DSL.name("name"), SQLDataType.VARCHAR)
    val FIRST_REPORT_DATE = createField(DSL.name("first_report_date"), SQLDataType.LOCALDATETIME)
    val TEST_RESULT_COUNT = createField(DSL.name("test_result_count"), SQLDataType.INTEGER)
    val TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR)
    val LOCATION = createField(DSL.name("location"), SQLDataType.VARCHAR)
    val SORT_ID = createField(DSL.name("sort_id"), SQLDataType.VARCHAR)

    companion object {
        val SUBMITTER = SubmitterTable()
    }

    override fun getRecordType(): Class<out SubmitterRecord> {
        return SubmitterRecord::class.java
    }
}

/**
 * A class represeting a record from the submitter view
 */
class SubmitterRecord : CustomRecord<SubmitterRecord>(SubmitterTable.SUBMITTER)

/**
 * POJO for reading results from the submitter query into.
 */
data class Submitter(
    val id: String,
    val name: String,
    val firstReportDate: OffsetDateTime,
    val testResultCount: Int,
    val type: SubmitterType,
    val location: String?
)

/**
 * The API filter names for the submitter API
 */
enum class SubmitterApiFilterNames : ApiFilterNames {
    SINCE,
    UNTIL
}

sealed class SubmitterApiSearchFilter<T> : ApiFilter<SubmitterRecord, T> {
    /**
     * Filters results to those where the created_at is greater than or equal to the passed in date
     * @param value the date that results will be greater than or equal to
     */
    class Since(override val value: LocalDateTime) :
        SubmitterApiSearchFilter<LocalDateTime>() {
        override val tableField: TableField<SubmitterRecord, LocalDateTime> = SubmitterTable.SUBMITTER.FIRST_REPORT_DATE
    }

    /**
     * Filters results to those where the created_at is less than or equal to the passed in date
     * @param value the date that results will be less than or equal to
     */
    class Until(override val value: LocalDateTime) : SubmitterApiSearchFilter<LocalDateTime>() {
        override val tableField: TableField<SubmitterRecord, LocalDateTime> = SubmitterTable.SUBMITTER.FIRST_REPORT_DATE
    }
}

/**
 * Collection of the available filters for the Submitter API
 */
object SubmitterApiSearchFilters : ApiFilters<SubmitterRecord, SubmitterApiSearchFilter<*>, SubmitterApiFilterNames> {
    override val terms = mapOf(
        Pair(SubmitterApiFilterNames.SINCE, SubmitterApiSearchFilter.Since::class.java),
        Pair(SubmitterApiFilterNames.UNTIL, SubmitterApiSearchFilter.Until::class.java)
    )
}

/**
 * Search object that can be applied to the Submitter API
 * @see [ApiSearch] for more details on creating API searches
 */
class SubmitterApiSearch(
    override val filters: List<SubmitterApiSearchFilter<*>>,
    override val sortParameter: Field<*>?,
    override val sortDirection: SortDirection = SortDirection.DESC,
    page: Int = 1,
    limit: Int = 25
) : ApiSearch<Submitter, SubmitterRecord, SubmitterApiSearchFilter<*>>(
    Submitter::class.java,
    page,
    limit
) {
    override fun getCondition(filter: SubmitterApiSearchFilter<*>): Condition {
        return when (filter) {
            is SubmitterApiSearchFilter.Since -> filter.tableField.ge(filter.value)
            is SubmitterApiSearchFilter.Until -> filter.tableField.le(filter.value)
        }
    }

    override fun getSortColumn(): Field<*> {
        return sortParameter ?: SubmitterTable.SUBMITTER.FIRST_REPORT_DATE
    }

    override fun getPrimarySortColumn(): Field<*> {
        return SubmitterTable.SUBMITTER.SORT_ID
    }

    companion object :
        ApiSearchParser<Submitter, SubmitterApiSearch, SubmitterRecord, SubmitterApiSearchFilter<*>>(), Logging {
        override fun parseRawApiSearch(rawApiSearch: RawApiSearch): SubmitterApiSearch {
            val sortProperty =
                if (rawApiSearch.sort != null)
                    SubmitterTable.SUBMITTER.field(rawApiSearch.sort.property)
                else SubmitterTable.SUBMITTER.FIRST_REPORT_DATE
            val filters = rawApiSearch.filters.mapNotNull { filter ->
                when (SubmitterApiSearchFilters.getTerm(SubmitterApiFilterNames.valueOf(filter.filterName))) {
                    SubmitterApiSearchFilter.Since::class.java
                    -> SubmitterApiSearchFilter.Since(OffsetDateTime.parse(filter.value).toLocalDateTime())

                    SubmitterApiSearchFilter.Until::class.java
                    -> SubmitterApiSearchFilter.Until(OffsetDateTime.parse(filter.value).toLocalDateTime())

                    else -> {
                        logger.warn("${filter.filterName} did not map to a valid filter for SubmitterApiSearch")
                        null
                    }
                }
            }

            return SubmitterApiSearch(
                filters = filters,
                sortParameter = sortProperty,
                sortDirection = rawApiSearch.sort?.direction ?: SortDirection.DESC,
                page = rawApiSearch.pagination.page,
                limit = rawApiSearch.pagination.limit
            )
        }
    }
}

/**
 * Database access for fetching submitters out of the database.  A submitter is any provider, facility or sender
 * that have sent items to ReportStream
 *
 * @param db access to the DB
 */
class SubmitterDatabaseAccess(val db: DatabaseAccess = BaseEngine.databaseAccessSingleton) {

    private val reportGraph = ReportGraph(db)

    /**
     * Finds al the submitters (any facility, provider, sender) for a particular receiver applying
     * the passed in search request
     *
     *
     * @param search the search parameters to apply to the query
     * @param receiver the specific receiver that the submitters should be fetched for
     */
    fun getSubmitters(search: SubmitterApiSearch, receiver: Receiver): ApiSearchResult<Submitter> {
        return db.transactReturning { txn ->

            // TODO: https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/9411
            // Might need to have a date limit set to if the query does perform well to search all time
            val sentReportIdsForReceiver = DSL
                .using(txn)
                .select(ReportFile.REPORT_FILE.REPORT_ID)
                .from(ReportFile.REPORT_FILE)
                .join(Action.ACTION).on(Action.ACTION.ACTION_ID.eq(ReportFile.REPORT_FILE.ACTION_ID))
                .where(Action.ACTION.RECEIVING_ORG.eq(receiver.organizationName))
                .and(Action.ACTION.RECEIVING_ORG_SVC.eq(receiver.name))
                .and(Action.ACTION.ACTION_NAME.eq(TaskAction.send))
                .fetchInto(UUID::class.java)

            val itemGraph = reportGraph.itemAncestorGraphCommonTableExpression(sentReportIdsForReceiver)

            val submitterExpression = DSL.select(
                CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_ID
                    .`as`(SubmitterTable.SUBMITTER.ID),
                CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_NAME
                    .`as`(SubmitterTable.SUBMITTER.NAME),
                DSL.min(CovidResultMetadata.COVID_RESULT_METADATA.CREATED_AT)
                    .`as`(SubmitterTable.SUBMITTER.FIRST_REPORT_DATE),
                DSL.count().`as`(SubmitterTable.SUBMITTER.TEST_RESULT_COUNT),
                DSL.value(SubmitterType.PROVIDER.name).`as`(SubmitterTable.SUBMITTER.TYPE),
                DSL.`when`(
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_CITY.isNotNull,
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_CITY
                ).`when`(
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_COUNTY.isNotNull,
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_COUNTY
                ).otherwise("n/a")
                    .concat(", ")
                    .concat(CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_STATE)
                    .`as`(SubmitterTable.SUBMITTER.LOCATION),
                CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_ID
                    .concat(CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_NAME)
                    .`as`(SubmitterTable.SUBMITTER.SORT_ID)
            ).from(CovidResultMetadata.COVID_RESULT_METADATA)
                .join(ItemGraphTable.ITEM_GRAPH)
                .on(
                    ItemGraphTable.ITEM_GRAPH.PARENT_REPORT_ID
                        .eq(CovidResultMetadata.COVID_RESULT_METADATA.REPORT_ID),
                    ItemGraphTable.ITEM_GRAPH.PARENT_INDEX
                        .eq(CovidResultMetadata.COVID_RESULT_METADATA.REPORT_INDEX)
                )
                .where(CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_ID.isNotNull)
                .groupBy(
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_ID,
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_NAME,
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_CITY,
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_STATE,
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_COUNTY
                )
                .unionAll(
                    DSL.select(
                        DSL.value("null").`as`("id"),
                        CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_NAME
                            .`as`(SubmitterTable.SUBMITTER.NAME),
                        DSL.min(CovidResultMetadata.COVID_RESULT_METADATA.CREATED_AT)
                            .`as`(SubmitterTable.SUBMITTER.FIRST_REPORT_DATE),
                        DSL.count().`as`(SubmitterTable.SUBMITTER.TEST_RESULT_COUNT),
                        DSL.value(SubmitterType.FACILITY.name).`as`("type")
                            .`as`(SubmitterTable.SUBMITTER.TYPE),
                        DSL.`when`(
                            CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_COUNTY.isNotNull,
                            CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_COUNTY
                        ).otherwise("n/a")
                            .concat(", ")
                            .concat(CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_STATE)
                            .`as`(SubmitterTable.SUBMITTER.LOCATION),
                        DSL.value("null")
                            .concat(CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_NAME)
                            .`as`(SubmitterTable.SUBMITTER.SORT_ID)
                    ).from(CovidResultMetadata.COVID_RESULT_METADATA)
                        .join(ItemGraphTable.ITEM_GRAPH)
                        .on(
                            ItemGraphTable.ITEM_GRAPH.PARENT_REPORT_ID
                                .eq(CovidResultMetadata.COVID_RESULT_METADATA.REPORT_ID),
                            ItemGraphTable.ITEM_GRAPH.PARENT_INDEX
                                .eq(CovidResultMetadata.COVID_RESULT_METADATA.REPORT_INDEX)
                        )
                        .groupBy(
                            CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_NAME,
                            CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_COUNTY,
                            CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_PROVIDER_STATE
                        )
                ).unionAll(
                    DSL.select(
                        DSL.value("null").`as`("id").`as`(SubmitterTable.SUBMITTER.ID),
                        CovidResultMetadata.COVID_RESULT_METADATA.SENDER_ID.`as`(SubmitterTable.SUBMITTER.NAME),
                        DSL.min(CovidResultMetadata.COVID_RESULT_METADATA.CREATED_AT)
                            .`as`(SubmitterTable.SUBMITTER.FIRST_REPORT_DATE),
                        DSL.count().`as`(SubmitterTable.SUBMITTER.TEST_RESULT_COUNT),
                        DSL.value(SubmitterType.SUBMITTER.name).`as`("type")
                            .`as`(SubmitterTable.SUBMITTER.TYPE),
                        DSL.value("n/a").`as`(SubmitterTable.SUBMITTER.LOCATION),
                        DSL.value("null")
                            .concat(CovidResultMetadata.COVID_RESULT_METADATA.SENDER_ID)
                            .`as`(SubmitterTable.SUBMITTER.SORT_ID)
                    ).from(CovidResultMetadata.COVID_RESULT_METADATA)
                        .join(ItemGraphTable.ITEM_GRAPH)
                        .on(
                            ItemGraphTable.ITEM_GRAPH.PARENT_REPORT_ID
                                .eq(CovidResultMetadata.COVID_RESULT_METADATA.REPORT_ID),
                            ItemGraphTable.ITEM_GRAPH.PARENT_INDEX
                                .eq(CovidResultMetadata.COVID_RESULT_METADATA.REPORT_INDEX)
                        )
                        .groupBy(CovidResultMetadata.COVID_RESULT_METADATA.SENDER_ID)
                ).asTable(SubmitterTable.SUBMITTER)

            search.fetchResults(
                DSL.using(txn),
                DSL.withRecursive(itemGraph)
                    .select(submitterExpression.asterisk())
                    .from(submitterExpression)
            )
        }
    }
}