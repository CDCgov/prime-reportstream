package gov.cdc.prime.router.serializers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.CovidSenderAPI
import gov.cdc.prime.router.azure.SenderAPI
import kotlin.test.Test

/**
 * Tests to verify our jacksonMapper polymorphism on Sender is working correctly
 */
class JacksonSerializerTests {
    @Test
    fun `test serialize and deserialize Sender`() {
        val startSender = Sender(
            "test-sender",
            "testing",
            Sender.Format.HL7
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

    @Test
    fun `test serialize and deserialize SenderAPI`() {
        val startSender = SenderAPI(
            "test-sender",
            "testing",
            Sender.Format.HL7,
            meta = null
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
        val endSender = mapper.readValue(json, SenderAPI::class.java)

        // compare properties of start and end objects
        assertThat(startSender.javaClass.name).isEqualTo(endSender.javaClass.name)
        assertThat(startSender.name).isEqualTo(endSender.name)
        assertThat(startSender.topic).isEqualTo(endSender.topic)
    }

    @Test
    fun `test serialize and deserialize CovidSenderAPI`() {
        val startSender = CovidSenderAPI(
            "test-sender",
            "testing",
            Sender.Format.HL7,
            meta = null,
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
        val endSender = mapper.readValue(json, SenderAPI::class.java)

        // compare properties of start and end objects
        assertThat(startSender.javaClass.name).isEqualTo(endSender.javaClass.name)
        assertThat(startSender.name).isEqualTo(endSender.name)
        assertThat(startSender.topic).isEqualTo(endSender.topic)
    }
}