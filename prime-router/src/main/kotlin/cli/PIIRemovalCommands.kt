package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
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

class PIIRemovalCommands : CliktCommand(
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

    val idPaths = arrayListOf(
        "Bundle.entry.resource.ofType(Patient).identifier.value",
        "Bundle.entry.resource.ofType(DiagnosticReport).identifier.value",
        "Bundle.entry.resource.ofType(ServiceRequest).requester.resolve().practitioner.resolve().identifier.value",
        "Bundle.entry.resource.ofType(ServiceRequest)" +
            ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request\")" +
            ".extension(\"OBR.3\").value" +
            ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority\")" +
            ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id\").value",
        "Bundle.entry.resource.ofType(DiagnosticReport).identifier" +
            ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority\")" +
            ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id\")",
        "Bundle.entry.resource.ofType(Specimen)" +
            ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id\").value",
        "Bundle.entry.resource.ofType(ServiceRequest)" +
            ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request\")" +
            ".extension(\"OBR.2\").value" +
            ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority\")" +
            ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id\").value"
    )

    override fun run() {
        // Read the contents of the file
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")

        // Check on the extension of the file for supported operations
        if (inputFile.extension.uppercase() != "FHIR") {
            throw CliktError("File ${inputFile.absolutePath} is not a FHIR file.")
        }
        var bundle = FhirTranscoder.decode(contents)

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

        bundle = FhirTransformer("classpath:/metadata/fhir_transforms/common/remove-pii-enrichment.yml").process(bundle)

        val jsonObject = JacksonMapperUtilities.defaultMapper
            .readValue(FhirTranscoder.encode(bundle), Any::class.java)
        var prettyText = JacksonMapperUtilities.defaultMapper.writeValueAsString(jsonObject)
        prettyText = replaceIds(bundle, prettyText)

        // Write the output to the screen or a file.
        if (outputFile != null) {
            outputFile!!.writeText(prettyText, Charsets.UTF_8)
        }
        echo("Wrote output to ${outputFile!!.absolutePath}")
    }

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

    // unfortunately needs to be repeated because thye do not share a common ancestor
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

    private fun replaceIds(bundle: Bundle, prettyText: String): String {
        var updatedBundle = prettyText
        idPaths.forEach { path ->
            updatedBundle = replaceId(bundle, path, updatedBundle)
        }
        return updatedBundle
    }

    private fun replaceId(bundle: Bundle, path: String, prettyText: String): String {
        FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            path
        ).forEach { resourceId ->
            val newIdentifier = getFakeValueForElementCall("UUID")
            return prettyText.replace(resourceId.primitiveValue(), newIdentifier, true)
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