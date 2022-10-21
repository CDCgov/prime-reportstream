# Getting started with ReportStream's React application

## Run the React application

Our new React front-end is easy to get up and running on your machine. First, ensure the following dependencies
installed:

-   `node` (see .nvmrc for version specification) via `nvm`
-   `yarn` package manager

Use the directions here to install nvm: https://github.com/nvm-sh/nvm#install--update-script
Then:

```bash
nvm install 16.18.x # refer to nvmrc for exact current version
node -v # v16.18.x
npm -v # v8.x.x

npm install --global yarn
```

### Serving

Now you have the tools necessary to run the front-end application. Navigate into the `frontend-react` folder
and use `yarn` to serve it on `localhost:3000`

```bash
cd ../frontend-react
yarn
yarn start:localdev
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

yarn start:localdev # Runs the React app use for localdev
yarn build:staging # Builds the React app for staging
yarn build:production # Builds the React app for production

yarn lint # Runs the front-end linter
yarn lint:write # Runs the front-end linter and fixes style errors
```

## Static build info

This react app uses a static build approach and can be served from a static webserver (e.g. a storage bucket or CDN).
This means there are no environment variables to load because there's no local environment. These variables must be "baked" into
the html/javascript.

This is achieved using `env-cmd` to pass the appropriate .env file into the `react-script build` command.

This command loads the environment variables for develop (found in file `'.env.development'`) and runs the `[cmd]`

```json
env-cmd -f .env.development [cmd]
```

Here is the current build system.

```json
// these are the main commands

// local development, runs the scss watcher AND starts the local dev server in parallel
"start:localdev": "env-cmd -f .env.development npm-run-all -p watch-scss start-js",

// these builds are used for the different environments
"build:test": "env-cmd -f .env.test yarn build-base-prod",
"build:staging": "env-cmd -f .env.staging yarn build-base-prod",
"build:production": "env-cmd -f .env.production yarn build-base-prod",
// This is a special localdev build to include Content Security Policy <meta>
// should be used with `run-build-dir` command
"build:localdev:csp": "env-cmd -f .env.dev.csp yarn build-base-dev",

"build-base-prod": "yarn compile-scss-prod && react-scripts build && yarn copy-404page",
"build-base-dev": "yarn compile-scss-dev && react-scripts build && yarn copy-404page",
"start-js": "react-scripts start",

"copy-404page": "cp build/index.html build/404.html",
"compile-scss-prod": "sass --load-path=./node_modules/uswds/dist/scss --no-source-map --style=compressed --quiet src/global.scss:src/content/generated/global.out.css",
"compile-scss-dev":  "sass --load-path=./node_modules/uswds/dist/scss --embed-source-map --quiet-deps src/global.scss:src/content/generated/global.out.css",
"watch-scss": "yarn compile-scss-dev && sass --load-path=./node_modules/uswds/dist/scss --embed-source-map --quiet-deps -w src/global.scss:src/content/generated/global.out.css",
```

The build can then use variables like `%REACT_APP_TITLE%` in the index.html template and `process.env.REACT_APP_TITLE` in the React code.

One caveat, there is only a **single** .env file used per build type. Typically, multiple .env files are loaded (`.env`, `.env.develop` and `.env.local`), but with this approach only the relevant file is used.

-   local dev: `env.development`
-   staging: `.env.staging`
-   production: `env.production`

`.env` and `.env.local` are not currently used.

## Testing Content-Security-Policy locally

CSP (Content-Security-Policy) is different from CORS (Cross-Origin Resource Sharing).  
CSP

To use it:

1. Build using yarn command `build:dev:csp`
2. Local server-side env must be running locally on port 7071
3. Run using yarn command `run:build-dir`
4. Open browser debugger and watch console for errors/warnings as you use the site.

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

(FYI. The error is on `index.js:2` because minification removes line feeds.)

NOTE: This only works `run:build-dir` because webpack's dynamic runtime updating does injection that violates CSP
