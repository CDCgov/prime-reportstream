package gov.cdc.prime.router.fhirengine.utils

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle
import java.io.BufferedReader
import java.io.StringReader

/**
 * Converts raw FHIR data (ndjson Newline Delimited Json) to Bundle objects.
 */
class FHIRReader(private val actionLogger: ActionLogger) : Logging {
    /**
     * Returns one or more bundles read from the FHIR data in [rawMessage].
     */
    fun getBundles(rawMessage: String): List<Bundle> {
        val bundles: MutableList<Bundle> = mutableListOf()
        if (rawMessage.isBlank()) {
            actionLogger.error(InvalidReportMessage("Provided raw data is empty."))
        } else {
            val bufferedReader = BufferedReader(StringReader(rawMessage))
            val iterator = bufferedReader.lineSequence().iterator()
            var index = 1
            while (iterator.hasNext()) {
                try {
                    val line = iterator.next()
                    val bundle = FhirTranscoder.decode(line)
                    if (bundle.isEmpty) {
                        actionLogger.error(InvalidReportMessage("$index: Unable to find FHIR Bundle in provided data."))
                    } else {
                        bundles.add(bundle)
                    }
                } catch (e: Exception) {
                    logger.error(e)
                    actionLogger.error(InvalidReportMessage("$index: Unable to parse FHIR data."))
                }
                index++
            }
            bufferedReader.close()
        }

        return bundles
    }
}