# Using Apple Silicon Macs for Development

## Problems

In general, running Intel Docker images on Apple Silicon is a hit-or-miss proposition even with Apple's Rosetta
technology.
See the known Docker's documentation for known
limitations: https://docs.docker.com/desktop/mac/apple-silicon/#known-issues

At the time of the writing of this note, Microsoft's Azure Function Docker image is only `amd64` compatible, and not
compatible with Apple Silicon processors.
Microsoft has not announced plans to fix this problem.

Many of our local developer tools are set up to run in Docker containers.
Looking at our `docker-compose.yml` file, the `web_receiver`, `prime_dev`, and `settings` services do not work on Apple
Silicon.
Likewise, the `builder` service in our `docker-compose.build.yml` does not work as well.

## Workaround

Fortunately, ReportStream can run directly on your Apple Silicon computer, outside of Docker containers.
The approach outlined in this note uses `gradle` to set up your environment and to run your code directly.
This approach has the benefit of reducing your build-debug cycle time and is detailed in
the [Running Faster](faster-development.md) document.

### Step 1 - Read the getting started instructions

Read the [Getting Started](./getting-started.md) instructions as background information about various components
needed to work in ReportStream. The [Getting Started](./getting-started.md) document may have new information not found
in this document.

### Step 2 - Install dev tools

Installing the recommend tools including these for this note.

- [git](./install-git.md)
- [Docker Desktop](./install-docker.md) Install Docker Desktop directly.
- [OpenJDK](./install-openjdk.md) Install OpenJDK 11 using Brew.
- [Azure Functions Core Tools](./install-afct.md) Install the v3 of Azure Functions.
- [Gradle](./install-gradle.md) Install Gradle using Brew.

An IDE of your choice. Both VS Code and JetBrain's IntelliJ have ARM64 versions.

### Step 3 - Run `cleanslate.sh`

The `cleanslate.sh` script does the base work needed to start developing for ReportStream.
It only needs to be run once.  
This script runs on Apple processors, but it skips a few steps.  
We will need to do these missing steps by hand.

```bash
# build the project
./cleanslate.sh --verbose
# ...

# Check that a Postgres instance is running
docker ps
# CONTAINER ID   IMAGE         COMMAND                  CREATED          STATUS          PORTS                    NAMES
# 2962fb214203   postgres:11   "docker-entrypoint.s…"   57 minutes ago   Up 57 minutes   0.0.0.0:5432->5432/tcp   prime-router_postgresql_1
```

### Step 4 - Run support services

ReportStream depends on set of services to be up before running the main Azure service. The `cleanslate.sh` script
starts a Postgres database but skips starting a few more that are otherwise started by default when `cleanslate.sh`
start is run on a non-Apple processor:

- Azurite - a simulator of Azure storage
- Vault - a secret store
- SFTP - an SFTP server
- soap-webservice - SOAP web service emulator

```bash
docker-compose -f docker-compose.build.yml up --detach
```

Additionally, to ensure that Vault is running and the credentials are stored correctly, run the following (which is
normally covered by `cleanslate.sh` on non-Apple processors):

```bash
docker-compose up --detach vault 1>/dev/null 2>/dev/null
```

You can take down these services by running `./gradlew composeDown` or `docker-compose down` command.
For now, leave these services running and open up a new terminal session.

### Step 5 - Run ReportStream locally

Use Gradle to launch ReportStream, as it will set up the environment variables that ReportStream needs.

```bash
./gradlew run
```

_Note:_ for quicker development you can use `./gradlew quickrun` which skips some long running tasks, but use with
caution as it will not build the FatJar, run database related tasks, or run the tests.

ReportStream should continue to run after launching. A `ctrl-c` will kill the running ReportStream instance.
For now, keep ReportStream running, open a new terminal session.

### Step 6 - Seed the Postgres DB and Vault

To run tests, the Postgres DB and the credential vault need to be seeded with values.
We will need to have ReportStream running for these steps to work (see previous steps).
Again, we will use a Gradle task to do these steps.

```bash
./gradlew primeCLI --args "create-credential --type=UserPass --persist=DEFAULT-SFTP --user foo --pass pass"
./gradlew reloadTables
./gradlew reloadSettings
```

### Step 7 - Run tests

You should be able to run tests now to confirm that everything is working.

```bash
# Smoke test checks that most of the system is working
./gradlew testSmoke
```

### Step 8 - Build Front-end

> This will help you build and serve the front-end. For more information on the build, commands to know, etc, see
> the [Front-end README.md](../../../frontend-react/README.md).

Our new React front-end is easy to get up and running on your machine. First, ensure the following dependencies
installed:

- `node` (see .nvmrc for version specification) via `nvm`
- `yarn` package manager

Use the directions here to install nvm: https://github.com/nvm-sh/nvm#install--update-script
Then:

```bash
nvm install 18.15x # refer to nvmrc for exact current version
node -v # v18.15x
npm -v # v9.5.x

npm install --global yarn
```

#### Serving

Now you have the tools necessary to run the front-end application. Navigate into the `frontend-react` folder
and use `yarn` to serve it on `localhost:3000`

```bash
cd ../frontend-react
yarn
yarn start:localdev
```

#### Refreshing & stopping

The front-end application will run until you `Ctrl + C` to end the process in your terminal. Updates to the front-end
render when a file's changes are saved, eliminating the need to rebuild and serve the project!

### Step 9 - Test if the Front-end Served

If the window hasn't automatically opened, navigate to `http://localhost:3000`.
You should be able to login and utilize the interface. To ensure the front-end is talking to the `prime-router`
application,
log in and access `localhost:3000/daily-data`. Observe your network calls through your browser's dev tools, checking
for any error status codes.

## Next Steps

Now that you have builds and tests running, here are a few pointers to common recipes that developers use.

### Build and run the ReportStream functions

After a code update, a fast way to build and run the code again is

```bash
gradle package fatjar
gradle quickrun
```

### Debugging

`gradle quickrun` will open a debug port on your locally running ReportStream instance.
Connect your debugger remotely to port 5005.
For profiling use the JMX port 9090.

### Using gradle

`gradle tasks` will list all the tasks that Gradle can do for you.

### Start and stop dependent services

Instead of using a `docker-compose up sftp azurite vault` to bring up dependent services, you can use a script.

```bash
./devenv-infrastructure.sh up
```

This script runs in a detached mode so the rather noisy logs of these services are not in your face.
The same script can bring down dependent services.

```bash
./devenv-infrastructure.sh down
```

To see what containers are currently running:

```bash
docker ps
```

To examine the logs of dependent service that is currently running.

```bash
docker logs NAME --follow
```

where NAME is replaced by the container name.
For example, `prime-router_azurite_1` is the name of the Azurite container.

### Format your code

The Kotlin code in the project follows the KTLint style guide.
There is a git hook that checks for conformance to this style guide.
To reformat your new code to be in compliance:

```bash
gradle ktlintFormat
```

### Add environment variables to your shell profile

Both the ReportStream Azure Functions and ReportStream CLI use environment variables extensively.
This note uses `gradle` tasks to set up these environment variables for you.
You can, however, set up these variables directly in your shell profile script.
In this way, you can run the './prime' CLI and functions directly.  
Here's a list of environment variables that are used at the time of writing this note.

```bash
CREDENTIAL_STORAGE_METHOD=HASHICORP_VAULT
VAULT_TOKEN=<get from .vault/env/env.local>
VAULT_API_ADDR=http://localhost:8200
AzureWebJobsStorage="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=<find online>;BlobEndpoint=http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;"
PartnerStorage="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=<find online>;BlobEndpoint=http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;"
POSTGRES_USER=prime
POSTGRES_PASSWORD=changeIT!
POSTGRES_URL=jdbc:postgresql://localhost:5432/prime_data_hub
PRIME_ENVIRONMENT=local
OKTA_baseUrl=hhs-prime.oktapreview.com
OKTA_clientId=0oa2fs6vp3W5MTzjh1d7
OKTA_redirect=http://localhost:7071/api/download
JAVA_HOME=$(/usr/libexec/java_home)
```

## Things That Might Go Wrong

You may run into problems. Here are few of the common ones.

### Azurite

Building while Azurite is running can cause a problem if the `clean` Gradle task is used, because the clean task will
remove the `build` folder that Azurite uses. You can solve this by restarting Azurite.

### Vault files

A message like

```
Couldn't find env file: /Users/username/Projects/prime-reportstream/prime-router/.vault/env/.env.local
```

usually means that local vault files have been deleted.
Running `.cleanslate.sh` again will populate these files.

### Docker-compose warnings

A warning that is common is an orphan container warning.

```
WARNING: Found orphan containers (prime-router_postgresql_1) for this project.
```

This is a benign warning caused by running Postgres in different `docker-compose` script.

### IntelliJ IDEA can't find Node in $PATH

If you're using `brew` to install `node` and using IntelliJ products, you may see an error when attempting to
auto-detect `node` in your PATH variable. Open Preferences and navigate to Node.js and NPM under Languages & Frameworks.
Point the Node interpreter to your Homebrew installation path

ex: `/opt/homebrew/opt/node@14/bin/node`

This should simultaneously solve the Node interpreter and Package manager errors and display proper version numbers for
`npm` and `yarn` on the list of package managers.
