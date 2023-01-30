# Cypress

We use [Cypress](https://www.cypress.io/) as our automated test runner for end-to-end testing.  

(As of writing this README, we have not decided on whether to use Cypress for component testing.)

## Test Patterns

### Page Objects

Our approach is partly inspired by [the page object model pattern](https://www.selenium.dev/documentation/test_practices/encouraged/page_object_models/), wherein we have a 1:1 interface with pages or page components in tests.  They should not contain any expectations or assertions; instead, they represent possible actions that can be taken on their corresponding pages.  

Each page object is extended off a base page common to the module, and every page object directly used in specs is instantiated as a singleton:

```ts
import { BasePage } from "./Base";

export class ExamplePage extends BasePage {
    path = "/example"
    
    elementsMap = {
        heading: "h1",
        subheading: "h2",
        emailInput: "[data-testid='emailInput']",
        passwordInput: "[data-testid='passwordInput']",
        submitButton: "[data-testid='submitButton']",
    };

    fillFormAndSubmit(email: string, password: string): Chainable<JQuery<HTMLElement>> {
        const { emailInput, passwordInput, submitButton } = this.getElements();
        emailInput.type("test@example.com");
        passwordInput.type("verysecurepassword");
        return submitButton.click();
    }
    
    // ...other properties/methods
}
```

There are two basic instance properties of note:
- `path`, which designates the path associated with this page object.  This is used in BasePage's `go` method, which simply navigates to `this.path` in specs.  If custom logic is needed for navigation, `go` can simply be overridden in whatever page object requires it.
- `elementsMap`, which is a map of shortcut property name to a selector on the page object/page component object.  The elements can be accessed through `getElements`.  In the above example, we could use `examplePage.getElements().heading` to find the `<h1>` element, `examplePage.getElements().subheading` to find the `<h2>` element`, and so on.

Page objects are exported as singletons and as classes for ad hoc instantiation.  However, since pages are likely singular, it's more likely that only the singletons will be used in this fashion. 

#### Page Component Objects

There are times that the same set of page elements will be accessed across multiple pages.  In this case, we can introduce _page component objects_ as a common interface of that shared logic.  Functionally, page component objects operate in the same way as page objects in that they're essentially a grouping of elements and convenience operators with one key difference: they shouldn't be navigated to since they're not a 1:1 with a navigable page.

Like page objects, page components objects are exported as singletons and as classes for ad hoc instantiation.  The singletons have sensible defaults, but instantiating the classes will sometimes be necessary for overriding said defaults. 

### Commands

[Cypress commands](https://docs.cypress.io/api/cypress-api/custom-commands) can be used as a means of making our specs faster, more robust, and more straightforward to write.  They can also be used to reduce redundancy in specs -- especially in cases of branching user paths -- by bypassing the UI and "prepopulating" state that doesn't explicitly need to be tested as part of certain specs.

**However, commands are NOT a substitution for user flows that should actually be tested within the browser!**  Even if a command exists to bypass a user flow, we may also want to have a spec specifically for that bypassed user flow; our end-to-end tests should validate that all of our code changes don't interrupt what a user can interact with on our site. 

### Mocks

We run full end-to-end tests on non-production environments, meaning we'll be making real API requests on local, staging, and demo environments.  Consequently, mocks should be avoided whenever possible on these environments.

However, on production, we will need to mock certain API endpoints 1) to ensure stability of "data" and 2) to mitigate any test data leakage. 

TK

## Running Cypress specs

The Cypress specs can be run in two mode: `open` and `run`:

`open`: opens the Cypress GUI for interactivity.  Choose this mode for stepping through and debugging specs. 

`run`: runs the specs in a headless browser.  Choose this mode in Dockerized/CI environments or for a quick sanity check on a branch. 

### Local

1. Ensure you have a `cypress.development.json` file with the correct values.  If yours is missing values or you're not sure, someone from the Experience team can provide theirs.
2. Spin up your local environment
3. Run `yarn run cypress:local:open` for `open` mode or `yarn run cypress:local:run` for `run` mode.

### Staging

TK
   
### Production

TK