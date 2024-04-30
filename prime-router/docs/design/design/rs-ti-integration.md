# ReportStream and Intermediary Integration

## ETOR

Our partners on the ETOR initiative need access to ETOR-specific data 
(as of April 2024: time received, a hash of the message, and any linking between orders and results, combined status across RS and TI, etc.).
The metadata response is required to be a FHIR resource, something that isn't supported by ReportStream, and that ETOR-specific functionality 
belong in the ETOR microservice. The main ReportStream application needs to be the front door for authentication
and consistency purposes, while still allowing partners access to data stored in the CDC Intermediary.

This functionality is supported by the API endpoints located in `SubmissionFunction#getEtorMetadata` and
`DeliveryFunction#getEtorMetadata`. These endpoints act as wrappers to the metadata endpoint that exists within the CDC
Intermediary.
They perform authentication against the calling entity using the existing ReportStream auth framework before
searching for the correct reportId to call the CDC Intermediary.
