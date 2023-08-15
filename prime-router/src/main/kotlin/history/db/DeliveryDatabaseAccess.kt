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
import java.time.OffsetDateTime
import java.util.UUID

private const val EXPIRATION_DAYS_OFFSET = 60

class Delivery(
    val orderingProvider: String?,
    val orderingFacility: String?,
    val submitter: String?,
    val reportId: UUID,
    val createdAt: OffsetDateTime,
    val expirationDate: OffsetDateTime,
    val testResultCount: Int
)

class DeliveryTable : CustomTable<DeliveryRecord>(DSL.name("delivery")) {

    val ORDERING_PROVIDER = createField(DSL.name("ordering_provider"), SQLDataType.VARCHAR)
    val ORDERING_FACILITY = createField(DSL.name("ordering_facility"), SQLDataType.VARCHAR)
    val SUBMITTER = createField(DSL.name("submitter"), SQLDataType.VARCHAR)
    val REPORT_ID = createField(DSL.name("report_id"), SQLDataType.UUID)
    val CREATED_AT = createField(DSL.name("created_at"), SQLDataType.OFFSETDATETIME)
    val EXPIRATION_DATE = createField(DSL.name("expiration_date"), SQLDataType.OFFSETDATETIME)
    val TEST_RESULT_COUNT = createField(DSL.name("test_result_count"), SQLDataType.INTEGER)
    val SORT_ID = createField(DSL.name("sort_id"), SQLDataType.VARCHAR)

    companion object {
        val DELIVERY = DeliveryTable()
    }

    override fun getRecordType(): Class<out DeliveryRecord> {
        return DeliveryRecord::class.java
    }
}

class DeliveryRecord : CustomRecord<DeliveryRecord>(DeliveryTable.DELIVERY)

enum class DeliveryApiFilterNames : ApiFilterNames {
    SINCE,
    UNTIL
}

sealed class DeliveryApiSearchFilter<T> : ApiFilter<DeliveryRecord, T> {
    /**
     * Filters results to those where the created_at is greater than or equal to the passed in date
     * @param value the date that results will be greater than or equal to
     */
    class Since(override val value: OffsetDateTime) :
        DeliveryApiSearchFilter<OffsetDateTime>() {
        override val tableField: TableField<DeliveryRecord, OffsetDateTime> = DeliveryTable.DELIVERY.CREATED_AT
    }

    /**
     * Filters results to those where the created_at is less than or equal to the passed in date
     * @param value the date that results will be less than or equal to
     */
    class Until(override val value: OffsetDateTime) :
        DeliveryApiSearchFilter<OffsetDateTime>() {
        override val tableField: TableField<DeliveryRecord, OffsetDateTime> = DeliveryTable.DELIVERY.CREATED_AT
    }
}

object DeliveryApiSearchFilters : ApiFilters<DeliveryRecord, DeliveryApiSearchFilter<*>, DeliveryApiFilterNames> {
    override val terms = mapOf(
        Pair(DeliveryApiFilterNames.SINCE, DeliveryApiSearchFilter.Since::class.java),
        Pair(DeliveryApiFilterNames.UNTIL, DeliveryApiSearchFilter.Until::class.java)
    )
}

class DeliveryApiSearch(
    override val filters: List<DeliveryApiSearchFilter<*>>,
    override val sortParameter: Field<*>?,
    override val sortDirection: SortDirection = SortDirection.DESC,
    page: Int = 1,
    limit: Int = 25
) : ApiSearch<Delivery, DeliveryRecord, DeliveryApiSearchFilter<*>>(
    Delivery::class.java,
    page,
    limit
) {
    override fun getCondition(filter: DeliveryApiSearchFilter<*>): Condition {
        return when (filter) {
            is DeliveryApiSearchFilter.Since -> filter.tableField.ge(filter.value)
            is DeliveryApiSearchFilter.Until -> filter.tableField.le(filter.value)
        }
    }

    override fun getSortColumn(): Field<*> {
        return sortParameter ?: DeliveryTable.DELIVERY.CREATED_AT
    }

    override fun getPrimarySortColumn(): Field<*> {
        return DeliveryTable.DELIVERY.SORT_ID
    }

    companion object :
        ApiSearchParser<Delivery, DeliveryApiSearch, DeliveryRecord, DeliveryApiSearchFilter<*>>(),
        Logging {
        override fun parseRawApiSearch(rawApiSearch: RawApiSearch): DeliveryApiSearch {
            val sortProperty = if (rawApiSearch.sort != null)
                DeliveryTable.DELIVERY.field(rawApiSearch.sort.property)
            else DeliveryTable.DELIVERY.CREATED_AT
            val filters = rawApiSearch.filters.mapNotNull { filter ->
                when (DeliveryApiSearchFilters.getTerm(DeliveryApiFilterNames.valueOf(filter.filterName))) {
                    DeliveryApiSearchFilter.Since::class.java
                    -> DeliveryApiSearchFilter.Since(OffsetDateTime.parse(filter.value))

                    DeliveryApiSearchFilter.Until::class.java
                    -> DeliveryApiSearchFilter.Until(OffsetDateTime.parse(filter.value))

                    else -> {
                        logger.warn("${filter.filterName} did not map to a valid filter for SubmitterApiSearch")
                        null
                    }
                }
            }
            return DeliveryApiSearch(
                filters = filters,
                sortParameter = sortProperty,
                sortDirection = rawApiSearch.sort?.direction ?: SortDirection.DESC,
                page = rawApiSearch.pagination.page,
                limit = rawApiSearch.pagination.limit
            )
        }
    }
}

class DeliveryDatabaseAccess(val db: DatabaseAccess = BaseEngine.databaseAccessSingleton) {

    private val reportGraph = ReportGraph(db)

    fun getDeliveries(search: DeliveryApiSearch, receiver: Receiver): ApiSearchResult<Delivery> {
        return db.transactReturning { txn ->
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

            val deliveriesExpression = DSL.select(
                CovidResultMetadata
                    .COVID_RESULT_METADATA.ORDERING_PROVIDER_NAME
                    .`as`(DeliveryTable.DELIVERY.ORDERING_PROVIDER),
                CovidResultMetadata
                    .COVID_RESULT_METADATA.ORDERING_FACILITY_NAME
                    .`as`(DeliveryTable.DELIVERY.ORDERING_FACILITY),
                CovidResultMetadata.COVID_RESULT_METADATA.SENDER_ID
                    .`as`(DeliveryTable.DELIVERY.SUBMITTER),
                ReportFile.REPORT_FILE.REPORT_ID.`as`(DeliveryTable.DELIVERY.REPORT_ID),
                ReportFile.REPORT_FILE.CREATED_AT.`as`(DeliveryTable.DELIVERY.CREATED_AT),
                // Currently an open issue for doing this via the DSL
                // https://github.com/jOOQ/jOOQ/issues/6723
                DSL.field(
                    "\"public\".\"report_file\".\"created_at\" + INTERVAL '$EXPIRATION_DAYS_OFFSET days'",
                    SQLDataType.OFFSETDATETIME
                )
                    .`as`(DeliveryTable.DELIVERY.EXPIRATION_DATE),
                DSL.sum(ReportFile.REPORT_FILE.ITEM_COUNT).`as`(DeliveryTable.DELIVERY.TEST_RESULT_COUNT),
                ReportFile.REPORT_FILE.REPORT_ID.cast(SQLDataType.VARCHAR)
                    .concat(
                        CovidResultMetadata
                            .COVID_RESULT_METADATA.ORDERING_PROVIDER_NAME
                    )
                    .concat(
                        CovidResultMetadata
                            .COVID_RESULT_METADATA.ORDERING_FACILITY_NAME
                    )
                    .concat(CovidResultMetadata.COVID_RESULT_METADATA.SENDER_ID)
                    .`as`(DeliveryTable.DELIVERY.SORT_ID)
            )
                .from(CovidResultMetadata.COVID_RESULT_METADATA)
                .join(ItemGraphTable.ITEM_GRAPH)
                .on(
                    ItemGraphTable.ITEM_GRAPH.PARENT_REPORT_ID
                        .eq(CovidResultMetadata.COVID_RESULT_METADATA.REPORT_ID),
                    ItemGraphTable.ITEM_GRAPH.PARENT_INDEX
                        .eq(CovidResultMetadata.COVID_RESULT_METADATA.REPORT_INDEX)
                )
                .join(ReportFile.REPORT_FILE)
                .on(ReportFile.REPORT_FILE.REPORT_ID.eq(ItemGraphTable.ITEM_GRAPH.STARTING_REPORT_ID))
                .groupBy(
                    ReportFile.REPORT_FILE.REPORT_ID,

                    CovidResultMetadata
                        .COVID_RESULT_METADATA.ORDERING_PROVIDER_NAME,
                    CovidResultMetadata.COVID_RESULT_METADATA.ORDERING_FACILITY_NAME,
                    CovidResultMetadata.COVID_RESULT_METADATA.SENDER_ID,
                ).asTable(DeliveryTable.DELIVERY)

            search.fetchResults(
                DSL.using(txn),
                DSL
                    .withRecursive(itemGraph)
                    .select(deliveriesExpression.asterisk())
                    .from(deliveriesExpression.asTable(DeliveryTable.DELIVERY.name))
            )
        }
    }
}