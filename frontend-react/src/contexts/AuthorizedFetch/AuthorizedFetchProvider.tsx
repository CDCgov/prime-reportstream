import axios, { AxiosError } from "axios";
import { createContext, PropsWithChildren, useCallback } from "react";

import { AxiosOptionsWithSegments, RSEndpoint } from "../../config/endpoints";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext";
import { RSNetworkError } from "../../utils/RSNetworkError";
import useSessionContext from "../Session/useSessionContext";

export type AuthorizedFetcher<T = any> = (
    EndpointConfig: RSEndpoint,
    options?: Partial<AxiosOptionsWithSegments>,
) => Promise<T>;

export type IAuthorizedFetchContext<T = any> = AuthorizedFetcher<T>;

export const AuthorizedFetchContext = createContext<IAuthorizedFetchContext>(
    () => Promise.reject("fetcher uninitialized"),
);
const AuthorizedFetchProvider = ({
    children,
}: PropsWithChildren<{ initializedOverride?: boolean }>) => {
    const { activeMembership, authState = {} } = useSessionContext();
    const { properties } = useAppInsightsContext();
    const authorizedFetch = useCallback(
        async function <TData>(
            EndpointConfig: RSEndpoint,
            options: Partial<AxiosOptionsWithSegments> = {},
        ): Promise<TData> {
            const headerOverrides = options?.headers ?? {};

            const authHeaders = {
                "x-ms-session-id": properties.context.getSessionId(),
                "authentication-type": "okta",
                authorization: `Bearer ${
                    authState?.accessToken?.accessToken ?? ""
                }`,
                organization: `${activeMembership?.parsedName ?? ""}`,
            };
            const headers = { ...authHeaders, ...headerOverrides };

            const axiosConfig = EndpointConfig.toAxiosConfig({
                ...options,
                headers,
            });

            try {
                const res = await axios<TData>(axiosConfig);
                return res.data;
            } catch (e: any) {
                if (e instanceof AxiosError) {
                    throw new RSNetworkError(e);
                }
                throw e;
            }
        },
        [
            activeMembership?.parsedName,
            authState?.accessToken?.accessToken,
            properties,
        ],
    );

    return (
        <AuthorizedFetchContext.Provider value={authorizedFetch}>
            {children}
        </AuthorizedFetchContext.Provider>
    );
};

export default AuthorizedFetchProvider;
