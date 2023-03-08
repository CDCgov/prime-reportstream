# Best Practices

This document serves as a living document for best practices for contributing to `frontend-react` code and processes.  This is not intended as a resource for best practices in JavaScript, React, CSS, or any other core technology we use (there are much better guides for that linked below); rather, it's meant as a guideline on our overall approach and how we use them in this codebase.

Keep in mind that with any other guidelines, there are always going to be exceptions.  In these cases, use your best judgment, talk to your teammates, and make sure to note historical reasoning via inline or review comments.

## Guiding Principles

### Effort is not measured by lines of code

> Why?
>
> Lines of code committed is an unreliable metric for the amount of effort put into any particular changeset.  Essentially, this comes down to a "quality versus quantity" argument, but much more eloquent authors have penned articles like [this](https://www.pluralsight.com/blog/teams/lines-of-code-is-a-worthless-metric--except-when-it-isn-t-) and [this](https://www.gitclear.com/count_lines_of_code_at_your_own_peril) about the topic.

### Strive for reusability; don't reinvent the wheel

> Why?
>
> In many cases, there are already vetted, well-tested, and well-maintained libraries out there that can assist us in accomplishing our goals.  When considering building our own custom solutions, we also need to factor in the long-term costs of maintenance (e.g., testing, versioning, augmenting) before we make a decision.  It may very well be the case that we _do_ need a custom solution, but it's always worth doing some investigation to see if something that already exists fits the bill.

### Don't let perfection be the enemy of done

> Why?
>
> It goes without saying, but no codebase or approach will ever be perfect; users and requirements will change, and it's arguably more important to leave space for flexibility and extensibility so that we can respond accordingly to where the future takes us.
>
> That said, it's always important to weigh options and other teams' bandwidths properly.  Are "suboptimal" solutions good enough for now or are there hard blockers or issues?

### Opt for clarity over cleverness; avoid premature optimizations

> Why?
> 
> The goal of our software is long-term scalability, which involves multiple humans reading and working in the codebase at any given time.  Regardless of how familiar we may be with the underlying technologies, patterns, and processes we use, code becomes legacy as soon as it's merged into the master branch; it no longer "belongs" to any individual person because another person may need to work on it the next day.  Therefore, it's one of our jobs as a team to reduce the barrier to (re-)entry for each other and ourselves down the line.  
> 
> Read more about this [here](https://thedailywtf.com/articles/Programming-Sucks!-Or-At-Least%2c-It-Ought-To-) and [here](https://www.joshwcomeau.com/career/clever-code-considered-harmful/).

---

## Processes

### Branch names

Branch names should follow the format:

```  
experience/XXXX/some-short-description-of-the-feature  
```  

where `XXXX` refers to the GitHub issue/ZenHub ticket number

> Why?
>
> - Git gets confused with purely numeric branches.
> - It helps fellow teammates not have to remember specific ticket numbers when switching between branches locally.

### Commit messages

Commit messages should follow the format:

```  
# subject  
Experience-XXXX: Adds some new feature  
  
# empty line separating subject and description   
# description  
This changeset includes a new feature that allows users to sort tables by multiple columns.
   
Also included in this changeset:  
- A few backfilled tests  
- I listed this item
- Cleaned up some logic  
  
```  

> Why?
>
> Having a detailed commit message helps to track exactly why a change was made, which can come in handy while debugging or rewriting logic down the line.  Unlike pull request descriptions, commit messages are directly attached to the commits (hence the name), so they can be viewed as human-understandable snapshots of their changesets in Git history.  As such, they should provide context over why the changeset is being introduced (e.g., to facilitate another feature, to fix a critical user bug, et cetera) and a brief, high-level overview of what is changing.

See [this GitHub blog post](https://github.blog/2022-06-30-write-better-commits-build-better-projects/) for more context about commit messages.

### Pull request content

Pull requests should be broken down into as many small, atomic pieces as possible.  Sizes and LOCs obviously depend on the logic, but generally speaking if a change exceeds ~500 LOC of production code (not tests or dependencies), breaking it down into smaller pieces is recommended.

In cases in which there needs to be one "atomic" change, it's recommended to create a long-lived feature branch off of which smaller branches can be created.  This'll allow reviewers to inspect the changes in a stepwise manner, and each changeset will have an approval by the time the long-lived branch is merged back into `master`.

> Why?
> 
> Smaller pull requests help reviewers by:
> - Focusing their attention on individual logical changes to make suggestions and catch potential bugs
> - Reducing the amount of complexity and overhead they need to maintain during reviews
> - Not causing slowdown on GitHub
> 
> Smaller pull requests help developers by:
> - Getting proposals in front of eyes sooner rather than later
> - Allowing a more TDD-approach to think about edge/corner cases
> - Breaking their work down into better stopping points

---

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

- Event-handlers use the `handle<target><event>` pattern when declared and the `on<target><event>` pattern as a named parameter or prop

```ts
// on declaration
function handleBackButtonClick() { ... }  
  
function handleTextareaChange() { ... }  
  
function handleFormSubmit() { ... }  

...

// as a prop
<ExampleComponent onBackButtonClick={handleBackButtonClick} />

// as a parameter
function doSomething(onBackButtonClick) { ... }
```  

- Avoid generic names like `a|b|c`, `i|j|k`, `tmp`, `obj` and opt for more descriptive ones like `idx`, `child`, `element`, et cetera
- Add units when conversions are appropriate: `timeInHours`, `sizeInBytes`, `heightInInches`
- Use predicate naming for computed boolean values: `isReady`, `shouldDisplayTime`, `willUpdate`

### Prefer using function declarations over function expressions for first-class functions and components

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

> Why?
>
> Function declarations are hoisted to the top of the scope so they can be referenced prior to where they are in the same scope, and they have a `name` property by default, which allows functions to be identified in native JavaScript or React stack traces without decorators.

```js  

// bad 
doA(); // ReferenceError: Cannot access 'doA' before initialization  
  
const doA = () => {
    ...
};

// good 
doA(); // valid invocation  
  
function doA() {  
    ...
}  
```  

See [this GitHub issue](https://github.com/airbnb/javascript/issues/794) for more information.

### Prefer using enums or constants for generalizable values instead of hardcoded numbers and strings

> Why?
>
> [Magic numbers](https://en.wikipedia.org/wiki/Magic_number_(programming)#:~:text=In%20computer%20programming%2C%20a%20magic,see%20List%20of%20file%20signatures) should be avoided in order to help ensure that our code's logic is as transparent as possible.  Enums are preferred since they can be type-checked, but if enums aren't able to be used for whatever reason, we can still give visibility with constants.

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
	    window.alert(`Hey, it's been roughly ${DELAY_TIME} milliseconds`);
	}, DELAY_TIME);
}  
```  

This would also encourage us to consider parameterization of this value:

```js  
function delayedAlert(delayTime) {  
    window.setTimeout(() => {
	    window.alert(`Hey, it's been roughly ${delayTime} milliseconds`);
	}, delayTime);
}  
```  

(Parameterization isn't always necessary or helpful in every circumstance, but there's usually no harm in considering it.)

In short, striving for more human-readable enums and constants helps us:
- Keep logic readable, readily understandable, and easier to update
- Maintains a single source of truth for would-be generalized values
- Encourage parameterization of said values

### Avoid `any` and down-casting types

Try to use type guards or `unknown`s rather than `any` and down-casting types.

> Why?
>
> In TypeScript, `any` is a catch-all type that essentially disables strict type-checking for a given variable.  As such, it introduces unsafe runtime operations into our code since TypeScript can't reconcile between expected and actual type usages.

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

## React

### Prefer writing function components over class components

> Why?
>
> Function components can do almost everything that class components can do (with `useState` and `useEffect`), and they also encourage more functional composition rather than relying on instance properties and methods.  Although there are no plans to deprecate class components (yet), hooks are only able to be used in function components, and many modern libraries are starting to only support hook-based composition out of the box.

Ultimately, it's in our best interest to align ourselves with this trend of moving to function components, but there's one big exception to this rule: when an [ErrorBoundary](https://reactjs.org/docs/error-boundaries.html) is needed.  This is due to the fact that there's no function component equivalent of `getDerivedStateFromError` and `componentDidCatch` yet.

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
        <LegacyClassComponent { ...hookValue } />
    );
}  
```  

### Uncouple logic from the render cycle when possible

React is a tool whose sole purpose is to render; by React's own definition, it's a "JavaScript library for building user interfaces."  On its own, it's not a tool for state management, for authentication, for parsing data, et cetera.  This means that not all of our logic should be bound to React -- or at the very least, it doesn't all have to exist within components or hooks.  To that point, **when complex or generalizable logic can sensibly and easily be taken out of the render cycle, we should do it**.

> Why?
>
> There are a few benefits to this approach:
> - Easier to reevaluate complexity
> - Better encapsulation of logic for separation of concerns
> - Helps to prevent unnecessary re-renders
> - Splits logic into smaller, unit-testable parts -- and may obviate the need for rendering in tests
> - Keeps JSX as flat and simplistic as possible

Consider the following example:

```tsx
type User = {
    age: number;    
    name: string;
};

function UserList({ users, sortProperty }: { users: User[], sortProperty: "age" | "name" }) {
    
    const sortedUsers = useMemo(() => {
        if (sortProperty === "age") {
            return users.sort((a, b) => a.age - b.age);
        }
        
        return users.sort((a, b) => {
            const nameA = a.name.toUpperCase();
            const nameB = b.name.toUpperCase();

            if (nameA < nameB) {
                return -1;
            }

            if (nameA > nameB) {
                return 1;
            }

            return 0;
        });
    }, [users, sortProperty]);
    
    return (
        <ul>
            { sortedUsers.map((user) => (
                <li key={user.name}>{user.name}, {user.age}</li>
            )) }
        </ul>
    )
}
```  

Unless we need the reusable logic to tap into render functionality (or it'd be difficult to extricate otherwise), we can just turn this into a plain JavaScript function that can be exported/imported from anywhere (including inside a component or a hook):

```tsx
function sortUsers(users: User[], sortProperty: "age" | "name") {
    if (sortProperty === "age") {
        return users.sort((a, b) => a.age - b.age);
    }

    return users.sort((a, b) => {
        const nameA = a.name.toUpperCase();
        const nameB = b.name.toUpperCase();

        if (nameA < nameB) {
            return -1;
        }

        if (nameA > nameB) {
            return 1;
        }

        return 0;
    });
}  

// then just call this function in the component
function UserList({ users, sortProperty }: { users: User[], sortProperty: "age" | "name" }) {
    const sortedUsers = useMemo(() => {
        return sortUsers(users, sortProperty);
    }, [users, sortProperty]);

    return (
        <ul>
            { sortedUsers.map((user) => (
                <li key={user.name}>{user.name}, {user.age}</li>
            )) }
        </ul>
    )
}
```  


### Strive for single-purpose ("dumb" or purely presentational) components and hooks

> Why?
>
> This keeps components and hooks much more flexible in usage, which lends to the React ethos of composition.  Additionally, this breaks complex logic down for easier maintenance; ensuring that each component is focused around one purpose helps break up long logic chains for easier testability, debuggability, and upkeep, and it (usually) also helps keep the components and hooks smaller so they're more likely to be reusable across the codebase.

The concept of "dumb"/purely presentational components has been around in the React ecosystem for a while: components whose sole purpose is to present and display data.  And although this concept was much easier to abide by with the [container/presenter pattern](https://medium.com/@dan_abramov/smart-and-dumb-components-7ca2f9a7c7d0) before the introduction of hooks, keeping this clear separation of concerns is still important for scalability.

Put simply, **dumb components are presentational whereas container/smart components are concerned with maintaining events and state.**  When writing a new component, it's important to question whether all the logic should exist in one place or if it would be more maintainable (and possibly readable) split out between different components or utility functions.

Take the following example:

```tsx  
import React, { useState } from "react";  
import { useQuery } from "react-query"

import { someResourceDefinition } from "./ResourceDefinitions";
import Spinner from "./Spinner";  
  
function CommentsPage() {
    const [focusedComment, setFocusedComment] = useState<Comment>(null);
    const { data, isLoading } = useQuery(someResourceDefinition); 
    
    return (  
        <section>
            <h2>Comments</h2>
            <Spinner isLoading={isLoading}>
                {() => (
                    <div>
                        <ul>
                            {comments.map((comment) => (
                                <li key={comment.id}>
                                    <p>{comment.title}</p>
                                    <button
                                        type="button"
                                        onClick={() => setFocusedComment(comment)}
                                    >
                                        See details
                                    </button>
                                </li>
                            ))}
                        </ul>

                        {Boolean(focusedCommentText) && (
                            <aside>
                                <p>{comment.title}</p>
                                <p>{comment.createdAt}</p>
                                <p>{comment.text}</p>
                            </aside>
                        )}
                    </div>
                )}
            </Spinner>
        </section>
    );
}   
```  

There are a few things happening in this component:
- Fetching the list of comments
- Rendering the spinner
- Rendering each individual comment
- Maintaining the state of the detailed comment view

Instead, it may be better to separate the responsibilities of the single component into multiple components:

```tsx  
import React, { useState } from "react";
import { useQuery } from "react-query"

import { someResourceDefinition } from "./ResourceDefinitions";
import Spinner from "./Spinner"

// primary concerns: fetching comments, rendering spinner while request is in flight 
function CommentsPage() {
    const { data, isLoading } = useQuery(someResourceDefinition);

    return (
        <section>
            <h2>Comments</h2>
            <Spinner isLoading={isLoading}>
                {() => <CommentsList comments={data} /> }
            </Spinner>
        </section>
    );
}

// primary concerns: rendering the list of comments and the focused comment
function CommentsList({ comments }: { comments: Comment[] }) {
    const [focusedComment, setFocusedComment] = useState<Comment>(null);
    
    return (
        <div>
            <ul>
                {comments.map((comment) => (
                    <CommentsListItem 
                        key={comment.id} 
                        comment={comment}
                        onClick={() => setFocusedComment(comment)}
                    />
                ))}
            </ul>

            {Boolean(focusedCommentText) && (
                <FocusedComment comment={focusedComment} />
            )}
        </div>
    )
}

// primary concerns: rendering an individual comment and binding an onClick handler
function CommentsListItem({ comment, onClick }: { comment: Comment, onClick: (comment: Comment) => void }) {
    return (
        <li>
            <p>{comment.title}</p>
            <button
                type="button"
                onClick={() => onClick(comment)}
            >
                See details
            </button>
        </li>
    );
}

// primary concern: just rendering!
function FocusedComment({ comment }: { comment: Comment }) {
    return (
        <aside>
            <p>{comment.title}</p>
            <p>{comment.createdAt}</p>
            <p>{comment.text}</p>
        </aside>
    );
}
```  

This example may be overkill depending on the circumstances, but the idea here is to try to make components focus on at most one or two primary concerns.  Maintaining a clear separation of concerns between components helps us:
- Keep presentational components agnostic to where they're rendered so they can be more easily reused
- Write smaller, more readable components
- Group more complex logic in manager/parent components for single source of truth
- Write better, more granular and focused tests

Note that it's not always necessary or wise to split logic out.  For example, if there's very specific computational logic that will never exist separately from the presentational logic, keeping them together is perfectly reasonable.  Like everything else in programming, it depends on the circumstances.

### Memoize when needed, not all the time

```tsx
function ExampleComponent({ isReady }: { isReady: boolean }) {
    // bad - a re-computation will be needed anytime propA changes anyway so memoization here is an unnecessary reference
    const memoizedValue = useMemo(() => propA ? "a" : "1", [isReady]);

    // good - just compute on each render
    const inferredValue = propA ? "a" : "1";
    
    // ...
}
```

> Why?
>
> Premature optimizations are generally bad practice regardless of their context, but in React, this can cause staleness, slowdown, unnecessary memory consumption.  Essentially, `useMemo` and `useCallback` create a new reference to keep track of the memoized value or function between renders, and this may effectively double the operations if a render necessitates recomputed references.  JavaScript garbage collection is enough to clean up between renders in many cases.
> 
> See [this article](https://kentcdodds.com/blog/usememo-and-usecallback  ) for more context.

### Treat non-primitives as immutable objects

```tsx
const user = {
    fullName: "Big Dummy",
    ...
};

// bad - mutation won't cause a reference change for a re-render
user.fullName = "Bigger Dummy";
<ExampleComponent user={user} />

// good - spread as a new prop
<ExampleComponent user={{ ...user, fullName: "Bigger Dummy"}} />

// better - explicit spread to create a new reference
const updatedUser = {
    ...user,
    fullName: "Bigger Dummy"
}
<ExampleComponent user={updatedUser} />
```

> Why?
>
> React understands _reference_ changes, not value changes, for state updates and re-renders.  It's generally better to treat non-primitives as immutable objects so we ensure reference changes whenever they're needed.

### Use Contexts sparingly

React Contexts are just a way to share data within a tree of React components to avoid [prop-drilling](https://kentcdodds.com/blog/prop-drilling).  It is not intended for a global state management system in and of itself [nor is it a substitute an actual global state management tool like Redux](https://blog.isquaredsoftware.com/2021/01/context-redux-differences/), though it can sometimes be used as such like in our SessionContext since an authentication change 1) should be infrequent and 2) should trigger an app-wide re-render.  However, this should be a rare occurrence.

> Why?
>
> There are a few drawbacks to using Contexts:
> - Tight coupling between Context and descendant hooks/components
> - Potential performance issues

**Decreased reusability of hooks and components**

Using Context values introduces an explicit dependency in the descendant hooks and components.  Consider the following component:

```tsx
import React, { useContext } from "react";
import MyContext from "...";

function ContextualComponent() {
    const myContextValue = useContext(MyContext);

    return (
        <div>
            <p>{myContextValue.property1}</p>
            <p>{myContextValue.property2}</p>
            <p>{myContextValue.property3}</p>                        
        </div>
    );
}
```  

Because there's a explicit dependency on `MyContext` in ContextualComponent, it 1) must be rendered in a `MyContext.Provider` ancestor and 2) there's no way of passing in custom values to the component instead of the Context values.  While it's true that there are workarounds for this -- using default values and/or passing in overriding props -- using said workarounds would significantly reduce the need for Contexts by introducing a competing source of truth.

Having an explicit dependency on Context values may be intentional in certain cases (e.g., form states), but generally, this should be used in rare circumstances.  See [the React docs](https://reactjs.org/docs/context.html#before-you-use-context) (and [the beta docs](https://beta.reactjs.org/reference/react/useContext)) for more information and alternate approaches.

**Potential performance issues**

Contextual descendants/consumers re-render whenever a new Context value is computed, and this can bypass explicitly memoized components.  This isn't much of a performance hit in small, leaf-node components, but this could also trigger a re-renders of much more significant and complex ancestor nodes -- effectively causing a re-render of the entire tree.  Preventing this requires much more thought and consideration for stabilized Context values.

See [this article](https://blog.thoughtspile.tech/2021/10/04/react-context-dangers/ ) for more information.

#### Try to use effects only when synchronizing with external state or the DOM

> Why?
>
> Effects are run after a single render loop so maintaining internal state in effects means that any meaningful change will result in at least two renders: one to flush the initial state to the DOM and then another that immediately follows the flushed DOM.  For internal state, there are usually better solutions like updating in event-handlers or event subscriptions -- or just infer values without using state at all.

```tsx
function LoadingComponent({ isReady }: { isReady: boolean }) {
    // bad - no need to use an effect because the state will update _after_ the second render
    const [isLoading, setIsLoading] = useState(() => !isReady);
    useEffect(() => {
        setIsLoading(!isReady);
    }, [isReady]);

    // good - just infer the value during same render
    const isLoading = !isReady;
}
```  

See [the React docs](https://beta.reactjs.org/learn/you-might-not-need-an-effect) and [this YouTube video](https://www.youtube.com/watch?v=HPoC-k7Rxwo) for more information.

## CSS

### Use classes for low specificity

```scss
// bad - high specificity (1000)
<div style="background-color: orange;"></div>

// bad - high specificity (0100)
#whatever {
    background-color: yellow;
}

// bad - lower specificity (0001) than class selector but tag selectors are vague
div {
    background-color: blue;
}

// bad - avoid !importants as much as possible since they ignore specificity
div {
    background-color: purple !important;
}

// good - low specificity (0010) and allows for class composition
.base-rule {
    background-color: red;
}

// good - low specificity (0020) and composed
.base-rule.variant-rule {
    background-color: pink;
}
```

> Why?
>
> Low-specificity selectors in CSS rules makes them more easily overridden if a specific visual variant or ad hoc style needs to be applied.

### Use specificity and comments to point out custom overrides

```scss
// bad - don't apply custom overrides globally (unless intended)
.usa-table {
    background-color: rebeccapurple;
}
.usa-table thead {
    background-color: greenyellow;
}

// good - use a custom class for added specificity (0010)
.message-receivers-table {
    .usa-table {
        background-color: rebeccapurple;
    }
    .usa-table thead {
        background-color: greenyellow;
    }
}

// better - use a custom class and wrap in feature comments
// MessageReceivers.tsx
.message-receivers-table {
    .usa-table {
        background-color: rebeccapurple;
    }
    .usa-table thead {
        background-color: greenyellow;
    }
}
// END -- MessageReceivers.tsx 
```

> Why?
> 
> Given the way that specificity and the cascade work, it's too easy to apply a global override in CSS.  Using custom classes ensures that we target exactly what we want to modify and adding comments helps future maintainers know where these classes are used.  Essentially, we want to approach CSS as a closed system when applying custom rules. 

---

## Further Reading
- Airbnb's various styleguides: [JavaScript](https://github.com/airbnb/javascript), [React](https://github.com/airbnb/javascript/tree/master/react), [CSS](https://github.com/airbnb/css)
- [React beta docs](https://beta.reactjs.org/) (much better than their current docs)
- [JavaScript: The Good Parts](https://cdn.tc-library.org/Rhizr/Files/daaz74mzphKfnHsen/files/JavaScript-%20The%20Good%20Parts.pdf )
- [JavaScript Allongée](https://leanpub.com/javascriptallongesix/read)