# Releases

## Release Versioning

## Deployments

### Staging Tests
As part of the deployment process, the deployment branch needs to be tested on Staging to ensure everything works.
To access Staging, ReportStream needs to log in and verify credentials. We currently have 2 ways of logging in.

In both cases, you start with the following Terminal command: `./prime login --env staging`. Upon completing the logging
process, a token is stored locally and the logs will state for how long the token is valid.
To overwrite the token before it expires, you can add the `--force` parameter to the command.

#### Okta Login Page
This is the main method for users to gain Staging access.

1. On your Terminal, enter `./prime login --env staging`
2. An Okta login page will open on your web browser.
3. Enter your Okta credentials, then wait for the Success message.
4. A token will be stored locally and can be used to run tests.

#### Okta Client Credentials Api
The credentials Api is primarily used for automated testing.
To access it, add the `--useApiKey` parameter to the Terminal log in command.

1. On the ReportStream Azure account, go to `pdhstaging-functionapp` and the Configuration section of it
2. Get the value for `OKTA_authKey` values from `Application settings` and `export` it on your Terminal:
3. On your Terminal, enter `export OKTA_authKey=KEY_GOES_HERE`
4. On your Terminal, enter `./prime login --env staging --useApiKey`
5. A token will be stored locally and can be used to run tests.