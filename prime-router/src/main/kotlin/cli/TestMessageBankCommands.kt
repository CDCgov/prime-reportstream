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

    private fun replaceIds(bundle: Bundle, prettyText: String): String {
        var updatedBundle = prettyText
        updatedBundle = replaceId(bundle, "Bundle.entry.resource.ofType(Patient).identifier.value", updatedBundle)
        updatedBundle = replaceId(
            bundle,
            "Bundle.entry.resource.ofType(DiagnosticReport).identifier.value", updatedBundle
        )
        updatedBundle = replaceId(
            bundle,
            "Bundle.entry.resource.ofType(ServiceRequest).requester.resolve().practitioner.resolve().identifier.value",
            updatedBundle
        )
        updatedBundle = replaceId(
            bundle,
            "Bundle.entry.resource.ofType(ServiceRequest)" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request\")" +
                ".extension(\"OBR.3\").value" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority\")" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id\").value",
                    updatedBundle
        )
        updatedBundle = replaceId(
            bundle,
            "Bundle.entry.resource.ofType(DiagnosticReport).identifier" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority\")" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id\")",
            updatedBundle
        )
        updatedBundle = replaceId(
            bundle,
            "Bundle.entry.resource.ofType(Specimen)" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id\").value",
            updatedBundle
        )
        updatedBundle = replaceId(
            bundle,
            "Bundle.entry.resource.ofType(ServiceRequest)" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/obr-observation-request\")" +
                ".extension(\"OBR.2\").value" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/assigning-authority\")" +
                ".extension(\"https://reportstream.cdc.gov/fhir/StructureDefinition/universal-id\").value",
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