import {
    useMutation,
    UseMutationOptions,
    useQuery,
    UseQueryOptions,
} from "@tanstack/react-query";

import {
    RSEndpoint,
    RSEndpointOptions,
    RSOptionsWithSegments,
} from "../../config/api/RSEndpoint";

export function useRSQuery<
    T extends RSEndpoint<any>,
    TQueryFnData extends Awaited<ReturnType<T["queryFn"]>> = Awaited<
        ReturnType<T["queryFn"]>
    >,
    TError = unknown,
    TData = TQueryFnData
>(
    endpointConfig: T,
    rsOptions?: RSEndpointOptions<T>,
    queryOptions?: Omit<
        UseQueryOptions<TQueryFnData, TError, TData, [string, object]>,
        "queryKey" | "queryFn" | "initialData"
    > & {
        initialData?: () => undefined;
    }
) {
    return useQuery(
        [endpointConfig.meta.queryKey, rsOptions] as [string, object],
        endpointConfig.queryFn as () => TQueryFnData,
        queryOptions
    );
}

export function useRSMutation<
    T extends RSEndpoint<any>,
    M extends string & Uppercase<string & keyof T["fetchers"]>,
    TData = ReturnType<T["fetchers"][M]>,
    TError = unknown,
    TVariables = void,
    TContext = unknown
>(
    endpointConfig: T,
    method: M,
    fn: (
        ...args: TVariables[]
    ) => Partial<Omit<RSOptionsWithSegments, "method">>,
    mutationOptions?: Omit<
        UseMutationOptions<TData, TError, TVariables, TContext>,
        "mutationFn"
    >
) {
    if (!endpointConfig.fetchers[method.toLocaleLowerCase()])
        throw new Error(`This endpoint does not support ${method} requests`);
    return useMutation<TData, TError, TVariables, TContext>(
        [endpointConfig.meta.queryKey],
        (args: any) =>
            endpointConfig.fetchers[method.toLocaleLowerCase()](fn(args)),
        mutationOptions
    );
}
