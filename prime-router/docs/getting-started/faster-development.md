# Tips for Faster Development

Using the Docker containers makes it simple to run the baseline locally, but takes some time to tear down
the containers, rebuild and redeploy.  If you are wanting to 
test changes more quickly and/or save memory you can then run the Azure functions locally.

## Limitations
1. You need to run all required services like the database, Azurite, Vault, etc.  See Setup section below.
1. The settings table will NOT be updated automatically and any updates must be done manually
1. Logs only scroll in the terminal that runs the functions

## Advantages
1. Saves some system memory due to not running some Docker containers
1. Faster to re-run the functions without having to tear down the containers
1. Docker containers that run the services can be left running forever

## Instructions
### Setup
1. Add `sftp` to your hosts file to point to localhost or 127.0.0.1.  Alternatively, you can change all the host 
   references of sftp to locahost in the organizations.yml file.
1. The following commands start the database and other services needed by the Azure functions.  This is
a one time procedure and only needs to be run at workstation startup or when you perform a clean:

```bash
docker-compose -f docker-compose.build.yml up --detach
docker-compose up --scale prime_dev=0 --scale settings=0 --scale web_receiver=0 --detach
```

### Running the Azure functions
To compile and run the functions just run the following command.  You will then be able to access the API at 
http://localhost:7071.  All logs will be shown in the same terminal you run this command in:

`./gradlew run`

Need to re-run faster?  You can skip the test and migrate tasks, which takes quite some time to complete, but do so at your own risk as the 
tests will not run:

`./gradlew quickRun`

To stop the run simply press CTRL-C in the window running the functions.

## Debugging
Connect your debugger remotely to port 5005.  For profiling use JMX port 9090.