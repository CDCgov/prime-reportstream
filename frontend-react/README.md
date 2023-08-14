# Getting started with ReportStream's React application

## Run the React application

Our new React front-end is easy to get up and running on your machine. First, ensure the following dependencies
installed:

-   `node` (see .nvmrc for version specification) via `nvm`
-   `yarn` package manager

Use the directions here to install nvm: https://github.com/nvm-sh/nvm#install--update-script
Then:

```bash
nvm install 18.15.x # refer to nvmrc for exact current version
node -v # v18.15.x
npm -v # v9.5.x

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

yarn run storybook # Runs a local instance of Storybook showcase of all of the components within our .stories files

yarn lint # Runs the front-end linter
yarn lint:write # Runs the front-end linter and fixes style errors
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

## Chromatic and CI Autobuilds

[Chromatic](https://www.chromatic.com/) is a tool for hosting and publishing different versions of a given repository's Storybook. We use Chromatic to host an up-to-date version of all of our Storybook components (any file that ends with `**.stories.tsx` syntax) so that our non-technical folks can see all of our components on the web. All of our CI Autobuild Github workflows can be found in both `.github/workflows/chromatic-master.yml` and `.github/workflows/chromatic-pr.yml`.

`.github/workflows/chromatic-master.yml` triggers a Chromatic build anytime a PR gets merged into our `master` branch.

`.github/workflows/chromatic-pr.yml` triggers a Chromatic build anytime a file with `// AutoUpdateFileChromatic` comment on its FIRST LINE is checked in to a PR. The goal here is to automatically update our Chromatic anytime a file that has an associated Storybook is modified.
