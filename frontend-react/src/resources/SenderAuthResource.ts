import { getAppInsightsHeaders } from "../TelemetryService";

import AuthResource from "./AuthResource";

export default class SenderAuthResource extends AuthResource {
    pk(_parent?: any, _key?: string): string | undefined {
        throw new Error("Method not implemented.");
    }

    static useFetchInit = (init: RequestInit): RequestInit => {
        const { organization, token } = JSON.parse(
            sessionStorage.getItem("__deprecatedFetchInit") ?? "{}",
        );

        return {
            ...init,
            headers: {
                ...init.headers,
                ...getAppInsightsHeaders(),
                Authorization: `Bearer ${token ?? ""}`,
                Organization: organization || "",
                "authentication-type": "okta",
            },
        };
    };
}
