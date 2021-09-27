import { useOktaAuth } from "@okta/okta-react";
import { Resource } from "@rest-hooks/rest";
import { groupToOrg } from "../webreceiver-utils";

export default class AuthResource extends Resource {
    // @ts-ignore
    // eslint-disable-next-line class-methods-use-this
    pk(parent?: any, key?: string): string | undefined {
        throw new Error("Method not implemented.");
    }

    static useFetchInit = (init: RequestInit): RequestInit => {
        /* eslint-disable react-hooks/rules-of-hooks */ // TODO: Make sure this is ok!
        const { authState } = useOktaAuth();

        // finds the first organization that does not have the word "sender" in it
        const organization = groupToOrg(
            authState!.accessToken?.claims.organization.find((o:string) => !o.toLowerCase().includes('sender'))
        );

        return {
            ...init,
            credentials: 'include',
            headers: {
                ...init.headers,
                Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                Organization: organization,
                'Access-Control-Allow-Origin': '*',
                'authentication-type': 'okta',
            },
        };
    };

    static getBaseUrl = () => {
        if (window.location.origin.includes("localhost")) {
            return "http://localhost:7071";
        }
        if (window.location.origin.includes("staging")) {
            return "https://staging.prime.cdc.gov";
        }
        return "https://prime.cdc.gov";
    };
}
