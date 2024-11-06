package gov.cdc.prime.reportstream.submissions

import com.microsoft.applicationinsights.attach.ApplicationInsights
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SubmissionsApplication

fun main(args: Array<String>) {
    ApplicationInsights.attach()
    runApplication<SubmissionsApplication>(*args)
}