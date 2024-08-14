package gov.cdc.prime.router.logging

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.logging.LogMeasuredTime.measureAndLogDurationWithReturnedValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.apache.logging.log4j.kotlin.KotlinLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class LogMeasuredTimeTests {

    @AfterEach
    fun afterEach() {
        unmockkStatic(MDC::put)
    }

    @Test
    fun `logs the duration`() {
        val mockLogger = mockk<KotlinLogger>()
        mockkObject(LogMeasuredTime)
        every { LogMeasuredTime getProperty "logger" } returns mockLogger
        every { mockLogger.info(any(String::class)) } returns Unit
        mockkStatic(MDC::put)
        every { MDC.put(any(), any()) } returns Unit

        val value = measureAndLogDurationWithReturnedValue("test") {
            "returned value"
        }

        assertThat(value).isEqualTo("returned value")
        verify(exactly = 1) {
            mockLogger.info("test")
            MDC.put("durationInSecs", any(String::class))
        }
    }

    @Test
    fun `logs the duration with extra logging context passed in`() {
        val mockLogger = mockk<KotlinLogger>()
        mockkObject(LogMeasuredTime)
        every { LogMeasuredTime getProperty "logger" } returns mockLogger
        every { mockLogger.info(any(String::class)) } returns Unit
        mockkStatic(MDC::put)
        every { MDC.put(any(), any()) } returns Unit

        val value = measureAndLogDurationWithReturnedValue("test", mapOf("foo" to "bar")) {
            "returned value"
        }

        assertThat(value).isEqualTo("returned value")
        verify(exactly = 1) {
            mockLogger.info("test")
            MDC.put("foo", "bar")
            MDC.put("durationInSecs", any(String::class))
        }
    }

    @Test
    fun `logs the duration and the size if the returned value is a collection`() {
        val mockLogger = mockk<KotlinLogger>()
        mockkObject(LogMeasuredTime)
        every { LogMeasuredTime getProperty "logger" } returns mockLogger
        every { mockLogger.info(any(String::class)) } returns Unit
        mockkStatic(MDC::put)
        every { MDC.put(any(), any()) } returns Unit

        val value = measureAndLogDurationWithReturnedValue("test", mapOf("foo" to "bar")) {
            listOf("returned value")
        }

        assertThat(value).isEqualTo(listOf("returned value"))
        verify(exactly = 1) {
            mockLogger.info("test")
            MDC.put("foo", "bar")
            MDC.put("durationInSecs", any(String::class))
            MDC.put("size", "1")
        }
    }
}