import { AppInsightsContext } from "@microsoft/applicationinsights-react-js";
import { OktaAuth } from "@okta/okta-auth-js";
import { Security } from "@okta/okta-react";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { Suspense, useCallback, useMemo } from "react";
import { HelmetProvider } from "react-helmet-async";
import { createBrowserRouter, RouteObject, RouterProvider } from "react-router-dom";
import { CacheProvider, NetworkErrorBoundary } from "rest-hooks";

import AuthStateGate from "./AuthStateGate";
import { AppConfig } from "../../config";
import FeatureFlagProvider from "../../contexts/FeatureFlag/FeatureFlagProvider";
import SessionProvider from "../../contexts/Session/SessionProvider";
import ToastProvider from "../../contexts/Toast";
import { appQueryClient } from "../../network/QueryClients";
import { ErrorPage } from "../../pages/error/ErrorPage";
import DAPScript from "../../shared/DAPScript/DAPScript";
import { permissionCheck } from "../../utils/PermissionsUtils";
import { RSConsole } from "../../utils/rsConsole/rsConsole";
import { createTelemetryService } from "../../utils/TelemetryService/TelemetryService";
import { PERMISSIONS } from "../../utils/UsefulTypes";

import RSErrorBoundary from "../RSErrorBoundary/RSErrorBoundary";

export interface AppProps {
    routes: RouteObject[];
    config: AppConfig;
}

export type AppRouter = ReturnType<typeof createBrowserRouter>;

/**
 * App entrypoint that bootstraps all needed systems. Provides AppErrorBoundary to catch errors in
 * children before session is initialized.
 */
function App({ config, routes }: AppProps) {
    const { reactPlugin: aiReactPlugin } = useMemo(
        () => createTelemetryService(config.APPLICATION_INSIGHTS),
        [config.APPLICATION_INSIGHTS],
    );
    const rsConsole = useMemo(
        () => new RSConsole({ ai: aiReactPlugin, ...config.RSCONSOLE }),
        [aiReactPlugin, config.RSCONSOLE],
    );
    const oktaAuth = useMemo(() => new OktaAuth(config.OKTA_AUTH), [config.OKTA_AUTH]);
    const router = useMemo(() => createBrowserRouter(routes), [routes]);

    const Fallback = useCallback(() => <ErrorPage type="page" />, []);
    const restoreOriginalUri = useCallback(
        /**
         * If their destination is the home page, send them to their most relevant
         * group-type page. Otherwise, send them to their original destination.
         */
        (oktaAuth: OktaAuth, originalUri: string) => {
            if (!router) throw new Error("Router uninitialized");

            const authState = oktaAuth.authStateManager.getAuthState();
            let url = originalUri;
            if (originalUri === "/") {
                /* PERMISSIONS REFACTOR: Redirect URL should be determined by active membership type */
                if (authState?.accessToken && permissionCheck(PERMISSIONS.PRIME_ADMIN, authState.accessToken)) {
                    url = "/admin/settings";
                }
                if (authState?.accessToken && permissionCheck(PERMISSIONS.SENDER, authState.accessToken)) {
                    url = "/submissions";
                }
                if (authState?.accessToken && permissionCheck(PERMISSIONS.RECEIVER, authState.accessToken)) {
                    url = "/daily-data";
                }
            }
            /**
             * Labeled as internal api, but they can't be bothered to give us a proper
             * way to do this outside of a router context.
             */
            void router.navigate(url);
        },
        [router],
    );

    return (
        <RSErrorBoundary rsConsole={rsConsole}>
            <AppInsightsContext.Provider value={aiReactPlugin}>
                <Security restoreOriginalUri={restoreOriginalUri} oktaAuth={oktaAuth}>
                    <AuthStateGate>
                        <QueryClientProvider client={appQueryClient}>
                            <SessionProvider config={config} rsConsole={rsConsole}>
                                <HelmetProvider>
                                    <FeatureFlagProvider>
                                        <NetworkErrorBoundary fallbackComponent={Fallback}>
                                            <CacheProvider>
                                                <ToastProvider>
                                                    <DAPScript pathname={location.pathname} />
                                                    <Suspense>
                                                        <RouterProvider router={router} />
                                                    </Suspense>
                                                    <ReactQueryDevtools initialIsOpen={false} />
                                                </ToastProvider>
                                            </CacheProvider>
                                        </NetworkErrorBoundary>
                                    </FeatureFlagProvider>
                                </HelmetProvider>
                            </SessionProvider>
                        </QueryClientProvider>
                    </AuthStateGate>
                </Security>
            </AppInsightsContext.Provider>
        </RSErrorBoundary>
    );
}

/**
 * Catch errors from app initialization (worst-case scenario of no telemetry).
 */
function AppWrapper(props: AppProps) {
    return (
        <RSErrorBoundary isGlobalConsole>
            <App {...props} />
        </RSErrorBoundary>
    );
}

export default AppWrapper;
