import { SimpleError } from "../../utils/UsefulTypes";

import { createRequestConfig, RSRequestConfig } from "./NewApi";
import { WatersResponse } from "./WatersApi";
import ValidateApi from "./ValidateApi";

export type ValidatePost = (
    client: string,
    fileName: string,
    contentType: string,
    fileContent: string,
    organizationName: string,
    accessToken: string
) => Promise<WatersResponse>;

const postData: ValidatePost = async (
    client,
    fileName,
    contentType,
    fileContent,
    organizationName,
    accessToken
) => {
    let textBody;
    let response;

    try {
        /* Create a stable config reference with useMemo(). */
        const config: RSRequestConfig | SimpleError = createRequestConfig<{
            org: string;
            sender: string;
        }>(ValidateApi, "validate", "POST", accessToken, organizationName);

        if (!(config instanceof SimpleError)) {
            // TODO: Extend custom headers for NewApi
            response = await fetch(config.url, {
                method: "POST",
                headers: {
                    ...config.headers,
                    "Content-Type": contentType,
                    payloadName: fileName,
                    client: client,
                },
                body: fileContent,
            } as RequestInit);
        }

        if (response) {
            textBody = await response.text();

            // if this JSON.parse fails, the body was most likely an error string from the server
            return JSON.parse(textBody);
        } else {
            throw Error("no response from server");
        }
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

const ValidateApiFunctions = { postData };

export default ValidateApiFunctions;
