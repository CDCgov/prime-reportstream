import React, { createContext, useContext } from "react";
import { AccessToken } from "@okta/okta-auth-js";
import {
    MutationFunction,
    MutationKey,
    QueryFunction,
    QueryKey,
    useMutation,
    UseMutationOptions,
    UseMutationResult,
    useQuery,
    UseQueryOptions,
    UseQueryResult,
} from "@tanstack/react-query";

import {
    useCreateFetch,
    AuthorizedFetchTypeWrapper,
    AuthorizedFetcher,
} from "../hooks/UseCreateFetch";
import { MembershipSettings } from "../hooks/UseOktaMemberships";

import { useSessionContext } from "./SessionContext";

// this is synthesized copy pasta from the react query codebase since they don't export function types
// see https://github.com/TanStack/query/blob/f6eeab079d88df811fd827767db1457083e85b01/packages/react-query/src/useQuery.ts
type RSUseQuery<
    TQueryFnData = unknown,
    TError = unknown,
    TData = TQueryFnData,
    TQueryKey extends QueryKey = QueryKey,
> = {
    (
        queryKey: QueryKey,
        queryFn: QueryFunction<TQueryFnData, TQueryKey>,
        options?: Omit<
            UseQueryOptions<TQueryFnData, TError, TData, TQueryKey>,
            "queryKey" | "queryFn" | "initialData"
        > & { initialData?: () => undefined },
    ): UseQueryResult<TData, TError>;
};

type RSUseMutation<
    TData = unknown,
    TError = unknown,
    TVariables = unknown,
    TContext = unknown,
> = {
    (
        mutationKey: MutationKey,
        mutationFn: MutationFunction<TData, TVariables>,
        options?: Omit<
            UseMutationOptions<TData, TError, TVariables, TContext>,
            "mutationKey" | "mutationFn"
        >,
    ): UseMutationResult<TData, TError, TVariables, TContext>;
};

interface IAuthorizedFetchContext {
    authorizedFetchGenerator: AuthorizedFetchTypeWrapper;
    initialized: boolean;
}
export const AuthorizedFetchContext = createContext<IAuthorizedFetchContext>({
    authorizedFetchGenerator: () => () =>
        Promise.reject("fetcher uninitialized"),
    initialized: false,
});

export function wrapUseQuery<
    TQueryFnData,
    TError,
    TData,
    TQueryKey extends QueryKey,
>(initialized: boolean) {
    return function (
        queryKey: QueryKey,
        queryFn: QueryFunction<TQueryFnData, TQueryKey>,
        options?: Omit<
            UseQueryOptions<TQueryFnData, TError, TData, TQueryKey>,
            "queryKey" | "queryFn" | "initialData"
        > & { initialData?: () => undefined },
    ) {
        return useQuery<TQueryFnData, TError, TData, TQueryKey>(
            queryKey as TQueryKey,
            queryFn,
            {
                enabled: options?.enabled
                    ? options.enabled && initialized
                    : initialized,
                ...options,
            },
        );
    };
}

export function wrapUseMutation<
    TData,
    TError,
    TVariables,
    TContext,
    TMutationKey extends MutationKey,
>() {
    return function (
        mutationKey: MutationKey,
        mutationFn: MutationFunction<TData, TVariables>,
        options?: Omit<
            UseMutationOptions<TData, TError, TVariables, TContext>,
            "mutationKey" | "mutationFn"
        >,
    ) {
        return useMutation<TData, TError, TVariables, TContext>(
            mutationKey as TMutationKey,
            mutationFn,
            {
                ...options,
            },
        );
    };
}

export const AuthorizedFetchProvider = ({
    children,
    initializedOverride = false,
}: React.PropsWithChildren<{ initializedOverride?: boolean }>) => {
    const { oktaToken, activeMembership, initialized } = useSessionContext();
    const generator = useCreateFetch(
        oktaToken as AccessToken,
        activeMembership as MembershipSettings,
    );

    return (
        <AuthorizedFetchContext.Provider
            value={{
                authorizedFetchGenerator: generator,
                initialized: initializedOverride || initialized,
            }}
        >
            {children}
        </AuthorizedFetchContext.Provider>
    );
};

// an extra level of indirection here to allow for generic typing of the returned fetch function
export function useAuthorizedFetch<
    TQueryFnData = unknown,
    TError = unknown,
    TData = TQueryFnData,
    TQueryKey extends QueryKey = QueryKey,
>(): {
    authorizedFetch: AuthorizedFetcher<TQueryFnData>;
    rsUseQuery: RSUseQuery<TQueryFnData, TError, TData, TQueryKey>;
} {
    const { authorizedFetchGenerator, initialized } = useContext(
        AuthorizedFetchContext,
    );
    return {
        authorizedFetch: authorizedFetchGenerator<TQueryFnData>(),
        rsUseQuery: wrapUseQuery<TQueryFnData, TError, TData, TQueryKey>(
            initialized,
        ),
    };
}

export function useAuthorizedMutationFetch<
    TData = unknown,
    TError = unknown,
    TVariables = unknown,
    TContext = unknown,
    TMutationKey extends MutationKey = MutationKey,
>(): {
    authorizedFetch: AuthorizedFetcher<TData>;
    rsUseMutation: RSUseMutation<TData, TError, TVariables, TContext>;
} {
    const { authorizedFetchGenerator } = useContext(AuthorizedFetchContext);
    return {
        authorizedFetch: authorizedFetchGenerator<TData>(),
        rsUseMutation: wrapUseMutation<
            TData,
            TError,
            TVariables,
            TContext,
            TMutationKey
        >(),
    };
}
