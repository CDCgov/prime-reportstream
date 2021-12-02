/* eslint-disable react-hooks/rules-of-hooks */
import { useOktaAuth } from "@okta/okta-react";
import { Resource } from "@rest-hooks/rest";

import { GLOBAL_STORAGE_KEYS } from "../components/GlobalContextProvider";

export default class AuthResource extends Resource {
    pk(parent?: any, key?: string): string | undefined {
        throw new Error("Method not implemented.");
    }

    static useFetchInit = (init: RequestInit): RequestInit => {
        const { authState } = useOktaAuth();
        const organization = localStorage.getItem(
            GLOBAL_STORAGE_KEYS.GLOBAL_ORG
        );

        return {
            ...init,
            headers: {
                ...init.headers,
                Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                Organization: organization!,
            },
        };
    };
}
