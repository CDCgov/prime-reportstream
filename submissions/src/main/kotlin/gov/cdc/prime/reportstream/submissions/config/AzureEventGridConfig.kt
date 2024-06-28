package gov.cdc.prime.reportstream.submissions.config

import com.azure.core.credential.AzureKeyCredential
import com.azure.messaging.eventgrid.EventGridEvent
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder
import com.azure.messaging.eventgrid.EventGridPublisherAsyncClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AzureEventGridConfig {

    @Value("\${azure.eventgrid.endpoint}")
    private lateinit var eventGridEndpoint: String

    @Value("\${azure.eventgrid.key}")
    private lateinit var eventGridKey: String

    @Bean
    fun eventGridPublisherClient(): EventGridPublisherAsyncClient<EventGridEvent>? {
        return EventGridPublisherClientBuilder()
            .endpoint(eventGridEndpoint)
            .credential(AzureKeyCredential(eventGridKey))
            .buildEventGridEventPublisherAsyncClient()
    }
}
