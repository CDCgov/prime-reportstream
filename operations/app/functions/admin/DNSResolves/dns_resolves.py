import azure.functions as func
import dns.resolver


def main(req: func.HttpRequest) -> func.HttpResponse:

    dns_address = req.params.get("dns_address")

    if dns_address:

        my_resolver = dns.resolver.Resolver()
        my_resolver.nameservers = [dns_address]
        
        try:
            # Read root of container to validate access
            list(my_resolver.query('google.com'))
            dns_resolves = 1
        except Exception:
            dns_resolves = 0

        return func.HttpResponse(
            body=f'{{"dns_resolves":"{dns_resolves}"}}',
            status_code=200,
            headers={"Content-Type": "application/json"},
        )
    else:
        return func.HttpResponse(
            body='{"error":"Please pass a dns_address on the query string"}',
            status_code=400,
        )
