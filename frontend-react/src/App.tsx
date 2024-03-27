import { AppInsightsContext } from "@microsoft/applicationinsights-react-js";
import { OktaAuth } from "@okta/okta-auth-js";
import { Security } from "@okta/okta-react";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { Suspense, useCallback, useRef } from "react";
import { HelmetProvider } from "react-helmet-async";
import {
    createBrowserRouter,
    RouteObject,
    RouterProvider,
} from "react-router-dom";
import { CacheProvider, NetworkErrorBoundary } from "rest-hooks";

import RSErrorBoundary from "./components/RSErrorBoundary";
import { AppConfig } from "./config";
import AuthorizedFetchProvider from "./contexts/AuthorizedFetch/AuthorizedFetchProvider";
import FeatureFlagProvider from "./contexts/FeatureFlag/FeatureFlagProvider";
import SessionProvider from "./contexts/Session/SessionProvider";
import ToastProvider from "./contexts/Toast";
import { appQueryClient } from "./network/QueryClients";
import { ErrorPage } from "./pages/error/ErrorPage";
import DAPScript from "./shared/DAPScript/DAPScript";
import { permissionCheck } from "./utils/PermissionsUtils";
import {
    createTelemetryService,
    ReactPlugin,
} from "./utils/TelemetryService/TelemetryService";
import { PERMISSIONS } from "./utils/UsefulTypes";

import "react-toastify/dist/ReactToastify.css";

export interface AppProps {
    routes: RouteObject[];
    config: AppConfig;
}

/**
 * App entrypoint that bootstraps all needed systems.
 */
function App({ config, routes }: AppProps) {
    // @ts-expect-error The proper typing will make the rest of function think this is possibly null, which is incorrect
    const aiReactPluginRef = useRef<ReactPlugin>(null as ReactPlugin);
    // @ts-expect-error The proper typing will make the rest of function think this is possibly null, which is incorrect
    const oktaAuthRef = useRef<OktaAuth>(null as OktaAuth);
    const routerRef = useRef<ReturnType<typeof createBrowserRouter>>(
        // @ts-expect-error The proper typing will make the rest of function think this is possibly null, which is incorrect
        null as RemixRouter,
    );

    if (!aiReactPluginRef.current)
        aiReactPluginRef.current = createTelemetryService(
            config.APPLICATION_INSIGHTS,
        ).reactPlugin;
    if (!oktaAuthRef.current)
        oktaAuthRef.current = new OktaAuth(config.OKTA_AUTH);
    if (!routerRef.current) routerRef.current = createBrowserRouter(routes);

    const Fallback = useCallback(() => <ErrorPage type="page" />, []);

    const restoreOriginalUri = useCallback(
        /**
         * If their destination is the home page, send them to their most relevant
         * group-type page. Otherwise, send them to their original destination.
         */
        (oktaAuth: OktaAuth, originalUri: string) => {
            const authState = oktaAuth.authStateManager.getAuthState();
            let url = originalUri;
            if (originalUri === "/") {
                /* PERMISSIONS REFACTOR: Redirect URL should be determined by active membership type */
                if (
                    authState?.accessToken &&
                    permissionCheck(
                        PERMISSIONS.PRIME_ADMIN,
                        authState.accessToken,
                    )
                ) {
                    url = "/admin/settings";
                }
                if (
                    authState?.accessToken &&
                    permissionCheck(PERMISSIONS.SENDER, authState.accessToken)
                ) {
                    url = "/submissions";
                }
            }
            /**
             * Labeled as internal api but they can't be bothered to give us a proper
             * way to do this outside of a router context.
             */
            void routerRef.current.navigate(url);
        },
        [],
    );

    return (
        <RSErrorBoundary>
            <AppInsightsContext.Provider value={aiReactPluginRef.current}>
                <Security
                    restoreOriginalUri={restoreOriginalUri}
                    oktaAuth={oktaAuthRef.current}
                >
                    <QueryClientProvider client={appQueryClient}>
                        <SessionProvider config={config}>
                            <HelmetProvider>
                                <AuthorizedFetchProvider>
                                    <FeatureFlagProvider>
                                        <NetworkErrorBoundary
                                            fallbackComponent={Fallback}
                                        >
                                            <CacheProvider>
                                                <ToastProvider>
                                                    <DAPScript
                                                        pathname={
                                                            location.pathname
                                                        }
                                                    />
                                                    <Suspense>
                                                        <RouterProvider
                                                            router={
                                                                routerRef.current
                                                            }
                                                        />
                                                    </Suspense>
                                                    <ReactQueryDevtools
                                                        initialIsOpen={false}
                                                    />
                                                </ToastProvider>
                                            </CacheProvider>
                                        </NetworkErrorBoundary>
                                    </FeatureFlagProvider>
                                </AuthorizedFetchProvider>
                            </HelmetProvider>
                        </SessionProvider>
                    </QueryClientProvider>
                </Security>
            </AppInsightsContext.Provider>
        </RSErrorBoundary>
    );
}

export default App;
