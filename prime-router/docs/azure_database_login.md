# Azure Database Login

This is the process for logging into our database via Azure Active Directory.

## Login to Azure CLI

```aidl
az login
```

## Request a token for Active Directory

```aidl
az account get-access-token --resource-type oss-rdbms
```

You will receive a response that looks like:

```aidl
{
  "accessToken": "token",
  "expiresOn": "2021-04-22 16:22:54.350957",
  "subscription": "sub",
  "tenant": "id",
  "tokenType": "Bearer"
}
```

Copy the value from `accessToken`. This will be your password and it will expire in one hour after request. If your token expires, run this command again.

## Login to PostgreSQL

Using your PostgreSQL tool of choice, login with the following details:

* User: `reportstream_pgsql_admin@dbservername`
* Pasword: `<the accessToken from above>`

Note: The tool pgAdmin is not supported at this time. [Microsoft notes access tokens are longer](https://docs.microsoft.com/en-us/azure/postgresql/howto-configure-sign-in-aad-authentication#connecting-to-azure-database-for-postgresql-using-azure-ad) than pgAdmin's char size for passwords.

### Example in staging

```aidl
export PGPASSWORD=<accessToken>
psql "host=pdhstaging-pgsql.postgres.database.azure.com user=reportstream_pgsql_admin@pdhstaging-pgsql dbname=prime_data_hub sslmode=require"
```