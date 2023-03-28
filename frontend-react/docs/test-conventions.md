# Testing Conventions

## Guiding Principles

Automated tests are an integral part of ongoing maintenance, and they help to fill in the cracks that manual testing inevitably leaves behind.  Of course, that's not to say that manual testing has no purpose; manual testing and automated testing are both essential at increasing the surface area of overall test coverage.

To that end, we should subject our test code to the same rigor and scrutiny that we would our source code; tests should be clean, deterministic, and readily understandable for the benefit of anyone working in the codebase, regardless of how long they've been working in it.

See RTL's [Guiding Principles doc](https://testing-library.com/docs/guiding-principles/) for the source of inspiration and generally good guidelines. 

### Tests should read like a manual for source code

Tests are often one of the first places to look when dealing with familiar code because they offer insight into how specific pieces function in isolation.  Because of this, we should strive to make our tests read like a manual for the related source code: "given this input, I should expect this output" or more practically, "given these conditions, I should expect this behavior."  Most concepts in programming should be able to be boiled down into inputs and outputs, and tests are no different in that regard.

```tsx
function sum(...numbers: number[]) {
    return numbers.reduce((total, value) => total + value, 0);
}

// bad - what does it mean to function correctly?
describe("sum", () => {
    test("functions correctly", () => {
        expect(sum()).toEqual(0);

        expect(sum(1)).toEqual(1);
        expect(sum(1, 2)).toEqual(3);
        expect(sum(1, 2, 3)).toEqual(6);
        expect(sum(1, 10, 100, 1000)).toEqual(1111);
    });
});

// good - use descriptions to explain what's actually happening
describe("sum", () => {
    describe("with no arguments", () => {
        test("returns 0", () => {
            expect(sum()).toEqual(0);
        });
    });

    describe("with arguments", () => {
        test("returns the sum of all the arguments", () => {
            expect(sum(1)).toEqual(1);
            expect(sum(1, 2)).toEqual(3);
            expect(sum(1, 2, 3)).toEqual(6);
            expect(sum(1, 10, 100, 1000)).toEqual(1111);
        });
    });
})
```

> Why?
>
> Having clearer and more sensible descriptions calls attention to what the purpose of the test actually is, which makes it easier to follow along with the underlying logic or to group similarly intentioned tests together.


### Tests should check behavior, not implementation

Especially in the world of front-end code, it's especially important to test our code from a user's perspective -- this means our tests should be prioritizing checking behavior ("what happens") rather than implementation details ("how it happens").

```tsx
function List({ items }: { items: string[] }) {
    return (
        <ul className="bg-light-gray">
            { items.slice(0, 10).map((item) => (
                <li className="bg-white" key={item}>{item}</li>
            )) }
        </ul>
    );
}

// bad - don't test markup or classes, which are more likely to change over time
test("renders ul with bg-light-gray", () => {
    expect(ulNode).toHaveClass("bg-light-gray");
});

// good - check what the user would see
describe("when there are ten or more items", () => {
    test("renders ten items", () => {
        expect(liNodes).toHaveLength(10);
    });
});
```

Testing in the browser means that we're mostly going to be testing markup implicitly, but we should rarely (or never) be testing against markup explicitly.  See [Testing Library's doc](https://testing-library.com/docs/queries/about/#priority) for more information.

> Why?
>
> Testing behavior keeps tests agnostic to the implementation, which makes them more resistant to changes and refactors.  Ultimately, changing implementation shouldn't automatically imply needing to update tests unless the behavior is also changing.

Somewhat of an exception to the aforementioned rule is pure functions, whose behavior is essentially its implementation:

```tsx
// source code
function capitalize(str?: string) {
    if (!str) {
        return "";
    }
    
    return str.charAt(0).toUpperCase() + str.substring(1).toLowerCase();
}

// tests
describe('capitalizeString', () => {
    describe("when the provided string is falsy", () => {
        test("returns an empty string", () => {
            expect(capitalizeString()).toEqual("");
            expect(capitalizeString("")).toEqual("");
        });
    });

    describe("when the provided string is truthy", () => {
        test("returns the string with the first character capitalized (if applicable)", () => {
            expect(capitalizeString("a")).toEqual("A");
            expect(capitalizeString("A")).toEqual("A");
			
            expect(capitalizeString("abc")).toEqual("Abc");
            expect(capitalizeString("Abc")).toEqual("Abc");
            expect(capitalizeString("ABc")).toEqual("Abc");
            expect(capitalizeString("ABC")).toEqual("Abc");
    
            expect(capitalizeString("1")).toEqual("1");
            expect(capitalizeString("1abc")).toEqual("1abc");
    
            // and so on...
        });
    });
});
```

### Tests should be as explicit as possible

```tsx
// bad
expect(sum(1, 2, 3)).toEqual(1 + 2 + 3);

const arrayA = [1, 2, 3];
const arrayB = [4, 5, 6];
const arrayC = [7, 8, 9];
expect(
    concatenate(arrayA, arrayB, arrayC)
).toEqual([
    arrayA,
    arrayB,
    arrayC
].flat());
       
// good
expect((sum(1, 2, 3))).toEqual(6);
expect(
    concatenate(arrayA, arrayB, arrayC)
).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9]);
```

> Why?
>
> Tests should read as superficially clearly as possible, and they shouldn't reimplement the underlying logic.

### Test coverage percentage is a barometer, not a doctrine

> Why?
>
> It's still a good idea to keep an eye on the test coverage percentage as loose guidance (70% - 80%) as a metric for confidence in our code, but 100% coverage is a fallacy.  Especially on the client side where much of our code will be fed through a renderer verbatim, it's less important to attain "complete" coverage, and it may encourage an anti-pattern of introducing tautological tests.

Consider the following example:

```tsx
// given an array of words, return a string that formats them into a human-readable list  
// ex) ["apple", "banana", "cantaloupe", "durian"] ==> "apple, banana, cantaloupe, and durian"  
function toSentence(words: string[]) {
    if (words.length < 1) {
        return "N/A";
    }
    
    return new Intl.ListFormat().format(words);
}

function ToSentenceComponent({ words }) {  
    return <p>{toSentence(words)}</p>;
}  
```  

If the underlying `toSentence` function already has sufficient test cases (for edge cases, for variadic-ness, for falsy values, et cetera), then testing the same cases for ToSentenceComponent is redundant; it ultimately tests that React is rendering correctly rather than our own logic.

### Tests should be independent of each other and (generally) deterministic

- "Independent" means tests A and B should be able to be run 1) separately, 2) together, and 3) in different orders without affecting each other
- "Generally deterministic" means that a single test should have the same predictable behavior given the same type and range of values.  Randomized values are permitted (i.e., Faker.js), provided they're within the same expected types and ranges.

```tsx
// bad - creation of user is handled in first test case,
// which is a dependency in the second test case
describe("user account creation", () => {
    // assume Api is a mocked stateful server
    const api = new Api();
    const userCredentials = { ... };

    test("allows creation of a user", async () => {
        const response = await api.create(userCredentials);
        expect(response.code).toEqual(200);
    });
    
    test("allows lookup of a user", async () => {
        const response = await api.get(userCredentials);
        expect(response.body).toEqual(...);
    });
});

// good - no dependencies between the two tests
describe("user account", () => {
    const userCredentials = { ... };

    describe("when creating a user account", () => {
        let api;

        beforeEach(() => {
            api = new Api();
        });
        
        it("allows creation of a user", async () => {
            const response = await api.create(userCredentials);
            expect(response.code).toEqual(200);
        });
    });

    describe("when looking up a user", () => {
        let api;

        beforeEach(async () => {
            api = new Api();
            // create a user before running an expectation on the get
            // a better solution would be to prepopulate the mock server (which would ordinarily be done through MSW)
            const response = await api.create(userCredentials);
            expect(response.code).toEqual(200);
        });
        
        test("allows lookup of a user", async () => {
            const response = await api.get(userCredentials);
            expect(response.body).toEqual(...);
        });
    });
});
```

> Why?
>
> Predictability is paramount in maintaining healthy test suites and letting us trust that our source code is functioning properly.  Tests that are neither independent nor deterministic create intermittent failures, which ultimately degrades our confidence in them and adds more time in debugging test flickers.

### Tests should check both optimistic cases and pessimistic cases

```tsx
const NUMBER_REGEX = /[0-9]+/;

function containsNumber(value: string) {
    return NUMBER_REGEX.test(value);
}

describe("containsNumber", () => {
    // bad
    test("returns true if the value contains a number", () => {
        expect(containsNumber("123")).toEqual(true);
        expect(containsNumber("abc123")).toEqual(true);
        expect(containsNumber("123abc")).toEqual(true);
        ...
    });
    
    // good - accounts for false return values
    test("returns true if the value contains a number", () => {
        expect(containsNumber("123")).toEqual(true);
        expect(containsNumber("abc123")).toEqual(true);
        expect(containsNumber("123abc")).toEqual(true);
        ...
        expect(containsNumber("abc")).toEqual(false);
        expect(containsNumber("abcxyz")).toEqual(false);
        ...
    });

    // also good (+ clearer about intentions!)
    test("returns true if the value contains a number", () => {
        expect(containsNumber("123")).toEqual(true);
        expect(containsNumber("abc123")).toEqual(true);
        expect(containsNumber("123abc")).toEqual(true);
        ...
    });
    test("returns false if the value does not contain a number", () => {
        expect(containsNumber("abc")).toEqual(false);
        expect(containsNumber("abcxyz")).toEqual(false);
        ...
    });   
});
```

> Why?
>
>Type-checking can of course help mitigate pessimistic cases since disallowed types will throw errors at compilation time, but any application with real users should account for unexpected values and logical blind spots.  This is especially important for optional function arguments and object properties due to the possibility of falsy values.

### Tests should validate _custom logic_, not third-party logic

If a component will always render in the same way given its props, there's no point in testing that because that's fundamentally testing React's renderer.  However, if there's custom forking logic in how a component renders, that's _our_ logic so it should be tested.

```tsx
// bad - this component will always render in the same way
// so we're ultimately just testing that React works properly
function StaticComponent({ message }: { message: string }) {
    return (
        <div>
            <p>{ message }</p>
        </div>
    );
}

describe("StaticComponent", () => {
    beforeEach(() => {
        render(<StaticComponent message="Hello!" />);
    });

    test("renders correctly", () => {
        expect(screen.getByText("Hello!")).toBeVisible();
    });
});

// good - we have forking logic so we should verify it works
function DynamicComponent({ message }: { message?: string }) {
    if (message) {
        return <p>{message}</p>;
    }
    
    return <p>This is a fallback message.</p>;
}

describe("DynamicComponent", () => {
    describe("when no message is provided", () => {
        beforeEach(() => {
            render(<DynamicComponent />);
        });

        test("renders the fallback message", () => {
            expect(screen.getByText("This is a fallback message.")).toBeVisible();
        });
    });

    describe("when a message is provided", () => {
        beforeEach(() => {
            render(<DynamicComponent message="Hello!" />);
        });

        test("renders the provided message", () => {
            expect(screen.getByText("Hello!")).toBeVisible();
        });
        
        test("does not render the fallback message", () => {
            expect(screen.queryByText("This is a fallback message.")).toBeNull();
        });    
    });
});
```

```tsx
function useCustomHook() {
    const { data, isLoading } = useQuery(...);
    
    const otherStuff = ...;
    
    return {
        data,
        isLoading,
        otherStuff
    };
}

function CustomComponent() {
    const { isLoading } = useCustomHook();
    
    if (isLoading) {
        return <p>Loading...</p>
    }
    
    return <p>Hello, {data.name}!</p>;
}

// bad - `isLoading` is logic that comes directly from react-query, so it's redundant to test
describe("useCustomHook", () => {
    const { result } = renderHook(() => useCustomHook());
    expect(result.current.isLoading).toEqual(true);
});

// good - assume `isLoading` functionality works and just test our custom logic that uses it
describe("CustomComponent", () => {
    describe("while loading", () => {
        test("renders a loading message", () => {
            // ...set up and rendering

            expect(screen.getByText("Loading...")).toBeVisible();
        })
    });
    
    describe("when loaded", () => {
        test("renders a welcome message", () => {
            // ...set up and rendering

            expect(screen.getByText("Hello, ReportStream!")).toBeVisible();
        })
    });
});
```

> Why?
>
> It's redundant to test third-party library logic, even implicitly, because that logic is entirely out of our control and really should already be tested by the maintainers of said library.

---

## Test Practices

### Use `before*` and `after*` hooks for setup/teardown and `test`/`it` for expectations

```tsx
// bad
describe("LoginForm", () => {
    test("shows the welcome message", async () => {
        jest.spyOn(someModule, "getWelcomeMessage").mockReturnValue("Welcome!");
    
        render(<LoginFor />);
    
        await userEvent.type(emailInputNode, "abc@123.com");
        await userEvent.type(passwordInputNode, "password");

        expect(screen.getByText("Welcome!")).toBeVisible();
        
        jest.restoreAllMocks();
    });
});

// good
describe("LoginForm", () => {
    let emailInputNode;
    let passwordInputNode;

    beforeEach(async () => {
        jest.spyOn(someModule, "getWelcomeMessage").mockReturnValue("Welcome!");
        jest.spyOn(someModule, "getDeniedMessage").mockReturnValue("Denied!");

        render(<LoginForm />);
    
        await userEvent.type(emailInputNode, "abc@123.com");
        await userEvent.type(passwordInputNode, "password");   
    });
    
    afterEach(() => {
        jest.restoreAllMocks();
    });
    
    test("shows the welcome message", () => {
        expect(screen.getByText("Welcome!")).toBeVisible();
    });
});
```

> Why?
>
> `before` and `after` are specifically meant for setup and teardown, respectively, and they should be used as such to keep each piece of any given test focused -- most notably, it encourages running more granular expectations under the same conditions.  This also allows us to add more granular expectations when new features (or regressions) need to be handled.
> 
> Note that `before` and `after` hooks are scoped to the parent `describe` block, so some care is needed for execution orders.  See [the Jest docs](https://jestjs.io/docs/setup-teardown) for more information.

#### Use `describe`s to separate conditions

```tsx
// bad
describe("LoginForm", () => {
    test("clears the form when the user clicks 'Continue'", () => {
        ...
    });
    
    test("clears the form when the user clicks 'Cancel'", () => {
        ...
    });
});

// good
describe("LoginForm", () => {
    describe("when the user clicks 'Continue'", () => {
        test("clears the form", () => {
            ...
        });
    });

    describe("when user clicks 'Cancel'", () => {
        test("clears the form", () => {
            ...
        });
    });
});
```

> Why?
>
> By organizing our tests into sets of conditions, it becomes much easier to add in more expectations in the future to account for new features and regressions.

### Use `async` only as needed

```tsx
import { useQuery } from "react-query";

function UserPanel() {
    const { data } = useUserResource(...);

    if (!data) {
        return <p>Loading...</p>;
    }

    return (
        <div>
            <p>Hello, { data.firstName }!</p>
        </div>
    );
}

// test
import * as reactQueryExports from "react-query";

describe("UserPanel", () => {
    beforeEach(() => {
        jest.spyOn(reactQueryExports, "useQuery").mockReturnValue({
            data: {
                firstName: "Big Dummy"
            },
            isLoading: false,
            error: {}
        });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    // bad
    test("renders the user's first name", async () => {
        expect(await screen.findByText("Big Dummy")).toBeVisible();
    });

    // good - no need to await because we have a synchronous mock
    test("renders the user's first name", () => {
        expect(screen.getByText("Big Dummy")).toBeVisible();
    });
});
```

> Why?
>
> Using asynchronous finders can build up test runtimes because they're adding overhead through Promise resolutions.  In many cases, we can just run synchronous expectations especially when we have synchronous mocks.

### Add regression tests for bugfixes

> Why?
> 
> Whenever a bugfix is added, it's highly recommended to add a regression test to prevent the same bug from recurring in the future.  
> However, the bugfix and its tests don't have to be part of the same changeset; if something is causing massive critical failures on the site and tests would take a while to write, feel free to split them out into a fast-follow pull request!

