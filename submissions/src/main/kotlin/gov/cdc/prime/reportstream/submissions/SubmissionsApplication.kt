package gov.cdc.prime.reportstream.submissions

import com.microsoft.applicationinsights.attach.ApplicationInsights
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SubmissionsApplication

fun main(args: Array<String>) {
    // Attach Azure Application Insights here.
    ApplicationInsights.attach()

    runApplication<SubmissionsApplication>(*args)
}