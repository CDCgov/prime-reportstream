import { PropsWithChildren } from "react";
import { Security } from "@okta/okta-react";
import { OktaAuth } from "@okta/okta-auth-js";
import { QueryClientProvider } from "@tanstack/react-query";

import SessionProvider, { OktaHook } from "../contexts/SessionContext";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetchContext";
import { appQueryClient } from "../network/QueryClients";

interface AppWrapperProps {
    oktaAuth: OktaAuth;
    restoreOriginalUri: (_oktaAuth: any, originalUri: string) => void;
    oktaHook: OktaHook;
}

export const AppWrapper = ({
    children,
    oktaAuth,
    restoreOriginalUri,
    oktaHook,
}: PropsWithChildren<AppWrapperProps>) => {
    return (
        <Security oktaAuth={oktaAuth} restoreOriginalUri={restoreOriginalUri}>
            <SessionProvider oktaHook={oktaHook}>
                <QueryClientProvider client={appQueryClient}>
                    <AuthorizedFetchProvider>
                        {children}
                    </AuthorizedFetchProvider>
                </QueryClientProvider>
            </SessionProvider>
        </Security>
    );
};
