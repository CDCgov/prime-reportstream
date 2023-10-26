import { useCallback } from "react";
import { AccessToken } from "@okta/okta-auth-js";
import axios, { AxiosError } from "axios";

import { RSEndpoint, AxiosOptionsWithSegments } from "../config/endpoints";
import { RSNetworkError } from "../utils/RSNetworkError";
import { useSessionContext } from "../contexts/SessionContext";
import { useAppInsightsContext } from "../contexts/AppInsightsContext";
import { MembershipSettings } from "../utils/OrganizationUtils";

export type AuthorizedFetcher<T> = (
    EndpointConfig: RSEndpoint,
    options?: Partial<AxiosOptionsWithSegments>,
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
    activeMembership: MembershipSettings,
    fetchHeaders: Record<string, string | number | undefined>,
) {
    const authHeaders = {
        ...fetchHeaders,
        "authentication-type": "okta",
        authorization: `Bearer ${oktaToken?.accessToken || ""}`,
        organization: `${activeMembership?.parsedName || ""}`,
    };

    return async function <T>(
        EndpointConfig: RSEndpoint,
        options: Partial<AxiosOptionsWithSegments> = {},
    ): Promise<T> {
        const headerOverrides = options?.headers || {};
        const headers = { ...authHeaders, ...headerOverrides };

        const axiosConfig = EndpointConfig.toAxiosConfig({
            ...options,
            headers,
        });

        try {
            const res = await axios(axiosConfig);
            return res.data;
        } catch (e: any) {
            throw new RSNetworkError<T>(e as AxiosError<T>);
        }
    };
}

// this is extrapolated into a separate object (and referenced from this object within the hook)
// in order to allow mocking of the wrapper fn in tests
export const auxExports = {
    createTypeWrapperForAuthorizedFetch,
};

export const useCreateFetch = (): AuthorizedFetchTypeWrapper => {
    const { activeMembership, authState = {} } = useSessionContext();
    const { fetchHeaders } = useAppInsightsContext();

    const generator = useCallback(
        () =>
            auxExports.createTypeWrapperForAuthorizedFetch(
                authState?.accessToken as Partial<AccessToken>,
                activeMembership as MembershipSettings,
                fetchHeaders,
            ),
        [authState?.accessToken, activeMembership, fetchHeaders],
    );

    return generator;
};
