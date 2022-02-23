import { Resource } from "@rest-hooks/rest";

import {
    getStoredOktaToken,
    getStoredOrg,
} from "../components/GlobalContextProvider";

export default class AuthResource extends Resource {
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
            },
        };
    };
}
