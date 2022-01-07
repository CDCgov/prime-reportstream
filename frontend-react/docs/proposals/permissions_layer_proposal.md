# React app permissions

## Problem
The ReportStream React application is managing showing/hiding content and turning abilities on and off via ad hoc permissions checks using Okta’s `authState` integration from the `useOktaAuth()` hook. An example of this can be seen in the code for showing or hiding the Submissions navigation item.

```typescript
const {authState} = useOktaAuth()
/* ... */
if (authState !== null && authState.isAuthenticated) {
    if (permissionCheck(PERMISSIONS.PRIME_ADMIN, authState)) {
        itemsMenu.splice(
            1,
            0,
            <NavLink
                to="/submissions"
                key="submissions"
                data-attribute="hidden"
                hidden={true}
                className="usa-nav__link"
            >
                <span>Submissions</span>
            </NavLink>
        );
    }
}
```

The two stand-out issues with our permissions checks are a lack of specificity in our claims and scopes, and the inability to access auth outside of a React component or as-needed inside inner functions because of the rules of Hooks.

---

## Current approach
The structure for our bearer tokens can be seen below:
```json
{
  [...]
  "scp": [
    "email",
    "openid"
  ],
  "organization": [
    "DHmd_phd",
    "DHPrimeAdmins"
  ],
  [...]
}
```

### Login
On login, we are doing two things currently: filtering out the `DHPrimeAdmins` group then applying the `0th` index as the default organization to load, and storing the bearer token in `sessionStorage`. These are done via functions called in [Login.tsx](../../src/pages/Login.tsx) in the `onSuccess` function. 

> Note:  This is where we begin to see a lack of specificity become troublesome. As soon as an organization has more than a single user type, a new organization would need to be made. We also assume one's default organization which can be a bad practice in some applications.

### Checking
As an example of how we check these, first we have to be inside the component at the base level to call `useOktaAuth()`, then we pass a permission name and the authState to a `permissionCheck()` function that returns a boolean based on the user's authentication.

#### `Component A`
```typescript
const { authState } = useOktaAuth();
/* ... */
if (permissionCheck(PERMISSIONS.SENDER, authState)) {
    // Do the thing
}
```
#### `permissionCheck()`
```typescript
const permissionCheck = (permission: string, authState: AuthState) => {
    if (permission === PERMISSIONS.RECEIVER) {
        return reportReceiver(authState);
    }
    return authState.accessToken?.claims.organization.find((o: string) =>
        o.includes(permission)
    );
};
```

> Note:  Using the hook to get our `authState` prevents us from using it as-needed within inner and outer functions. Notice it must be passed from the component to the function rather than being readily available as-needed in the function. 

---

## A new approach

### Addressing the solutions
After doing some research on how permissions are handled currently, and what some best practices are, I have a few proposals on how to move forward.

1. We are over-simplifying how we permit users by solely relying on their organization. -> We should utilize scopes and claims to better manage what permissions a user has.
2. Relying on Okta's React library to access our authentication restricts us to only accessing our authentication information in a component. -> We should create our own Auth class that can be accessed outside of the restraints of hooks.

In the code snippet of our current bearer token, the `scp` field is where we would return those scopes. We can add claims and scopes via Okta's admin UI. For example, let's say we have an `admin`, `sender`, and `receiver` scope. 

| Scope       | Claims                               |
| ----------- | ------------------------------------ |
| admin       | data.view, data.upload, orgs.allOrgs |
| sender      | data.upload, orgs.my-phd             |
| receiver    | data.view, orgs.my-phd               |

How these are sent through a bearer token might affect how we want to use them. For example, in Okta, we might have a `receiver` scope that serves as a baseline receiver, but perhaps a user needs additional permission to upload data. Whereas checking `if (user.isReceiver())` is our way of assuming user has the `data.read` claim, we also must give the user a `data.write` claim individually, rather than the entire `sender` scope. Then, our conditional may appear as such: `if (user.isReceiver() || user.canUpload())`

#### Scope approach
```json
{
  ...
  "scp": [
    "email",
    "openid",
    "receiver"
  ]
  ...
}
```
#### Claims approach
```json
{
  ...
  "scp": [
    "email",
    "openid"
  ],
  "rs_receive": true,
  "rs_settings": [
      "read",
      "write"
  ]
  ...
}
```

---

## Steps to achieve this

### Okta configurations
First, we would need to configure Okta with the proper claims and scopes. After that is where things get tricky! So far, ReportStream users do not have any custom information stored in their Okta user object, so we have nothing relevant to base claims off of other than what group somebody is in. 

> Note:  Check with SimpleReport as they appear to have set up (or attempted to set up) similar configurations in the past.

The **ONLY** thing is, while this is going on, we must keep the `organization` claim untouched as that is the basis of the entire React app's current permissions check. Once we are able to successfully provide claims to individuals, then we can implement code changes. 

### The `Authentication` class

This is a basic approach to how we can handle parsing claims into our own Objects. This object needs to be persisted so long as a user is logged in (likely via `sessionStorage`).
```typescript
export class Authentication {
    accessToken: string;
    claims: UserClaims;
    orgs: String[];

    constructor(token: AccessToken) {
        this.accessToken = token.accessToken;
        this.claims = token.claims;
        this.orgs = token.claims.organization;
        document.cookie = generateCookie(accessToken, claims)
    }

    function updateStore() {
        /* Creates and updates session storage version of Object */
    }

    /* Accessible inside and outside of React components */
    function getAuth(): Authentication {
        return /* Authentication from sessionStorage */
    }

    function userHasClaim(claim: string): boolean {
        return this.claims.includes(claim)
    }

}
```

### Logging in and storing auth
Upon login, we may instantiate a new `Authorization` with the provided information like so:
```typescript
const onSuccess = (tokens: Tokens | undefined) => {
    const auth = new Authorization(tokens?.accessToken) || throw Error
    updateOrganization(groupToOrg(auth.orgs[0]);
    oktaAuth.handleLoginRedirect(tokens);
};
```
> This code does not seek to fix the default org assumption

### Fixing the implementation

Lastly, we'll conclude by fixing up the implementation of our permissions checks across the app. Considering the `restoreOriginalUri` function from above, we can swap out the conditional for this and match the logic.
```typescript
const restoreOriginalUri = async (_oktaAuth: any, originalUri: string) => {
    const authState: Authorization | undefined = cookieAuth;
    if (authState?.userHasClaim(CLAIM.SENDER) && 
        !authState?.userHasClaim(CLAIM.RECEIVER)) {
            history.replace(
                toRelativeUrl(
                    `${window.location.origin}/upload`,
                    window.location.origin
                )
            );
            return;
    }
    history.replace(toRelativeUrl(originalUri, window.location.origin));
};
```