package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Organization.OrganizationContactComponent
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Patient.ContactComponent
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.StringType

class PIIRemovalCommands :
    CliktCommand(
        name = "piiRemoval",
    ) {
    override fun help(context: Context): String = "Remove PII"

    /**
     * The input file to process.
     */
    private val inputFile by option("-i", "--input-file", help = "Input file to process")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    /**
     * Output file to write the data with PII removed.
     */
    private val outputFile by option("-o", "--output-file", help = "output file")
        .file()

    /**
     * FHIR paths for ids to remove
     */
    val idPaths = arrayListOf(
        "Bundle.entry.resource.ofType(Patient).identifier.value",
        "Bundle.entry.resource.ofType(ServiceRequest).requester.resolve().practitioner.resolve().identifier.value",
        "Bundle.entry.resource.ofType(DiagnosticReport).identifier.value"
    )

    /**
     * Method called when the command is run
     */
    override fun run() {
        // Read the contents of the file
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")

        // Check on the extension of the file for supported operations
        if (inputFile.extension.uppercase() != "FHIR") {
            throw CliktError("File ${inputFile.absolutePath} is not a FHIR file.")
        }
        val bundle = FhirTranscoder.decode(contents)

        // Write the output to the screen or a file.
        if (outputFile != null) {
            outputFile!!.writeText(removePii(bundle), Charsets.UTF_8)
        }
        echo("Wrote output to ${outputFile!!.absolutePath}")
    }

    fun removePii(bundle: Bundle): String {
        bundle.entry.map { it.resource }.filterIsInstance<Patient>()
            .forEach { patient ->
                patient.name.forEach { name ->
                    name.given = mutableListOf(StringType(getFakeValueForElementCall("PERSON_GIVEN_NAME")))
                }
                patient.address.forEach { address ->
                    address.line = mutableListOf(StringType(getFakeValueForElementCall("STREET")))
                }
                patient.telecom.forEach { telecom ->
                    handleTelecom(telecom)
                }
                patient.contact.forEach { contact ->
                    handlePatientContact(contact)
                }
            }

        bundle.entry.map { it.resource }.filterIsInstance<Organization>()
            .forEach { organization ->
                organization.address.forEach { address ->
                    address.line = mutableListOf(StringType(getFakeValueForElementCall("STREET")))
                }
                organization.telecom.forEach { telecom ->
                    handleTelecom(telecom)
                }
                organization.contact.forEach { contact ->
                    handleOrganizationalContact(contact)
                }
            }

        bundle.entry.map { it.resource }.filterIsInstance<Practitioner>()
            .forEach { practitioner ->
                practitioner.address.forEach { address ->
                    address.line = mutableListOf(StringType(getFakeValueForElementCall("STREET")))
                    address.city = getFakeValueForElementCallUsingGeoData("CITY", address.state)
                    address.postalCode = getFakeValueForElementCallUsingGeoData("POSTAL_CODE", address.state)
                    address.district = getFakeValueForElementCallUsingGeoData("COUNTY", address.state)
                }
                practitioner.telecom.forEach { telecom ->
                    handleTelecom(telecom)
                }
                practitioner.name.forEach { name ->
                    name.given = mutableListOf(StringType(getFakeValueForElementCall("PERSON_GIVEN_NAME")))
                }
            }

        val bundleAfterTransform = FhirTransformer(
            "classpath:/metadata/fhir_transforms/common/remove-pii-enrichment.yml"
        ).process(bundle)

        val jsonObject = JacksonMapperUtilities.defaultMapper
            .readValue(FhirTranscoder.encode(bundleAfterTransform), Any::class.java)
        val prettyText = JsonMapper.builder().build().writeValueAsString(jsonObject)
        return replaceIds(bundleAfterTransform, prettyText)
    }

    /**
     * Replaces the patient contact PII data
     */
    private fun handlePatientContact(contact: ContactComponent): ContactComponent {
        contact.name.given = mutableListOf(StringType(getFakeValueForElementCall("PERSON_GIVEN_NAME")))
        contact.name.family = getFakeValueForElementCall("PERSON_FAMILY_NAME")
        contact.address.line = mutableListOf(StringType(getFakeValueForElementCall("STREET")))
        contact.address.city = getFakeValueForElementCallUsingGeoData("CITY", contact.address.state)
        contact.address.postalCode = getFakeValueForElementCallUsingGeoData("POSTAL_CODE", contact.address.state)
        contact.address.district = getFakeValueForElementCallUsingGeoData("COUNTY", contact.address.state)
        contact.telecom.forEach { telecom ->
            handleTelecom(telecom)
        }
        return contact
    }

    /**
     * Replaces the organizational contact PII. Unfortunately needs to be repeated because they do not share a common
     * ancestor
     */
    private fun handleOrganizationalContact(contact: OrganizationContactComponent): OrganizationContactComponent {
        contact.name.given = mutableListOf(StringType(getFakeValueForElementCall("PERSON_GIVEN_NAME")))
        contact.name.family = getFakeValueForElementCall("PERSON_FAMILY_NAME")
        contact.address.line = mutableListOf(StringType(getFakeValueForElementCall("STREET")))
        contact.address.city = getFakeValueForElementCallUsingGeoData("CITY", contact.address.state)
        contact.address.postalCode = getFakeValueForElementCallUsingGeoData("POSTAL_CODE", contact.address.state)
        contact.address.district = getFakeValueForElementCallUsingGeoData("COUNTY", contact.address.state)
        contact.telecom.forEach { telecom ->
            handleTelecom(telecom)
        }
        return contact
    }

    /**
     * Replaces PII in a telecom
     */
    private fun handleTelecom(telecom: ContactPoint): ContactPoint {
        if (telecom.system == ContactPoint.ContactPointSystem.EMAIL) {
            telecom.value = getFakeValueForElementCall("EMAIL")
        } else if (telecom.system == ContactPoint.ContactPointSystem.PHONE ||
            telecom.system == ContactPoint.ContactPointSystem.FAX
        ) {
            telecom.value = getFakeValueForElementCall("TELEPHONE")
        }
        return telecom
    }

    /**
     * Replaces the required IDs in the bundle
     */
    private fun replaceIds(bundle: Bundle, prettyText: String): String {
        var updatedBundle = prettyText
        idPaths.forEach { path ->
            updatedBundle = replaceId(bundle, path, updatedBundle)
        }
        return updatedBundle
    }

    /**
     * Replaces the ID for a specific ID
     */
    private fun replaceId(bundle: Bundle, path: String, prettyText: String): String {
        FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            path
        ).forEach { resourceId ->
            if (resourceId.primitiveValue() != null) {
                val newIdentifier = getFakeValueForElementCall("UUID")
                return prettyText.replace(resourceId.primitiveValue(), newIdentifier, true)
            }
        }
        return prettyText
    }

    /**
     * Gets a fake value for a given type
     */
    private fun getFakeValueForElementCall(dataType: String): String = CustomFhirPathFunctions().getFakeValueForElement(
        mutableListOf(mutableListOf(StringType(dataType)))
    )[0].primitiveValue()

    /**
     * Gets a fake value for a given type that requires geo data
     */
    private fun getFakeValueForElementCallUsingGeoData(
        dataType: String,
        state: String,
    ): String = CustomFhirPathFunctions().getFakeValueForElement(
        mutableListOf(mutableListOf(StringType(dataType)), mutableListOf(StringType(state)))
    )[0].primitiveValue()
}