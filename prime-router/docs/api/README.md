# Swagger UI

Swagger UI is a tool that allows you to visually interact with ReportStream APIs that have been defined using the OpenAPI
Specification (OAS). It provides a user-friendly interface that makes it easier to understand and explore capabilities of
the API without requiring external tools such as curl or Postman.

This directory contains all open api spec resources:

1. ./: specs in yaml format derived from other process (vs auto generated specs under ./generated)
2. ./swagger-ui: swagger ui artifacts (with customization - see custom and maintain notes below)
3. ./generated: auto generated api spec(s) from annotated kotlin source files

## Purpose

The main purpose of Swagger UI is to:

1. **Documentation:** generates documentation in a terse yet approachable format that serves as great reference
   material.

2. **API Visualization:** provides a structured interface that displays API endpoints, request parameters, response 
   structures, and example requests and responses in a structured, readable, and navigable format.

3. **API Testing:** facilitates direct interaction with the API. You can input parameters, make requests, and
   see the responses right in your browser with no external tools.

## Usage
With an API selected the UI will display a list of endpoints. Each endpoint can be expanded to display the
usage, parameters, and expected output; as well as an interface to make an API call.

### Accessing the Swagger UI Interface

The interface consists of static web assets that are compiled and served similarly to our front-end web application.
The UI is served and accessible on the same server as the reportstream web ui / front-end using a particular URL / path.
To access it, navigate to TLD for your chosen environment and append `/swagger-ui` to the end of the root url.

**Staging** https://staging.prime.cdc.gov/swagger-ui
**Local** https://localhost:8080/swagger-ui

Please note that while all environments can be selected in the swagger-ui (regardless of your chosen URL),
you may encounter cross-origin restrictions when trying to access one environment from outside its URL.

### Authentication

Swagger has built-in support for authentication; it will map user-provided credentials into the appropriate headers and
even handle obtaining and refreshing a bearer token using OAuth.

Regardless of chosen authentication method, the steps are similar:
Near the top-right of the interface you'll find an `Authorize` button which will bring up a menu with the authentication
methods available (described below). Each option will require some inputs.

#### OAuth

OAuth utilizes a `client ID` and a `secret` that is encoded as a JSON Web-Token. This token is submitted to the server to
request a bearer token which will be used to authenticate further request. The bearer token expires after 5 minutes so
it must be periodically refreshed using a separate API call.

Provide the client ID / secret and select a scope in the Authorize menu and Swagger will automate the above OAuth process.

#### Server-to-server

Based on OAuth2, this method is based on best practices outlined in the SMART on FHIR. It follows a similar process to
OAuth above: a client secret, ID, and scope is encoded as a JWT and sent to the server; which responds with a bearer
token the client can use to authenticate and authorize further requests.

#### API Key (x-functions-key)

This method utilizes a simple API key and a client ID that is mapped to a header value and sent with every request. It
is less secure, deprecated, and should only be used in edge cases where OAuth is not possible.

## Notes

### Swagger UI Customization and Maintenance
This section is currently empty.

### Swagger UI Distribution Update History

|    date    |  dist |              comments                |
|------------|-------|--------------------------------------------------------------|
| 07/24/2023 | 5.1.0 | only change made in swagger-initializer.js to prevent js injection |
