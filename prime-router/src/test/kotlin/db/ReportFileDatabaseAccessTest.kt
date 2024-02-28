package gov.cdc.prime.router.db

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class ReportFileDatabaseAccessTest {

    @Nested
    inner class ReportFileDatabaseTests() {

        @Test
        fun `Test can find a single report`() {
            createReport()
            val reportFileDatabaseAccess =
                ReportFileDatabaseAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess)
            val rows = reportFileDatabaseAccess.getReports(ReportFileApiSearch(emptyList(), null))
            assertThat(rows.totalCount).isEqualTo(1)
        }

        @Test
        fun `Test returns no data`() {
            val reportFileDatabaseAccess =
                ReportFileDatabaseAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess)
            val rows = reportFileDatabaseAccess.getReports(ReportFileApiSearch(emptyList(), null))
            assertThat(rows.totalCount).isEqualTo(0)
        }
    }

    private fun createReport() {
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val action = Action()
            val actionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
            val report = ReportFile().setSchemaTopic(Topic.FULL_ELR).setReportId(UUID.randomUUID())
                .setActionId(actionId).setSchemaName("schema").setBodyFormat("hl7").setItemCount(1)
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                report, txn, action
            )
        }
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