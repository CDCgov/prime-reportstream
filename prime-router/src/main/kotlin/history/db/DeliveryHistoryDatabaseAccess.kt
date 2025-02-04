package gov.cdc.prime.router.history.db

import com.fasterxml.jackson.annotation.JsonIgnore
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ApiFilter
import gov.cdc.prime.router.azure.ApiFilterNames
import gov.cdc.prime.router.azure.ApiFilters
import gov.cdc.prime.router.azure.ApiSearch
import gov.cdc.prime.router.azure.ApiSearchParser
import gov.cdc.prime.router.azure.ApiSearchResult
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.RawApiSearch
import gov.cdc.prime.router.azure.SortDirection
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.azure.DatabaseDeliveryAccess
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

private const val EXPIRATION_DAYS_OFFSET = 30L
data class DeliveryHistory(
    val deliveryId: String?,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
    @JsonIgnore // Instead, use receiver, defined below.
    val receivingOrg: String,
    @JsonIgnore // Instead, use receiver, defined below.
    val receivingOrgSvc: String?,
    val receivingOrgSvcStatus: String? = null,
    val reportId: String? = null,
    val topic: Topic?,
    val reportItemCount: Int? = null,
    @JsonIgnore
    val bodyUrl: String? = null,
    @JsonIgnore
    val schemaName: String,
    val fileType: String,
    val fileName: String? = bodyUrl?.substringAfter("%2F").orEmpty(),
) {

    /**
     * The fullName of the recipient of the input report, or less, if missing some fields.
     */
    var receiver: String? = ""

    init {
        receiver = when {
            receivingOrg.isBlank() -> ""
            receivingOrgSvc.isNullOrBlank() -> receivingOrg
            else -> Receiver.createFullName(receivingOrg, receivingOrgSvc)
        }
    }
}

class DeliveryHistoryTable : CustomTable<DeliveryHistoryRecord>(DSL.name("delivery_history")) {

    val DELIVERY_ID = createField(DSL.name("delivery_id"), SQLDataType.VARCHAR)
    val CREATED_AT = createField(DSL.name("created_at"), SQLDataType.OFFSETDATETIME)
    val EXPIRES_AT = createField(DSL.name("expires_at"), SQLDataType.OFFSETDATETIME)
    val RECEIVING_ORG = createField(DSL.name("receiving_org"), SQLDataType.VARCHAR)
    val RECEIVING_ORG_SVC = createField(DSL.name("receiving_org_svc"), SQLDataType.VARCHAR)
    val RECEIVING_ORG_SVC_STATUS = createField(DSL.name("receiving_org_svc_status"), SQLDataType.VARCHAR)
    val REPORT_ID = createField(DSL.name("report_id"), SQLDataType.UUID)
    val TOPIC = createField(DSL.name("topic"), SQLDataType.VARCHAR)
    val REPORT_ITEM_COUNT = createField(DSL.name("report_item_count"), SQLDataType.INTEGER)
    val BODY_URL = createField(DSL.name("body_url"), SQLDataType.VARCHAR)
    val SCHEMA_NAME = createField(DSL.name("schema_name"), SQLDataType.VARCHAR)
    val FILE_TYPE = createField(DSL.name("file_type"), SQLDataType.VARCHAR)

    companion object {
        val DELIVERY_HISTORY = DeliveryHistoryTable()
    }

    override fun getRecordType(): Class<out DeliveryHistoryRecord> = DeliveryHistoryRecord::class.java
}

class DeliveryHistoryRecord : CustomRecord<DeliveryHistoryRecord>(DeliveryHistoryTable.DELIVERY_HISTORY)

enum class DeliveryHistoryApiFilterNames : ApiFilterNames {
    SINCE,
    UNTIL,
}

sealed class DeliveryHistoryApiSearchFilter<T> : ApiFilter<DeliveryHistoryRecord, T> {
    /**
     * Filters results to those where the created_at is greater than or equal to the passed in date
     * @param value the date that results will be greater than or equal to
     */
    class Since(override val value: OffsetDateTime) : DeliveryHistoryApiSearchFilter<OffsetDateTime>() {
        override val tableField: TableField<DeliveryHistoryRecord, OffsetDateTime> =
            DeliveryHistoryTable.DELIVERY_HISTORY.CREATED_AT
    }

    /**
     * Filters results to those where the created_at is less than or equal to the passed in date
     * @param value the date that results will be less than or equal to
     */
    class Until(override val value: OffsetDateTime) : DeliveryHistoryApiSearchFilter<OffsetDateTime>() {
        override val tableField: TableField<DeliveryHistoryRecord, OffsetDateTime> =
            DeliveryHistoryTable.DELIVERY_HISTORY.CREATED_AT
    }
}

object DeliveryHistoryApiSearchFilters :
    ApiFilters<DeliveryHistoryRecord, DeliveryHistoryApiSearchFilter<*>, DeliveryHistoryApiFilterNames> {
    override val terms = mapOf(
        Pair(DeliveryHistoryApiFilterNames.SINCE, DeliveryHistoryApiSearchFilter.Since::class.java),
        Pair(DeliveryHistoryApiFilterNames.UNTIL, DeliveryHistoryApiSearchFilter.Until::class.java)
    )
}

class DeliveryHistoryApiSearch(
    override val filters: List<DeliveryHistoryApiSearchFilter<*>>,
    override val sortParameter: Field<*>?,
    override val sortDirection: SortDirection = SortDirection.DESC,
    page: Int = 1,
    limit: Int = 25,
) : ApiSearch<DeliveryHistory, DeliveryHistoryRecord, DeliveryHistoryApiSearchFilter<*>>(
    DeliveryHistory::class.java,
    page,
    limit
) {
    override fun getCondition(filter: DeliveryHistoryApiSearchFilter<*>): Condition = when (filter) {
            is DeliveryHistoryApiSearchFilter.Since -> filter.tableField.ge(filter.value)
            is DeliveryHistoryApiSearchFilter.Until -> filter.tableField.le(filter.value)
        }

    override fun getSortColumn(): Field<*> = sortParameter ?: DeliveryHistoryTable.DELIVERY_HISTORY.CREATED_AT

    override fun getPrimarySortColumn(): Field<*> = DeliveryHistoryTable.DELIVERY_HISTORY.REPORT_ID

    companion object :
        ApiSearchParser<
            DeliveryHistory,
            DeliveryHistoryApiSearch,
            DeliveryHistoryRecord,
            DeliveryHistoryApiSearchFilter<*>
            >(),
        Logging {
        override fun parseRawApiSearch(rawApiSearch: RawApiSearch): DeliveryHistoryApiSearch {
            val sortProperty = if (rawApiSearch.sort != null) {
                DeliveryHistoryTable.DELIVERY_HISTORY.field(rawApiSearch.sort.property)
            } else {
                DeliveryHistoryTable.DELIVERY_HISTORY.CREATED_AT
            }
            val filters = rawApiSearch.filters.mapNotNull { filter ->
                when (
                    DeliveryHistoryApiSearchFilters.getTerm(
                    DeliveryHistoryApiFilterNames.valueOf(filter.filterName)
                    )
                ) {
                    DeliveryHistoryApiSearchFilter.Since::class.java,
                    -> DeliveryHistoryApiSearchFilter.Since(OffsetDateTime.parse(filter.value))

                    DeliveryHistoryApiSearchFilter.Until::class.java,
                    -> DeliveryHistoryApiSearchFilter.Until(OffsetDateTime.parse(filter.value))

                    else -> {
                        logger.warn("${filter.filterName} did not map to a valid filter for SubmitterApiSearch")
                        null
                    }
                }
            }
            return DeliveryHistoryApiSearch(
                filters = filters,
                sortParameter = sortProperty,
                sortDirection = rawApiSearch.sort?.direction ?: SortDirection.DESC,
                page = rawApiSearch.pagination.page,
                limit = rawApiSearch.pagination.limit
            )
        }
    }
}

class DeliveryHistoryDatabaseAccess(
    internal val db: DatabaseAccess = BaseEngine.databaseAccessSingleton,
    internal val workflowEngine: WorkflowEngine,
) : Logging {

   private val databaseDeliveryAccess: DatabaseDeliveryAccess = DatabaseDeliveryAccess()

    fun getDeliveries(
        search: DeliveryHistoryApiSearch,
        organization: String,
        orgService: String?,
        receivingOrgSvcStatus: List<CustomerStatus>?,
        reportIdStr: String?,
        fileName: String?,
    ): ApiSearchResult<DeliveryHistory> {
        require(organization.isNotBlank()) {
            "Invalid organization."
        }

        val reportId: UUID?
        try {
            reportId = if (reportIdStr != null) UUID.fromString(reportIdStr) else null
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid format for report ID: $reportIdStr")
        }

        val whereClause = this.createWhereCondition(
                organization, orgService, receivingOrgSvcStatus, reportId, fileName
            )

        val deliveriesExpression = DSL.select(
                Tables.REPORT_FILE.ACTION_ID.`as`(DeliveryHistoryTable.DELIVERY_HISTORY.DELIVERY_ID),
                Tables.REPORT_FILE.CREATED_AT,
                // Currently an open issue for doing this via the DSL
                // https://github.com/jOOQ/jOOQ/issues/6723
                DSL.field(
                    "\"public\".\"report_file\".\"created_at\" + INTERVAL '$EXPIRATION_DAYS_OFFSET days'",
                    SQLDataType.OFFSETDATETIME
                )
                .`as`(DeliveryHistoryTable.DELIVERY_HISTORY.EXPIRES_AT),
                Tables.REPORT_FILE.RECEIVING_ORG,
                Tables.REPORT_FILE.RECEIVING_ORG_SVC,
                DSL.jsonbGetAttributeAsText(Tables.SETTING.VALUES, "customerStatus")
                .`as`(DeliveryHistoryTable.DELIVERY_HISTORY.RECEIVING_ORG_SVC_STATUS),
                Tables.REPORT_FILE.REPORT_ID,
                Tables.REPORT_FILE.SCHEMA_TOPIC.`as`(DeliveryHistoryTable.DELIVERY_HISTORY.TOPIC),
                Tables.REPORT_FILE.ITEM_COUNT.`as`(DeliveryHistoryTable.DELIVERY_HISTORY.REPORT_ITEM_COUNT),
                Tables.REPORT_FILE.BODY_URL,
                Tables.REPORT_FILE.SCHEMA_NAME,
                Tables.REPORT_FILE.BODY_FORMAT.`as`(DeliveryHistoryTable.DELIVERY_HISTORY.FILE_TYPE)
            )
            .from(
                Tables.REPORT_FILE
                    .join(Tables.SETTING)
                    .on(
                        Tables.REPORT_FILE.RECEIVING_ORG
                            .eq(DSL.jsonbGetAttributeAsText(Tables.SETTING.VALUES, "organizationName"))
                    )
                    .and(
                        Tables.REPORT_FILE.RECEIVING_ORG_SVC
                            .eq(DSL.jsonbGetAttributeAsText(Tables.SETTING.VALUES, "name"))
                    )
                    .and(Tables.SETTING.IS_ACTIVE)
            )
            .where(whereClause).asTable(DeliveryHistoryTable.DELIVERY_HISTORY)

        val results = db.transactReturning { txn ->

            val historyResults = search.fetchResults(
                DSL.using(txn),
                deliveriesExpression.asterisk(),
                deliveriesExpression.asTable(DeliveryHistoryTable.DELIVERY_HISTORY.name),
            )
            val reportFiles = db
                .fetchReportFileByIds(historyResults.results.map { UUID.fromString(it.reportId) })
            historyResults.map { history ->
                val receiver = if (history.receiver != null) {
                    workflowEngine.settings.findReceiver(history.receiver!!)
                } else {
                    null
                }
                val report = reportFiles.find { it.reportId == UUID.fromString(history.reportId) }
                if (report != null) {
                    history.copy(
                        fileName = Report.formExternalFilename(
                            report.reportId,
                            report.schemaName,
                            MimeFormat.valueOf(report.bodyFormat),
                            report.createdAt,
                            workflowEngine.metadata,
                            receiver?.translation?.nameFormat,
                            receiver?.translation
                        )
                    )
                } else {
                    history
                }
            }
        }

        return results
    }

    /**
     * Add various filters to the DB query.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param orgService is a specifier for an organization, such as the client or service used to send/receive
     * @param receivingOrgSvcStatus is the status for the receiving service in an organization, i.e. active/inactive
     * @param reportId is the reportId to get results for.
     * @param fileName  is the fileName to get results for.
     * @return a jooq Condition statement to use in where().
     */
    private fun createWhereCondition(
        organization: String,
        orgService: String?,
        receivingOrgSvcStatus: List<CustomerStatus>?,
        reportId: UUID?,
        fileName: String?,
    ): Condition {
        var filter = databaseDeliveryAccess.organizationFilter(organization, orgService)

        if (receivingOrgSvcStatus != null) {
            val statusList = receivingOrgSvcStatus.map { it.name.lowercase() }
            filter = filter.and(
                DSL.jsonbGetAttributeAsText(Tables.SETTING.VALUES, "customerStatus")
                    .`in`(statusList)
            )
        }

        if (reportId != null) {
            filter = filter.and(Tables.REPORT_FILE.REPORT_ID.eq(reportId))
        }

        if (fileName != null) {
            filter = filter.and(Tables.REPORT_FILE.BODY_URL.likeIgnoreCase("%$fileName"))
        }

        return filter
    }
}