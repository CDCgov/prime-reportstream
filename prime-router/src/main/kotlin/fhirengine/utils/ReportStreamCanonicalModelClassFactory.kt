package fhirengine.utils

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.model.v27.datatype.CWE
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory

/**
 * Custom HL7 Model class factory that overrides some of the default behavior in the HAPI library.  The purpose
 * is to allow and HL7 message with a version <2.7 to be able to be parsed into in the v27 structures.
 *
 */
class ReportStreamCanonicalModelClassFactory<T : Message>(theClass: Class<T>) : CanonicalModelClassFactory(theClass) {
    /**
     * This overrides modifies the behavior for how the removed CE datatype is handle.  Instead of returning null and
     * then later generating an error while parsing, a CE datatype is massaged into a CWE.  This works because the first
     * six fields of the CE are identical to a CWE
     */
    override fun getTypeClass(theName: String?, theVersion: String?): Class<out Type> {
        if (theName == "CE") {
            return CWE::class.java
        }
        return super.getTypeClass(theName, theVersion)
    }
}