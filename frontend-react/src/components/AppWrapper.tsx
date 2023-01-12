import { PropsWithChildren, useEffect } from "react";
import { Security } from "@okta/okta-react";
import { OktaAuth } from "@okta/okta-auth-js";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { AppInsightsContext } from "@microsoft/applicationinsights-react-js";

import SessionProvider, { OktaHook } from "../contexts/SessionContext";
import { AuthorizedFetchProvider } from "../contexts/AuthorizedFetchContext";
import { appQueryClient } from "../network/QueryClients";
import { FeatureFlagProvider } from "../contexts/FeatureFlagContext";
import { ai } from "../TelemetryService";
import { trackAppInsightEvent } from "../utils/Analytics";

const AppInsightsProvider = (props: PropsWithChildren<{}>) => (
    <AppInsightsContext.Provider value={ai.reactPlugin!!}>
        {props.children}
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
    const onUnload = () => {
        return "unloading";
    };

    const onVisibilityChange = () => {
        console.log(document.visibilityState);
        trackAppInsightEvent("session", {
            sessionLength: 100,
        });
    };

    useEffect(() => {
        window.addEventListener("beforeunload", onUnload);
        window.addEventListener("visibilitychange", onVisibilityChange);
    }, []);
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
                        <ReactQueryDevtools initialIsOpen={false} />
                    </QueryClientProvider>
                </SessionProvider>
            </AppInsightsProvider>
        </Security>
    );
};
