# Getting started with ReportStream's React application
## Run the React application

Our new React front-end is easy to get up and running on your machine. First, ensure the following dependencies
installed:
- `node` (version 14.x)
- `yarn` package manager
```bash
brew install node@14
# Follow the instructions at the end of the brew installation process
# and use the following command to ensure node is working.
node -v # v14.X.X
npm -v # v6.X.X

npm install --global yarn
```

### Serving

Now you have the tools necessary to run the front-end application. Navigate into the `frontend-react` folder
and use `yarn` to serve it on `localhost:3000`

```bash
cd ../frontend-react
yarn
yarn start
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

yarn start # Runs the React app
yarn test # Runs the React app test suite
yarn build # Builds the React app

yarn lint # Runs the front-end linter
yarn lint:write # Runs the front-end linter and fixes style errors
```

