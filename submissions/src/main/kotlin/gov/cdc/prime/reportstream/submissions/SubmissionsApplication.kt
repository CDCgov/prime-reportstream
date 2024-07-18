package gov.cdc.prime.reportstream.submissions

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SubmissionsApplication

fun main(args: Array<String>) {
    runApplication<SubmissionsApplication>(*args)
}