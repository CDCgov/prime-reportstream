package gov.cdc.prime.router.azure.observability.event

import com.microsoft.applicationinsights.TelemetryClient
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class AzureEventServiceTest {

    inner class Fixture {
        val event = TestEvent(
            "some value",
            25
        )
        val mockedTelemetryClient = mockk<TelemetryClient>(relaxed = true)
        val azureEventService = AzureEventServiceImpl(mockedTelemetryClient)
    }

    @Test
    fun `pass event data class to Azure telemetry client`() {
        val f = Fixture()

        f.azureEventService.trackEvent(f.event)

        verify(exactly = 1) {
            f.mockedTelemetryClient.trackEvent(
                "TestEvent",
                mapOf(
                    "property1" to "some value",
                    "property2" to "25"
                ),
                emptyMap()
            )
        }
    }

    @Test
    fun `pass event data class to Azure telemetry client via ReportStreamEventName`() {
        val f = Fixture()

        f.azureEventService.trackEvent(ReportStreamEventName.REPORT_RECEIVED, f.event)

        verify(exactly = 1) {
            f.mockedTelemetryClient.trackEvent(
                ReportStreamEventName.REPORT_RECEIVED.name,
                mapOf(
                    "property1" to "some value",
                    "property2" to "25"
                ),
                emptyMap()
            )
        }
    }
}

data class TestEvent(val property1: String, val property2: Int) : AzureCustomEvent