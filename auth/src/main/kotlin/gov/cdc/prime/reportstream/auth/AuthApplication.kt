package gov.cdc.prime.reportstream.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AuthApplication

fun main(args: Array<String>) {
//    ApplicationInsights.attach()
    runApplication<AuthApplication>(*args)
}