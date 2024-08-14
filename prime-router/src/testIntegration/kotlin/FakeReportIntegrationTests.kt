package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test

class FakeReportIntegrationTests {
    @Test
    fun `test row context getting expected zip code results`() {
        // arrange
        val metadata = Metadata.getInstance()
        assertThat(metadata).isNotNull()
        val zipCodeTable = metadata.findLookupTable("zip-code-data")
        assertThat(zipCodeTable).isNotNull()
        val state = "VT"
        val county = "Rutland"
        val matchingCityRows = zipCodeTable!!.FilterBuilder().startsWithIgnoreCase("state_abbr", state)
            .startsWithIgnoreCase("county", county).findAllUnique("city")
        val matchingZipRows = zipCodeTable.FilterBuilder().startsWithIgnoreCase("state_abbr", state)
            .startsWithIgnoreCase("county", county).findAllUnique("zipcode")
        // act
        val context = FakeReport.RowContext(
            metadata,
            state,
            null,
            county
        )
        assertThat(matchingCityRows).contains(context.city)
        assertThat(matchingZipRows).contains(context.zipCode)
    }
}