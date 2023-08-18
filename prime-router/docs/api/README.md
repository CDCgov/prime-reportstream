# Swagger UI

Swagger UI is a tool that allows you to visually interact with ReportStream APIs that have been defined using the OpenAPI
Specification (OAS). It provides a user-friendly interface that makes it easier to understand and explore capabilities of
the API without requiring external tools such as curl or Postman.

This directory contains all open api spec resources:

- ./swagger-ui: swagger ui artifacts (with customization - see custom and maintain notes below)
- ./generated: auto generated api spec(s) from annotated kotlin source files

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

The interface consists of static web assets that are compiled from annotations in our back-end code and served similarly
to our front-end web application. The UI can only be accessed locally in the azure storage instance once compiled. To
access it, run the prime-router stack, then navigate to the URL below:

**Local** http://127.0.0.1:10000/devstoreaccount1/apidocs/index.html

Please note you may encounter cross-origin restrictions when trying to access one environment from outside its URL.

### Authentication

Swagger has built-in support for authentication; it will map user-provided credentials into the appropriate headers and
even handle obtaining and refreshing a bearer token using OAuth.

Regardless of chosen authentication method, the steps are similar:
Near the top-right of the interface you'll find an `Authorize` button which will bring up a menu with the authentication
methods available (described below). Each option will require some inputs.

####  OKTA

OKTA provides an implementation of OAuth2 for authentication. It utilizes a `client ID` and a `secret` that is encoded
as a JSON Web-Token. This token is submitted to the server to request a bearer token which will then be used to
authenticate further requests. The bearer token expires after 5 minutes so it must be periodically refreshed using a
separate API call.

Provide the client ID / secret and select a scope in the Authorize menu and Swagger will automate the above OAuth process.

#### Server-to-server

Based on OAuth2, this method is based on best practices outlined in the SMART on FHIR. It follows a similar process to
OAuth above: a client secret, ID, and scope is encoded as a JWT and sent to the server; which responds with a bearer
token the client can use to authenticate and authorize further requests.

The initial JWT must be created outside of the swagger environment. An example using a python script is provided in this
repo (examples/generate-jwt-python). See the README instructions in that directory for instructions, summarized here:
1. Install python dependencies for generating JWT
2. Set variables on lines 28-30 in the script (`my_client_id`, `my_kid`, and `my_rsa_keypair_file`)
3. Run the script! The penultimate curl contains the JWT used to retrieve a bearer token. The final curl contains an
   example using that bearer token.

#### API Key (x-functions-key)

This method utilizes a simple API key and a client ID that is mapped to a header value and sent with every request. It
is less secure, deprecated, and should only be used in edge cases where OAuth is not possible. This key is generated
and stored in the Azure cloud and provided by reportstream to the end user.

## Developer Notes

The openapi spec for reportstream is generated from annotations in the code. This facilitates accurate and timely
updates and an otherwise ameliorates a lengthy, manual, repetitive, and error-prone process.

### Swagger UI Customization and Maintenance
Since the swagger files are generated from annotations in the code, any changes must be made to the annotations
(or in the config of the underlying library) rather than the swagger YAML files themselves. These annotations use openapi
classes and data structures to decorate functions with any metadata to document functions.

For example (From `ApiKeysFunction.kt` function `getApiKeysV1` for retrieval of api keys): 

```kotlin
    @Operation(
        summary = "Retrieve API keys for the organization (v1), return API keys when successful",
        description = OPERATION_GET_KEYS_DESC,
        tags = [KEY_MGMT_TAG],
        parameters = [
            Parameter(
                name = PARAM_NAME_ORGNAME,
                required = true,
                description = PARAM_DESC_ORGNAME_GET,
            )
        ],
        responses = [
            ApiResponse(
                responseCode = HTTP_200_OK,
                description = HTTP_200_GET_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiKeysResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Example response for API key retrieval",
                                summary = "Example response when key retrieved successfully",
                                value = EX_GET_APIKEYS_RESP
                            ),
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_404_NOT_FOUND,
                description = HTTP_404_ERR_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_400_BAD_REQ,
                description = HTTP_400_ERR_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            )
        ]
    )
    @GET
    @Path("v1/settings/organizations/{organizationName}/public-keys")
    fun getV1( . . . 
```

For more examples of such annotations, search for `@Operation` in the codebase. 

### Swagger UI Distribution Update History

|    date    |  dist |              comments                |
|------------|-------|--------------------------------------------------------------|
| 07/24/2023 | 5.1.0 | only change made in swagger-initializer.js to prevent js injection |
