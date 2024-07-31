# AuthElement

Use the `AuthElement` component to restrict access to a page. Because `react-router-dom`
no longer supports custom route components, instead, we can use this middle layer component
to define authorized user types and required user flags. The component uses the session
context to authorize a user. It checks:

- Logged in status: if not logged in, it redirects to `/login`
- Feature flag (optional): use `requiredFeatureFlag` prop to define the flag required
- User type (optional): use `requiredUserType` prop to define one or many user types authorized

> `MemberType.PRIME_ADMIN` is always permitted

Use the `element` prop to define the page you are requiring authorization for.

### Example use

```typescript jsx
import {AuthElement} from "./AuthElement";
import {MemberType} from "./UseOktaMemberships";

const PageAsFunction = () => <div>I'm a page</div>
export const PageWithAuth = () => (
    <AuthElement
        element={Page}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
)

// OR

const PageWithProps = () => <div>I'm a page</div>
export const PageWithPropsAndAuth = () => (
    <AuthElement
        element={<PageAsComponent myProp={true} />}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
)
```