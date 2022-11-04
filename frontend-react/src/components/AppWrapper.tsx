import { FC, PropsWithChildren } from "react";
import { Security } from "@okta/okta-react";
import { OktaAuth } from "@okta/okta-auth-js";
import { QueryClientProvider } from "@tanstack/react-query";
import { AppInsightsContext } from "@microsoft/applicationinsights-react-js";
import ReactMarkdown from "react-markdown";

import SessionProvider, { OktaHook } from "../contexts/SessionContext";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetchContext";
import { appQueryClient } from "../network/QueryClients";
import { FeatureFlagProvider } from "../contexts/FeatureFlagContext";
import { ai } from "../TelemetryService";

import children = ReactMarkdown.propTypes.children;

const AppInsightsProvider: FC<PropsWithChildren<{}>> = () => (
    <AppInsightsContext.Provider value={ai.reactPlugin}>
        {children}
    </AppInsightsContext.Provider>
);

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
            <AppInsightsProvider>
                <SessionProvider oktaHook={oktaHook}>
                    <QueryClientProvider client={appQueryClient}>
                        <AuthorizedFetchProvider>
                            <FeatureFlagProvider>
                                {children}
                            </FeatureFlagProvider>
                        </AuthorizedFetchProvider>
                    </QueryClientProvider>
                </SessionProvider>
            </AppInsightsProvider>
        </Security>
    );
};
