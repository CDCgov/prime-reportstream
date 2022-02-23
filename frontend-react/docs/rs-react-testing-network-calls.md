# Testing Network Calls

## A brief history

When using our former networking library, we ran into issues testing both positive and negative test cases for components; what happens if we don't get data, or the data is incomplete? Enter: mock service worker. The `msw` library makes it easy to configure test handlers that'll give you a ton of flexibility when testing API interfaces and components displaying dynamically fetched data.

## The gist

The test setup process consists of two main tasks:
- setting up your mock service
- listening to the service in your `.test.tsx` file

### Mock services

Setting up a mock service feels rather close to setting up a Node-based API! The `rest` module provided by `msw` includes all your standard specs: get, post, put, delete, patch, options, head, and all. To keep it simple, we'll use `rest.get` as our example.

```typescript
import { rest } from "msw";
import { setupServer } from "msw/node";

const handlers = [
    /* Make some handlers */
]

export const server = setupServer(...handlers)
```

The boilerplate code above should serve as an efficient copy/paste way of starting a new mock service module. In essence, all we're doing is creating an array of endpoints and destructuring them into n-many arguments for `setupServer()`, which works its magic through `msw`.

### Handlers

Let's add a handler to return a successful response from our HistoryApi.

```typescript
const listEndpoint: Endpoint = HistoryApi.list();

const handlers = [
    /* Successfully returns array of Reports */
    rest.get(`http://localhost:3000${listEndpoint.url}`, (req, res, ctx) => {
        return res(ctx.json(HistoryApi.testResponse(3)));
    })
]
```

The parameters necessary are an endpoint (as a string), and a callback function, inside of which you return your mock data. For more information on what can be done using request (req), response (res), and context (ctx), [visit the msw documentation page](https://mswjs.io/docs/getting-started/mocks)!

Here is an example of returning a failed API response:

```typescript
    /* Fails to return array of Reports with 404 */
    rest.get(`http://localhost:3000${detailFail.url}`, (req, res, ctx) => {
        return res(ctx.status(404));
    })
```
> NOTE: As a best practice, I propose we adopt a Success Service / Fail Service pattern, rather than combining all into a single mock service. This isn't necessary for API class tests, but you'll see once we get to components, it really helps simplify things.
> 
> See below for more.

## Using a mock service in tests

Our network calls are handled with a custom React hook. This adds a layer of convenience when designing and implementing components that use the network layer, but it adds a slight complexity to testing.

### API testing

If you're testing an API class, you'll need to render the hook without rendering a component. For this, `testing-library/react-hooks` exists! You can see their `renderHook` function in action below:

```typescript
test("positive response", async () => {
    const { result, waitForNextUpdate } = renderHook(() =>
        useNetwork<Report>(HistoryApi.detail("test"))
    );
    await waitForNextUpdate();

    expect(result.current.loading).toBeFalsy();
    expect(result.current.status).toBe(200);
    expect(result.current.message).toBe("");
    expect(result.current.data).toBeDefined();
});
```

This async-await setup is necessary for asserting the proper state since the info is not returned at call time, but rather after the promise is resolved. 

### Component testing

Components *do* need to render when unit testing, and for that, we can rely on our usual `render` method from the `testing-library/react` library. However, because we need both success and failure tests, we're going to need to create two separate mock services in our mock service module. Let's revisit that:

```typescript
import { rest } from "msw";
import { setupServer } from "msw/node";

const successHandlers = [
    rest.get(`http://localhost:3000${detailFail.url}`, (req, res, ctx) => {
        return res(ctx.status(404));
    })
]

const failureHandlers = [
    rest.get(`http://localhost:3000${detailFail.url}`, (req, res, ctx) => {
        return res(ctx.status(404));
    })
]

export const successServer = setupServer(...successHandlers)
export const failureServer = setupServer(...failureHandlers)
```

Now what we have are two distinct services with a single goal: success and failure respectively. This is crucial so we can change our behavior in component unit tests depending on which service we listen to.

### The Jest setup

In Jest, we can use a `describe` block to separate out logic that runs before and after all tests, and after *each* test. This is how we can configure pass/fail testing for components.

```typescript
import ...
import { successServer, failureServer } from './MockService'

describe("Success Tests", () => {
    // Listen, reset, and close SUCCESS server
    beforeAll(() => successServer.listen());
    afterEach(() => successServer.resetHandlers());
    afterAll(() => successServer.close());

    test(...)
    test(...)
})

describe("Failure Tests", () => {
    // Listen, reset, and close FAILURE server
    beforeAll(() => failureServer.listen());
    afterEach(() => failureServer.resetHandlers());
    afterAll(() => failureServer.close());

    test(...)
    test(...)
})
```
