import { Resource } from "@rest-hooks/rest";

import {
    getStoredOktaToken,
    getStoredOrg,
} from "../contexts/SessionStorageTools";

export default class AuthResource extends Resource {
    static automaticValidation = 'warn' as const;

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
                Authorization: `Bearer ${accessToken}`,
                Organization: organization || "",
                "authentication-type": "okta",
            },
        };
    };
}
