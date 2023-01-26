import {
    QueryKey,
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

export type UseRSQueryOptions<
    T extends RSEndpoint<any>,
    TQueryFnData = Awaited<ReturnType<T["queryFn"]>>,
    TError = unknown,
    TData = TQueryFnData,
    TQueryKey extends QueryKey = [string, RSEndpointOptions<T> | undefined]
> = Omit<
    UseQueryOptions<TQueryFnData, TError, TData, TQueryKey>,
    "queryKey" | "queryFn" | "initialData"
> & {
    initialData?: () => undefined;
};

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
    queryOptions?: UseRSQueryOptions<T, TQueryFnData, TError, TData>
) {
    return useQuery(
        [endpointConfig.meta.queryKey, rsOptions],
        endpointConfig.queryFn as () => TQueryFnData,
        queryOptions
    );
}

export function useRSMutation<
    T extends RSEndpoint<any>,
    M extends Uppercase<string & keyof T["fetchers"]>,
    TData = Lowercase<M> extends keyof T["fetchers"]
        ? T["fetchers"][Lowercase<M>] extends (...args: never) => infer D
            ? D
            : never
        : never,
    TError = unknown,
    TContext = unknown
>(
    endpointConfig: T,
    method: M,
    fn: (
        variables: any,
        options?: {}
    ) => Partial<Omit<RSOptionsWithSegments<T>, "method">>,
    mutationOptions?: Omit<
        UseMutationOptions<TData, TError, Parameters<typeof fn>[0], TContext>,
        "mutationFn"
    >
) {
    if (!endpointConfig.fetchers[method.toLocaleLowerCase() as any])
        throw new Error(`This endpoint does not support ${method} requests`);
    return useMutation<TData, TError, Parameters<typeof fn>[0], TContext>(
        [endpointConfig.meta.queryKey],
        (
            variables: Parameters<typeof fn>[0],
            options?: Parameters<typeof fn>[1]
        ) =>
            endpointConfig.fetchers[method.toLocaleLowerCase() as any](
                fn(variables, options)
            ),
        mutationOptions
    );
}
