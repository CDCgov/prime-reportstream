# Setup Apple Silicon Macs for Development

## Problem

At the time of writing this note, Microsoft's Azure Function docker container is not compatible with Apple Silicon processors. 
Microsoft has not announced plans to fix this problem. 

## Workarounds

Fortunately, most of our tooling works on Apple Silicon, and it is possible to run ReportStream without a Docker container.  

### Step 1 - Follow the getting started instructions

Follow most of the [Getting Started](getting_started.md) instructions, including:

1. Installing the recommend tools 
2. Run `./cleanslate.sh`

The step that runs the `./devenv-infrastructure.sh` will not work. 

### Step 2 - Run dependencies

ReportStream depends on set of containers to be up before running. Run these now in their Docker containers.

```bash
docker-compose up sftp redox azurite vault --detach
```

### Step 3 - Run ReportStream
With the dependent services running, we can run ReportStream locally. 

```bash
gradle run
```

A `ctrl-c` will kill the running ReportStream process. For now, keep ReportStream running and go to next step.

### Step 4 - Setup Settings and Vault
To run tests, the settings db and the vault need to be configured.
The `cleanslate.sh` script has likely failed to populate the settings and vault, so we do this by hand now.
This step only need to be done once. 

In a new shell with ReportStream running in the first shell, execute these commands.

```bash
./prime create-credential --type=UserPass --persist=DEFAULT-SFTP --user foo --pass pass 
./prime multiple-settings set --input settings/organizations.yml
```

### Step 5 - Run tests
You should be able to run tests now to confirm that everything is working. 

```bash
gradle testEnd2End
```

## Final Notes

VS Code and JetBrain's IntelliJ all have ARM64 versions. Be sure to install those. 