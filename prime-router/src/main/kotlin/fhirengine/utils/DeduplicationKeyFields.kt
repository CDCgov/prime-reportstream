package gov.cdc.prime.router.fhirengine.utils

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Specimen

// if it's not holding data then probably doesn't need to be a class. convert to simple object?
// constructer will likely also need sender info but this breaks some of the neatness going on here hmmmm
abstract class DeduplicationKeyFields(private val bundle: Bundle) {
    // Needs method entry point to initialize this from FHIRConverter... where grab from?
    val senderID = ""
    // val bundle = bundle

    // PRIVATE
    // Should take in map of key fields and turn them into a single string
    fun createStringFromKeyFields(): String { // maybe expose this as #getHashStringFromBundle(bundle) ??
        var rawStr = "Hash STRING"
        rawStr = addSenderToPreHashString(rawStr)

        // Order absolutely needs to happen. Probably enforced by sorting into alphabetical order by key

        return HashGenerator.generateHashFromString(rawStr)
    }

    // Idea for this method is to meet requirement that sender id must be incoporated into pre-hash string
    // so that each hash is unique per sender.
    // PRIVATE
    fun addSenderToPreHashString(aggregateString: String): String = senderID.plus(aggregateString)

    // PUBLIC, should be access point for these classes.
    // 1. User instantiates KeyFields class with bundle & sender
    // 2. User calls this method. Hashed string is returned. Private methods encapsulate business logic.
    fun getHashFromBundle(): String = createStringFromKeyFields()
}

// Each business case for deduplication will need to host its own logic for pulling the important key fields that
// matter for that use case. Essentially these classes host the logic for how to pull the key fields out of a bundle.
//
class ORUR01KeyFields(bundle: Bundle) : DeduplicationKeyFields(bundle) {
    val keyFields = mapOf(
        // hl7Value: SPM.2
        // fhirValue: Specimen.Identifier
        "Specimen_ID" to
            bundle.entry.map {
                it.resource
            }.filterIsInstance<Specimen>().map { it.identifier }.flatten().map { it.value }.toString(),

        // hl7Value: SPM.30
        // fhirValue: Specimen.accessionIdentifier
        "Accession_number" to
            bundle.entry.map {
                it.resource
            }.filterIsInstance<Specimen>().map { it.accessionIdentifier }.map { it.value }.toString(),

        // hl7Value: SPM.17
        // fhirValue: Specimen.collection.collectedDateTime

        // hl7Value: PID.3
        // fhirValue: Patient.identifier

        // hl7Value: PID.5
        // fhirValue: Patient.name

        // hl7Value: PID.7
        // fhirValue: Patient.birthDate birthDate.extension[1].valueDateTime

        // hl7Value: OBR.22
        // fhirValue: DiagnosticReport.issued

        // hl7Value: OBR.25
        // fhirValue: DiagnosticReport.status

        // hl7Value: OBX.23
        // fhirValue: Observation.Performer -> Organization.Identifier.value

        // hl7Value: OBX.23
        // fhirValue: Observation.Performer -> Organization.name

        // hl7Value: OBX.3.1
        // fhirValue: Observation.resource.code.system

        // hl7Value: OBX.3.3
        // fhirValue: Observation.resource.code.coding

        // hl7Value: OBX.14, OBR.7, SPM.17
        // fhirValue: Observation.resource.issued DiagnosticReport.effectiveDateTime DiagnosticReport.effectivePeriod.start

        // hl7Value: OBX.5
        // fhirValue: Observation.resource.valueCodeableConcept.coding.code

        // hl7Value: OBX.3.3
        // fhirValue: Observation.resource.valueCodeableConcept.coding.system
    )
}