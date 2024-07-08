# Authentication and Authorization design

## Current design

We currently use a variety of methods to authenticate users and authorize actions in our system. 
We use a mix/match of the following for different endpoints:

- Okta
  - A cloud-hosted identity and access management service
- Server2Server
  - Homegrown implementation of client credentials OIDC flow
- Azure Function Keys
  - Azure configured shared secrets for authorization
  - Does not handle authentication at all! (Any user can use any key and the system will work)

## Reasons for updating

- Unify authn/authz to one consistent method as having multiple methods makes it difficult to determine who is using which method
- Reduce attack surface area since currently we have the weaknesses of each method
- Minimize authn/authz issues being the cause for missing sent reports
- Make life easier for Engagement team by having a single choice when onboarding a new sender/receiver

## Requirements

- Protect certain endpoints from only being accessed by identified trusted entities
- Centralize user management
- Revoke access if a secret is compromised
- Audit which clients are accessing which resources
- Fine-tune access control for specific clients
- Unify authn/authz approach for all microservices
- Allow clients the ability to self-service to ease client onboarding

## Options discussed in 14765

- Azure Application Gateway
  - This is Azure's load balancer and does not handle authn/authz
- Azure Front Door
  - This is Azure's CDN technology and does not handle authn/authz
- Azure Identity Management
  - This is Azure's own implementation of handling authn/authz for Microsoft products and is not available for implementation in our application

## Identity Provider Options

### Azure AD

Pros
- Integrated into the Microsoft ecosystem that we currently reside
- Scales well

Cons
- Complex setup
 
### Okta

Pros
- A trusted industry standard
- Highly scalable
- Offers self-service to ease Engagement teams workload
- We already have many of our clients set up here!

Cons
- Single point of failure if there is an outage

### Homegrown

Pros
- As flexible as we need it to be
- Cheap

Cons
- Custom authn/authz code is often vulnerable to attacks we may not have considered
- Additional work for us to write and maintain

## Protocol

We will be using OAuth 2.0 with OIDC. OAuth 2.0 is an industry-standard authorization protocol and OIDC is
another open standard on top of OAuth that adds an identity layer (authentication).

We will be using the **client credentials** flow which was designed with machine-to-machine communication in 
mind. See the image below for how the flow works generally.

![Client Credentials Flow](clientCredentialsFlow.png)

## Definitions

- **authn**: Authenticate/authentication
- **authz**: Authorize/authorization
- **IAM**: Identity and access management
- **IdP**: Identity Provider
- **Okta**: A cloud-hosted identity and access management service
- **Server2Server**: Homegrown JWT authn/authz
- **VPC**: Virtual Private Cloud
- **Resource Server**: A server that will require authn/authz to access. In our case Report Stream
- **Authorization Server**: A server that handles authn/authz and issuing/revoking credentials

## Architecture options

### Spring reverse proxy microservice

This approach would create a new microservice whose sole responsibility is authn/authz. It would sit in front of
all requests to protected endpoints.

### Prerequisites
- Designate protected endpoints and put them behind a VPC.
  - Traffic must flow through our authn/authz service  
  - Protected endpoints should not be accessible to the public internet

A typical report request would look like this
- Client makes request to authorization server (or us in the homegrown solution) to retrieve token with their own credentials
- Client uses retrieved token with request to Report Stream
- Request comes in and hits our auth microservice instead of Report Stream directly
- Depending on our implementation, we decide if that request is legitimate and allowed to access the designated endpoint
  - In cloud-based solutions, we would make calls out to an external authz server
    - We could also introspect the token locally if speed is a factor but its more code to write/maintain
  - In a homegrown solution, we would introspect certain cookies and headers and compare them to what we have in a datastore
- If the request is unauthenticated or unauthorized, short-circuit the request to a 401
- If the request is authenticated and authorized for the resource, pass it along to the internal protected endpoint

Pros
- Extremely scalable
- All authn/authz code is in one place
- Security concerns can be centralized as all requests go through a single public endpoint
- Simplifies business logic as it no longer has to worry about authn/authz
- Extensible outside of core RS services if required

Cons
- Highly complex setup from both an engineering and devops perspective
- New service needs to be written and maintained
- A single point of failure if the service goes down
- Can lack customization options for very specific use-cases

Code example:
```kotlin

// handles all incoming requests
@RequestMapping("/**")
fun protectedReadEndpoint(
    @RequestBody(required = false) body: String,
    method: HttpMethod, 
    request: HttpServletRequest,
    response: HttpServletResponse
) {
    // this is all psudeocode!
    
    // check if we need to auth at all
    val authRequired = condigService.isAuthRequired(request.path, method)
    
    return if (authRequired) {
        // call out to auth service
        val authResult = authService.checkToken(request)
        
        // possible results
        when (authResult) {
            is Unauthorized, MissingToken -> UnauthorizedResponse
            is Authorized -> httpClient.request(method, protectedEndpoint, body)
        }
    } else {
        // pass through if no auth needed
        httpClient.request(method, protectedEndpoint, body)
    }
}

```

### Use libraries with limited code in shared project

This approach would attempt to use existing libraries to annotate request mappings in our Spring app.

We would have to write custom code in the shared project to handle configuration and authn/authz for Report Stream
endpoints that still live in our Azure Functions app.

Pros
- Simple implementation for a small project like ours
- Authn/authz annotations would be directly adjacent to the endpoint mappings they protect
- Can handle complex authorization requirements for specific endpoints with more ease
- Simplifies project structure without new deployments
- No additional devops support

Cons
- Less scalable solution
- Would have to deal with handling authn/authz with 2 different web frameworks
  -  Easy for Spring but could require some custom work for Azure Functions

A typical report request would look like this
- Client makes request to authorization server (or us in the homegrown solution) to retrieve token with their own credentials
- Client uses retrieved token with request to Report Stream
- Request hits Report Stream code directly
- Depending on our implementation, we decide if that request is legitimate and allowed to access the designated endpoint
    - In cloud-based solutions, we would make calls out to an external authz server
      - We could also introspect the token locally if speed is a factor but its more code to write/maintain
    - In a homegrown solution, we would introspect certain cookies and headers and compare them to what we have in a datastore
- If the request is unauthenticated or unauthorized, short-circuit the request to a 401
- If the request is authenticated and authorized for the resource, continue to the business logic

Code example:
```kotlin

// has read scope
@GetMapping("/api/v1/protected/endpoint")
@PreAuthorize("hasAuthority('SCOPE_read')") // scope 
fun protectedReadEndpoint() {
    // read that anyone with the read scope can see
}

// has admin authority
@PostMapping("/api/v1/protected/endpoint")
@PreAuthorize("hasAuthority('Admin')") // role
fun protectedWriteEndpoint() {
    // write that only admins can do
}
```
