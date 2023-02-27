import { QueryClient } from "@tanstack/react-query";

export const appQueryClient = new QueryClient({
    defaultOptions: {
        queries: {
            suspense: true,
            useErrorBoundary: true,
            retry: false,
            staleTime: Infinity,
            cacheTime: Infinity,
            refetchOnWindowFocus: false,
        },
    },
});
export const getTestQueryClient = () =>
    new QueryClient({
        // to allow for faster testable failures
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });
