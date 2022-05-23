import azure.functions as func
from ping3 import ping


def main(req: func.HttpRequest) -> func.HttpResponse:

    address = req.params.get("address")

    if address:

        r = ping(address, timeout=10)

        if r == "None":
            result = "Timeout"
        elif r == "False":
            result = "Cannot resolve"
        else:
            result = "Success"

        return func.HttpResponse(
            body=f'{{"result":"{result}"}}',
            status_code=200,
            headers={"Content-Type": "application/json"},
        )
    else:
        return func.HttpResponse(
            body='{"error":"Please pass an address on the query string"}',
            status_code=400,
        )
