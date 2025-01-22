# Authorization Design

## Requirements
- Okta used to manage access for all users and machines
- Ability to easily add, modify, and remove user permissions
- Data are only accessible by those we trust
- Feature parity with [deprecated design](authz-deprecated-implementation.md)

## Access Token Design

### Scopes
Scopes refer to actions that can be done on certain endpoints. Scopes are requested during the login process. We 
will set up rules in Okta that allow scopes to be requested based on organization membership. A requested scope will 
only be returned given you have the correct group membership.

| Scope       | Actions                                                       | Okta Group              |
|-------------|---------------------------------------------------------------|-------------------------|
| super_admin | Anything! (org membership does not matter)                    | ReportStream-SuperAdmin |
| org:write   | Able to update organizations                                  | ReportStream-OrgAdmin   |
| org:read    | Able to access read-only information related to organizations | ReportStream-User       |
| submit      | Able to submit reports                                        | ReportStream-Submit     |
| elims       | Special `elims` specific actions (see below)                  | ReportStream-Elims      |

Breakdown of `elims` actions
- View submissions page on the ReportStream website
- View daily data page of all the receivers I am sending to so that I can see what they received

#### How to set up in Okta

1. Create a scope
<img src="img/okta-scope.png" width="600"/>
<br><br>
2. Create an Access Policy
<img src="img/access-policy.png" width="600"/>
<br><br>
3. Add a Rule to the Policy. Ensure you have limited the scope to the group membership.
<img src="img/access-policy-rule.png" width="600"/>
<br><br>
4. Add an additional rule to deny Access to users not in the group. Ensure the default scopes rule is first in priority.
<img src="img/default-access-policy-rule.png" width="600"/>
<br><br>

### Organizations
Organizations are set up as groups within Okta and will be included in the token. The combination of the scopes and 
groups will allow us to make authorization decisions on most endpoints. Organizations are easily added to the token 
for users via the UI. I will delve into how to set it up for senders later in the document.

<img src="img/groups-claim.png" width="600"/>

### Submit claim

There will also be an optional `userSubmit` or `appSubmit` claim containing specific organizations AND optional sender. These values will 
be dot seperated. If a sender is not included it will be assumed to be a wildcard and allow all senders under that 
organization. Unfortunately the claim names have to seperated as to access the user profile and application user profile 
require different expressions.

examples:
```
"md-phd.full-elr": Only allow to send from md-phd as full-elr
"ca-phd": Allow submitting from any sender under ca-phd
```

1. Update the default user to add an additional profile attribute
<img src="img/submit-attr.png" width="600"/>
<br><br>
2. Add specific values to the user's profile that you want in the access token
<img src="img/user-profile.png" width="600"/>
<br><br>
3. Add a claim referencing the user profile attribute
<img src="img/user-submit.png" width="600"/>
<br><br>


### Claims example

Given the following token claims scenario:
```json
{
  "scp": [
    "openid",
    "email",
    "org:read",
    "submit"
  ],
  "org": [
    "md-phd",
    "ca-phd"
  ],
  "userSubmit": [
    "md-phd.full-elr",
    "ca-phd"  
  ]  
}
```

This particular user would be able to read information about `md-phd` and `ca-phd`. They would also be able to submit 
reports only as `md-phd.full-elr` or for any sender under `ca-phd`. Note that the current implementation uses an 
`organization` claim. This will continue to be present during the transition but will eventually be phased out.

## Setting up a new user

In this system, setting up a new user would be quite easy. Create the user in Okta and then add it to the required 
groups. If this particular user must also manually submit reports you will need to directly modify their profile to add 
the specific senders. For users this can be done in the Okta UI.

### Difficulty with sender setup
Senders are set up as application users within Okta to allow the machine-to-machine client credentials OAuth 2.0 flow. The 
difficulty arises because while Okta allows you to add application users to groups, it does not allow you to easily add 
those groups to the generated access token JWT. The workaround there is adding the necessary information to the Okta 
application user's profile. This can only be done via API at the moment though.

My suggestion is having a sender setup API and CLI that when given the appropriate values, will set them in the profile.

Here is some psudeocode on what that might look like using the Okta admin SDK. Given this would happen in the `auth` 
project we could easily authorize that the user kicking off this job only be a superadmin.
```kotlin
fun setApplicationProfile(
    clientId: String,
    submit: List<String>
) {
    val application = applicationApi.getApplication(clientId, null)
    val groups = getApplicationGroups(clientId)
    application.putprofileItem("groups", groups)
    application.putprofileItem("appSubmit", submit)
    applicationApi.replaceApplication(arnejAppId, application)
}
```

We will also need to keep these profiles up to date, therefore, we would need some sort of daily job to check that groups
are correctly synced. It also wouldn't hurt to be able to be able to kick off this job manually during times of
rapid group changes.

## Authorization check

There will have to be multiple approaches to doing the actual authorization check given it will work differently across 
different application frameworks.

### Spring

In Spring we can leverage method security. This allows us to write custom predicates in SpEL (Spring expression language) 
that will check our access token *before* our application logic runs which will dramatically simplify our code. If this 
predicate fails then the framework will throw an `AuthorizationDeniedException` which can be caught, logged, and a 
ReportStream specific 403 response can be returned.

```kotlin
@PreAuthorize("hasAuthority('SCOPE_org:read') and #oauth2.claim('org').contains(#org)")
@GetMapping("/api/v1/hello")
fun hello(org: String): String {
    return "Hello $org!"
}
```

### Azure Functions

Azure functions code will contain a bit more boilerplate code and will have to be done in controller logic. Please note 
that the below is pseudocode and can be missing important pieces.

```kotlin
@FunctionName("hello")
fun hello(
    @HttpTrigger(
        name = "hello",
        methods = [HttpMethod.GET],
        authLevel = AuthorizationLevel.ANONYMOUS
    ) request: HttpRequestMessage<String?>,
    @BindingName("org") org: String,
): String {
    if (!authorize(request, "org:read", org)) {
        return "403"    
    }
    
    return "Hello $org" 
}

// this would live in some authorization class
fun authorize(
    request: HttpRequestMessage,
    requiredScope: String,
    maybeRequiredOrg: String?
): Boolean {
    val claims = getClaims(request) // pseudocode to grab the claims
    val containsRequiredScope = claims["scp"].contains(requiredScope)
    val containsRequiredOrg = maybeRequiredOrg?.let { requiredOrg ->
        claims["org"].contains(requiredOrg)
    } ?: true
    
    return containsRequiredScope && containsRequiredOrg
}

```
