package gov.cdc.prime.router.db

import com.zaxxer.hikari.HikariDataSource
import gov.cdc.prime.router.azure.DatabaseAccess
import org.testcontainers.containers.PostgreSQLContainer

class ReportStreamTestDatabaseContainer : PostgreSQLContainer<ReportStreamTestDatabaseContainer>("postgres:11-alpine") {

    companion object {
        val containerInstance: ReportStreamTestDatabaseContainer = ReportStreamTestDatabaseContainer()

        val testDatasource: HikariDataSource by lazy {
            DatabaseAccess.getDataSource(
                containerInstance.jdbcUrl,
                containerInstance.username,
                containerInstance.password
            )
        }

        val testDatabaseAccess: DatabaseAccess by lazy {
            DatabaseAccess(testDatasource)
        }
    }
}