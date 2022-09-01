# Testing & Network Calls

## A brief history

When using our former networking library, we ran into issues testing both positive and negative test cases for components; what happens if we don't get data, or the data is incomplete? Enter: mock service worker. The `msw` library makes it easy to configure test handlers that'll give you a ton of flexibility when testing API interfaces and components displaying dynamically fetched data.

## The gist

The test setup process consists of two main tasks:

-   setting up your mock service
-   listening to the service in your `.test.tsx` file

### Mock services

Setting up a mock service feels rather close to setting up a Node-based API! The `rest` module provided by `msw` includes all your standard specs: get, post, put, delete, patch, options, head, and all. To keep it simple, we'll use `rest.get` as our example.

```typescript
import { rest } from "msw";
import { setupServer } from "msw/node";

const handlers = [
    /* Make some handlers */
];

export const server = setupServer(...handlers);
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
    }),
];
```

The parameters necessary are an endpoint (as a string), and a callback function, inside of which you return your mock data. For more information on what can be done using request (req), response (res), and context (ctx), [visit the msw documentation page](https://mswjs.io/docs/getting-started/mocks)!

Here is an example of returning a failed API response:

```typescript
/* Fails to return array of Reports with 404 */
rest.get(`http://localhost:3000${detailFail.url}`, (req, res, ctx) => {
    return res(ctx.status(404));
});
```

> NOTE: As a best practice, I propose we adopt a Success Service / Fail Service pattern, rather than combining all into a single mock service. This isn't necessary for API class tests, but you'll see once we get to components, it really helps simplify things.
>
> See below for more.

For an example see our [lookup table mock server](https://github.com/CDCgov/prime-reportstream/blob/58599e355ad2a0b357005190c185a8b8497f0b81/frontend-react/src/__mocks__/LookupTableMockServer.ts).

### Testing for failure

For the sake of completeness, we need both success and failure tests, which means we're going to need to create two separate mock services in our mock service module. Here's an example of how we can mock failure and success responses for the same request:

```typescript
import { rest } from "msw";
import { setupServer } from "msw/node";

const successHandlers = [
    rest.get(tableListUrl, (req, res, ctx) => {
        return res(ctx.status(200));
    }),
];

const failureHandlers = [
    rest.get(tableListUrl, (req, res, ctx) => {
        return res(ctx.status(404));
    }),
];

export const successServer = setupServer(...successHandlers);
export const failureServer = setupServer(...failureHandlers);
```

Now what we have are two distinct services with a single goal: success and failure respectively. This is crucial so we can change our behavior in component unit tests depending on which service we listen to.

Note that if we are testing failure and success for the same endpoint, but the endpoint enables us to differentiate behavior based on, payload, or query param, we can build handlers for failure cases without creating a new msw server. We can either set up new handlers for urls with different parameters, or differentiate based on the request within the body of the handler function.

## Using a mock service in tests

Our network calls are handled with a custom React hook. This adds a layer of convenience when designing and implementing components that use the network layer, but it adds a slight complexity to testing.

### Testing custom fetch hooks

If you're testing a hook, you'll need to render the hook without rendering a component. For this, `testing-library/react-hooks` exists! You can see their `renderHook` function in action below:

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

See our tests for the useValueSets hook for more examples (link tbd).

### Testing components that use custom fetch hooks

Components _do_ need to render when unit testing, and for that, we can rely on our usual `render` method from the `testing-library/react` library. Since our components will be using custom hooks to communicate with the network / API, and any custom hooks that we have built will be testing using msw using the strategies we've talked about above, components are free to mock out the hooks used to make the requests and focus on testing presentation, and any business logic that may live within the components.

```typescript
let mockUseValueSetsTable = jest.fn();

jest.mock("../../../hooks/UseValueSets", () => {
    return {
        useValueSetsTable: () => mockUseValueSetsTable(),
    };
});

describe("ValueSetsIndex", () => {
    test("Renders with no errors", () => {
        mockUseValueSetsTable = jest.fn(() => ({
            valueSetArray: [] as ValueSet[],
            error: null,
        }));
        renderWithFullAppContext(<ValueSetsIndex />);
        const headers = screen.getAllByRole("columnheader");
        const title = screen.getByText("ReportStream Value Sets");
        const rows = screen.getAllByRole("row");

        expect(headers.length).toEqual(4);
        expect(title).toBeInTheDocument();
        expect(rows.length).toBe(1); // +1 for header
    });

```

There are quite a few ways to do this sort of mocking (jest.mock, jest.spyOn, or using mock files), and ReportStream's tests are currently not very opinionated on which way is best. More guidance on that to come!

### The Jest setup

In Jest, we can use a `describe` block to separate out logic that runs before and after all tests, and after _each_ test. This is how we can configure pass/fail testing for components.

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

In general, `describe` blocks can be used for tests that are testing the same basic piece of code. Usually this means setting up `describe` blocks for each function you are testing, but could also group tests by functionality, such as tests for success conditions vs. failure conditions, or tests of a function when in a particular set of states.

Within `describe` blocks, `test` blocks are used to hold specific test cases. In some situations multiple test cases are handled within a single `test` block, but for ease of tracking failures it is best to separate cases when possible.
