# Report Stream and Intermediary Integration 

## ETOR

For the purpose of supporting our partners on the ETOR initiative, there is a need to make certain data available to our 
partners to identify the status of the selected and related messages passing through both the CDC Intermediary and Report Stream.  

This functionality is supported by the API endpoints located in `SubmissionFunction#getTiMetadata` and 
`DeliveryFunction#getTiMetadata`. These endpoints act as wrappers to the metadata endpoint that exists within the CDC Intermediary.
They perform authentication against the calling entity using the existing Report Stream auth framework before 
searching for the correct reportId to call the Intermediary. 