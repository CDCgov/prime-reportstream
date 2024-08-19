package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType

class TestMessageBankCommands : CliktCommand(
    name = "piiRemoval",
    help = "Remove PII"
) {
    /**
     * The input file to process.
     */
    private val inputFile by option("-i", "--input-file", help = "Input file to process")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    /**
     * Optional output file.  if no output file is specified then the output is printed to the screen.
     */
    private val outputFile by option("-o", "--output-file", help = "output file")
        .file()

    override fun run() {
        // Read the contents of the file
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")

        // Check on the extension of the file for supported operations
        if (inputFile.extension.uppercase() != "FHIR") {
            throw CliktError("File ${inputFile.absolutePath} is not a FHIR file.")
        }
        val bundle = FhirTranscoder.decode(contents)

        bundle.entry.map { it.resource }.filterIsInstance<Patient>()
            .forEach { patient ->
                val state = FhirPathUtils.evaluate(
                    null,
                    bundle,
                    bundle,
                    "Bundle.entry.resource.ofType(Patient).address.state"
                ).first().primitiveValue()

                patient.name.forEach { name ->
                    name.given = mutableListOf(StringType(getFakeValueForElementCall("PERSON_GIVEN_NAME")))
                    name.family = getFakeValueForElementCall("PERSON_FAMILY_NAME")
                }
                patient.address.forEach { address ->
                    address.line = mutableListOf(StringType(getFakeValueForElementCall("STREET")))
                    address.city = getFakeValueForElementCallUsingGeoData("CITY", state)
                    address.postalCode = getFakeValueForElementCallUsingGeoData("POSTAL_CODE", state)
                    address.district = getFakeValueForElementCallUsingGeoData("COUNTY", state)
                }
                patient.telecom.forEach { telecom ->
                    if (telecom.system == ContactPoint.ContactPointSystem.EMAIL) {
                        telecom.value = getFakeValueForElementCall("EMAIL")
                    } else if (telecom.system == ContactPoint.ContactPointSystem.PHONE ||
                        telecom.system == ContactPoint.ContactPointSystem.FAX
                    ) {
                        telecom.value = getFakeValueForElementCall("TELEPHONE")
                    }
                }
//                patient.birthDate = SimpleDateFormat("yyyy-MM-ddThh:mm:ss.SSSZ").parse(getFakeValueForElementCall("BIRTHDAY"))
                patient.contact.forEach { contact ->
                    contact.name.given = mutableListOf(StringType(getFakeValueForElementCall("PERSON_GIVEN_NAME")))
                    contact.name.family = getFakeValueForElementCall("PERSON_FAMILY_NAME")
                    contact.address.line = mutableListOf(StringType(getFakeValueForElementCall("STREET")))
                    contact.address.city = getFakeValueForElementCallUsingGeoData("CITY", state)
                    contact.address.postalCode = getFakeValueForElementCallUsingGeoData("POSTAL_CODE", state)
                    contact.address.district = getFakeValueForElementCallUsingGeoData("COUNTY", state)
                    contact.telecom.forEach { telecom ->
                        telecom.value = getFakeValueForElementCall("TELEPHONE")
                    }
                    contact.organization.identifier.value = getFakeValueForElementCall("ID_NUMBER")
                }
//                patient.managingOrganization.
            }

//        bundle.entry.map { it.resource }.filterIsInstance<ServiceRequest>()
//            .forEach { serviceRequest ->
//                serviceRequest.subject.resource.
//                serviceRequest.requester =
//                patient.name.forEach { name ->
//                    name.given =
//                        name.family =
//                }
//                patient.address.forEach {
//                    it.line = null
//                    it.city = null
//                    it.postalCode =
//                        it.district =
//                }
//                patient.telecom.forEach { telecom ->
//                    telecom.value =
//                }
//                patient.birthDate = null
//                patient.deceased = null
//                patient.identifier = null
//                patient.contact = null
//            }

        val jsonObject = JacksonMapperUtilities.defaultMapper
            .readValue(FhirTranscoder.encode(bundle), Any::class.java)
        var prettyText = JacksonMapperUtilities.defaultMapper.writeValueAsString(jsonObject)
        prettyText = replaceIds(bundle, prettyText)
        prettyText = replaceNeededExtensions(bundle, prettyText)

        // Write the output to the screen or a file.
        if (outputFile != null) {
            outputFile!!.writeText(prettyText, Charsets.UTF_8)
        }
        echo("Wrote output to ${outputFile!!.absolutePath}")
    }

    private fun replaceIds(bundle: Bundle, prettyText: String): String {
        var updatedBundle = prettyText
        updatedBundle = replaceId(bundle, "Bundle.entry.resource.ofType(Patient).identifier.value", updatedBundle)
        updatedBundle = replaceId(
            bundle,
            "Bundle.entry.resource.ofType(DiagnosticReport).identifier.value", updatedBundle
        )
        updatedBundle = replaceId(
            bundle,
            "Bundle.entry.resource.ofType(ServiceRequest)" +
                ".requester.resolve().practitioner.resolve().identifier.value",
                    updatedBundle
        )
        return updatedBundle
    }

    private fun replaceId(bundle: Bundle, path: String, prettyText: String): String {
        FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            path
        ).forEach { resourceId ->
            val newIdentifier = getFakeValueForElementCall("ID_NUMBER")
            return prettyText.replace(resourceId.primitiveValue(), newIdentifier, true)
        }
        return prettyText
    }

    private fun replaceNeededExtensions(bundle: Bundle, prettyText: String): String {
        // middle name
        FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(Patient).name[0]" +
                ".extension(%`rsext-xpn-human-name`).extension.where(url=\"XPN.3\").value"
        ).forEach { middleName ->
            val newMiddleName = getFakeValueForElementCall("PERSON_GIVEN_NAME")
            return prettyText.replace(middleName.primitiveValue(), newMiddleName, true)
        }

        // patient address 2
        FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(Patient).address" +
                ".extension(%`rsext-xad-address`).extension.where(url = \"XAD.2\").value"
        ).forEach { middleName ->
            val newMiddleName = getFakeValueForElementCall("PATIENT_STREET_ADDRESS_2")
            return prettyText.replace(middleName.primitiveValue(), newMiddleName, true)
        }

        return prettyText
    }

    private fun getFakeValueForElementCall(dataType: String): String {
        return CustomFhirPathFunctions().getFakeValueForElement(
            mutableListOf(mutableListOf(StringType(dataType)))
        )[0].primitiveValue()
    }

    private fun getFakeValueForElementCallUsingGeoData(dataType: String, state: String): String {
        return CustomFhirPathFunctions().getFakeValueForElement(
            mutableListOf(mutableListOf(StringType(dataType)), mutableListOf(StringType(state)))
        )[0].primitiveValue()
    }
}