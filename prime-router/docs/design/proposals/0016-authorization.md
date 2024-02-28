## Combining Okta (Human) and Server2Server Auth

August 2022

This is a follow-on to [0001-authentication.md](./0001-authentication.md), which covers our original plan for supporting Server-to-server auth (also known as FHIR Auth, and two-legged public/private key auth).

ReportStream has many Azure Functions that implement REST endpoints.  For many of them, it makes sense to allow for either Humans or Servers to use the endpoint.   This document outlines how we implement support for Azure functions that can handle either server2server auth or Okta auth.

### Background

ReportStream supports three kinds of authentication:  x-functions-key, server2server, and Okta.
1. x-functions-key is simple built-in azure functionality.   In Azure, you create function keys associated with an individual Azure Function endpoint, then you pass that key into the function, and Azure handles the authentication for you.  There is no real authorization - if you have the key, you are "in".   Our x-functions-key support is deprecated, but still in use as of this writing (Aug 2022)
2. Server2server authentication/authorization uses the "official" FHIR standard for how to do good auth.  The organization that wants to use a ReportStream service creates a public private key pair, and gives ReportStream the public key.   They use the private key to create a signed jwt token, ReportStream authenticates the signature, generates a temporary access token, and can use the claims in the jwt for authorization.
3. Okta is for humans to connect to ReportStream.  More details on Okta authorization below.

Since the first technique is deprecated, this discussion centers on how to have a single way to authorize claims found
in both Okta jwts and in Server2server jwts.

## Authentication

The discussion below is about _authorization_ only.  For reference, here's where to find information about _Authentication_ uses these two techniques:  

- Okta _authentication_ is based on okta tokens, and can be read about using Okta docs
- Server2server _authentication_ is discussed in detail in [0001-authentication.md](./0001-authentication.md).

## Authorization

### Human Authorization uses Okta Group names

This is how ReportStream's Okta handles Authorization right now:

Okta has groups following these forms below.   Inside ReportStream, each maps to claims on two things: (orgname, PrincipalLevel).  The Available PrincipalLevels are: SYSTEM_ADMIN, ORGANIZATION_ADMIN, USER.    

| Example Okta Group   | Claims  |
| -------------------- | :------- |
| DHoh-doh | USER access to the oh-doh organization |
| DHSender_oh-doh | USER access to the oh-doh organization |
| DHoh-dohAdmins | Organization ADMIN-level access to the oh-doh organization |
| DHSender_oh-dohAdmins | Organization ADMIN-level access to the oh-doh organization |
| DHPrimeAdmins | Overall ReportStream Administrator access |

A user can be a member of one or more Okta Groups.

You can see that ReportStream server-side code ignores the “Sender_” portion - that is, DHoh-doh and DHSender_oh-doh have the same claims.

### Server to Server Authorization uses Scopes

This is how ReportStream's Server2server auth handles Authorization right now:

[The official "standard" we follow is documented here](https://hl7.org/fhir/uv/bulkdata/authorization/index.html)

It states:
```
Clients SHALL use “system” scopes that parallel SMART “user” scopes.
System scopes have the format system/(:resourceType|*).(read|write|*)
```

Example scope string from the standard:  `"scope": "system/*.read”`

ReportStream, does not quite follow that syntax.  We leave out the "system/" part.  
Our current scope is of the form:  `“scope”: “simple_report.default.report”`. 
If a user has this claim, they have the right to report data to the simple_report.default org/grou.

### Requirements for a reconciled human/machine authorization language

- ReportStream wants to support both models:  roles, for Humans, and permissions, for Servers
  - Humans – we think in terms of roles (belonging to a Group).
  - Servers - we think in terms of permissions (being granted a scope claim)
- Want to allow for principles (humans, servers) to have many scope claims, not just one scope.
- Want a simple model with simple rules
- The implementation of this should be hidden from users
- Should allow for future fine-grained permissions that we haven’t dreamed up yet

### Proposal to reconcile Okta Groups and Scopes

- The proposal is that, internal to ReportStream, all claims to request authorization to use a resource will be represented only by Scopes.  Internally, once authenticated, every principal (server or human) will have a set of scope claims.
- All scope claims will be of the form: `<orgname>.<server-or-receiver-name>.<role-or-permission>`
- Use * for wildcard “all”. 
- For clarity, explicitly call out primeadmin role, eg,    `*.*.primeadmin`

- ReportStream code will cleanly map incoming Okta “DH” group names to internal scopes.

### How the Okta to Scope mapping will work

| Meaning / Example / Use Case                                        | Okta Group                                          | Scope Claim that it Maps To  |
|---------------------------------------------------------------------|-----------------------------------------------------|------------------------------|
| Ability for a server to submit submissions to simple_report.default | n/a      (no human in this example)                 | `simple_report.default.report` |
| Human Org accesses submission APIs at a user level role             | DHsimple_report, DHSender_simple_report             | `simple_report.*.user`         |
| Human Org accesses submission APIs at an admin level role           | DHsimple_reportAdmins, DHSender_simple_reportAdmins | `simple_report.*.admin`        |
| Human Prime Admin role                                              | DHPrimeAdmins, `*.*.primeadmin`                     | `*.*.primeadmin`               |

### How Authorization Will Work

1. If Human, map DH Groups to scope claims (see table above)
2. Match the list of scopes _claimed_ by the principal, to the list of scopes _required_ by the function.
3. Any function can require any scopes it wants, depending on its particular needs.
4. Matching has no semantics.  It knows nothing or orgs or receivers or roles or permissions.  It just does dumb string matches.
5. Any one exact string match == Authorized.  If no strings match == Unauthorized

### Examples

| Example / Use Case | Human or Server makes this claim | Azure Fn Requires (claim must be one of...)                          | Result |
| ---------------------------- | ---------- |----------------------------------------------------------------------| ------------------ |
| Server wants to submit data to oh-doh.default sender | `oh-doh.default.report` | `oh-doh.default.report`, `oh-doh.*.user`, `oh-doh.*.admin`, `*.*.primeadmin` | Authorized |
| Human wants to submit data to oh-doh.default sender | `oh-doh.*.user` | `oh-doh.default.report`, `oh-doh.*.user`, `oh-doh.*.admin`, `*.*.primeadmin` | Authorized |
| Human is member of two orgs; requests data about OH | `oh-doh.*.user`, `md-phd.*.user` | `oh-doh.*.user`, `oh-doh.*.admin`, `*.*.primeadmin`                        | Authorized |
| OH User make illegal request for data about NY | `oh-doh.*.user` | `ny.*.user`, `*.*.primeadmins`                                          | Not authorized |

## Further Work

You've probably wondered by now why we don't get rid of the oddball `DHsimple_reportAdmin` style Okta Groups and simply use scopes in Okta.
The work outlined in this proposal is a step in that direction.  At the moment, we are mildly constrained in Okta because we share the instance with 
Simple Report.

This combination of Okta and Server2server auth will give us a canonical way to write our auth code, for all present and future Azure Functions.
In particular, we can now implement Server2server auth in all our APIs, including our Settings and Lookup APIs, which will be useful
to end users, and will allow us to write 100% automated tests against those endpoints.
