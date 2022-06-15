# How to use the ReportStream API interface

## Design

The API interface behaves like an iPhone case. It's a shell that we put around `axios` for type 
safety, configuration simplification and integrity, and to make our configurations easily testable. 
The idea is that every API should have a base URL (i.e. `/api/test`) and a map of endpoints and their 
allowed HTTP methods. A consumer hook will handle parsing this, and we will build our API as a set of 
hooks that utilize the underlying consumer hook.

## 1. Build an API

To build your API, first, add your API base url to the `ApiBaseUrls` enumerated class in `NewApi.ts`. Then,
create your `API`-type variable:

```typescript
import {ApiBaseUrls, EndpointMap} from "./NewApi";

const MyEndpoints: EndpointMap = new Map()

const MyApi: API = {
    url: ApiBaseUrls.LOOKUP_TABLES,
    endpoints: MyEndpoints
}
```

## 2. Populate your `EndpointsMap`

Your `Endpoint.url` does not need to include the base URL because `Api.baseUrl` is used when constructing 
urls. You can define endpoints that use variables in the url using the `:` identifier:

```typescript
MyEndpoints.set(
    "list", // Endpoint key/name
    { url: "/test", methods: ["GET"] } // Endpoint config
)

MyEndpoints.set(
    "detail", // Endpoint key/name
    { url: "/test/:id", methods: ["GET", "PUT"] } // Endpoint config
)
```

## 3. Create the hook consumer
(blocked by Issue #5557)

The way we consume and use endpoints is by creating proxy hooks that call out to our underlying consumer 
hook with a unique configuration.

```typescript

const useLookupTableDetail = () =>
    useApi(
        LookupTableApi, // API definition
        "detail", // Endpoint name (key in api.endpoints map)
        "GET", // Method (is validated with endpoint object first)
        { id: 123 }, // Any URL variables (i.e. .../:id)
    );

const useLookupTableList = () =>
    useApi(
        LookupTableApi,
        "list",
        "GET",
        {}, // Pass empty object if no url variables
        { params: { showInactive: true } } // Any other AxiosRequestConfig fields you need
    )
```

## 4. Consume and use in components
(blocked by Issue #5557)

To consume in a component, you can simply call the function and store the object as a variable or 
destructure the response:

```typescript
const lookupTableList = useLookupTableList()
const { data, loading } = useLookupTableList()

```
