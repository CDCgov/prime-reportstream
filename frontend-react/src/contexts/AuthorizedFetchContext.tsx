import React, { createContext, useCallback, useContext } from "react";
import { AccessToken } from "@okta/okta-auth-js";
import axios, { Method, AxiosRequestConfig } from "axios";

import { MembershipSettings } from "../hooks/UseOktaMemberships";

import { useSessionContext } from "./SessionContext";

// this should be contained in a config file
// TODO: move all config specific global variables to a config file
export const API_ROOT = `${process.env.REACT_APP_BACKEND_URL}/api`;

type AuthorizedFetcher<T> = (params: AuthorizedFetchParams) => Promise<T>;

// this wrapper is needed to allow typing of the fetch return value from the location
// where the hook is being called. If we returned an AuthorizedFetcher directly from the hook
// there wouldn't be a way to type it from the location calling the hook, as that would
// require typing the `useContext` call in a way that useContext doesn't support
type AuthorizedFetchTypeWrapper = <T>() => AuthorizedFetcher<T>;

interface IAuthorizedFetchContext {
    authorizedFetchGenerator: AuthorizedFetchTypeWrapper;
}

export interface EndpointConfig {
    path: string;
    method: Method;
}

// maybe this could take the segments param and form the url here then hook side?
interface AuthorizedFetchParams extends EndpointConfig {
    options?: Partial<AxiosRequestConfig>;
}

export const AuthorizedFetchContext = createContext<IAuthorizedFetchContext>({
    authorizedFetchGenerator: () => () =>
        Promise.reject("fetcher uninitialized"),
});

// takes in auth data and returns
//  a generic function that returns
//    a function that can be used to make an API call
export function createTypeWrapperForAuthorizedFetch(
    oktaToken: Partial<AccessToken>,
    activeMembership: MembershipSettings
) {
    const authHeaders = {
        "authentication-type": "okta",
        authorization: `Bearer ${oktaToken?.accessToken || ""}`,
        organization: `${activeMembership?.parsedName || ""}`,
    };
    return async function <T>({
        path,
        method,
        options = {},
    }: AuthorizedFetchParams): Promise<T> {
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
}

export const AuthorizedFetchProvider = ({
    children,
}: React.PropsWithChildren<{}>) => {
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
                authorizedFetchGenerator: generator,
            }}
        >
            {children}
        </AuthorizedFetchContext.Provider>
    );
};

// we can refactor this when we introduce resources,
// but if this takes a resource that contains a list of configs
// we can likely avoid taking the path and method args at request time, and just read from the resource somehow
// and all that is passed in are things that would change at request time (path params, query params, custom headers, payload, etc.)
export const useAuthorizedFetch = <T,>(): AuthorizedFetcher<T> => {
    const { authorizedFetchGenerator } = useContext(AuthorizedFetchContext);
    return authorizedFetchGenerator<T>();
};
