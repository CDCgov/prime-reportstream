/* eslint-disable react-hooks/rules-of-hooks */
import { QueryHook, Middleware } from "react-query-kit";
import axios, { AxiosRequestConfig } from "axios";
import { QueryFunction } from "@tanstack/react-query";

import { useSessionContext } from "../contexts/Session";
import { useAppInsightsContext } from "../contexts/AppInsights";
import { RSEndpoint } from "../config/endpoints";

export type AuthMiddleware<TData> = Middleware<
    QueryHook<TData, FetchVariables>
>;

/**
 * react-query middleware that prepares the fetch configuration (dynamic url, auth etc.)
 * from an expected RSEndpoint variable. Will disable the query if it cannot pass on
 * a fetchConfig (either built here or given).
 */
export const authMiddleware: Middleware<QueryHook<unknown, FetchVariables>> = (
    useQueryNext,
) => {
    return (options) => {
        if (!options.variables?.endpoint) throw new Error("Endpoint not found");
        const { fetchHeaders } = useAppInsightsContext();
        const { authState, activeMembership } = useSessionContext();
        const authHeaders = {
            ...fetchHeaders(),
            "authentication-type": "okta",
            authorization: `Bearer ${
                authState?.accessToken?.accessToken ?? ""
            }`,
            organization: `${activeMembership?.parsedName ?? ""}`,
        };
        const headers = {
            ...authHeaders,
            ...options.variables.fetchConfig?.headers,
        };
        const axiosConfig = options.variables.endpoint.toAxiosConfig({
            ...options.variables.fetchConfig,
            headers,
        });
        const fetchConfig =
            options.variables?.fetchConfig || axiosConfig
                ? {
                      ...options.variables?.fetchConfig,
                      ...axiosConfig,
                  }
                : undefined;
        const newOptions = {
            ...options,
            variables: {
                ...options.variables,
                fetchConfig,
            },
            enabled:
                (options.enabled == null || options.enabled) && !!fetchConfig,
        };
        return useQueryNext(newOptions);
    };
};

export type FetchVariables = {
    endpoint: RSEndpoint;
    fetchConfig?: Partial<AxiosRequestConfig>;
};
export type FetchConfigQueryKey = [string, FetchVariables];
export type AuthFetch<TData> = QueryFunction<TData, FetchConfigQueryKey>;

/**
 * Calls fetch with the provided fetch config from variables.
 */
export const authFetch: QueryFunction<unknown, FetchConfigQueryKey> = async ({
    queryKey,
}) => {
    const [, variables] = queryKey;
    if (!variables.fetchConfig) throw new Error("Fetch config not found");

    const res = await axios<unknown>(variables.fetchConfig);
    return res.data;
};

/**
 * Convenience function to get queryFn and middleware for auth that
 * can be typed. THE MIDDLEWARE MUST COME AFTER OTHER MIDDLEWARE THAT
 * SUPPLIES THE ENDPOINT.
 */
export function getAuthFetchProps<TData>() {
    return {
        authFetch: authFetch as AuthFetch<TData>,
        authMiddleware: authMiddleware as AuthMiddleware<TData>,
    };
}
