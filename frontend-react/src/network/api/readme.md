> TODO: Re-write this according to our new API layer

# How to use the ReportStream API interface

## Design

The developer experience was center of mind when considering how to structure this module. You, the engineer,
have two tasks: define your API, and consume its endpoints. For this, your main concerns will be the 
`API` object type and the `useEndpoint` consumer hook. Everything else is handled under the hood as explained below.

TL;DR:  Create an API, consume the endpoint, and utilize the state/controller in a component.

## 1. Build an API

The `API` object houses three key elements needed to use the consumer: your resource, your base URL, and 
your endpoints. The `resource` can be any "newable" object (i.e. objects instantiated with the `new`
keyword). We have a `Newable<T>` type available checking this. So long as you supply it a `class`, it will 
be able to runtime type check your incoming data. Endpoints map is a definition of what endpoints exist for 
and API, and what methods they can access.

```typescript
import {ApiBaseUrls, EndpointMap} from "./NewApi";

class MyResource {}

const MyEndpoints: EndpointMap = new Map()
const MyApi: API = {
    resource: MyResource,
    url: ApiBaseUrls.LOOKUP_TABLES,
    endpoints: MyEndpoints
}
```

Your `Endpoint.url` does not need to include the base URL because `Api.baseUrl` is used when constructing 
urls. You can define endpoints that use variables in the url using the `:` identifier, a convention shared 
with `react-router`, so it should feel familiar. You'll see how to pass data into the URL in the next section.

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

## 2. Consume the endpoint and assign it a proxy hook name

The main consumer for an endpoint is `useEndpoint<T,P>`. This hook takes in an API, the endpoint key, 
and method. To type your response data, use the `T` generic. Additionally, you can pass in a params object, 
defined by the second generic, labeled `P`. 

```typescript
import useEndpoint from "./UseEndpoint";

// ONLY GET calls on render
const useGetLookupTableDetail = (id: number) =>
    // useEndpoint<D, P> where D is data resource and P is params
    useEndpoint<MyResource, { id: number }>(
        LookupTableApi, // API definition
        "detail", // Endpoint name (key in api.endpoints map)
        "GET", // Method (is validated with endpoint object first)
        { id: id }, // Any URL variables (i.e. .../:id)
    );

// PATCH does not call on render
const usePatchLookupTableDetail = (id: number) =>
    useEndpoint<MyResource, { id: number }>(
        LookupTableApi,
        "detail",
        "PATCH",
        { id: id },
        { data: { ... } } // Example of passing data using `advancedConfig`
    )
```

Notice how our hooks are just calling a hook with some defined parameters. This allows us to keep non-changing 
data out of our render and state, and only pass in what we need from the component, which in this case would be 
`id`.

## 4. Consume and use in components

To consume in a component, you can simply call the function and store the object as a variable or 
destructure the response.

```typescript
// Standard call
const response = useGetLookupTableDetail(123)
// or destructured call
const { data, loading, error } = useGetLookupTableDetail(123)
```

If you are using a non-`GET` method, you can destructure another attribute, a function called `trigger`,
from your API call to trigger these through any interface.

```typescript jsx
const { data, loading, error, trigger } = usePostLookupTableDetails(123)
return (
    <button onClick={() => trigger()}>Add</button>
)
```
