package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isSuccess
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test

class JacksonMapperUtilitiesTests {
    @Test
    fun `test mapper date time serialization format`() {
        data class JsonTestClass(var timestamp: OffsetDateTime)

        fun testJsonDateTime(jsonResult: String) {
            val jsonParser = JsonFactory().createParser(jsonResult)
            jsonParser.nextToken()
            jsonParser.nextToken()
            assertThat(jsonParser.nextToken()).isEqualTo(JsonToken.VALUE_STRING)
            val jsonValue = jsonParser.text
            assertThat(jsonValue).isNotEmpty()
            assertThat(jsonValue).endsWith("Z")
            assertThat { JacksonMapperUtilities.timestampFormatter.parse(jsonValue) }.isSuccess()
        }

        var estTimestamp = JsonTestClass(OffsetDateTime.now(ZoneOffset.of("-0500")))
        testJsonDateTime(JacksonMapperUtilities.defaultMapper.writeValueAsString(estTimestamp))
        testJsonDateTime(JacksonMapperUtilities.allowUnknownsMapper.writeValueAsString(estTimestamp))

        estTimestamp = JsonTestClass(OffsetDateTime.now(ZoneOffset.UTC))
        testJsonDateTime(JacksonMapperUtilities.defaultMapper.writeValueAsString(estTimestamp))
        testJsonDateTime(JacksonMapperUtilities.allowUnknownsMapper.writeValueAsString(estTimestamp))
    }
}