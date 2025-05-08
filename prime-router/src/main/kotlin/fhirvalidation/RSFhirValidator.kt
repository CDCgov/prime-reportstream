package gov.cdc.prime.router.fhirvalidation

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.IValidatorModule
import ca.uhn.fhir.validation.ValidationResult
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import java.io.File
import java.net.URL

class RSFhirValidator : Logging {

    companion object {
        val ctx: FhirContext = FhirContext.forR4()
        private val validator: FhirValidator? = getPackageValidator()
        private val parser = ctx.newJsonParser()
        val mapper = ObjectMapper()

        // Maps FHIR resource type to profile to be validated against
        val type2url = mapOf(
            "Bundle" to "http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-reporting-bundle",
            "Bundle2" to "http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-content-bundle",
            "Device" to "http://hl7.org/fhir/StructureDefinition/Device",
            "DiagnosticReport" to "http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab",
            "MessageHeader" to "http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-messageheader",
            "Observation" to "http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation",
            "Organization" to "http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-organization",
            "Patient" to "http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-patient",
            "Practitioner" to "http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner",
            "PractitionerRole" to "http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-practitionerrole",
            "Provenance" to "http://hl7.org/fhir/us/core/StructureDefinition/us-core-provenance",
            "Specimen" to "http://hl7.org/fhir/us/core/StructureDefinition/us-core-specimen",
            "ServiceRequest" to "http://hl7.org/fhir/us/core/StructureDefinition/us-core-servicerequest",
        )

        fun getPackageValidator(): FhirValidator? {
            val usCore = NpmPackageValidationSupport(ctx)
            usCore.loadPackageFromClasspath("hl7.fhir.us.core-7.0.0.tgz")
            val usphpl = NpmPackageValidationSupport(ctx)
            usphpl.loadPackageFromClasspath("usphpl-package.r4.tgz")
            val supportChain = ValidationSupportChain(
                usphpl,
                usCore,
                DefaultProfileValidationSupport(ctx),
                CommonCodeSystemsTerminologyService(ctx),
                InMemoryTerminologyServerValidationSupport(ctx),
                SnapshotGeneratingValidationSupport(ctx)
            )
            return getFhirValidator(supportChain)
        }

        fun getFhirValidator(supportChain: ValidationSupportChain): FhirValidator? {
            val validator = ctx.newValidator()
            val module: IValidatorModule = FhirInstanceValidator(supportChain)
            validator.registerValidatorModule(module)
            return validator
        }
    }

    fun validateResource(resource: IBaseResource, addProfiles: Boolean = true): ValidationResult? {
        if (addProfiles) {
            prepareResourceForValidation(resource)
        }
        return validator?.validateWithResult(resource) ?: null
    }

    fun validateResource(url: URL, addProfiles: Boolean = true): ValidationResult? {
        val resource = parser.parseResource(url.openStream())
        return validateResource(resource, addProfiles)
    }

    // path should be within resources folder starting with /
    fun validateFhirInResourcesDir(path: String, addProfiles: Boolean = true): ValidationResult? {
        val url: URL = javaClass.getResource(path)
        return validateResource(url, addProfiles)
    }

    // Adds a meta.profile to resource if it doesn't exist
    private fun prepareResourceForValidation(resource: IBaseResource) {
        if (resource.meta.profile.isEmpty()) {
            val type = resource.fhirType()
            val url = type2url[type]
            if (url == null) {
                logger.warn("No profile for resource type: $type")
            } else {
                resource.meta.addProfile(url)
            }
        }
        if (resource is Bundle) {
            val bundle = resource as Bundle
            for (child in bundle.entry) {
                prepareResourceForValidation(child.resource)
            }
        }
    }

    fun mapJsonNodes(node: JsonNode, op: (JsonNode) -> Unit) {
        op(node)
        for (el in node.elements()) {
            mapJsonNodes(el, op)
        }
    }

    fun parseFileToNode(path: String): JsonNode {
        val file = File(path)
        val root = mapper.readTree(file)
        return root
    }

    fun addFhirProfiles(path: String) {
        val root = parseFileToNode(path)
        mapJsonNodes(root) { node ->
            if (node.isObject) {
                val ob = node as ObjectNode
                if (ob.has("resourceType")) {
                    val type = ob.get("resourceType").asText()
                    println(type)
                    val url = type2url[type]
                    if (url != null) {
                        addNodeProfile(ob, url)
                    }
                }
            }
        }
        val writer = mapper.writer(DefaultPrettyPrinter())
        // For now the new file name has .json tacked on
        writer.writeValue(File(path + ".json"), root)
    }

    private fun addNodeProfile(ob: ObjectNode, profileUrl: String) {
        // if meta exists, use it
        if (ob.get("meta") == null) {
            ob.putObject("meta")
        }
        val meta: ObjectNode = ob.get("meta") as ObjectNode
        // If profile exists, leave it alone
        if (meta.get("profile") == null) {
            meta.putArray("profile")
            val profile: ArrayNode = meta.get("profile") as ArrayNode
            profile.add(profileUrl)
        }
    }
}

// A validator that directly loads profiles to validator
//    fun getProfileValidator(): FhirValidator? {
//        val defaultSupport = DefaultProfileValidationSupport(ctx)
//        val profileSupport = PrePopulatedValidationSupport(ctx)
// //        val terminologySupport = InMemoryTerminologyServerValidationSupport(ctx)
//        val supportChain = ValidationSupportChain(profileSupport, defaultSupport)
//
//        // explicitly load profiles
//        val resource = object {}.javaClass.classLoader.getResource("fhirvalidation")
//        val dir = File(resource!!.toURI())
//        val jsonParser: IParser = ctx.newJsonParser()
//        dir.listFiles()?.forEach {
//            val sd = jsonParser.parseResource(FileReader(it)) as StructureDefinition
//            profileSupport.addStructureDefinition(sd)
//        }
//        return getFhirValidator(supportChain)
//    }