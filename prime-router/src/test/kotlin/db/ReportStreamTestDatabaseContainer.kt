package gov.cdc.prime.router.db

import gov.cdc.prime.router.azure.DatabaseAccess
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Test container for the report stream database
 *
 */
class ReportStreamTestDatabaseContainer : PostgreSQLContainer<ReportStreamTestDatabaseContainer>("postgres:11-alpine") {

    companion object {
        fun getDataSourceFromContainer(container: ReportStreamTestDatabaseContainer): DatabaseAccess {
            val dataSource = DatabaseAccess.getDataSource(
                container.jdbcUrl,
                container.username,
                container.password
            )
            val flyway = Flyway.configure().configuration(mapOf(Pair("flyway.postgresql.transactional.lock", "false")))
                .dataSource(dataSource).load()
            flyway.migrate()
            return DatabaseAccess(
                dataSource
            )
        }
    }
}