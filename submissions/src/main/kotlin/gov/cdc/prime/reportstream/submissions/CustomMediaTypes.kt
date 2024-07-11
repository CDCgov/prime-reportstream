package gov.cdc.prime.reportstream.submissions

import org.springframework.http.MediaType

object CustomMediaTypes {
    val APPLICATION_HL7_V2 = MediaType("application", "hl7-v2")
    val APPLICATION_FHIR_NDJSON = MediaType("application", "fhir+ndjson")
}