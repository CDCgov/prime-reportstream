                                                                                                                                                                                             How to use `<RSErrorBoundary>` && `<Suspense>` with React components

The boundary/suspense pattern helps alleviate error and loading state management from every component into two easily-managed components that wrap your page or component. First in the tree, you'll have an `ErrorBoundary` to catch any errors thrown and render a UI for the user, then, nested below that, a `Suspense` to handle the UI while the data is fetched.

The system is simple: we throw errors from anywhere in our app, and the first boundary on the way up the tree will catch and render it. This functionality has been extended by adding `RSNetworkError` and some custom logic that lets us render _different_ UIs depending on an error's status code.

Steps to use:

- [Wrap your components](#helper-functions)
- Make a network request (this will activate the suspense)
- Throw an error if your call fails (handled by our fetch system)

## Helper functions

To begin using the error boundary, you can wrap components in your jsx using the included helper functions:

- `withCatch`: Wraps your component with `RSErrorBoundary` to catch errors at that level of the DOM
- `withSuspense`: Wraps your component with `Suspense`, providing a spinner UI while data further down the DOM fetches
- `withCatchAndSuspense`: Wraps with both wrappers at the same level of the DOM

```typescript jsx
import {withCatchAndSuspense} from "./RSErrorBoundary";
const MyComponent = () => withCatchAndSuspense(<MyComponentContent/>);
// OR
const App = () => {
    return (
        <>
            <Helmet>
                <title>
                    Sample | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section className="grid-container">
                {withCatchAndSuspense(<MyComponentContent />)}
            </section>
        </>
    );
};
```

## Add a custom error type

Custom error types help us implement ReportStream specific error functionality for different error cases. For example, in the current system, the custom `RSNetworkError` takes a `name` parameter, with values from will the `ErrorName` enum, to identify the specific error type when it gets to the error boundary, and allow the boundary to take specific actions based on the error name.

- `name` will take an `ErrorName` enum value to identify the specific error type when it gets to the error boundary

#### Boilerplate

```typescript
/** Throw from any failing network calls, and pass in the status code to
 * match it with the right display */
export class RSMyNewError extends Error {
    /* Used for identifying unique content to display for error */
    name: ErrorName;
    /* Build a new RSMyNewError */
    constructor(message: string, code: ErrorName, displayAsPage?: boolean) {
        super(message); // Sets message
        this.name = name;
        Object.setPrototypeOf(this, RSMyNewError.prototype);
    }
}
```
