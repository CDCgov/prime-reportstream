import { PropsWithChildren } from "react";
import { Security } from "@okta/okta-react";
import { OktaAuth } from "@okta/okta-auth-js";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import SessionProvider, { OktaHook } from "../contexts/SessionContext";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetchContext";

interface AppWrapperProps {
    oktaAuth: OktaAuth;
    onAuthRequired: () => void;
    restoreOriginalUri: (_oktaAuth: any, originalUri: string) => void;
    oktaHook: OktaHook;
}

const queryClient = new QueryClient();

export const AppWrapper = ({
    children,
    oktaAuth,
    onAuthRequired,
    restoreOriginalUri,
    oktaHook,
}: PropsWithChildren<AppWrapperProps>) => {
    return (
        <Security
            oktaAuth={oktaAuth}
            onAuthRequired={onAuthRequired}
            restoreOriginalUri={restoreOriginalUri}
        >
            <SessionProvider oktaHook={oktaHook}>
                <QueryClientProvider client={queryClient}>
                    <AuthorizedFetchProvider>
                        {children}
                    </AuthorizedFetchProvider>
                </QueryClientProvider>
            </SessionProvider>
        </Security>
    );
};
