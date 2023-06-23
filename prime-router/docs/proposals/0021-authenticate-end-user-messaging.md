# Surfacing OAuth errors to end users

## Problem

- In order to send data to ReportStream, users must configure a public/private key pair
- After uploading the public key, a user must create a signed JWT to request an access token to make API requests
- The server can encounter different errors while parsing and validating the JWS which typically can be remediated by the
end user 
- However, the server does not respond with enough information to assist a user in determining what needs to be fixed

## Goal

Enable end users to remediate, on their own, errors that are generated when requesting an access token with a JWS 
by surfacing more details on what the specific error was.

## Proposal

The OAuth [spec](https://datatracker.ietf.org/doc/html/rfc6749#section-5.2) specifies that the server must return an 
error code as well as two optional fields that can be used to provide a user with additional information on the error 
encountered.

- `error_description`: a human-readable text providing additional information on the error 
- `error_uri`: a URI that opens to a site that explains the error

Using an `error_description` in the response is the simplest approach but has the notable downside that the API
is now responsible for managing content for the end user which is outside the purview of the API and would typically be
handled by a different team.

Instead, the `error_uri` provides a better option as the API team can identify the error cases that can occur, but 
the actual content can be maintained on the ReportStream site.  This approach lines up with past approaches where the
site is the resource we expect end users to consult when trying to accomplish a task.

An example of an error response from the server would look like if the user generated:

```json
{
    "error": "invalid_scope",
    "error_uri": "https://reportstream.cdc.gov/resources/authentication#valid-scope"
}
```

And the URL would point to a specific anchor on the resource page for authentication that would explain what the scope
value should be when generating the signed JWT.

This is just an example of one possibility of how the URLs could be formed; in practice, the API would be responsible
for providing a list of possible errors and the webex team would be responsible for providing the URL that should be 
returned when that error is encountered.