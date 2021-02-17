package gov.cdc.prime.router.azure

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.ReportSource
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Source
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.azure.db.Tables.SETTING
import gov.cdc.prime.router.azure.db.Tables.SETTING_HISTORY
import gov.cdc.prime.router.azure.db.Tables.TASK
import gov.cdc.prime.router.azure.db.Tables.TASK_SOURCE
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Setting
import gov.cdc.prime.router.azure.db.tables.pojos.SettingHistory
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.azure.db.tables.pojos.TaskSource
import gov.cdc.prime.router.azure.db.tables.records.TaskRecord
import org.flywaydb.core.Flyway
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.JSON
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.postgresql.Driver
import java.sql.Connection
import java.sql.DriverManager
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

const val databaseVariable = "POSTGRES_URL"
const val userVariable = "POSTGRES_USER"
const val passwordVariable = "POSTGRES_PASSWORD"

typealias DataAccessTransaction = Configuration

/**
 * A data access layer for the database. Hides JOOQ, Hikari, JDBC and other low-level abstractions.
 */
class DatabaseAccess(private val create: DSLContext) {
    constructor(dataSource: DataSource = hikariDataSource) : this(DSL.using(dataSource, SQLDialect.POSTGRES))
    constructor(connection: Connection) : this(DSL.using(connection, SQLDialect.POSTGRES))

    class Header(
        val task: Task,
        val sources: List<TaskSource>,
        val reportFile: ReportFile,
        val engine: WorkflowEngine = WorkflowEngine()
    ) {
        // Populate the header with useful metadata objs, and the blob body.
        val receiver: Receiver?
        val schema: Schema?
        val content: ByteArray?

        init {
            receiver = if (reportFile.receivingOrg != null && reportFile.receivingOrgSvc != null)
                engine.settings.findReceiver(reportFile.receivingOrg + "." + reportFile.receivingOrgSvc)
            else null

            schema = if (reportFile.schemaName != null)
                engine.metadata.findSchema(reportFile.schemaName)
            else null

            content = if (reportFile.bodyUrl != null)
                engine.blob.downloadBlob(reportFile.bodyUrl)
            else null
        }
    }

    fun checkConnection() {
        create.selectFrom(REPORT_FILE).where(REPORT_FILE.REPORT_ID.eq(UUID.randomUUID())).fetch()
    }

    /**
     * Make the other calls in the context of a SQL transaction
     */
    fun transact(block: (txn: DataAccessTransaction) -> Unit) {
        create.transaction { txn: Configuration -> block(txn) }
    }

    /**
     * Make the other calls in the context of a SQL transaction, returning a result
     */
    fun <T> transactReturning(block: (txn: DataAccessTransaction) -> T): T {
        return create.transactionResult { txn: Configuration -> block(txn) }
    }

    /**
     * Take a report and put into the database after already serializing the body of the report
     */
    fun insertHeader(
        report: Report,
        bodyFormat: String,
        bodyUrl: String,
        nextAction: Event,
        txn: DataAccessTransaction? = null,
    ) {
        fun insert(txn: Configuration) {
            val task = createTaskRecord(report, bodyFormat, bodyUrl, nextAction)
            DSL.using(txn).executeInsert(task)
            report.sources.forEach {
                insertTaskSource(report, it, txn)
            }
        }

        if (txn != null) {
            insert(txn)
        } else {
            create.transaction { innerTxn -> insert(innerTxn) }
        }
    }

    private fun insertTaskSource(report: Report, source: Source, txn: DataAccessTransaction) {
        fun insertReportSource(report: Report, source: ReportSource) {
            DSL.using(txn)
                .insertInto(
                    TASK_SOURCE,
                    TASK_SOURCE.REPORT_ID,
                    TASK_SOURCE.FROM_REPORT_ID,
                    TASK_SOURCE.FROM_REPORT_ACTION,
                ).values(
                    report.id,
                    source.id,
                    source.action,
                ).execute()
        }

        fun insertClientSource(report: Report, source: ClientSource) {
            DSL.using(txn)
                .insertInto(
                    TASK_SOURCE,
                    TASK_SOURCE.REPORT_ID,
                    TASK_SOURCE.FROM_SENDER,
                    TASK_SOURCE.FROM_SENDER_ORGANIZATION,
                ).values(
                    report.id,
                    source.client,
                    source.organization,
                ).execute()
        }

        fun insertTestSource(report: Report) {
            DSL.using(txn)
                .insertInto(
                    TASK_SOURCE,
                    TASK_SOURCE.REPORT_ID
                ).values(
                    report.id,
                ).execute()
        }

        when (source) {
            is ReportSource -> insertReportSource(report, source)
            is ClientSource -> insertClientSource(report, source)
            is TestSource -> insertTestSource(report)
            else -> TODO()
        }
    }

    /**
     * Fetch a particular task and taskSource. In addition, lock the row for updating, so
     * other connections will not grab it.
     */
    fun fetchAndLockHeader(reportId: ReportId, txn: DataAccessTransaction): Header {
        val ctx = DSL.using(txn)
        val task = ctx
            .selectFrom(TASK)
            .where(TASK.REPORT_ID.eq(reportId))
            .forUpdate()
            .fetchOne()
            ?.into(Task::class.java)
            ?: error("Could not find $reportId that matches a task")

        val taskSources = ctx
            .selectFrom(TASK_SOURCE)
            .where(TASK_SOURCE.REPORT_ID.eq(reportId))
            .fetch()
            .into(TaskSource::class.java)

        val reportFile = ActionHistory.fetchReportFile(reportId, ctx)
        ActionHistory.sanityCheckReport(task, reportFile, false)

        return Header(task, taskSources, reportFile)
    }

    /**
     * Fetch all tasks associated with a receiver. In addition, lock the rows for updating, so
     * other connections will not grab it.
     */
    fun fetchAndLockHeaders(
        nextAction: TaskAction,
        at: OffsetDateTime?,
        receiver: Receiver,
        limit: Int,
        txn: DataAccessTransaction,
    ): List<Header> {
        val cond = if (at == null) {
            TASK.RECEIVER_NAME.eq(receiver.fullName)
                .and(TASK.NEXT_ACTION.eq(nextAction))
        } else {
            TASK.RECEIVER_NAME.eq(receiver.fullName)
                .and(TASK.NEXT_ACTION.eq(nextAction))
                .and(TASK.NEXT_ACTION_AT.eq(at))
        }
        val ctx = DSL.using(txn)
        val tasks = ctx
            .selectFrom(TASK)
            .where(cond)
            .limit(limit)
            .forUpdate()
            .skipLocked() // Allows the same query to run in parallel. Otherwise, the query would lock the table.
            .fetch()
            .into(Task::class.java)

        val ids = tasks.map { it.reportId }
        val taskSources = ctx
            .selectFrom(TASK_SOURCE)
            .where(TASK_SOURCE.REPORT_ID.`in`(ids))
            .fetch()
            .into(TaskSource::class.java)

        val reportFiles = ids
            .map { ActionHistory.fetchReportFile(it, ctx) }
            .map { (it.reportId as ReportId) to it }
            .toMap()
        ActionHistory.sanityCheckReports(tasks, reportFiles, false)

        // taskSources seems erroneous.  All the sources for all Tasks are attached to each indiv. task. ?
        // todo remove the !!
        return tasks.map { Header(it, taskSources, reportFiles[it.reportId]!!) }
    }

    fun fetchDownloadableHeaders(
        since: OffsetDateTime?,
        receiverName: String,
    ): List<Header> {
        val cond = if (since == null) {
            TASK.SENT_AT.isNotNull()
                .and(TASK.RECEIVER_NAME.like("$receiverName%"))
        } else {
            TASK.RECEIVER_NAME.like("$receiverName%")
                .and(TASK.CREATED_AT.ge(since))
                .and(TASK.SENT_AT.isNotNull())
        }
        val tasks = create
            .selectFrom(TASK)
            .where(cond)
            .fetch()
            .into(Task::class.java)

        val ids = tasks.map { it.reportId }
        val taskSources = create
            .selectFrom(TASK_SOURCE)
            .where(TASK_SOURCE.REPORT_ID.`in`(ids))
            .fetch()
            .into(TaskSource::class.java)

        val reportFiles = ActionHistory.fetchDownloadableReportFiles(since, receiverName, create)
        ActionHistory.sanityCheckReports(tasks, reportFiles, false)

        // todo fix the !!.  Right now the sanityCheck guarantees non-null.
        return tasks.map { Header(it, taskSources, reportFiles[it.reportId]!!) }
    }

    fun fetchHeader(
        reportId: ReportId,
        orgName: String
    ): Header {

        val task = create
            .selectFrom(TASK)
            .where(TASK.REPORT_ID.eq(reportId).and(TASK.RECEIVER_NAME.like("$orgName%")).and(TASK.SENT_AT.isNotNull()))
            .fetchOne()
            ?.into(Task::class.java)
            ?: error("Could not find $reportId/$orgName that matches a task")

        val taskSources = create
            .selectFrom(TASK_SOURCE)
            .where(TASK_SOURCE.REPORT_ID.eq(reportId))
            .fetch()
            .into(TaskSource::class.java)

        val reportFile = ActionHistory.fetchReportFile(reportId, create)
        ActionHistory.sanityCheckReport(task, reportFile, false)

        return Header(task, taskSources, reportFile)
    }

    /**
     * Update the header of a report with new values
     */
    fun updateHeader(
        reportId: ReportId,
        currentEventAction: Event.EventAction,
        nextEventAction: Event.EventAction,
        nextActionAt: OffsetDateTime? = null,
        retryToken: String? = null,
        txn: DataAccessTransaction,
    ) {
        fun finishedField(currentEventAction: Event.EventAction): Field<OffsetDateTime> {
            return when (currentEventAction) {
                Event.EventAction.RECEIVE -> TASK.TRANSLATED_AT
                Event.EventAction.TRANSLATE -> TASK.TRANSLATED_AT
                Event.EventAction.BATCH -> TASK.BATCHED_AT
                Event.EventAction.SEND -> TASK.SENT_AT
                Event.EventAction.WIPE -> TASK.WIPED_AT

                Event.EventAction.BATCH_ERROR,
                Event.EventAction.SEND_ERROR,
                Event.EventAction.WIPE_ERROR -> TASK.ERRORED_AT

                Event.EventAction.NONE -> error("Internal Error: NONE currentAction")
            }
        }

        DSL
            .using(txn)
            .update(TASK)
            .set(TASK.NEXT_ACTION, nextEventAction.toTaskAction())
            .set(TASK.NEXT_ACTION_AT, nextActionAt)
            .set(TASK.RETRY_TOKEN, if (retryToken != null) JSON.valueOf(retryToken) else null)
            .set(finishedField(currentEventAction), OffsetDateTime.now())
            .where(TASK.REPORT_ID.eq(reportId))
            .execute()
    }

    /**
     * Settings section
     */

    fun fetchSetting(type: SettingType, organizationName: String, name: String, txn: DataAccessTransaction): Setting? {
        return DSL
            .using(txn)
            .selectFrom(SETTING)
            .where(
                SETTING.TYPE.eq(type),
                SETTING.SETTING_NAME.eq(name),
                SETTING.ORGANIZATION_NAME.eq(organizationName)
            )
            .fetchOne()
            ?.into(Setting::class.java)
    }

    fun fetchSettings(type: SettingType, txn: DataAccessTransaction): List<Setting> {
        return DSL
            .using(txn)
            .selectFrom(SETTING)
            .where(
                SETTING.TYPE.eq(type)
            )
            .fetch()
            .into(Setting::class.java)
    }

    fun fetchSettings(type: SettingType, organizationName: String, txn: DataAccessTransaction): List<Setting> {
        return DSL
            .using(txn)
            .selectFrom(SETTING)
            .where(
                SETTING.TYPE.eq(type),
                SETTING.ORGANIZATION_NAME.eq(organizationName)
            )
            .fetch()
            .into(Setting::class.java)
    }

    fun insertSetting(setting: Setting, txn: DataAccessTransaction) {
        DSL
            .using(txn)
            .insertInto(SETTING)
            .set(SETTING.SETTING_ID, DSL.defaultValue(SETTING.SETTING_ID))
            .set(SETTING.TYPE, setting.type)
            .set(SETTING.ORGANIZATION_NAME, setting.organizationName)
            .set(SETTING.SETTING_NAME, setting.settingName)
            .set(SETTING.VALUES, setting.values)
            .set(SETTING.VERSION, setting.version)
            .set(SETTING.CREATED_AT, setting.createdAt)
            .set(SETTING.CREATED_BY, setting.createdBy)
            .execute()
    }

    fun updateSetting(setting: Setting, txn: DataAccessTransaction) {
        DSL
            .using(txn)
            .update(SETTING)
            .set(SETTING.VERSION, setting.version)
            .set(SETTING.VALUES, setting.values)
            .set(SETTING.CREATED_BY, setting.createdBy)
            .set(SETTING.CREATED_AT, setting.createdAt)
            .where(
                SETTING.TYPE.eq(setting.type),
                SETTING.SETTING_NAME.eq(setting.settingName),
                SETTING.ORGANIZATION_NAME.eq(setting.organizationName)
            )
            .execute()
    }

    fun deleteSetting(name: String, organizationName: String, type: SettingType, txn: DataAccessTransaction) {
        DSL
            .using(txn)
            .deleteFrom(SETTING)
            .where(
                SETTING.TYPE.eq(type),
                SETTING.SETTING_NAME.eq(name),
                SETTING.ORGANIZATION_NAME.eq(organizationName)
            )
            .execute()
    }

    fun findSettingVersion(name: String, organizationName: String, type: SettingType, txn: DataAccessTransaction): Int {
        val record = DSL
            .using(txn)
            .select(DSL.max(SETTING_HISTORY.VERSION))
            .from(SETTING_HISTORY)
            .where(
                SETTING_HISTORY.TYPE.eq(type),
                SETTING_HISTORY.SETTING_NAME.eq(name),
                SETTING_HISTORY.ORGANIZATION_NAME.eq(organizationName)
            )
            .fetchOne()
        return record?.getValue(DSL.max(SETTING_HISTORY.VERSION)) ?: -1
    }

    fun insertSettingHistory(settingHistory: SettingHistory, txn: DataAccessTransaction) {
        DSL
            .using(txn)
            .insertInto(SETTING_HISTORY)
            .set(SETTING_HISTORY.SETTING_ID, DSL.defaultValue(SETTING_HISTORY.SETTING_ID))
            .set(SETTING_HISTORY.TYPE, settingHistory.type)
            .set(SETTING_HISTORY.ORGANIZATION_NAME, settingHistory.organizationName)
            .set(SETTING_HISTORY.SETTING_NAME, settingHistory.settingName)
            .set(SETTING_HISTORY.VALUES, settingHistory.values)
            .set(SETTING_HISTORY.IS_DELETED, settingHistory.isDeleted)
            .set(SETTING_HISTORY.VERSION, settingHistory.version)
            .set(SETTING_HISTORY.CREATED_AT, settingHistory.createdAt)
            .set(SETTING_HISTORY.CREATED_BY, settingHistory.createdBy)
            .execute()
    }

    /**
     * Common companion object
     */

    companion object {
        /**
         * Create a connection pool
         *
         * Dev Note: Why a connection pool with a "stateless" Azure function? The
         * reason is that Azure functions are actually deployed in a web server.
         * That is functions amortize startup costs by reusing an existing process for a function invocation.
         * Hence, a connection pool is a win in latency after the first initialization.
         */
        val hikariDataSource: HikariDataSource by lazy {
            DriverManager.registerDriver(Driver())

            val password = System.getenv(passwordVariable)
            val user = System.getenv(userVariable)
            val databaseUrl = System.getenv(databaseVariable)
            val config = HikariConfig()
            config.jdbcUrl = databaseUrl
            config.username = user
            config.password = password
            config.addDataSourceProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
            config.addDataSourceProperty("cachePrepStmts", "true")
            config.addDataSourceProperty("prepStmtCacheSize", "250")
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            // See this info why these are a good value
            //  https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
            config.minimumIdle = 2
            config.maximumPoolSize = 8
            // This strongly recommended to be set "be several seconds shorter than any database or infrastructure
            // imposed connection time limit". Not sure what value is but have observed that connection are closed
            // after about 10 minutes
            config.maxLifetime = 180000
            val dataSource = HikariDataSource(config)

            val flyway = Flyway.configure().dataSource(dataSource).load()
            flyway.migrate()

            dataSource
        }

        val dataSource: DataSource get() = hikariDataSource

        fun toSource(taskSource: TaskSource): Source {
            return when {
                taskSource.fromReportId != null -> {
                    ReportSource(taskSource.fromReportId, taskSource.fromReportAction)
                }
                taskSource.fromSender != null -> {
                    ClientSource(taskSource.fromSenderOrganization, taskSource.fromSender)
                }
                else -> {
                    TestSource
                }
            }
        }

        fun createTaskRecord(
            report: Report,
            bodyFormat: String,
            bodyUrl: String,
            nextAction: Event,
        ): TaskRecord {
            return TaskRecord(
                report.id,
                nextAction.eventAction.toTaskAction(),
                nextAction.at,
                report.schema.name,
                report.destination?.fullName ?: "",
                report.itemCount,
                bodyFormat,
                bodyUrl,
                report.createdDateTime,
                null,
                null,
                null,
                null,
                null,
                null
            )
        }

        fun createTask(
            report: Report,
            bodyFormat: String,
            bodyUrl: String,
            nextAction: Event,
        ): Task {
            return Task(
                report.id,
                nextAction.eventAction.toTaskAction(),
                nextAction.at,
                report.schema.name,
                report.destination?.fullName ?: "",
                report.itemCount,
                bodyFormat,
                bodyUrl,
                report.createdDateTime,
                null,
                null,
                null,
                null,
                null,
                null,
            )
        }
    }
}