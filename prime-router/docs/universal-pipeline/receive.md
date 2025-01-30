# Universal Pipeline Receive Step

`Receive` is the first step a report goes through when entering the Universal Pipeline (UP). Despite its name, this step
actually occurs in the `ReportFunction.kt` file.

## How it works

There are two HTTP endpoints that will kick off this function: [/reports](../api/reports.yml)
and [/waters](../api/waters-reports.yml). The UP can receive data in either HL7 or FHIR via these two endpoints.

### Azure Function Access Key Security

[Azure Function Access Key](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-http-webhook-trigger?tabs=python-v2%2Cisolated-process%2Cnodejs-v4%2Cfunctionsv2&pivots=programming-language-csharp#authorization-keys)
security is supported by
the `/reports` endpoint. Static and unchanging API Keys can be defined
in the Azure environment for the Reports function and sent to the client. This method is less secure than FHIR Auth
Security and thus the `/reports` endpoint should only be used over the `/waters` endpoint in special circumstances where
clients or particular integrations do not have the technical ability to use the FHIR Auth Security.

### FHIR Auth Security

[FHIR Auth Security (OAUth2.0)](https://docs.smarthealthit.org/authorization/best-practices/) is supported by
the `/waters` endpoint and is the recommended auth clients should use. See
the [authentication design note](../design/features/0002-authentication.md) for details.

### Validation

Currently, very basic validation of the received report occurs in the `receive` step, such as:

- ensuring that the report is in a format the pipeline can process`
- verifying the sender is valid and enabled

### Processing

Once the request has been authenticated, authorized, and validated, the Reports function uploads the received report
to the blob store and triggers the [Convert](convert.md) step via the Azure queue.

### Retries

There is no async processing for this step, so a sender will get an immediate response and is responsible for retrying
the
request in the event of any failure down the line.