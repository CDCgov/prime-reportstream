import { QueryClient } from "@tanstack/react-query";

export const appClient = new QueryClient({
    defaultOptions: { queries: { useErrorBoundary: true, suspense: true } },
});
export const testClient = new QueryClient({
    // to allow for faster testable failures
    defaultOptions: { queries: { retry: false } },
});
