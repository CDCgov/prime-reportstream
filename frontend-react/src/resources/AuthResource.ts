/* eslint-disable react-hooks/rules-of-hooks */
import { useOktaAuth } from "@okta/okta-react";
import { Resource } from "@rest-hooks/rest";
import { groupToOrg } from "../webreceiver-utils";

export default class AuthResource extends Resource {
    pk(parent?: any, key?: string): string | undefined {
        throw new Error("Method not implemented.");
    }

    static useFetchInit = (init: RequestInit): RequestInit => {

        const { authState } = useOktaAuth();

        // finds the first organization that does not have the word "sender" in it
        const organization = groupToOrg(
            authState!.accessToken?.claims.organization.find(o => !o.toLowerCase().includes('sender'))
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

    static getBaseUrl = () => {
        if (window.location.origin.includes("localhost"))
            return "http://localhost:7071";
        else if (window.location.origin.includes("staging"))
            return "https://staging.prime.cdc.gov";
        else return "https://prime.cdc.gov";
    };
}
