package gov.cdc.prime.router.db

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ReportFileDatabaseAccessTest {

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