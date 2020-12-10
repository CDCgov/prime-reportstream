# Setting up SFTP

## Local

Steps

1. Create a local SFTP server
    
    This is easiest to accomplish using a docker image.  With docker installed and running, from the command line create a docker container instance using the following:
    ```
    docker run -p 22:22 -d atmoz/sftp foo:pass:::upload
    ```
    This will create an sftp server running on port 22 with a user/password of foo/pass and will store all of the files in the (container's) /home/foo/upload directory.  You can mount this to your host if you want e.g.

    ```
    docker run -p 22:22 -v ./sftp:/home/foo/upload -d atmoz/sftp foo:pass:::upload 
    ```
1.  Update the organizations.yml file to identify the transport

    For any organizations add a transport section to the yml file for example - 

    ```
      - name: az-phd
        description: Arizona PHD
        services:
        - name: elr
          topic: covid-19
          schema: az/az-covid-19
          jurisdictionalFilter: { patient_state: AZ }
          transforms: { deidentify: false }
          address: http://localhost:1181/
          format: CSV
          transport:
            type: SFTP
            host: localhost
            port: 2222
            filePath: ./upload
    ```
    the above specifies an SFTP transport for the az-phd organization at localhost:22 writing to the ./upload directory under the account

1. Add the user/password

    Within the account where you are invoking the `mvn azure-functions:run`you'll need to add environment variables for each organization you are sending to as follows

    * use the organization name in all caps, substitute a single underline (_) for a dash (-)
    * use the service name afterward (all caps), preceeded by a double underline (__)
    * user is the user name, password is the password

    For example - to set the above, use
    
    ```
        export AZ_PHD__ELR__USER=foo 
        export AZ_PHS__ELR__PASS=pass
    ```

1. Run the functions locally and test as before

    Note that if there isnt a service transport for the organization - SFTP will be skipped and the report will be directly moved to the 'sent' queue

## Running in a container

The steps are similar - but you'll (for now) need to log into the container itself and set the variables.  Working on automatically puting these into the environment when built.

For a local container use `docker exec -it <container name> /bin/bash` to get a bash shell and for the AzureCloud - you can use `az container exec --resource-group <resource-group> --name <container-name> --exec-command "/bin/bash"`