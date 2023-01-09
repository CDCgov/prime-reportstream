# General Best Practices in ReportStream Front-End App

This document serves as a living document for best practices for contributing to our front-end code and processes.  This is not intended as a resource for best practices in JavaScript, React, CSS, or any other core technology we use (there are much better guides for that linked below); rather, it's meant as a guideline on how we use them in this codebase.

Keep in mind that with any other guidelines, there are always going to be exceptions.  In these cases, use your best judgment, talk to your teammates, and make sure to note historical reasoning via inline or review comments.

## JavaScript/TypeScript

### Naming

- Variables and non-component functions (including hooks) use `camelCase`
 
```js
let hoursSpentOnHomework = 0;

const minutesPerSecond = 60;

const memoizedCallback = useCallback(...);

function doHomework() { ... }

function useHomeworkDoer() { ... }
```

- Components and file names use `PascalCase`
```jsx
// in ExampleClassComponent.tsx
class ExampleClassComponent extends Component { ... }

// in ExampleFunctionComponent.tsx
function ExampleFunctionComponent() { ... }
```

- Avoid generic names like `a|b|c`, `i|j|k`, `tmp`, `obj` and opt for more descriptive ones like `idx`, `child`, `element`, et cetera
- Add units when conversions are appropriate: `timeInHours`, `sizeInBytes`, `heightInInches`
- Use predicate naming for computed boolean values: `isReady`, `shouldDisplayTime`, `willUpdate`

### Prefer using function declarations over function expressions for first-class functions

```js
// function declaration
function doThing() {
    ...
} 

// function expression
const doThing = () => { 
    ...
};
```

Function declarations are hoisted to the top of the scope so they can be referenced prior to where they are in the same scope:

```js
doA(); // valid invocation

function doA() {
    ...
}
```

```js
doA(); // ReferenceError: Cannot access 'doA' before initialization

const doA = () => {
    ...
};
```

Additionally, defining a function as a declaration provides a `name` property by default, which allows the function to be identified in native JavaScript or React stack traces.

See [this GitHub issue](https://github.com/airbnb/javascript/issues/794) for more information.

### Prefer using enums or constants for generalizable values instead of hardcoded numbers and strings

[Magic numbers](https://en.wikipedia.org/wiki/Magic_number_(programming)#:~:text=In%20computer%20programming%2C%20a%20magic,see%20List%20of%20file%20signatures) should be avoided in order to help ensure that our code's logic is as transparent as possible.  Enums are preferred since they can be type-checked, but if enums aren't able to be used for whatever reason, we can still give visibility with constants:

```js
function delayedAlert() {
    setTimeout(() => {
        window.alert("Hey, it's been 3000 milliseconds")
    }, 3000);
}
```

In such a simple example, it's easy to see that we could elevate the `3000` into a constant just from the fact that it's referenced in two different places.  But in a more complex function, that `3000` could be buried under a lot of logic, and we could be relying on that same 3000 milliseconds to trigger another alert or pair it with an animation.  In that case, making it a constant would encourage us to keep one source of truth for this value:

```js
export const DELAY_TIME = 3000;

function delayedAlert() {
    window.setTimeout(() => {
        window.alert(`Hey, it's been roughly ${DELAY_TIME} milliseconds`)
    }, DELAY_TIME);
}
```

This would also encourage us to consider parameterization of this value:

```js
function delayedAlert(delayTime) {
    window.setTimeout(() => {
        window.alert(`Hey, it's been roughly ${delayTime} milliseconds`)
    }, delayTime);
}
```

(Parameterization isn't always necessary or helpful in every circumstance, but there's usually no harm in considering it.)

In short, striving for more human-readable enums and constants helps us:
- Keep logic readable, readily understandable, and easier to update
- Maintains a single source of truth for would-be generalized values
- Encourage parameterization of said values

### Implement graceful degradation when using new/experimental features

TK

### Avoid `any` types

In TypeScript, `any` is a catch-all type that essentially disables strict type-checking for a given variable.  As such, it introduces unsafe runtime operations into our code since TypeScript can't reconcile between expected and actual type usages:

```ts
let maybeNumber: any;

maybeNumber = "abc"; // valid (but unintuitive)

maybeNumber.toFixed(2); // ERROR: valid at compile time but will throw a runtime error
maybeNumber.toExponential(2); // ERROR: valid at compile time but will throw a runtime error
maybeNumber.charAt(0); // valid (but unintuitive)
```

If an ambiguous type is needed, prefer using `unknown` instead to call attention to the type ambiguity and to encourage type guards instead:

```ts
let maybeNumber: unknown;

maybeNumber = 123.45;

maybeNumber.toFixed(2); // ERROR: 'maybeNumber' is of type 'unknown'
maybeNumber.toExponential(2); // ERROR: 'maybeNumber' is of type 'unknown'

// type guard - explicitly check the type
if (typeof maybeNumber === "number") {
    maybeNumber.toFixed(2); // valid with the wrapping type guard
    maybeNumber.toExponential(2); // valid with the wrapping type guard
}
```

Type assertions can also be used with `unknown`s, but they may cause runtime errors since they bypass compile-type type-checking:

```ts
let maybeNumber: unknown;

maybeNumber = 123.45;

(maybeNumber as number).toFixed(2); // valid (but fragile)
(maybeNumber as string).charAt(0); // ERROR: valid at compile time but will throw a runtime error
```

For this reason, it's better to use type guards rather than assertions.

## React

### Prefer writing function components over class components

Function components can do almost everything that class components can do (with `useState` and `useEffect`), and they also encourage more functional composition rather than relying on instance properties and methods.  Although there are no plans to deprecate class components (yet), hooks are only able to be used in function components, and many modern libraries are starting to only support hook-based composition out of the box.  Ultimately, it's in our best interest to align ourselves with this trend, but there's one big exception to this rule: when an [ErrorBoundary](https://reactjs.org/docs/error-boundaries.html) is needed.  This is due to the fact that there's no function component equivalent of `getDerivedStateFromError` and `componentDidCatch` yet.

If you need pointers on migrating from a class component to a function component, there are numerous guides out there including [this one from DigitalOcean](https://www.digitalocean.com/community/tutorials/five-ways-to-convert-react-class-components-to-functional-components-with-react-hooks).  But in cases when we can't migrate a legacy class component to a function component but need to use a hook, consider wrapping it in a function component and invoking the hook there:

```js
import useFancyNewHook from "...";

class LegacyClassComponent extends Component {
    render() {
        return (...);
    }
}

function LegacyWrapperWithFancyNewHook() {
    const hookValue = useFancyNewHook(...);
    
    return (
        <LegacyClassComponent { ...hookValue } />;
    )
}
```

### Hooks versus HOCs: sharing logic between components

With the seemingly ever-shifting landscape of React over the last several years, there have been lots of different methods of encapsulating shared logic between components.  Two of which, hooks and HOCs, are still used fairly widely in the React ecosystem.

#### Writing hooks

Hooks are a first-class solution to sharing logic between components.  (That's not to say no other solutions are valid, just that React specifically accounts/checks for hooks in the render cycle.)  And given the emphasis on writing function components, it's recommended that we prefer to write hooks when there's logic we can generalize between components.

See [the Rules of Hooks](https://reactjs.org/docs/hooks-rules.html) for more information.

#### Writing HOCs (higher-order components)

TK

### Memoize when needed, not all the time

`useMemo` and `useCallback` are two memoizing hooks provided by React.
- https://kentcdodds.com/blog/usememo-and-usecallback

### Treat non-primitives as immutable objects
- React understands reference changes (mostly through `Object.is`), not value changes, for state updates

### Use the right tool for state management

TK
- Component state for UI updates
- Don’t overuse effects -- can data just be inferred on re-render?

### Use Contexts sparingly
React Contexts are just a way to share data within a tree of React components to avoid [prop-drilling](https://kentcdodds.com/blog/prop-drilling).  It is not intended for a global state management system in and of itself [nor is it a substitute an actual global state management tool [like Redux](https://blog.isquaredsoftware.com/2021/01/context-redux-differences/), though it can sometimes be used as such like in our SessionContext.

However, there are a few drawbacks with relying too heavily on Contexts:

**Decreased reusability of hooks and components**

Using Context values introduces an explicit dependency in the descendant hooks and components.  

Consider the following component:

```jsx
import React from "react";
```

In the above example, the coupling of the Context and the component 

This may be intentional in certain cases (e.g., form states), but generally, Contexts should be used in rare circumstances.  See [the React docs](https://reactjs.org/docs/context.html#before-you-use-context) for more information and alternate approaches.

**Potential performance issues**

TK

https://blog.thoughtspile.tech/2021/10/04/react-context-dangers/

### Uncouple logic from the render cycle when possible

React is a tool _whose sole purpose is to render_; by React's own definition, it's "[a] JavaScript library for building user interfaces."  On its own, it's not a tool for state management, for authentication, for parsing data, et cetera.  This means that not all of our logic should be bound to React -- or at the very least, it doesn't all have to exist within components or hooks.  To that point, **when complex or generalizable logic can sensibly and easily be taken out of the render cycle, we should do it**.

Consider the following example:

```js
function PriceRange({ lowPrice, highPrice }) {
    const numberFormatter = Intl.NumberFormat(
        'en-US', 
        { style: 'currency', currency: 'USD' }
    );

    // given two numbers, return a formatted price range like "$100 - $200"
    function getFormattedPriceRange(lowRange, highPrice): string {
        return `${numberFormatter.format(lowRange)} - ${numberFormatter.format(highPrice)}`;
    }
    
    return (
        <p>{ getFormattedPriceRange(lowPrice, highPrice) }</p>
    );
}
```

Unless we _need_ the reusable logic to tap into render functionality, we can just turn this into a plain JavaScript function that can be exported/imported from anywhere (including inside a component or a hook):

```js
// don't need to reinstantiate the formatter each function call
// since JavaScript formatters can get pretty costly to keep 
const numberFormatter = Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

function getFormattedPriceRange(lowRange: number, highRange: number): string {
    return `${numberFormatter.format(lowRange)} - ${numberFormatter.format(highRange)}`;
}
```

```jsx
// then just invoke the new function in the component
function PriceRange({ lowPrice, highPrice }) {
    return (
        <p>{ getFormattedPriceRange(lowPrice, highPrice) }</p>
    );
}
```

There are a few benefits to this approach:
- Easier to reevaluate complexity
- Better encapsulation of logic for separation of concerns
- Helps to prevent unnecessary re-renders 
- Splits logic into smaller, unit-testable parts -- and may obviate the need for rendering in tests   
- Keeps JSX as flat and simplistic as possible

### Strive for single-purpose ("dumb") components and hooks

The concept of "dumb" components has been around in the React ecosystem for a while: components whose sole purpose is to present and display data.  And although this concept was much easier to abide by with the [container/presenter pattern](https://medium.com/@dan_abramov/smart-and-dumb-components-7ca2f9a7c7d0) before the introduction of hooks, keeping this clear separation of concerns is still important for scalability.  Ensuring that each component is focused around one purpose helps break up long logic chains for easier testability, debuggability, and maintenance, and it (usually) also helps keep the components and hooks smaller so they're more likely to be reusable.

Put simply, **dumb components are presentational whereas smart components are concerned with maintaining events and state.**  When writing a new component, it's important to question whether all the logic should exist in one place or if it would be more maintainable (and possibly readable) split out between different components or utility functions.  

Take the following example:

```tsx
import React, { useState } from "react";
import Spinner from "..."

function CommentPage() {
    const [detailedCommentId, setDetailedCommentId] = useState();
    
    return (
        <section>
            <h1>Comments</h1>
            <ul>
                { comments.map((comment) => {
                    <li key={comment.id}>
                        <p>{comment.title}</p>
                        <button type="button" onClick={() => setDetailedCommentId(comment.id)}>
                        </button>
                    </li>
                }) }
            </ul>
        </section>
    )
}
```

There are a few things happening in this component:
- Fetching the list of comments
- Rendering the spinner 
- Rendering each individual comment
- Maintaining the state of the detailed comment view

```tsx
function CommentList() {
}

function CommentListItem() {
}
```

Maintaining a clear separation of concerns between components helps us:
- Keep presentational components agnostic to where they're rendered so they can be reused
- Write smaller, more readable components
- Group more complex logic in manager/parent components for single source of truth
- Write better, more granular and focused tests

Note that it's not always necessary or wise to split logic out.  For example, if there's very specific computational logic that will never exist separately from the presentational logic, keeping them together is perfectly reasonable.  Like everything else in programming, it depends on the circumstances.

### Using react-query

TK

#### Interacting with the query cache

TK

#### Enabling/disabling queries

TK

#### Dependent queries

TK

## CSS

### Prefer using USWDS/Trussworks before adding custom CSS

TK

### Keep selector specificity as low as possible

TK

## Testing

TK - with separate doc

### Types of tests

Using the right kind of test can sometimes be a tricky decision.  The [test pyramid](https://martinfowler.com/articles/practical-test-pyramid.html) is generally good guidance on how many unit, integration, and end-to-end tests should be maintained in a codebase given the resources needed for each type of test, but it's not always applicable.

#### Unit tests
Unit testing is used to validate logic in isolation. They ensure that all the "atoms" of the code ecosystem continue to function properly and that they can be interchangeable parts.  Although unit tests can often skirt the boundaries of testing implementation details, they can be incredibly useful for detecting regressions quickly, and being able to pinpoint failures as soon as possible is a huge benefit.

##### When to add a unit test
- The newly added logic can be tested outside of rendering logic
- A component helper function gets too long (and can be split out into a domain's utils)

#### Integration tests

##### When to add an integration test

#### End-to-end tests
System testing, end-to-end testing, acceptance testing...whatever you call it, it's a beast! What system testing does is validate that an entire user flow functions from end to end (client, server, database) -- naturally, they require a substantial amount of overhead for test setup and runtime environment.
(More coming soon!)

##### When to add an end-to-end test

### Coverage

We keep track of Jest test coverage as a _guideline, not a blocking requirement._  Trying to achieve 100% coverage is always an exercise in futility, and it may actually encourage the anti-pattern of writing redundant or tautological tests.  What's far more important than the coverage percentage is ensuring that our core, unique, or forking logic is tested.

Consider the following example:

```js
// given an array of words, return a string that formats them into a human-readable list
// ex) ["apple", "banana", "cantaloupe", "durian"] ==> "apple, banana, cantaloupe, and durian"
function toSentence(words) {
    return new Intl.ListFormat().format(words);
} 

function ToSentenceComponent({ words }) {
    return <p>{toSentence(words)}</p>;
}
```

If `toSentence` already has sufficient test cases (for null set, for variadic-ness, for falsy values, et cetera), then testing the same cases for ToSentenceComponent is redundant; it ultimately tests that React is rendering correctly rather than our own logic.

### Adding Regression Tests
Whenever a bugfix is added, it's highly recommended to add a regression test to prevent the same bug from recurring in the future.  However, the bugfix and its tests don't have to be part of the same changeset; if something is causing massive critical failures on the site and tests would take a while to write, feel free to split them out into a fast-follow pull request!

### Test Runners

#### Jest

We write Jest tests to validate behavior rather than implementation details.  To that point, we use [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/) to structure said behavior-driven tests.

#### Cypress

Use the right test for the circumstances!

## Architectural decisions

### Build or buy?
- Do we _need_ to create custom code for something or does a good, well-tested third-party package already exist out there?  Our goal is to organize and visualize submission data for our clients.  If a feature doesn’t explicitly involve that, chances are we should use a package.

TK

## Further Reading
- Airbnb's various styleguides: [JavaScript](https://github.com/airbnb/javascript), [React](https://github.com/airbnb/javascript/tree/master/react), [CSS](https://github.com/airbnb/css)
- [React beta docs](https://beta.reactjs.org/) (much better than their current docs)
- https://cdn.tc-library.org/Rhizr/Files/daaz74mzphKfnHsen/files/JavaScript-%20The%20Good%20Parts.pdf
- https://leanpub.com/javascriptallongesix/read
