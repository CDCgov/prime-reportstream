package gov.cdc.prime.router.db

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.ext.ScriptUtils
import org.testcontainers.jdbc.JdbcDatabaseDelegate

class ReportStreamTestDatabaseSetupExtension : BeforeAllCallback, AfterEachCallback {
    override fun beforeAll(context: ExtensionContext?) {
        ReportStreamTestDatabaseContainer.containerInstance.start()
        val flyway = Flyway.configure().configuration(mapOf(Pair("flyway.postgresql.transactional.lock", "false")))
            .dataSource(ReportStreamTestDatabaseContainer.testDatasource).load()
        flyway.migrate()
    }

    override fun afterEach(context: ExtensionContext?) {
        ScriptUtils.executeDatabaseScript(
            JdbcDatabaseDelegate(ReportStreamTestDatabaseContainer.containerInstance, ""),
            "",
            """
                TRUNCATE TABLE public.action CASCADE;
                TRUNCATE TABLE public.action_log CASCADE;
                TRUNCATE TABLE public.covid_result_metadata CASCADE;
                TRUNCATE TABLE public.elr_result_metadata CASCADE;
                TRUNCATE TABLE public.item_lineage CASCADE;
                TRUNCATE TABLE public.jti_cache CASCADE;
                TRUNCATE TABLE public.receiver_connection_check_results CASCADE;
                TRUNCATE TABLE public.report_file CASCADE;
                TRUNCATE TABLE public.report_lineage CASCADE;
                TRUNCATE TABLE public.task CASCADE;
            """.trimIndent(),
        )
    }
}