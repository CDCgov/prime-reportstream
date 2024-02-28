package gov.cdc.prime.router.db

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.history.db.DeliveryApiSearch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeliveryDatabaseAccessTest {

    @Nested
    inner class DeliveryApiSearchTest {

        @Test
        fun `Test generates filters correctly`() {
            val rawSearchString = """
                {
                    "sort": {
                        "direction": "DESC",
                        "property": "type"
                    },
                    "pagination": {
                        "page": 1,
                        "limit": 100
                    },
                    "filters": [
                        {
                            "value": "2023-05-21T00:00:00+00:00",
                            "filterName": "UNTIL"
                        }
                    ]
                }
            """.trimIndent()
            val request = MockHttpRequestMessage(rawSearchString)
            val search = DeliveryApiSearch.parse(request)
            val condition = search.getWhereClause()
            assertThat(condition).isNotNull()
            val conditionSql = condition!!.toString()
            assertThat(conditionSql)
                .contains("\"delivery\".\"created_at\" <= timestamp with time zone '2023-05-21 00:00:00+00:00'")
        }

        @Test
        fun `Test generates multiple filters correctly`() {
            val rawSearchString = """
                {
                    "sort": {
                        "direction": "DESC",
                        "property": "type"
                    },
                    "pagination": {
                        "page": 1,
                        "limit": 100
                    },
                    "filters": [
                        {
                            "value": "2023-05-21T00:00:00+00:00",
                            "filterName": "UNTIL"
                        },
                        {
                            "value": "2023-05-21T00:00:00+00:00",
                            "filterName": "SINCE"
                        }
                    ]
                }
            """.trimIndent()
            val request = MockHttpRequestMessage(rawSearchString)
            val search = DeliveryApiSearch.parse(request)
            val condition = search.getWhereClause()
            assertThat(condition).isNotNull()
            val conditionSql = condition!!.toString()
            assertThat(conditionSql)
                .contains(
                    "(\n" +
                        "  \"delivery\".\"created_at\" <= timestamp with time zone '2023-05-21 00:00:00+00:00'\n" +
                        "  and \"delivery\".\"created_at\" >= timestamp with time zone '2023-05-21 00:00:00+00:00'\n" +
                        ")"
                )
        }
    }
}