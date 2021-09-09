/* eslint-disable react-hooks/rules-of-hooks */
import { useOktaAuth } from "@okta/okta-react";
import { senderClient } from "../webreceiver-utils";
import AuthResource from "./AuthResource";

export default class SenderAuthResource extends AuthResource {
    pk(parent?: any, key?: string): string | undefined {
        throw new Error("Method not implemented.");
    }

    static useFetchInit = (init: RequestInit): RequestInit => {

        const { authState } = useOktaAuth();
        const senderOrganization = senderClient(authState);

        return {
            ...init,
            headers: {
                ...init.headers,
                Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                Organization: senderOrganization,
                'authentication-type': 'okta'
            },
        };
    };
}
