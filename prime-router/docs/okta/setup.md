# Okta Setup

## Setting up Okta backend integration

Okta authentication is used for specific API calls that are made from the frontend on behalf of a logged in user. See
[this document](../authz/authz-deprecated-implementation.md) for a list of API calls utilizing Okta.

This functionality requires the `RS_OKTA_baseUrl` environment variable to be set to the hostname of the Okta instance.
Use of this environment variable allows local workstation, demo, staging, and prod environments to point to different 
Okta endpoints. As of this writing, the expected values are:
* `reportstream.okta.com` for production
* `reportstream.oktapreview.com` for all other environments

## Development notes

* Okta authentication is attempted for nearly all API calls utilizing a bearer token, but as of this writing, most
  API calls use RS's built in server to server authentication; requests to Okta are currently only expected to succeed 
  for specific API calls that are requested via the frontend on behalf of a logged in user.
* The email function uses a hardcoded value for the Okta URL instead of the environment variable.
* The only DevOps involvement in this process is storing the necessary value as secrets in Azure, which are then 
  translated to environment variables.