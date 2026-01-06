import { QueryClient, QueryClientConfig } from "@tanstack/react-query";
import { Middleware, QueryHook } from "react-query-kit";

//import { showError } from "../components/AlertNotifications";

/**
 * QueryClient config that allows middleware via `use`. BEWARE: Default
 * middleware specified here will run BEFORE middleware in created hooks!
 */
export const config = {
    defaultOptions: {
        queries: {
            throwOnError: true,
            retry: false,
            staleTime: Infinity,
            gcTime: Infinity,
            refetchOnWindowFocus: false,
            // TODO: Implement this at a higher level. Likely an error boundary
            // that stops the propagation and triggers our preferred notification
            // method.
            /*onError: (error: any) => {
                const errorString = `Something went wrong: ${error.message}`;
                const e = new Error(errorString, { cause: error });
                showError(errorString);
                rsConsole.error(e);
            },*/
        },
    },
} as const satisfies QueryClientConfig & {
    defaultOptions: { queries: { use?: readonly Middleware<QueryHook>[] } };
};

export const appQueryClient = new QueryClient(config);

export const getTestQueryClient = () =>
    new QueryClient({
        // to allow for faster testable failures
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });
