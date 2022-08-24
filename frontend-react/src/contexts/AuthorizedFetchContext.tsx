import React, { createContext, useCallback, useContext } from "react";
import { AccessToken } from "@okta/okta-auth-js";
import axios, { Method, AxiosRequestConfig } from "axios";

import { MembershipSettings } from "../hooks/UseOktaMemberships";
import { useSessionContext } from "./SessionContext";

// this should be contained in a config file
// TODO: move all config specific global variables to a config file
export const API_ROOT = `${process.env.REACT_APP_BACKEND_URL}/api`;

interface AuthorizedFetchProviderProps {}

export interface IAuthorizedFetchContext {
    authorizedFetch: AuthorizedFetcher;
}

type AuthorizedFetchConfig = {
    path: string;
    method: Method;
    options: Partial<AxiosRequestConfig>;
};

type AuthorizedFetcher = (config: AuthorizedFetchConfig) => Promise<unknown>;

export const AuthorizedFetchContext = createContext<IAuthorizedFetchContext>({
    authorizedFetch: () => Promise.reject("fetcher uninitialized"),
});

export const authorizedFetchFor = (
    oktaToken: Partial<AccessToken>,
    activeMembership: MembershipSettings
): AuthorizedFetcher => {
    const authHeaders = {
        "authentication-type": "okta",
        authorization: `Bearer ${oktaToken?.accessToken || ""}`,
        organization: `${activeMembership?.parsedName || ""}`,
    };
    return async ({ path, method, options }) => {
        const url = `${API_ROOT}/${path}`;
        const headerOverrides = options?.headers || {};
        const headers = { ...headerOverrides, ...authHeaders };
        // do we want to commit to returning .data here?
        return axios({
            ...options,
            url,
            method,
            headers,
        });
    };
};

export const AuthorizedFetchProvider = ({
    children,
}: React.PropsWithChildren<AuthorizedFetchProviderProps>) => {
    const { oktaToken, activeMembership } = useSessionContext();

    // passing an inline function to satisfy linter re: dependencies
    const authorizedFetch = useCallback(
        () =>
            authorizedFetchFor(
                oktaToken as Partial<AccessToken>,
                activeMembership as MembershipSettings
            ),
        [oktaToken, activeMembership]
    );
    return (
        <AuthorizedFetchContext.Provider
            value={{
                authorizedFetch: authorizedFetch(),
            }}
        >
            {children}
        </AuthorizedFetchContext.Provider>
    );
};

export const useAuthorizedFetch = () => useContext(AuthorizedFetchContext);
