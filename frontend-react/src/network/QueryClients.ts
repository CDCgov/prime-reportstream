import { QueryClient } from "@tanstack/react-query";

export const appQueryClient = new QueryClient({
    defaultOptions: {
        queries: { suspense: true, useErrorBoundary: true },
        mutations: { useErrorBoundary: true },
    },
});
export const testQueryClient = new QueryClient({
    // to allow for faster testable failures
    defaultOptions: { queries: { retry: false } },
});
