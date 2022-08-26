import { useCallback } from "react";
import { AccessToken } from "@okta/okta-auth-js";
import axios, { Method, AxiosRequestConfig } from "axios";

import { MembershipSettings } from "./UseOktaMemberships";

// this should be contained in a config file
// TODO: move all config specific global variables to a config file
export const API_ROOT = `${process.env.REACT_APP_BACKEND_URL}/api`;

// odd that there isn't already a useable implementation of this somewhere
// or is there? where should this live?
export enum HTTPMethods {
    GET = "GET",
    POST = "POST",
    PUT = "PUT",
    DELETE = "DELETE",
    PATCH = "PATCH",
}

export type AuthorizedFetcher<T> = (
    params: AuthorizedFetchParams
) => Promise<T>;

// this wrapper is needed to allow typing of the fetch return value from the location
// where the hook is being called. If we returned an AuthorizedFetcher directly from the hook
// there wouldn't be a way to type it from the location calling the hook, as that would
// require typing the `useContext` call in a way that useContext doesn't support
export type AuthorizedFetchTypeWrapper = <T>() => AuthorizedFetcher<T>;

export interface EndpointConfig {
    path: string;
    method: Method;
}

// maybe this could take the segments param and form the url here then hook side?
interface AuthorizedFetchParams extends EndpointConfig {
    options?: Partial<AxiosRequestConfig>;
}

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

export const useCreateFetch = (
    oktaToken: Partial<AccessToken>,
    activeMembership: MembershipSettings
): AuthorizedFetchTypeWrapper => {
    const generator = useCallback(
        () =>
            createTypeWrapperForAuthorizedFetch(
                oktaToken as Partial<AccessToken>,
                activeMembership as MembershipSettings
            ),
        [oktaToken, activeMembership]
    );

    return generator;
};
