import { EndpointConfig } from "./Api";
import { watersApi, WatersResponse } from "./WatersApi";

export const watersApiFunctions = async function postData(
    client: string,
    fileName: string,
    contentType: string,
    fileContent: string
) {
    const endpoint: EndpointConfig<WatersResponse> = watersApi.postReport(
        client,
        fileName,
        contentType
    );
    let textBody;
    let response;

    try {
        const headers = new Headers();
        if (endpoint.headers) {
            for (let headerKey in endpoint.headers) {
                headers.append(
                    headerKey,
                    endpoint.headers[headerKey] as string
                );
            }
        }

        response = await fetch(endpoint.url, {
            method: "POST",
            headers,
            body: fileContent,
        });

        textBody = await response.text();

        // if this JSON.parse fails, the body was most likely an error string from the server
        return JSON.parse(textBody);
    } catch (error) {
        return {
            ok: false,
            status: response ? response.status : 500,
            errors: [
                {
                    details: textBody ? textBody : error,
                },
            ],
        };
    }
};
