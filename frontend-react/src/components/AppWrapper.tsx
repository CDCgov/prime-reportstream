import { PropsWithChildren } from "react";
import { Security } from "@okta/okta-react";
import { OktaAuth } from "@okta/okta-auth-js";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { AppInsightsContext } from "@microsoft/applicationinsights-react-js";
import { HelmetProvider } from "react-helmet-async";

import SessionProvider from "../contexts/SessionContext";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetchContext";
import { appQueryClient } from "../network/QueryClients";
import { FeatureFlagProvider } from "../contexts/FeatureFlagContext";
import { ai } from "../TelemetryService";

const AppInsightsProvider = (props: PropsWithChildren<{}>) => (
    <AppInsightsContext.Provider value={ai.reactPlugin!!}>
        {props.children}
    </AppInsightsContext.Provider>
);

interface AppWrapperProps {
    oktaAuth: OktaAuth;
    restoreOriginalUri: (_oktaAuth: any, originalUri: string) => void;
}

export const AppWrapper = ({
    children,
    oktaAuth,
    restoreOriginalUri,
}: PropsWithChildren<AppWrapperProps>) => {
    return (
        <HelmetProvider>
            <Security
                oktaAuth={oktaAuth}
                restoreOriginalUri={restoreOriginalUri}
            >
                <AppInsightsProvider>
                    <SessionProvider>
                        <QueryClientProvider client={appQueryClient}>
                            <AuthorizedFetchProvider>
                                <FeatureFlagProvider>
                                    {children}
                                </FeatureFlagProvider>
                            </AuthorizedFetchProvider>
                            <ReactQueryDevtools initialIsOpen={false} />
                        </QueryClientProvider>
                    </SessionProvider>
                </AppInsightsProvider>
            </Security>
        </HelmetProvider>
    );
};
