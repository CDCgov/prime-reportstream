package gov.cdc.prime.router.db

import com.zaxxer.hikari.HikariDataSource
import gov.cdc.prime.router.azure.DatabaseAccess
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Test container for the report stream database
 *
 */
class ReportStreamTestDatabaseContainer : PostgreSQLContainer<ReportStreamTestDatabaseContainer>("postgres:11-alpine") {

    companion object {
        val containerInstance: ReportStreamTestDatabaseContainer = ReportStreamTestDatabaseContainer()

        /**
         * Uses the [testDatasource] to set up the Postgres test container for use in tests
         */
        fun setup() {
            val flyway = Flyway.configure().configuration(mapOf(Pair("flyway.postgresql.transactional.lock", "false")))
                .dataSource(testDatasource).load()
            flyway.migrate()
        }

        /**
         * Datasource that is configured to use the parameters of the test container
         */
        private val testDatasource: HikariDataSource by lazy {
            DatabaseAccess.getDataSource(
                containerInstance.jdbcUrl,
                containerInstance.username,
                containerInstance.password
            )
        }

        /**
         * [DatabaseAccess] instance that could be used by tests to perform assertions against a real database
         */
        val testDatabaseAccess: DatabaseAccess by lazy {
            DatabaseAccess(testDatasource)
        }
    }
}