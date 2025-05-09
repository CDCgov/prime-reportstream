# Getting started with ReportStream's React application

## Run the React application

Our new React front-end is straightforward to get up and running on your machine. First, ensure the following dependencies are 
installed:

- `node` (see .nvmrc for version specification) via `nvm`
- `yarn` package manager

Use the directions here to install nvm: https://github.com/nvm-sh/nvm#install--update-script
Then:

```bash
nvm install 20.x.x # refer to nvmrc for exact current version
node -v # v20.x.x
npm -v # v10.2.x

npm install --global yarn
```

### Serving

Now you have the tools necessary to run the front-end application. Navigate into the `frontend-react` folder
and use `yarn` to serve it on `localhost:3000`

```bash
cd ../frontend-react
yarn
yarn run dev
```

### Refreshing & stopping

The front-end application will run until you `Ctrl + C` to end the process in your terminal. Updates to the front-end
render when a file's changes are saved, eliminating the need to rebuild and serve the project!

### Ensure connection

If the window hasn't automatically opened, navigate to `http://localhost:3000`.
You should be able to login and utilize the interface. To ensure the front-end is talking to the `prime-router` application,
log in and access `localhost:3000/daily-data`. Observe your network calls through your browser's dev tools, checking
for any error status codes.

## Commands to know

```bash
yarn # Will install all dependencies in package.json

yarn run dev # Runs the React app use for local development
yarn run build:staging # Builds the React app for staging
yarn run build:production # Builds the React app for production

yarn run storybook # Runs a local instance of Storybook showcase of all of the components within our .stories files

yarn run lint # Runs the front-end linter
yarn run lint:fix # Runs the front-end linter and fixes style errors

yarn run test:e2e-ui # Runs a local instance of Playwright UI where you can view and run the e2e tests. This will run using mock data.
CI=true yarn run test:e2e-ui # Runs a local instance of Playwright UI that mimics Github integration

yarn run test:e2e-smoke # Runs the e2e tests that have the tag = @smoke and are meant to run against non mock data.
```

## Static build info

This react app uses a static build approach and can be served from a static webserver (e.g. a storage bucket or CDN).
This means there are no environment variables to load because there's no local environment. These variables must be "baked" into
the html/javascript.

This is achieved using `.env?.[ENVIRONMENT]?.local` files.

This command loads the environment variables for develop (found in file `'.env.development'`) and runs the `[cmd]`

```json
cross-env NODE_ENV=development [cmd]
```

The build can then use variables like `%VITE_TITLE%` in the index.html template and `import.meta.env.VITE_TITLE` in the React code.

## Testing Content-Security-Policy locally

CSP (Content-Security-Policy) is different from CORS (Cross-Origin Resource Sharing).  
CSP

To use it:

1. Build and run using yarn command `preview:build:csp`
2. Open browser debugger and watch console for errors/warnings as you use the site.

Example error would look like this:

```
index.js:2 Refused to apply inline style because it violates the
 following Content Security Policy directive:
 "style-src 'self' https://global.oktacdn.com https://cdnjs.cloudflare.com".
 Either the 'unsafe-inline' keyword, a hash ('sha256-B7Q+2rCrkIRlD5/BjZIWIMJPSHYlTD1AOL+zDLDYQVg='),
 or a nonce ('nonce-...') is required to enable inline execution.
 Note that hashes do not apply to event handlers,
 style attributes and javascript: navigations unless the 'unsafe-hashes' keyword is present.
```

## Chromatic and CI Autobuilds

[Chromatic](https://www.chromatic.com/) is a tool for hosting and publishing different versions of a given repository's Storybook. We use Chromatic to host an up-to-date version of all of our Storybook components (any file that ends with `**.stories.tsx` syntax) so that our non-technical folks can see all of our components on the web. All of our CI Autobuild Github workflows can be found in both `.github/workflows/chromatic-master.yml` and `.github/workflows/chromatic-pr.yml`.

`.github/workflows/chromatic-master.yml` triggers a Chromatic build anytime a PR gets merged into our `master` branch.

`.github/workflows/chromatic-pr.yml` triggers a Chromatic build anytime a file with `// AutoUpdateFileChromatic` comment on its FIRST LINE is checked in to a PR. The goal here is to automatically update our Chromatic anytime a file that has an associated Storybook is modified.

=======

## Running the e2e tests

[Playwright](https://playwright.dev/) is the framework used to create and run our e2e tests.

To get started you will need to create three separate OKTA users. An admin, sender, and receiver.

```
Example:
<yourname>+admin@googlegroups.com
<yourname>+sender@googlegroups.com
<yourname>+receiver@googlegroups.com

```

1. Assign the admin, sender, and receiver users to the Test Users Group
2. Assign the sender user to the DHSender_ignore Group.
3. Assign the receiver user to the DHak-phd Group and make sure that you have data locally to support that organization.
4. Create a `.env.test.local` file within frontend-react and add the following properties along with the values created from step #1:

```
TEST_ADMIN_USERNAME=""
TEST_ADMIN_PASSWORD=""
TEST_ADMIN_TOTP_CODE=""

TEST_SENDER_USERNAME=""
TEST_SENDER_PASSWORD=""
TEST_SENDER_TOTP_CODE=""

TEST_RECEIVER_USERNAME=""
TEST_RECEIVER_PASSWORD=""
TEST_RECEIVER_TOTP_CODE=""
```

_Check with an Okta administrator on the usage of the TOTP code_

```bash
npx playwright install # Installs supported default browsers

npx playwright install-deps # Installs system dependencies

yarn run test:e2e-ui # Runs a local instance of Playwright UI where you can view and run the e2e tests. This will run using mock data.

CI=true yarn run test:e2e-ui # Runs a local instance of Playwright UI that mimics Github integration.

yarn run test:e2e-smoke # Runs the e2e tests that have the tag = @smoke and are meant to run against non mock data.

```

Currently, the tests are running each time a pull request is made and must pass before the pull request can be merged into master. (These tests are using the reportstreamtesting OKTA accounts.)

## CSS Norms

For any new components we create, we have the pattern of Folder + Component + CSS + Storybook. If our component is called ExampleTable, then the structure should look like:

```
/ExampleTable/ExampleTable.tsx
/ExampleTable/ExampleTable.module.scss
/ExampleTable/ExampleTable.stories.tsx
```

Within `ExampleTable.module.scss`, the CSS structures should look as follows:

```
.ExampleTable {
  :global {

  }
}
```

Within the `ExampleTable.tsx` itself, the top-level element should use the CSS Module syntax:

```
import styles from "./ExampleTable.module.scss";

export const ExampleTable = () => {
  return (
    <div className={styles.ExampleTable}>
    </div>
  )
}
```

The idea here is that the top-level class of `.ExampleTable` will allow us to write simple, easy-to-read SASS in the component and stylesheet. No need for anymore CSS Module syntax as the `:global {}` allows us to write regular SASS within, which is protected by the name-spaced `.ExampleTable` CSS Module class. (If for some reason you need to revert to locally scoped variable within the `:global {}` block you can use `:local {}`)

Now, there are two scenarios in which we're writing CSS for our components: 1, writing brand-new styles. 2, overwriting USWDS styles.

For your own custom styles, you should follow a BEM methodology while keeping your naming as semantic as possible, so if our code looks like this:

<!-- ExampleTable.tsx -->

```
import styles from "./ExampleTable.module.scss";

export const ExampleTable = () => {
  return (
    <div className={styles.ExampleTable}>
      <div className="section">
        <div className="logo__container">
          <img className="logo__img" src="" />
          <p className="logo__text">Image</p>
        </div>
        <p className="content"></p>
        <p className="content content--alternate"></p>
      </div>
    </div>
  )
}
```

Then our CSS would look like:

<!-- ExampleTable.module.scss -->

```
.ExampleTable {
  :global {
    .section {
      padding: 1rem;
    }

    .logo {
      &__img {
        height: 12px;
        width: 12px;
      }

      &__text {
        font-size 8px;
      }
    }

    .content {
      font-size: 22px;
      line-height: 24px;
      color: black;

      &--alternate {
        color: blue;
      }
    }

  }
}
```

For overwriting USWDS styles, you'd just see look at the rendered DOM elements with Dev Tools, and find what selectors USWDS is using and then apply them like so:

<!-- ExampleTable.module.scss -->

```
.ExampleTable {
  :global {
    .usa-navbar {
      text-decoration: none;
    }
  }
}
```

These overwrites will ONLY be scoped to your particular component.

## Documentation Table of Contents

### General

- [Best Practices](docs/best-practices.md)
- [Content](docs/content.md)
- [Data fetching patterns](docs/data-fetching-patterns.md)
- [Feature flags](docs/feature-flags.md)
- [RS Auth Element](docs/rs-auth-element.md)
- [RS Error Boundary and Suspense](docs/rs-error-boundary-and-suspense.md)
- [RS IA Content System](docs/rs-ia-content-system.md)
- [RS IA Template System](docs/rs-ia-template-system.md)
- [RS React Testing Network Calls](docs/rs-react-testing-network-calls.md)
- [Test Conventions](docs/test-conventions.md)

### Proposals

- [Permissions Layer](docs/proposals/0001-permissions-layer-proposal.md)
- [Domain Driven Directory Structure](docs/proposals/0002-domain-driven-directory-structure.md)
- [USWDS React Components](docs/proposals/0003-uswds-react-components.md)
