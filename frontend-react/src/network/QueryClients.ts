import { QueryClient } from "@tanstack/react-query";

import { showError } from "../components/AlertNotifications";

export const appQueryClient = new QueryClient({
    defaultOptions: {
        queries: {
            suspense: true,
            useErrorBoundary: true,
            retry: false,
            staleTime: Infinity,
            cacheTime: Infinity,
            refetchOnWindowFocus: false,
            onError: (error: any) => {
                const errorString = `Something went wrong: ${error.message}`;
                showError(errorString);
                console.error(errorString);
            },
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
