import { Resource } from "@rest-hooks/rest";

import { getStoredOktaToken, getStoredOrg } from "../utils/SessionStorageTools";
import { getAppInsightsHeaders } from "../TelemetryService";

export default class AuthResource extends Resource {
    // Turn schema-mismatch errors (that break the app) into just console warnings.
    // These happen when extra fields are returned that are not defined in the schema
    static automaticValidation = "warn" as const;

    pk(_parent?: any, _key?: string): string | undefined {
        throw new Error("Method not implemented.");
    }

    static useFetchInit = (init: RequestInit): RequestInit => {
        const accessToken = getStoredOktaToken();
        const organization = getStoredOrg();

        return {
            ...init,
            headers: {
                ...init.headers,
                ...getAppInsightsHeaders(),
                Authorization: `Bearer ${accessToken}`,
                Organization: organization || "",
                "authentication-type": "okta",
            },
        };
    };
}
