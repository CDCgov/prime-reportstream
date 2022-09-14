# How to use `<RSErrorBoundary>` && `<Suspense>` with React components

The boundary/suspense pattern helps alleviate error and loading state management from every component into two easily-managed components that wrap your page or component. First in the tree, you'll have an `ErrorBoundary` to catch any errors thrown and render a UI for the user, then, nested below that, a `Suspense` to handle the UI while the data is fetched. 

The system is simple: we throw errors from anywhere in our app, and the first boundary on the way up the tree will catch and render it. This functionality has been extended by adding `RSNetworkError` and some custom logic that lets us render _different_ UIs depending on an error's enumerated code.

Steps to use:

- [Wrap your components](#helper-functions)
- Throw an error (see: [Custom Errors](#add-a-custom-error-type))
- Render an error page (see: [Custom Error Page Content](#add-new-error-page-content))

## Helper functions

To begin using the error boundary, you can wrap components in your jsx using the included helper functions:

- `withThrowableError`: Wraps your component with boundary
- `withSuspense`: Wraps your component with a suspense that renders a spinner until fetch called inside the component has returned
- `withNetworkCall`: Wraps your component with boundary and suspense

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

## Add a custom error type

Custom error types help us name and display errors according to members not present on a default `Error`. Currently, there are two extensions:

- `code` will take an `ErrorName` enum value to identify the specific error type when it gets to the error boundary
- `displayAs` (default: `"message"`) will tell the boundary if this error should display as a non-intrusive message banner or replace the entire page with an error ui

#### Boilerplate

```typescript
/** Throw from any failing network calls, and pass in the status code to
 * match it with the right display */
export class RSMyNewError extends Error {
    /* Used for identifying unique content to display for error */
    code: ErrorName;
    /* Used to determine if this error should render as a message or full page */
    displayAs: ErrorUI = ERROR_UI_DEFAULT;
    /* Build a new RSMyNewError */
    constructor(message: string, code: ErrorName, display?: ErrorUI) {
        super(message); // Sets message
        this.code = code;
        Object.setPrototypeOf(this, RSMyNewError.prototype);
    }
}
```

### Add a new `ErrorName` value(s) to enum

The `ErrorName` enum drives the error page rendering system. Every code can be linked to one or two UIs: a page and message (banner). You can find this enum in the `RSNetworkError.ts` file.

## Add new error page content

Once your codes are parsed, and your name is enumerated, you can create your own custom content as a jsx element. Once exported, add it to the switch that selects the proper element before rendering in `ErrorComponent.tsx`
