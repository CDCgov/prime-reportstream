# ReportStream Error Boundary

To catch errors like a `catch` block would in React, we use a component called an `ErrorBoundary`. React provides a baseline component that reads in an error, sets the component's state according to that data, and handles rendering either a fallback UI for errors, OR the child if no errors are thrown.

The system is simple: we throw errors from anywhere in our app, and the first boundary on the way up the tree will catch and render it. This functionality has been extended by adding `RSError` and some custom logic that lets us render _different_ UIs depending on an error's enumerated code.

Steps to use:

- [Use an error boundary](#set-up-a-boundary)
- Throw an error (see: [Custom Errors](#add-a-custom-error-type))
- Render an error page (see: [Custom Error Page Content](#add-new-error-page-content))

## Set up a boundary

To begin using the error boundary, you can wrap components in your jsx using the included `withNetworkCall` funciton:

```typescript jsx
import {withNetworkCall} from "./RSErrorBoundary";

const MyComponent = () => withNetworkCall(<MyComponentContent/>);

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
                {withNetworkCall(<MyComponentContent />)}
            </section>
        </>
    );
};
```

Now, any throws that happen from that point in the tree, all the way down, will be caught by this boundary and not affect anything above it in the tree. 

## Add a custom error type

To create a custom throwable that works within the `RSErrorBoundary` ecosystem, extend the `RSError` abstract custom error class. Intellisence or other code scanning tools should recognize that you need to implement:

- `parseCode` - a method for taking in a code whether hard-coded or sent from a server, and returning the right enumerated error name. Alternatively, you could instantiate your class with a single default code by returning it from this method
- `constructor` - should be easy, just read in the required variables and pass them to `super()`

## Add a new `ErrorName` value(s) to enum

The `ErrorName` enum drives the error page rendering system. Every code can be linked to one or two UIs: a page and message (banner). You can find this enum in the `RSError.ts` file.

## Add new error page content

Once your codes are parsed, and your name is enumerated, you can create your own custom content as a jsx element. Once exported, add it to the switch that selects the proper element before rendering in `ErrorPage`

```typescript jsx
/** Handles mapping to the right page or message content */
export const errorContent = (code?: ErrorName, asPage?: boolean) => {
    switch (code) {
        case ErrorName.UNKNOWN:
        default:
            return asPage ? <GenericPage /> : <GenericMessage />;
    }
};
```
