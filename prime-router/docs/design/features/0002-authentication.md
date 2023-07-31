# ReportStream authentication

## Summary

This document covers how authentication is handled by ReportStream.  Authentication is broken down into two implementations:

- Authentication for users who can log into the system via Okta
- Authentication based on SMART on FHIR OAuth guide

The entry point for this authentication is [here](https://github.com/CDCgov/prime-reportstream/blob/403347e889c2fb614659ee626e8c0fcdc2cf6c93/prime-router/src/main/kotlin/tokens/AuthenticatedClaims.kt#L198);
the code will attempt to first authenticate via Okta and then the SMART on FHIR OAuth implementation

### via Okta

Users with credentials via Okta (typically those who can log in to the report stream UI) login via Okta and a short-lived token is returned and users include that in requests to the API.  ReportStream uses the Okta JWT library to authenticate
and [decode](https://github.com/CDCgov/prime-reportstream/blob/403347e889c2fb614659ee626e8c0fcdc2cf6c93/prime-router/src/main/kotlin/tokens/OktaAuthentication.kt#L69) the JWT.

Most of this happens without a user necessarily being aware of the token as it's handled by the browser.

### via SMART on FHIR oauth implementation

This implementation follows the guide linked below and it is essentially an implementation of an Oauth flow and the image 
below shows the flow.  At a high level:

- the client uploads the public key for the key pair they generate
- they sign a JWT (aka a JWS) with their private key
- ReportStream attempts to verify the JWS with the stored public key
- If the signature is valid, ReportStream creates a temporary access token
- the client then uses that token to make API requests

![Authorization flow diagram](https://hl7.org/implement/standards/fhir/uv/bulkdata/authorization/backend-service-authorization-diagram.png)

## via Okta details

The majority of this flow is handled by Okta and the SDK that they provide for integrating.  The server is primarily 
responsible for invoking the SDK to authenticate the token and then parsing the Okta response into the scopes that
ReportStream uses.  That functionality can be found [here](https://github.com/CDCgov/prime-reportstream/blob/403347e889c2fb614659ee626e8c0fcdc2cf6c93/prime-router/src/main/kotlin/tokens/Scope.kt#L176).

## via SMART on FHIR oauth implementation details

### Terms

- `jwt` - JSON web token
- `client_id` - this is the name of the organization
- `scope` - this is a string that maps to a resource(s) that can be accessed. It follows the format `$orgName.*.$resource`
- `claims` - these are statements about the entity making the request, example `iss`
- `headers` - these are additional fields used while processing a request, example `alg`
- `kid` - this is a header that is included to indicate to the server which public key to use when verifying the token

### How are public keys stored

Public keys are stored within the organization setting, specifically they are captured as a list of JwkSets; each JwkSet
consists of the scope that keys are applicable for and then a list of serialized JWKs. Each JWK has a `kid` associated
with it that is unique within that JwkSet.

Here is an example:
```yaml
name: "ignore"
description: "Ignore"
jurisdiction: "FEDERAL"
filters: []
featureFlags: []
keys:
- scope: "ignore.*.report"
  keys:
  - kty: "RSA"
    kid: "ignore-report"
    "n": "ry..."
    e: "AQAB"
- scope: "ignore.*.admin"
  keys:
      - kty: "RSA"
        kid: "ignore-admin-1"
        "n": "ry..."
        e: "AQAB"
      - kty: "RSA"
        kid: "ignore-admin-2"
        "n": "y..."
        e: "AQAB"
```

### Adding a public key

Users will need to generate a key pair and the public key then needs to be persisted with their organization in the DB.
The two supported algorithms are:

- RSA
- EC

The following is an example of how to generate an RSA key:

```shell
openssl genrsa -out my-rsa-keypair.pem 2048
openssl rsa -in my-rsa-keypair.pem -outform PEM -pubout -out my-rsa-public-key.pem
```

The generated public key can then be added in two ways:

#### Via the API

Public keys can be added to an organization via a POST to the API; the request must include the public key in a PEM
format as it's body as well as the scope and kid for the key.  Here is an example request:

```shell
curl --location '$baseUrl/api/settings/organizations/ignore/public-keys?scope=ignore.*.report&kid=ignore.default2' \
--header 'Authorization: <TOKEN>' \
--header 'Content-Type: text/plain' \
--data '-----BEGIN PUBLIC KEY-----
<PEM CONTENTS>
-----END PUBLIC KEY-----'
```

**Note: The API currently only supports the `$orgName.*.report` scope, though this is subject to change as more of this 
functionality is moved into the ReportStream UI**

#### Via PRIME CLI

The key can be added with the `addkey` PRIME CLI command; that process is documented [here](https://github.com/CDCgov/prime-reportstream/blob/403347e889c2fb614659ee626e8c0fcdc2cf6c93/prime-router/docs/playbooks/how-to-use-token-auth.md#L38).
This method supports adding a key for any valid scope.

#### [FUTURE] JKU header

This is currently not implemented, but the SMART on FHIR guide recommends supporting the JKU header.  This header contains
a publicly accessible URL maintained by the client where the JwkSets can be accessed.  The advantage is the that client can handle rotating keys
without having to interact with ReportStream.

### Generating an access token

Once a public key has been stored with the organization, the client can now create signed JWTs to request a short-lived
access token that can be used to access resources in the system.  There is a python script [here](https://github.com/CDCgov/prime-reportstream/blob/403347e889c2fb614659ee626e8c0fcdc2cf6c93/prime-router/examples/generate-jwt-python/generate-jwt.py#L1)
that can be referenced when creating a signed JWT; the JWS can then be used with the token endpoint to get an access token.

```shell
curl -X POST 
  -H "Content-Type: application/x-www-form-urlencoded"  
  -d "scope=ignore.*.report&grant_type=client_credentials&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&client_assertion=<JWS>" 
  "$baseUrl/api/token" 
```

The server parses the JWT and uses the `iss` claim and `kid` header to find the organization requesting access and then
the specific public key that was generated alongside the private key used to sign the JWT.  The server will try all the found
public keys matching the `iss`, `scope` and `kid` to see if any of them can be used to verify the signature for the JWT.  If any of the keys work,
the server generates and returns an access token that is signed with a secret known only to the server.

#### Security checks

The following are checks that are performed by the server to guarantee that access tokens are only given to properly
authenticated clients

- **Signature verification**: the signature of the JWT is verified via the public key associated with the private key used to generate the signature
- **JTI**: each JWT must contain a unique `id` claim (aka a JTI).  The server stores the used values and will only accept a 
JWT with a JTI that it has not seen before (see [JtiCache](https://github.com/CDCgov/prime-reportstream/blob/403347e889c2fb614659ee626e8c0fcdc2cf6c93/prime-router/src/main/kotlin/tokens/JtiCache.kt#L7) for more details)
This prevents replay attacks.
- **Expiration time**: When a JWT is generated, it is configured with an expiration time.  The server will only accept a JWT that
has not expired.

### Using an access token

The returned access token that can then be used with an authorization server for requests to the server.

An example:
```shell
curl -X POST 
  -H "authorization:bearer <token>" 
  -H "client:ignore"  
  -H "content-type:application/hl7-v2" --data-binary "@./my-nonPII-data.hl7" 
  "$baseUrl/api/ignore"
```

The server uses the secret used to create the token to verify it and then allows access to the resource

### Differences with the spec

- The SMART on FHIR guide states the scopes should be "system" scopes that parallel the "user scopes" and
should have a format like `system/(:resourceType|*).(read|write|*)`.  This is in contrast to the scopes used in
ReportStream which look like `$orgName.*.report`.

## Libraries

- [json web token](https://github.com/jwtk/jjwt)
- [okta jwt verifier](https://github.com/okta/okta-jwt-verifier-java)

## References

- [Smart on FHIR Authorization guide](https://hl7.org/implement/standards/fhir/uv/bulkdata/authorization/)
- [OAuth spec](https://datatracker.ietf.org/doc/html/rfc6749)
- [jwt.io](https://jwt.io/)