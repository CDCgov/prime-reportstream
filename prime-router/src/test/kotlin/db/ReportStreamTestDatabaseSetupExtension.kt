package gov.cdc.prime.router.db

import gov.cdc.prime.router.azure.DatabaseAccess
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.ext.ScriptUtils
import org.testcontainers.jdbc.JdbcDatabaseDelegate
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * This extension can be used to annotate classes that would like to have access to a test DB container in order to
 * actually write and read data from a real database.  This extension will start the test container, run the flyway
 * migrations against it and after each test truncate all the common tables.
 *
 * After annotating a test class with this extension, [ReportStreamTestDatabaseContainer.testDatabaseAccess] can be
 * used in place of the default [DatabaseAccess]
 *
 */
@Testcontainers
class ReportStreamTestDatabaseSetupExtension : BeforeAllCallback, AfterEachCallback {
    /**
     * Starts and sets up the test Postgres container
     */
    override fun beforeAll(context: ExtensionContext?) {
        ReportStreamTestDatabaseContainer.containerInstance.start()
        ReportStreamTestDatabaseContainer.setup()
    }

    /**
     * After each test drop all the data in the tables
     */
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