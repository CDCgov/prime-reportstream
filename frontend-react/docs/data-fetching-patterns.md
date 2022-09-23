# Data Fetching Patterns in the ReportStream Frontend

This document will describe the patterns that the ReportStream frontend uses to communicate with the ReportStream API. The hope is that all of our API interactions can more or less follow the patterns layed out here.

## Background

Over its lifetime, the ReportStream frontend has been through a number of revisions in regards to its approach to data fetching.

The first concerted approach to standardization revolved around the [rest-hooks](https://resthooks.io/) library. Rest-hooks took a class-based approach to defining an API interface and provided a lot of useful functionality, but in the end, in addition to being somewhat complex to use and test, and underdocumented, was not flexible enough to handle our needs working with a somewhat fickle and non-RESTful API. We dropped the library and built our own layer(s).

In our two attempts and building our own system without the help of an external library largely faltered around the issues of maintenance and reliability, particularly in regards to state management, and flexibility for handling requests of different types. A decision was made to rethink our approach holistically and consider bringing in a new third party library.

As documented [here](https://cdc.sharepoint.com/:w:/r/teams/USDSatCDC/Shared%20Documents/PRIME%20ReportStream/Team%20Documents/Experience%20Team/Engineering/ReportStream%20Frontend%20Data%20Fetching%20Design%20Research.docx?d=wfe7ad07f71cc4845acc66ec2a1022226&csf=1&web=1&e=lfTMeB), a decision was made to implement [react-query](), and build a new data fetching system around it that would provide the flexibility and ease of use lacking in rest-hooks, while also being easy to maintain and feature rich, unlike our home grown solutions.

## Component Level

All components that require data from the API, or that push data to the API, should do so through simple invocation of custom hooks that will provide:

-   data, when available
-   loading state to be caught with Suspense
-   error state to be caught with ErrorBoundary

In general, fetch logic shouldn't live in components, and all a component should need to do is invoke a hook!

### Typing

Custom hooks can be generically typed in order to provide various types of return values, or to take multiple types of arguments. In the example below, the `useValueSetsTable` hook can return either `ValueSet[]` or `ValueSetRow[]`, so this type is specified in the generic for each hook invocation depending on what the component expects.

### Example

A simplified example taken from [ValueSetsIndex.tsx](https://github.com/CDCgov/prime-reportstream/blob/58599e355ad2a0b357005190c185a8b8497f0b81/frontend-react/src/pages/admin/value-set-editor/ValueSetsIndex.tsx)

```typescript
import { useValueSetsTable } from "../../../hooks/UseValueSets";
const ValueSetsTableContent = () => {
    const { valueSetArray } = useValueSetsTable<ValueSet[]>(
        LookupTables.VALUE_SET
    );
    render <>{valueSetArray}</>
}
export const ValueSetsTable = () => withCatchAndSuspense(<ValueSetsTableContent />);
```

## Custom Hooks Level

Most of the work of actually making a particular request will live in hooks that we build and maintain. These hooks wrap react-query functionality, as well as authorized fetch functions provided by our useAuthorized fetch hook. Any specific business logic around the requests - data manipulation, request chaining, conditional requests, etc. - can and probably should live within these hooks.

### react-query

React-query is a very full featured library that concerns itself with maintaining state for asynchronous actions, and providing good support around a cache for the results of those actions. While not specific to requests, react-query is built primarily to handle the complexities of making network requests within react, and that is what we use it for. For the most part, our patterns rely on two hooks provided by react-query, dealt with in more depth below. There is a lot more to react-query than this, including the QueryClient that allows for easy access to and manipulation of a request cache, and optimistic loading. The resources below should be a starting point for learning more about those.

#### UseQuery & rsUseQuery

[useQuery](https://tanstack.com/query/v4/docs/reference/useQuery?from=reactQueryV3&original=https://react-query-v3.tanstack.com/reference/useQuery), in our app, is the hook used to fetch data. `rsUseQuery` is our very special, slightly wrapped version of `useQuery` that functions more or less exactly the same way as `useQuery`, while also allowing us to avoid making unathenticated requests before our Okta library has fully initialized.

`rsUseQuery` can be used the same way that tanstack's documentation indicates, with the one caveat being that while `useQuery` is very open to multiple ways to structure the arguments passed in, `rsUseQuery` hardens the options around call signatures. `rsUseQuery` will always be called with the signature:

```
rsUseQuery(
  queryKey,
  queryFunction,
  options
)

```

We pass rsUseQuery a key ([see official docs for more](https://tanstack.com/query/v4/docs/guides/query-keys)), a function which will actually do the fetch, and an options object. On each render of the custom hook, useQuery will either run, and fetch data, or not, in the case that there is already fresh data available. React-query under the hood will periodically re-run the fetch function to get fresh data. We are running with default settings for this, but this behavior can be specified as well.

As shown below, `rsUseQuery` is provided by the `useAuthorizedFetch` hook.

A simplified example from [UseValueSets.ts](https://github.com/CDCgov/prime-reportstream/blob/master/frontend-react/src/hooks/UseValueSets.ts)

```typescript
export interface ValueSetsMetaResponse {
    valueSetMeta: LookupTable;
}
export const useValueSetsMeta = (
    dataTableName: string = LookupTables.VALUE_SET
): ValueSetsMetaResponse => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<LookupTable[]>();

    // get all lookup tables in order to get metadata
    const { data: tableData } = rsUseQuery([getTableList.queryKey], () =>
        authorizedFetch(getTableList)
    );

    const tableMeta = findTableMetaByName(tableData, dataTableName);

    return { valueSetMeta: tableMeta };
};
```

#### UseMutation

[useMutation](https://tanstack.com/query/v4/docs/reference/useMutation) is similar to useQuery, but instead of simply fetching data, this hook is used for handling functions that update data in some way, or have particular side effects. In practice this means that useMutation will come into play when we are making non-`GET` requests to the API.

UseMutation itself will take a function, much like useQuery, that represents the actual request. However, instead of performing the request or invoking the past function on render, the hook returns a `mutate` function that can be called at any time to perform the mutuation.

It is worth noting that in some cases, such as the example below, we will want to deal with a mutation and its return value in more traditionally asynchronous way. In that case, useMutation returns a `mutateAsync` function as well, which will return a Promise for the return value of the function passed into `useMutate`.

A simplified example from [UseValueSets.ts - link tbd](link tbd)

```typescript
export const useValueSetUpdate = () => {
    const { authorizedFetch } = useAuthorizedFetch<LookupTable>();

    const updateValueSet = ({ data, tableName }: UpdateValueSetOptions) => {
        return authorizedFetch(updateTable, {
            segments: { tableName: tableName },
            data,
        });
    };

    // generic signature is defined here https://github.com/TanStack/query/blob/4690b585722d2b71d9b87a81cb139062d3e05c9c/packages/react-query/src/useMutation.ts#L66
    // <type of data returned, type of error returned, type of variables passed to mutate fn, type of context (?)>
    const mutation = useMutation<
        LookupTable,
        RSNetworkError,
        UpdateValueSetOptions
    >(updateValueSet);
    return {
        saveData: mutation.mutateAsync,
        isSaving: mutation.isLoading,
    };
};
```

> **Note about Suspense**: Currently Tanstack does not support Suspense use for mutations, only queries. You will still need to use loading state given from the mutation hook to render a spinner conditionally while performing mutations.

#### Typing

Note that useQuery and useMutation are generics, and we can pass along types on each invocation to specify what behavior we expect, type-wise, for each use of the hook.

In the case of `useQuery<T>`, `T` represents the expected type of the return value from the passed function, or the expected type of `data` to be returned from the hook.

UseMutation has a more complicated signature:

```typescript
export function useMutation<
  TData = unknown,
  TVariables = void,
  TContext = unknown,
>
```

In this case `useMutation<LookupTable, RSNetworkError, UpdateValueSetOptions>` denotes that we expect a returned data type of `LookupTable`, a returned error type of `RSNetworkError` and for our mutation function to be called with `UpdateValueSetOptions`.

#### Resources

-   [Quick Start Docs](https://tanstack.com/query/v4/docs/quick-start)
-   [Deep Dive Video](https://www.youtube.com/watch?v=DocXo3gqGdI) - the creator of react query building a sample app in real time that utilizes all of the functionality of react query

### useAuthorizedFetch

In order for the useQuery and useMutation hooks used within our custom hooks to have access to an easy way to actually make the network calls that are the point of the whole thing, the app has a Provider that provides an authorizedFetch function. This function, accessed through the `useAuthorizedFetch` hook, will take two arguments (an Endpoint Config instance (covered in more detail below), and an options object containing anything not covered by the EndpointConfig that the axios call will need), and return a promise for the data returned from the network call.

UseAuthorizedFetch is a typescript generic, taking a single type declaration that will be used to type check the return value of both the authorizedFetch function returned by the hook, and the value of the `data` property output by the returned rsUseQuery hook.

For example, see the useQuery sample code above. In this hook, two different typed authorizedFetch functions are generated by calling useAuthorizedFetch twice. The usage of the first function provides a good example of a very simple call to authorizedFetch, and the second shows how a more complex call with dynamic segment data can be done. The useMutation examples show how useAuthorizedFetch can be used to pass additional information such as data payloads.

It is expected that all network calls to the ReportStream API will use an authorizedFetch function output by a call to useAuthorizedFetch.

## Underlying Functionality

The topics covered above should provide all the information needed to build a new component or page that utilizes requests against the ReportStream API. However, it is worthwhile, and perhaps sometimes necessary, to fully to understand some of the deeper functionality behind the scenes.

### Endpoint Configs

Endpoint configurations, as represented by instances of the RSEndpoint class, represent an endpoint on the ReportStream API that will be called. Each endpoint configuration instance includes:

-   path: the path to the endpoint. Paths can include dynamic segments to be populated at request time, which will be denoted, as in React Router and others, by a prefixed colon (`/:`)
-   method: the HTTP verb to be used
-   queryKey (optional): the single string key that will be used when referencing this endpoint in a useQuery invocation

Beyond this, the RSEndpoint class also includes built in methods and functionality that assist in creating the necessary configuration for each network request:

-   toDynamicUrl: in the case of an endpoint with a dynamic path, this function will take an object containing key-value pairs representing dynamic segment names and values, and build a functional path to use for a particular request
-   toAxiosConfig: given an options object (optionally including segment data), creates an Axios config object that can be used to make a request

#### Structure / Example

The ReportStream API is loosely based around groups of endpoints or resources. In cases where it makes sense to group endpoints it likely also makes sense to group endpoint configurations and other funcationality, such as types. For an example, see `lookupTables.ts`.

```typescript
// the shape used by the frontend client for value sets
export interface ValueSet {
    name: string;
    createdBy: string;
    createdAt: string;
    system: string;
}

/*... More Types */

export const lookupTablesEndpoints: RSApiEndpoints = {
    getTableList: new RSEndpoint({
        path: "/lookuptables/list",
        method: HTTPMethods.GET,
        queryKey: "lookupTables",
    }),
    getTableData: new RSEndpoint({
        // notice the react-router style colon demarcated dynamic path segments
        path: "/lookuptables/:tableName/:version/content",
        method: HTTPMethods.GET,
        queryKey: "lookupTable",
    }),
    updateTable: new RSEndpoint({
        path: "/lookuptables/:tableName",
        method: HTTPMethods.POST,
    }),
    activateTable: new RSEndpoint({
        path: "/lookuptables/:tableName/:version/activate",
        method: HTTPMethods.PUT,
    }),
};
```

### useCreateFetch

Ok, buckle up, let's talk function generation.

UseCreateFetch is a hook that given an okta token and a user's membership, will spit out a function that will spit out an authorizedFetch function. UseAuthorizedFetch calls useCreateFetch under the hood, and passes the generic type passed to useAuthorizedFetch on to the authorizedFetch function spit out by useCreateFetch.

Confused yet?

So, as you can see from the above explanation, useCreateFetch allows us to do two things:

-   close over global auth based information (token and membership) so that the rest of the application can make use of it without the developer or any pieces of higher level code needing to think about it
-   supply type information to the functions that are making requests for us so that we can reason about the shape of return values, and maintain some type safety

The first part there is easier to understand, so let's start there. Here is the meat of the functionality

```typescript
function createTypeWrapperForAuthorizedFetch(
    oktaToken: Partial<AccessToken>,
    activeMembership: MembershipSettings
) {
    const authHeaders = {
        "authentication-type": "okta",
        authorization: `Bearer ${oktaToken?.accessToken || ""}`,
        organization: `${activeMembership?.parsedName || ""}`,
    };

    return async function <T>(
        EndpointConfig: RSEndpoint,
        options: Partial<AxiosOptionsWithSegments> = {}
    ): Promise<T> {
        const headerOverrides = options?.headers || {};
        const headers = { ...authHeaders, ...headerOverrides };
        const axiosConfig = EndpointConfig.toAxiosConfig({
            ...options,
            headers,
        });
        return axios(axiosConfig).then(({ data }) => data);
    };
}
```

This is a function that takes in the auth data, and returns a function that uses the auth data but does not need to know anything about it. The benefits of this are clear if you look back at the rest of this doc and notice that there wasn't any talk about Okta tokens or memberships. This pattern allows us to write code without every worrying about that stuff!

_Ok that makes sense, but didn't you say that useCreateFetch returns a function that RETURNS A FUNCTION that will **RETURNS A FUNCTION** that will close over auth data and make an Axios request?_

Yeah, I know that sounds rough. This was done so that the invocation of the return value from `useCreateFetch` could be generically typed.

from UseCreateFetch:

```typescript
export const useCreateFetch = (
    oktaToken: Partial<AccessToken>,
    activeMembership: MembershipSettings
): AuthorizedFetchTypeWrapper => {
    const generator = useCallback(
        () =>
            // THIS FUNCTION IS WHAT WILL TAKE THE GENERIC TYPE
            auxExports.createTypeWrapperForAuthorizedFetch(
                oktaToken as Partial<AccessToken>,
                activeMembership as MembershipSettings
            ),
        [oktaToken, activeMembership]
    );

    return generator;
};
```

from UseAuthorizedFetch, where this is invoked:

```typescript
export const useAuthorizedFetch = <T>(): AuthorizedFetcher<T> => {
    const { authorizedFetchGenerator } = useContext(AuthorizedFetchContext);
    return authorizedFetchGenerator<T>();
};
```

Without the extra level of indirection, we would need to get into the realm of typing calls to UseContext, which, while possible, would not give us the results that we would want or expect. This level of complexity is intense, for sure, but as mentioned above, the hope is that the code has been structured in such a way that the deeper implementations don't need to be thought about or messed with when writing code to make requests.

## Error Handling and Suspense

The burden of error handling and suspense (loading UI) falls on the component system provided by React, _not_ our React Query integration. React uses two components to handle these two needs: a Suspense component, included as a part of the React library to declare where your loading UI should render and what it should look like, and the RSErrorBoundary, an adaptation of the ErrorBoundary component type given by React that'll catch errors and render an error UI instead of the children it wraps.

To wrap your components with one, or both, of these dom elements, use the included helper functions:

-   withSuspense will remove the wrapped component and replace it with <Spinner /> while any nested children fetch data or lazily load
-   withCatch will remove the wrapped component and replace it with the fallback error UI if any fetches or lazy loads throw errors
-   withCatchAndSuspense will wrap dom element with both tags, allowing for loading AND error UIs when loading/failing
