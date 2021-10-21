package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.jooq.JSONB
import org.junit.jupiter.api.Test

class DatabaseLookupTableAccessTests {
    @Test
    fun `test extract headers from json`() {
        var headers = DatabaseLookupTableAccess
            .extractTableHeadersFromJson(JSONB.jsonb("{\"a\": \"value1\", \"b\": \"value2\"}"))
        assertThat(headers).isEqualTo(listOf("a", "b"))

        headers = DatabaseLookupTableAccess
            .extractTableHeadersFromJson(JSONB.jsonb("{}"))
        assertThat(headers.isEmpty()).isTrue()
    }
}