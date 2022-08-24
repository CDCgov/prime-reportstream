# How to use the ReportStream API interface

1. Create your `API` by providing a resource, baseUrl, and endpoints map
2. Create a custom hook and a stable `RSRequestConfig` with `useMemo(() => createRequestConfig())`
3. Pass the configuration into `useRequestConfig` to consume it. This returns `{ data, error, loading, trigger }`
4. Cast the types of your destructured response using the TypeScript `as` keyword
5. Return what you need for your component

```typescript
import { createRequestConfig } from "./NewApi";

// Class is useless right now, no run-time type check
class RSSender {}
const SenderAPI: API = {
    resource: RSSender,
    baseUrl: "/api/settings/organizations",
    endpoints: new Map<string, Endpoint>([
        [
            "list",
            {
                url: "/:org/senders",
                methods: ["GET"],
            },
        ],
        [
            "detail",
            {
                url: "/:org/senders/:sender",
                methods: ["GET"],
            },
        ],
    ]),
};

const useSenderResource = () => {
    /* Access the session if you need a token and active memebrship. */
    const { memberships, oktaToken } = useSessionContext();
    /* Create a stable config reference with useMemo(). */
    const config = useMemo(
        () =>
            createRequestConfig<{ org: string; sender: string }>(
                SenderAPI,
                "detail",
                "GET",
                oktaToken?.accessToken,
                activeMembership?.parsedName,
                {
                    org: activeMembership?.parsedName || "",
                    sender: activeMembership?.senderName || "default",
                }
            ),
        /* Note: we DO want to update config ONLY when these values update. If the linter
         * yells about a value you don't want to add, add an eslint-ignore comment. */
        [oktaToken?.accessToken, activeMembership]
    );
    /* Pass the stable config into the consumer and cast the response with types. */
    const {
        data: sender,
        error,
        loading,
    } = useRequestConfig(config) as {
        data: Sender; // Ideally we can use our resource class instead of interfaces.
        error: string;
        loading: boolean;
    };
    /* Finally, return the values from the hook. */
    return {
        sender,
        error,
        loading,
    };
};
```
