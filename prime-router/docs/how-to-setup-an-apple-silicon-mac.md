# Setup Apple Silicon Macs for Development

## Problems

At the time of writing this note, Microsoft's Azure Function docker container is not compatible with Apple Silicon processors. 
Microsoft has not announced plans to fix this problem. 

The current `./devenv-infrastructure.sh` runs the Frontend build in a container. 
This container doesn't run because of the image is not `arm64`. 

## Workarounds

Fortunately, most of our tooling works on Apple Silicon, and it is possible to run ReportStream without a Docker container as explained in the [Running Faster](getting-started/faster-development.md) document. 

### Step 1 - Follow the getting started instructions

Follow most of the [Getting Started](getting_started.md) instructions, including:

1. Installing the recommend tools 
2. Run `./cleanslate.sh`

The step that runs the `./devenv-infrastructure.sh` will not work. 

### Step 2 - Run dependencies

ReportStream depends on set of containers to be up before running. Run these containers now using `docker-compose`.

```bash
# take down existing containers
docker-compose down

# make sure the Postgres is running 
docker-compose -f docker-compose.build.yml up --detach

# Now run the other containers except the function app
docker-compose up --scale prime_dev=0 --scale settings=0 --scale web_receiver=0 --detach
```

> Note: If you get an error running `docker-compose up` like *bind source path does not exist: .../build/ftps*.
> This is likely because a `clean` command has removed the build `build/ftps` directory. You can add the directory by hand using `mkdir build/ftps` and the `docker-compose` will run.

### Step 3 - Run ReportStream
With the dependent services running, we can run ReportStream locally. 

```bash
gradle run
```

A `ctrl-c` will kill the running ReportStream process. For now, keep ReportStream running and go to next step.

### Step 4 - Setup Settings and Vault
To run tests, the settings db and the vault need to be configured.
The `cleanslate.sh` script has likely failed to populate the settings and vault, so we do this by hand now.
This step only needs to be done once. 

In a new shell with ReportStream running in the first shell, execute these commands.

```bash
./prime create-credential --type=UserPass --persist=DEFAULT-SFTP --user foo --pass pass 
./prime multiple-settings set --input settings/organizations.yml
```

### Step 5 - Run tests
You should be able to run tests now to confirm that everything is working. 

```bash
# Smoke test checks that most of the system is working
gradle testSmoke
```

### Step 6 - Build Frontend

You should be able to build the frontend locally per the [ReadMe](../frontend/readme.md) of the frontend project. 

```bash
cd ./frontend/
npm ci
npm run build

# static site root built in `frontend/dist`
ls ./dist
```

### Step 7 - Test Frontend

Navigate to `http://localhost:8090/index.html`. You should be able to login and exercise the UI. 

## Final Notes

Building while Azurite is running can cause a problem because `clean` will remove the `build` folder that Azurite
uses. You can solve this restarting Azurite.

VS Code and JetBrain's IntelliJ all have ARM64 versions. Be sure to install those. 