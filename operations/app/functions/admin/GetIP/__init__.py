import socket
import azure.functions as func


def main(req: func.HttpRequest) -> func.HttpResponse:

    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    print(s.getsockname()[0])

    return func.HttpResponse(f'{{"ip_address":"{s.getsockname()[0]}"}}')
