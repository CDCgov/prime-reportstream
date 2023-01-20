import { useMutation, useQuery, UseQueryOptions } from "@tanstack/react-query";

import {
    AxiosOptionsWithSegments,
    HTTPMethods,
    RSEndpoint,
    RSOptions,
} from "../config/endpoints/RSEndpoint";

export function useRSQuery<T extends RSEndpoint<any>, TError = unknown>(
    endpointConfig: T,
    rsOptions: RSOptions,
    queryOptions: Omit<
        UseQueryOptions<ReturnType<T["fetchers"]["GET"]>, TError>,
        "queryKey" | "queryFn" | "initialData"
    > & { initialData?: () => undefined }
) {
    if (!endpointConfig.fetchers[HTTPMethods.GET])
        throw new Error("This endpoint does not support GET requests");
    return useQuery<ReturnType<T["fetchers"]["GET"]>, TError>(
        [endpointConfig.queryKey, rsOptions],
        endpointConfig.queryFn,
        queryOptions
    );
}

export function useRSMutation<
    T extends RSEndpoint<any>,
    M extends string & keyof T["fetchers"],
    TError = unknown,
    TVariables = void,
    TContext = unknown
>(
    endpointConfig: T,
    method: M,
    fn: (...args: any) => Partial<Omit<AxiosOptionsWithSegments, "method">>
) {
    if (!endpointConfig.fetchers[method])
        throw new Error(`This endpoint does not support ${method} requests`);
    return useMutation<
        ReturnType<T["fetchers"][M]>,
        TError,
        TVariables,
        TContext
    >((args: any) => endpointConfig.fetchers[method](fn(args)));
}
