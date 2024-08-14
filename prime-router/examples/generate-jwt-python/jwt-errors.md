# Errors when generating a signed JWT
As part of [the authentication process](https://reportstream.cdc.gov/resources/api/getting-started#set-up-authentication), an organization needs to generate a signed JSON Web Token (JWT). The signed JWT is used to retrieve an access (bearer) token. 
You may receive an error during this process. Here is a list of possible errors and how to fix them.

## Expired token
The token expiration time has passed. [Generate a new token](https://reportstream.cdc.gov/resources/api/getting-started#set-up-authentication), and submit before the expiration time.

## Unsigned JWT 
The JWT was generated without including the signature. [Review how to generate a signed JWT](https://reportstream.cdc.gov/resources/api/getting-started#set-up-authentication).

## Malformed JWT
The API cannot decode a JWT from your submission. [Review how to generate a signed JWT](https://reportstream.cdc.gov/resources/api/getting-started#set-up-authentication). Contact ReportStream if this error persists.

## Invalid scope
Make sure your request includes the scope (which resources you can access) and that it matches the expected format. For example: `{orgName}.*.report`

## No valid keys
There are no public keys associated with your organization or none of them can be used to verify the signature of the JWT. Check if your public key is correct or [submit a public key](https://reportstream.cdc.gov/resources/manage-public-key). Note: You will need to login to submit your key this way. If you cannot login, email us.

## No matching organization
Check that the organization is correct in the `iss` (issuer) field in your request. Contact ReportStream if this error persists.

## Missing JWT
Make sure your request includes a JWT. [Review how to generate a JWT](https://reportstream.cdc.gov/resources/api/getting-started#set-up-authentication).

## Missing scope
Check you have included a scope (which resources you can access) in your request for an access token. [Review instructions](https://reportstream.cdc.gov/resources/api/getting-started#set-up-authentication) for generating a JWT.

## ModuleNotFoundError error
Make sure you have python dependencies installed

## The term is not recognized as a cmdlet function script file or operable program
Make sure you have python installed and added to your path. You may need to restart your terminal or session.