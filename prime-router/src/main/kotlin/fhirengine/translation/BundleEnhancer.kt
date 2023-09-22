package gov.cdc.prime.router.fhirengine.translation

import ca.uhn.hl7v2.model.Message
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import io.github.linuxforhealth.hl7.data.Hl7RelatedGeneralUtils
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Patient

class BundleEnhancer {
    companion object {
        /**
         * A method that calls a series of methods that "enhance" the bundle with data we could not add
         * in the mapping files.
         * @param bundle - The FHIR bundle to enhance
         * @param hl7Message - The HL7 message to get the data from to use for enhancing
         */
        fun run(bundle: Bundle, hl7Message: Message) {
            enhanceBundleMetadata(bundle, hl7Message)
            handleBirthTime(bundle, hl7Message)
        }

        /**
         *  As documented in https://docs.google.com/spreadsheets/d/1_MOAJOykRWct_9cBG-EcPcWSpSObQFLboPB579DIoAI/edit#gid=0,
         *  the birthDate value needs an extension with a valueDateTime if PID.7 length is greater than 8. According to the
         *  fhir documentation https://hl7.org/fhir/json.html#primitive, if a value has an id attribute or extension,
         *  it is represented with an underscore before the name. Currently, it seems hl7v2-fhir-converter library does not
         *  support this, so this method is a workaround to add an extension to birthDate. There is also no support for
         *  getting the length of the field, for which this issue was created:
         *  https://github.com/LinuxForHealth/hl7v2-fhir-converter/issues/499
         *  This method looks in the [hl7Message] for the birthdate and add an extension to the [bundle] if it includes
         *  the time
         */

        private fun handleBirthTime(bundle: Bundle, hl7Message: Message) {
            // If it is an ORM message, we want to check if it is a timestamp and add it as an extension if it is.

            val birthTime = HL7Reader.getBirthTime(hl7Message)
            if (birthTime.length > 8) {
                val patient = try {
                    bundle.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient
                } catch (e: NoSuchElementException) {
                    bundle.addEntry().resource = Patient()
                    bundle.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient
                }

                val extension = Extension(
                    "http://hl7.org/fhir/StructureDefinition/patient-birthTime",
                    DateTimeType(Hl7RelatedGeneralUtils.dateTimeWithZoneId(birthTime, ""))
                )

                patient.birthDateElement.addExtension(extension)
            }
        }

        /**
         * Enhance the [bundle] metadata with data from an [hl7Message].  This is not part of the library configuration.
         */
        private fun enhanceBundleMetadata(bundle: Bundle, hl7Message: Message) {
            // For bundles of type MESSAGE the timestamp is the time the HL7 was generated.
            bundle.timestamp = HL7Reader.getMessageTimestamp(hl7Message)

            // The HL7 message ID
            val identifierValue = when (val mshSegment = hl7Message["MSH"]) {
                is ca.uhn.hl7v2.model.v27.segment.MSH -> mshSegment.messageControlID.value
                is ca.uhn.hl7v2.model.v251.segment.MSH -> mshSegment.messageControlID.value
                else -> ""
            }
            bundle.identifier.value = identifierValue
            bundle.identifier.system = "https://reportstream.cdc.gov/prime-router"
        }
    }
}