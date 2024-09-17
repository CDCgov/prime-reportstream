# Using Apple Silicon Macs for Development

> Note: All information duplicated by the [Backend Setup](../../getting-started/README.md) has been removed.

## Next Steps

Now that you have builds and tests running, here are a few pointers to common recipes that developers use.

### Build and run the ReportStream functions

After a code update, a fast way to build and run the code again is

```bash
gradle package fatjar
gradle quickrun
```

### Start and stop dependent services

Instead of using a `docker compose up sftp azurite vault` to bring up dependent services, you can use a script.

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
AzureWebJobsStorage="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=<find online>;BlobEndpoint=http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;TableEndpoint=http://127.0.0.1:10002/devstoreaccount1;"
PartnerStorage="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=<find online>;BlobEndpoint=http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;TableEndpoint=http://127.0.0.1:10002/devstoreaccount1;"
POSTGRES_USER=prime
POSTGRES_PASSWORD=changeIT!
POSTGRES_URL=jdbc:postgresql://localhost:5432/prime_data_hub
PRIME_ENVIRONMENT=local
RS_OKTA_baseUrl=reportstream.oktapreview.com
RS_OKTA_clientId=0oa8uvan2i07YXJLk1d7
RS_OKTA_redirect=http://localhost:7071/api/download
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

### Docker compose warnings

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
