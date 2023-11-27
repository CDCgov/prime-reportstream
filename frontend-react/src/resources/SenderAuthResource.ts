import AuthResource from "./AuthResource";

export default class SenderAuthResource extends AuthResource {
    pk(_parent?: any, _key?: string): string | undefined {
        throw new Error("Method not implemented.");
    }

    static useFetchInit = (init: RequestInit): RequestInit => {
        const {
            organization,
            token,
            fetchHeaders = {},
        } = JSON.parse(sessionStorage.getItem("__deprecatedFetchInit") ?? "{}");

        return {
            ...init,
            headers: {
                ...init.headers,
                ...fetchHeaders,
                Authorization: `Bearer ${token ?? ""}`,
                Organization: organization || "",
                "authentication-type": "okta",
            },
        };
    };
}
