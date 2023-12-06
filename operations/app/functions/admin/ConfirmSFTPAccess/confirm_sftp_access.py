import azure.functions as func
import os
from paramiko import SSHClient, WarningPolicy, AuthenticationException


def main(req: func.HttpRequest) -> func.HttpResponse:
    host = os.getenv("SFTP_HOST")

    if host:
        try:
            client = SSHClient()
            client.set_missing_host_key_policy(WarningPolicy())
            client.connect(hostname=host, timeout=30)
        except AuthenticationException as e:
            # AuthenticationException means we successfully connected to the SFTP server, just could not authenticate
            # which is expected. But the server is up and accepting connections.
            return func.HttpResponse(
                body=f'OK',
                status_code=200
            )
        except BaseException as e:
            # Any other type of exception indicates a problem.
            return func.HttpResponse(
                body=str(e),
                status_code=503
            )

    else:
        return func.HttpResponse(
            body='{"error":"Please pass a SFTP FQDN as a parameter to this function."}',
            status_code=400,
        )
