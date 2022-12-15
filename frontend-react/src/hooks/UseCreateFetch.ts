import { useCallback } from "react";
import { AccessToken } from "@okta/okta-auth-js";
import axios from "axios";

import { RSEndpoint, AxiosOptionsWithSegments } from "../config/endpoints";
import { RSNetworkError } from "../utils/RSNetworkError";
import { getAppInsightsHeaders } from "../TelemetryService";

import { MembershipSettings } from "./UseOktaMemberships";

export type AuthorizedFetcher<T> = (
    EndpointConfig: RSEndpoint,
    options?: Partial<AxiosOptionsWithSegments>
) => Promise<T>;

// this wrapper is needed to allow typing of the fetch return value from the location
// where the hook is being called. If we returned an AuthorizedFetcher directly from the hook
// there wouldn't be a way to type it from the location calling the hook, as that would
// require typing the `useContext` call in a way that useContext doesn't support
export type AuthorizedFetchTypeWrapper = <T>() => AuthorizedFetcher<T>;

// takes in auth data and returns
//  a generic function that returns
//    a function that can be used to make an API call
function createTypeWrapperForAuthorizedFetch(
    oktaToken: Partial<AccessToken>,
    activeMembership: MembershipSettings
) {
    const authHeaders = {
        ...getAppInsightsHeaders(),
        "authentication-type": "okta",
        authorization: `Bearer ${oktaToken?.accessToken || ""}`,
        organization: `${activeMembership?.parsedName || ""}`,
    };

    return async function <T>(
        EndpointConfig: RSEndpoint,
        options: Partial<AxiosOptionsWithSegments> = {}
    ): Promise<T> {
        const headerOverrides = options?.headers || {};
        const headers = { ...authHeaders, ...headerOverrides };
        // this system assumes that we want to be making authenticated
        // requests whenever possible
        if (!headers.authorization || headers.authorization.length < 8) {
            console.warn(
                `Unauthenticated request to '${EndpointConfig.url}'\n Options:`,
                options,
                `\n Endpoint: `,
                EndpointConfig
            );
        }
        const axiosConfig = EndpointConfig.toAxiosConfig({
            ...options,
            headers,
        });
        return axios(axiosConfig)
            .then(({ data }) => data)
            .catch((e: any) => {
                throw new RSNetworkError(e.message, e.response);
            });
    };
}

// this is extrapolated into a separate object (and referenced from this object within the hook)
// in order to allow mocking of the wrapper fn in tests
export const auxExports = {
    createTypeWrapperForAuthorizedFetch,
};

export const useCreateFetch = (
    oktaToken: Partial<AccessToken>,
    activeMembership: MembershipSettings
): AuthorizedFetchTypeWrapper => {
    const generator = useCallback(
        () =>
            auxExports.createTypeWrapperForAuthorizedFetch(
                oktaToken as Partial<AccessToken>,
                activeMembership as MembershipSettings
            ),
        [oktaToken, activeMembership]
    );

    return generator;
};
