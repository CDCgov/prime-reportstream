package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import io.github.linuxforhealth.fhir.FHIRContext
import io.github.linuxforhealth.hl7.ConverterOptions
import io.github.linuxforhealth.hl7.message.HL7MessageEngine
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle
import java.io.BufferedReader
import java.io.StringReader

/**
 * A set of behaviors and defaults for FHIR encoding and decoding.
 */
object FhirTranscoder : Logging {

    /**
     * Build a HL7MessageEngine for converting HL7 -> FHIR with provided [options].
     * @return the message engine
     */
    fun getMessageEngine(options: ConverterOptions? = null): HL7MessageEngine {
        val finalOptions =
            options ?: ConverterOptions.Builder().withBundleType(Bundle.BundleType.MESSAGE).withPrettyPrint().build()
        val context = FHIRContext(
            finalOptions.isPrettyPrint,
            finalOptions.isValidateResource,
            finalOptions.properties,
            finalOptions.zoneIdText
        )

        return HL7MessageEngine(context, finalOptions.bundleType)
    }

    /**
     * The default FhirContext. Used for JSON encoding/decoding
     * FhirContext is used under the hood in the FHIRContext referenced above
     */
    private val defaultContext: FhirContext = FhirContext.forR4()

    /**
     * Encode a FHIR [bundle] to a string using [parser].
     * @return a JSON string of the bundle
     */
    fun encode(bundle: Bundle, parser: IParser = defaultContext.newJsonParser()): String {
        return parser.encodeResourceToString(bundle)
    }

    /**
     * Decode a FHIR bundle from a [json] string using [parser].
     * @return a FHIR bundle
     */
    fun decode(json: String, parser: IParser = defaultContext.newJsonParser()): Bundle {
        return parser.parseResource(Bundle::class.java, json)
    }

    /**
     * Converts a FHIR [rawMessage] in ndjson format (Newline Delimited Json) into [Bundle] objects
     * and logs any error messages in [actionLogger]
     *
     * @returns a list of list of [Bundle]
     */
    fun getBundles(rawMessage: String, actionLogger: ActionLogger): List<Bundle> {
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
                    val bundle = decode(line)
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