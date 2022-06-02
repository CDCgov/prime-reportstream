from azure.keyvault.secrets import SecretClient
from azure.identity import DefaultAzureCredential
import azure.functions as func


def main(req: func.HttpRequest) -> func.HttpResponse:

    account = req.params.get("account")

    if account:
        creds = DefaultAzureCredential()

        vault_url = f"https://{account}.vault.azure.net"
        client = SecretClient(vault_url=vault_url, credential=creds)
        secret_properties = client.list_properties_of_secrets()

        try:
            # Read secret proprties to confirm access
            list(secret_properties)
            access = 1
        except Exception:
            access = 0

        return func.HttpResponse(
            body=f'{{"access":{access}}}',
            status_code=200,
            headers={"Content-Type": "application/json"},
        )
    else:
        return func.HttpResponse(
            body='{"error":"Please pass a vault name on the query string"}',
            status_code=400,
        )
