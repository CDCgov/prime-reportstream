# How to Onboard a New Organization to Send Data

Add subsections that mimic the linked resources: ([Brandon’s version](https://docs.google.com/document/d/1noB3lK2Nc_vbD4s5ZHgdTjgIjhCii63x_2bjBz7GM1I/edit#heading=h.be9yxi8thtdw), [Github version](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/how-to-onboard-a-sender.md))
Note for content writer: If there is overlap with the previous "Pipeline Configuration" section, hyperlink and reference the content instead of rewriting it..

## Sender Onboarding Process Overview

- high level steps explained and visualized

## Sender Configuration

- Validation Tool
 - Via ReportStream website
   - Via API
   
## Sending data to ReportStream

- (Exists in API programmer's guide)
- Authentication - explain how to set up server-server auth to connect with RS (Exists in API programmer's guide and elsewhere)
- API key management - explain how to manage keys in UI or through API
- Report endpoint - explain how to submit data (Exists in API programmer's guide)
- History endpoint - explain how to use + example
- Swagger API Documentation

## Rhapsody Configuration

Rhapsody is a health data pipeline that provides a visual interface representing various flows of health data. It uses
`Communication Points` of various types to interface with external systems.

### Using x-functions-key

The `HTTP Communications Point` uses a standard HTTP request to send data externally. It has been used in conjunction
with the `x-functions-key` authentication flow to connect clients in the `staging` environment. However, this auth flow is
deprecated and should not be used to onboard any new clients moving forward (documentation kept for posterity).

It is recommended to use oauth2 or server-to-server auth instead. The observed versions of Rhapsody cannot support the
OAuth2 authentication flow (javascript version too old).

#### HTTP Communications Point Configuration

| Name                             | Value                                     |
|----------------------------------|-------------------------------------------|
| COMMUNICATION POINT              | HTTP CLIENT                               |
| MODE                             | Out->In                                   |
| URL                              | https://staging.prime.cdc.gov/api/reports |
| HTTP METHOD                      | POST                                      |
| FOLLOW REDIRECTS                 | YES                                       |
| USE HTTPS                        | YES                                       |
| SSL PROTOCOL MODE                | TLSv1.2                                   |
| SPECIFY EXACT TLS VERSION        | YES                                       |
| HOSTNAME VERIFICATION            | YES                                       |
| PROTOCOL SUPPORT                 | Standard HTTP Only                        |
| READ TIMEOUT (MS)                | 10,000                                    |
| CONNECTION TIMEOUT (MS)          | 60,000                                    |
| PROXY TYPE                       | System                                    |
| REFRESH RATE (MS)                | 60,000                                    |
| MESSAGE CONTENT                  | Message Body                              |
| CONTENT TYPE                     | application/hl7-v2                        |
| ON 4xx ERROR RESPONSE            | Mark as connection failed                 |
| ON 5xx ERROR RESPONSE            | Mark as connection failed                 |
| DYNAMIC CONNECTION FAILED ACTION | Treat as message error                    |

##### Request Headers
| Name            | Value         |
| --------------- | ------------- |
| x-functions-key | <suppressed>  |
| client          | CDC-ELIMS-HL7 |