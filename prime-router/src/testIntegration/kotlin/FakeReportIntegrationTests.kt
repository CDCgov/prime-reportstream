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
        val matchingCityRows = zipCodeTable!!.filter(
            "city",
            mapOf(
                "state_abbr" to state,
                "county" to county
            )
        )
        val matchingZipRows = zipCodeTable.filter(
            "zipcode",
            mapOf(
                "state_abbr" to state,
                "county" to county
            )
        )
        // act
        val context = FakeReport.RowContext(
            metadata::findLookupTable,
            state,
            null,
            county
        )
        println(matchingCityRows.joinToString())
        println(matchingZipRows.joinToString())
        println("${context.city} - ${context.zipCode}")
        // assert
        assertThat(matchingCityRows).contains(context.city)
        assertThat(matchingZipRows).contains(context.zipCode)
    }
}