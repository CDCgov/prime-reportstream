package gov.cdc.prime.router.db

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class ReportFileDatabaseAccessTest {

    @Container
    val postgresDatabase = PostgreSQLContainer("postgres:11-alpine")

    @Test
    fun `Test loads data`() {
        val config = HikariConfig()
        config.jdbcUrl = postgresDatabase.jdbcUrl
        config.username = postgresDatabase.username
        config.password = postgresDatabase.password
        config.addDataSourceProperty(
            "dataSourceClassName",
            "org.postgresql.ds.PGSimpleDataSource"
        )
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        config.addDataSourceProperty(
            "connectionTimeout",
            "60000"
        ) // Default is 30000 (30 seconds)

        // See this info why these are a good value
        //  https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
        config.minimumIdle = 2
        config.maximumPoolSize = 25
        // This strongly recommended to be set "be several seconds shorter than any database or
        // infrastructure
        // imposed connection time limit". Not sure what value is but have observed that
        // connection are closed
        // after about 10 minutes
        config.maxLifetime = 180000
        val datasource = HikariDataSource(config)
        val databaseAccess = DatabaseAccess(datasource)
        val reportFileDatabaseAccess = ReportFileDatabaseAccess(databaseAccess)
        val rows = reportFileDatabaseAccess.getReports(ReportFileApiSearch(emptyList(), null))
        assertThat(rows.results.isEmpty())
    }

    @Nested
    inner class ReportFileApiSearchTest() {

        @Test
        fun `Test generates filters correctly, multiple filters`() {
            val rawSearchString = """
            {
                "sort": {
                    "direction": "ASC",
                    "property": "action_id"
                },
                "pagination": {
                    "page": 27,
                    "limit": 5
                },
                "filters": [
                    {
                        "value": "2023-05-21T00:00:00+00:00",
                        "filterName": "SINCE"
                    },
                    {
                        "value": "2023-05-20T00:00:00+00:00",
                        "filterName": "UNTIL"
                    }
                ]
            }
            """.trimIndent()
            val request = MockHttpRequestMessage(rawSearchString)
            val search = ReportFileApiSearch.parse(request)
            val condition = search.getWhereClause()
            assertThat(condition).isNotNull()
            val conditionSql = condition!!.toString()
            assertThat(conditionSql).contains(
                "\"public\".\"report_file\".\"created_at\" >= timestamp with time zone '2023-05-21 00:00:00+00:00'\n" +
                    "  and" +
                    " \"public\".\"report_file\".\"created_at\" <= timestamp with time zone '2023-05-20 00:00:00+00:00'"
            )
        }

        @Test
        fun `Test generates filters correctly, one filters`() {
            val rawSearchString = """
            {
                "sort": {
                    "direction": "ASC",
                    "property": "action_id"
                },
                "pagination": {
                    "page": 27,
                    "limit": 5
                },
                "filters": [
                    {
                        "value": "2023-05-20T00:00:00+00:00",
                        "filterName": "UNTIL"
                    }
                ]
            }
            """.trimIndent()
            val request = MockHttpRequestMessage(rawSearchString)
            val search = ReportFileApiSearch.parse(request)
            val condition = search.getWhereClause()
            assertThat(condition).isNotNull()
            val conditionSql = condition!!.toString()
            assertThat(conditionSql)
                .contains(
                    "\"public\".\"report_file\".\"created_at\" <= timestamp with time zone '2023-05-20 00:00:00+00:00'"
                )
        }

        @Test
        fun `Test generates filters correctly, no filters`() {
            val rawSearchString = """
            {
                "sort": {
                    "direction": "ASC",
                    "property": "action_id"
                },
                "pagination": {
                    "page": 27,
                    "limit": 5
                },
                "filters": [
                ]
            }
            """.trimIndent()
            val request = MockHttpRequestMessage(rawSearchString)
            val search = ReportFileApiSearch.parse(request)
            val condition = search.getWhereClause()
            assertThat(condition).isNull()
        }
    }
}