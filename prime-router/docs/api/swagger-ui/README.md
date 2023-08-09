# Swagger UI

Swagger UI is a tool that allows you to visually interact with ReportStream APIs that have been defined using the OpenAPI
Specification (OAS). It provides a user-friendly interface that makes it easier to understand and explore capabilities of
the API without requiring external tools such as curl or Postman.

## Purpose

The main purpose of Swagger UI is to:

1. **Documentation:** Swagger UI generates documentation in a terse yet approachable format that serves as great reference material

2. **API Visualization:** Swagger UI provides a structured interface that displays API endpoints, request parameters,
3. response structures, and example requests and responses in a structured, readable, and navigable format.

3. **API Testing:** Swagger UI facilitates direct interaction with the API. You can input parameters, make requests, and
see the responses right in your browser with no external tools

## Usage
With an API selected the Swagger UI will display a list of endpoints. Each endpoint can be expanded to display the
usage, parameters, and expected output; as well as an interface to make an API call.

###  Accessing the Swagger UI Interface

### Selecting an API

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

Provide the client ID and secret in the Authorize menu and Swagger will automate the above OAuth process.

#### API Key (x-functions-key)

This method utilizes a simple API key and a client ID that is mapped to a header value and sent with every request. It 
is less secure, deprecated, and should only be used in edge cases where OAuth is not possible.