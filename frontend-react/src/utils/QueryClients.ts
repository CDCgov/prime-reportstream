import { QueryClient } from "@tanstack/react-query";

export const defaultClient = new QueryClient();
export const testClient = new QueryClient({
    // to allow for faster testable failures
    defaultOptions: { queries: { retry: false } },
});
