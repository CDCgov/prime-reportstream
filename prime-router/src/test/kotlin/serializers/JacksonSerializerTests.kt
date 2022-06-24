package gov.cdc.prime.router.serializers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.Sender
import kotlin.test.Test

/**
 * Tests to verify our jacksonMapper polymorphism on Sender is working correctly
 */
class JacksonSerializerTests {

    @Test
    fun `test serialize and deserialize CovidSender`() {
        val startSender = CovidSender(
            "test-sender",
            "testing",
            Sender.Format.HL7,
            schemaName = "fake-schema"
        )
        val mapper = ObjectMapper().registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )

        // serialize sender to JSON
        val json = mapper.writeValueAsString(startSender)

        // deserialize sender to Object
        val endSender = mapper.readValue(json, Sender::class.java)

        // compare properties of start and end objects
        assertThat(startSender.javaClass.name).isEqualTo(endSender.javaClass.name)
        assertThat(startSender.name).isEqualTo(endSender.name)
        assertThat(startSender.topic).isEqualTo(endSender.topic)
    }
}