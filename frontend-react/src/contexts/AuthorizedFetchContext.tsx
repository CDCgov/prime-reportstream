import React, { createContext, useCallback, useContext } from "react";
import { AccessToken } from "@okta/okta-auth-js";
import axios, { Method, AxiosRequestConfig } from "axios";

import { MembershipSettings } from "../hooks/UseOktaMemberships";
import { useSessionContext } from "./SessionContext";

// this should be contained in a config file
// TODO: move all config specific global variables to a config file
export const API_ROOT = `${process.env.REACT_APP_BACKEND_URL}/api`;

interface AuthorizedFetchProviderProps {}

// we'll do a better job of typing when we introduce classes in the next phase??
type AuthorizedFetcher<T> = (config: AuthorizedFetchConfig) => Promise<T>;

// this wrapper is needed to allow typing of the fetch return value from the location
// where the hook is being called. If we returned an AuthorizedFetcher directly from the hook
// there wouldn't be a way to type it from the location calling the hook, as that would
// require typing the `useContext` call in a way that useContext doesn't support
type AuthorizedFetchTypeWrapper<T> = <T>() => AuthorizedFetcher<T>;

interface IAuthorizedFetchContext {
    authorizedFetchGenerator: AuthorizedFetchTypeWrapper<any>;
}

type AuthorizedFetchConfig = {
    path: string;
    method: Method;
    options?: Partial<AxiosRequestConfig>;
};

export const AuthorizedFetchContext = createContext<IAuthorizedFetchContext>({
    authorizedFetchGenerator: () => () =>
        Promise.reject("fetcher uninitialized"),
});

export function createTypeWrapperForAuthorizedFetch<T>(
    oktaToken: Partial<AccessToken>,
    activeMembership: MembershipSettings
): AuthorizedFetchTypeWrapper<T> {
    const authHeaders = {
        "authentication-type": "okta",
        authorization: `Bearer ${oktaToken?.accessToken || ""}`,
        organization: `${activeMembership?.parsedName || ""}`,
    };
    return () => {
        return async ({ path, method, options = {} }) => {
            const url = `${API_ROOT}${path}`;
            const headerOverrides = options?.headers || {};
            const headers = { ...headerOverrides, ...authHeaders };
            return axios({
                ...options,
                url,
                method,
                headers,
            }).then(({ data }) => data);
        };
    };
}

export const AuthorizedFetchProvider = ({
    children,
}: React.PropsWithChildren<AuthorizedFetchProviderProps>) => {
    const { oktaToken, activeMembership } = useSessionContext();

    // passing an inline function to satisfy linter re: dependencies
    const generator = useCallback(
        () =>
            createTypeWrapperForAuthorizedFetch(
                oktaToken as Partial<AccessToken>,
                activeMembership as MembershipSettings
            ),
        [oktaToken, activeMembership]
    );

    return (
        <AuthorizedFetchContext.Provider
            value={{
                authorizedFetchGenerator: generator(),
            }}
        >
            {children}
        </AuthorizedFetchContext.Provider>
    );
};

export const useAuthorizedFetch = <T,>() => {
    const { authorizedFetchGenerator } = useContext(AuthorizedFetchContext);
    return authorizedFetchGenerator<T>();
};
