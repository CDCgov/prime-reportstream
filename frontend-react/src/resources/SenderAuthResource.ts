import {
    getStoredOktaToken,
    getStoredOrg,
} from "../contexts/SessionStorageTools";

import AuthResource from "./AuthResource";

export default class SenderAuthResource extends AuthResource {
    pk(parent?: any, key?: string): string | undefined {
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
                Organization: organization,
                "authentication-type": "okta",
            },
        };
    };
}
