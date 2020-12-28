package gov.cdc.prime.router.azure

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.ReportSource
import gov.cdc.prime.router.Source
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.azure.db.Tables.TASK
import gov.cdc.prime.router.azure.db.Tables.TASK_SOURCE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.azure.db.tables.pojos.TaskSource
import gov.cdc.prime.router.azure.db.tables.records.TaskRecord
import org.flywaydb.core.Flyway
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.postgresql.Driver
import java.sql.Connection
import java.sql.DriverManager
import java.time.OffsetDateTime
import java.util.UUID

const val databaseVariable = "POSTGRES_URL"
const val userVariable = "POSTGRES_USER"
const val passwordVariable = "POSTGRES_PASSWORD"

typealias DataAccessTransaction = Configuration

/**
 * A data access layer for the database. Hides JOOQ, Hikari, JDBC and other low-level abstractions.
 */
class DatabaseAccess(private val connection: Connection = getConnection()) {
    private val create: DSLContext = DSL.using(connection, SQLDialect.POSTGRES)

    data class Header(val task: Task, val sources: List<TaskSource>)

    fun checkConnection() {
        create.selectFrom(TASK).where(TASK.REPORT_ID.eq(UUID.randomUUID()))
    }

    /**
     * Make the other calls in the context of a SQL transaction
     */
    fun transact(block: (txn: DataAccessTransaction) -> Unit) {
        create.transaction { txn: Configuration -> block(txn) }
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

        return Header(task, taskSources)
    }

    /**
     * Fetch all tasks associated with a receiver. In addition, lock the rows for updating, so
     * other connections will not grab it.
     */
    fun fetchAndLockHeaders(
        nextAction: TaskAction,
        at: OffsetDateTime?,
        receiverName: String,
        limit: Int,
        txn: DataAccessTransaction,
    ): List<Header> {
        val cond = if (at == null) {
            TASK.RECEIVER_NAME.eq(receiverName).and(TASK.NEXT_ACTION.eq(nextAction))
        } else {
            TASK.RECEIVER_NAME.eq(receiverName).and(TASK.NEXT_ACTION.eq(nextAction)).and(TASK.NEXT_ACTION_AT.eq(at))
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

        return tasks.map { Header(it, taskSources) }
    }

    /**
     * Update the header of a report with new values
     */
    fun updateHeader(
        reportId: ReportId,
        currentAction: Event.Action,
        nextAction: Event.Action,
        nextActionAt: OffsetDateTime? = null,
        txn: DataAccessTransaction,
    ) {
        fun finishedField(currentAction: Event.Action): Field<OffsetDateTime> {
            return when (currentAction) {
                Event.Action.TRANSLATE -> TASK.TRANSLATED_AT
                Event.Action.BATCH -> TASK.BATCHED_AT
                Event.Action.SEND -> TASK.SENT_AT
                Event.Action.WIPE -> TASK.WIPED_AT
                Event.Action.NONE -> error("Internal Error: NONE currentAction")
            }
        }

        DSL
            .using(txn)
            .update(TASK)
            .set(TASK.NEXT_ACTION, nextAction.toTaskAction())
            .set(TASK.NEXT_ACTION_AT, nextActionAt)
            .set(finishedField(currentAction), OffsetDateTime.now())
            .where(TASK.REPORT_ID.eq(reportId))
            .execute()
    }

    companion object {
        /**
         * Create a connection pool
         *
         * Dev Note: Why a connection pool with a "stateless" Azure function? The
         * reason is that Azure functions are actually deployed in a web server.
         * That is functions amortize startup costs by reusing an existing process for a function invocation.
         * Hence, a connection pool is a win in latency after the first initialization.
         */
        private val hikariDataSource: HikariDataSource by lazy {
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
            val dataSource = HikariDataSource(config)

            val flyway = Flyway.configure().dataSource(dataSource).load()
            flyway.migrate()

            dataSource
        }

        fun getConnection(): Connection {
            return hikariDataSource.connection
        }

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
                nextAction.action.toTaskAction(),
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
                nextAction.action.toTaskAction(),
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
                null
            )
        }
    }
}