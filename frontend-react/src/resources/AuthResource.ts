import { Resource } from "@rest-hooks/rest";

export default class AuthResource extends Resource {
    // Turn schema-mismatch errors (that break the app) into just console warnings.
    // These happen when extra fields are returned that are not defined in the schema
    static automaticValidation = "warn" as const;

    pk(_parent?: any, _key?: string): string | undefined {
        throw new Error("Method not implemented.");
    }

    static useFetchInit = (init: RequestInit): RequestInit => {
        const { organization, token, fetchHeaders } = JSON.parse(
            sessionStorage.getItem("__deprecatedFetchInit") ?? "{}",
        );

        return {
            ...init,
            headers: {
                ...init.headers,
                ...fetchHeaders,
                Authorization: `Bearer ${token ?? ""}`,
                Organization: organization ?? "",
                "authentication-type": "okta",
            },
        };
    };
}
