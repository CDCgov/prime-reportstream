# Azure Database Login

This is the process for logging into our database via Azure Active Directory.

## Login to Azure CLI

```shell
az login
```

## Request a token for Active Directory

```shell
az account get-access-token --resource-type oss-rdbms
```

You will receive a response that looks like:

```json
{
  "accessToken": "token",
  "expiresOn": "2021-04-22 16:22:54.350957",
  "subscription": "sub",
  "tenant": "id",
  "tokenType": "Bearer"
}
```

Copy the value from `accessToken`. This will be your password. It will expire in one hour after request. If your token expires, run this command again.

After connecting using this password, your session will remain active beyond the token expiration.

## Login to PostgreSQL
**NOTE: the below is currently not working. There is discussion of updating the process. In the meantime, please contact your on-boarding buddy or a teammate that has access to prod, and they can get you set up.**

Using your PostgreSQL tool of choice, login with the following details:

* For write / schema access: `reportstream_pgsql_admin@dbservername`
    * Only use this if you have a specific reason
* For read-only access: `reportstream_pgsql_developer@dbservername`
* Password: `<the accessToken from above>`

Note: The tool pgAdmin is not supported at this time. [Microsoft notes access tokens are longer](https://docs.microsoft.com/en-us/azure/postgresql/howto-configure-sign-in-aad-authentication#connecting-to-azure-database-for-postgresql-using-azure-ad) than pgAdmin's char size for passwords.

### Example in staging

Get your access token and set as envvar `PGPASSWORD` in one go:
```shell
export PGPASSWORD=$(az account get-access-token --resource-type oss-rdbms | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
```

Connect to the staging database with read-only access:
```shell
psql "host=pdhstaging-pgsql.postgres.database.azure.com user=reportstream_pgsql_developer@pdhstaging-pgsql dbname=prime_data_hub sslmode=require"
```
