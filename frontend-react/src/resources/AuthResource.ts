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

    static getBaseUrl = () => {
        // temporary sanity checking code, while we're working things out
        {
            if (window.location.origin.includes("localhost")) {
                if (
                    process.env.REACT_APP_BASE_URL !== "http://localhost:3000"
                ) {
                    console.error(`process.env.REACT_APP_BASE_URL is NOT correct for "localhost" 
                    '${process.env.REACT_APP_BASE_URL}'`);
                }
            } else if (window.location.origin.includes("staging")) {
                if (
                    process.env.REACT_APP_BASE_URL !==
                    "https://staging.prime.cdc.gov"
                ) {
                    console.error(`process.env.REACT_APP_BASE_URL is NOT correct for "staging" 
                    '${process.env.REACT_APP_BASE_URL}'`);
                }
            } else if (window.location.origin.includes("test")) {
                if (
                    process.env.REACT_APP_BASE_URL !==
                    "https://test.reportstream.cdc.gov"
                ) {
                    console.error(`process.env.REACT_APP_BASE_URL is NOT correct for "test" 
                    '${process.env.REACT_APP_BASE_URL}'`);
                }
            } else if (
                process.env.REACT_APP_BASE_URL !== "https://prime.cdc.gov"
            ) {
                console.error(`process.env.REACT_APP_BASE_URL is NOT correct for "production" 
                    '${process.env.REACT_APP_BASE_URL}'`);
            }
        }
        return process.env.REACT_APP_BASE_URL;
    };
}
