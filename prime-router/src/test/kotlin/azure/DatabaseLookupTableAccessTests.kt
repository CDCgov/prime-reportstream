package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.jooq.JSONB
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DatabaseLookupTableAccessTests {
    @Test
    fun `test extract headers from json`() {
        val headers = DatabaseLookupTableAccess
            .extractTableHeadersFromJson(JSONB.jsonb("""{"a": "value1", "b": "value2"}"""))
        assertThat(headers).isEqualTo(listOf("a", "b"))

        assertFailsWith<IllegalArgumentException>(
            block = {
                DatabaseLookupTableAccess
                    .extractTableHeadersFromJson(JSONB.jsonb("{}"))
            }
        )

        assertFailsWith<IllegalArgumentException>(
            block = {
                DatabaseLookupTableAccess
                    .extractTableHeadersFromJson(JSONB.jsonb("[]"))
            }
        )

        assertFailsWith<IllegalArgumentException>(
            block = {
                DatabaseLookupTableAccess
                    .extractTableHeadersFromJson(JSONB.jsonb(""))
            }
        )
    }
}