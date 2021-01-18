## Background

We wish to allow our Public Health customers to access their current feed configurations and make simple configuration changes.

## Goals

- An API that is useful for a settings page for the PHD customer.
- An API that is useful for on-boarding states by the Data Hub team.
- An API which doesn't require specific code knowledge.
- An API which is implementable in an Azure Function.
- Use Okta authentication.

## Proposal

- The Organization metadata moves out of the project's JAR into the projects' database in Azure and new JSON files for the CLI.
- Extend the current Azure Functions App with a Settings function.
- Add `/api/settings` path and sub-paths. The OpenAPI specification documents details [openapi.yml](openapi.yml) doc
- The CLI will add a `--organizations-file` parameter to pass in the equivalent information. One benefit is that configuration changes will not require a recompile.

Outside of this proposal, pages and tooling will follow.

## Alternatives Considered

- Use Existing Organization Structures - Although it would be faster to stick with the current `OrganizationService` class, the current structure contains many places that require knowledge of the code or the existing schemas.
- Use GraphQL - A standard restful API fits the simple queries that I envision for this data.
- Use OpenAPI 3.1 and JSON Schema - OpenAPI and JSON schema converge in the 3.1 version of OpenAPI, so they should be a powerful tool to model data and APIs. However, OpenAPI 3.1 is still in development at the current time. Sticking with OpenAPI 3.0. 

