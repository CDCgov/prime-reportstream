import { useIdleTimer } from "react-idle-timer";
import { Suspense, useCallback, useEffect, useRef } from "react";
import { CacheProvider, NetworkErrorBoundary } from "rest-hooks";
import { useLocation, useNavigate } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { HelmetProvider } from "react-helmet-async";
import type { OktaAuth } from "@okta/okta-auth-js";

import ScrollRestoration from "./components/ScrollRestoration";
import { useScrollToTop } from "./hooks/UseScrollToTop";
import { OKTA_AUTH } from "./oktaConfig";
import { permissionCheck } from "./utils/PermissionsUtils";
import { ErrorPage } from "./pages/error/ErrorPage";
import { AuthorizedFetchProvider } from "./contexts/AuthorizedFetchContext";
import { FeatureFlagProvider } from "./contexts/FeatureFlagContext";
import SessionProvider, { useSessionContext } from "./contexts/SessionContext";
import { appQueryClient } from "./network/QueryClients";
import { PERMISSIONS } from "./utils/UsefulTypes";
import {
    EventName,
    useAppInsightsContext,
} from "./contexts/AppInsightsContext";
import { preferredBrowsersRegex } from "./utils/SupportedBrowsers";
import DAPScript from "./shared/DAPScript/DAPScript";
import { AppConfig } from "./config";

import "react-toastify/dist/ReactToastify.css";

export interface AppProps {
    Layout: React.ComponentType;
    config: AppConfig;
}

/**
 * App entrypoint that bootstraps all needed systems. Expects a `Layout` component
 * prop that handles rendering content.
 */
function App(props: AppProps) {
    const navigate = useNavigate();
    const restoreOriginalUri = useCallback(
        /**
         * If their destination is the home page, send them to their most relevant
         * group-type page. Otherwise, send them to their original destination.
         */
        async (oktaAuth: OktaAuth, originalUri: string) => {
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
            navigate(url);
        },
        [navigate],
    );

    return (
        <SessionProvider
            oktaAuth={OKTA_AUTH}
            restoreOriginalUri={restoreOriginalUri}
        >
            <AppBase {...props} />
        </SessionProvider>
    );
}

const AppBase = ({ Layout, config }: AppProps) => {
    const location = useLocation();
    const { appInsights, setTelemetryCustomProperty } = useAppInsightsContext();
    const { oktaAuth, authState } = useSessionContext();
    const { email } = authState.idToken?.claims ?? {};
    const { logout, activeMembership } = useSessionContext();
    const sessionStartTime = useRef<number>(new Date().getTime());
    const sessionTimeAggregate = useRef<number>(0);
    const calculateAggregateTime = () => {
        return (
            new Date().getTime() -
            sessionStartTime.current +
            sessionTimeAggregate.current
        );
    };
    const Fallback = useCallback(() => <ErrorPage type="page" />, []);

    // do best-attempt window tracking
    useEffect(() => {
        const onUnload = () => {
            appInsights?.trackEvent({
                name: EventName.SESSION_DURATION,
                properties: {
                    sessionLength: calculateAggregateTime() / 1000,
                },
            });
        };
        const onVisibilityChange = () => {
            if (document.visibilityState === "hidden") {
                sessionTimeAggregate.current = calculateAggregateTime();
            } else if (document.visibilityState === "visible") {
                sessionStartTime.current = new Date().getTime();
            }
        };
        window.addEventListener("beforeunload", onUnload);
        window.addEventListener("visibilitychange", onVisibilityChange);

        return () => {
            window.removeEventListener("beforeunload", onUnload);
            window.removeEventListener("visibilitychange", onVisibilityChange);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Add any custom properties needed to telemetry
    useEffect(() => {
        setTelemetryCustomProperty("activeMembership", activeMembership);
    }, [activeMembership, setTelemetryCustomProperty]);

    // keep auth user up-to-date
    useEffect(() => {
        if (email && !appInsights?.sdk.context.user.authenticatedId) {
            appInsights?.sdk.setAuthenticatedUserContext(
                email,
                undefined,
                true,
            );
        } else if (!email && appInsights?.sdk.context.user.authenticatedId) {
            appInsights.sdk.clearAuthenticatedUserContext();
        }
    }, [email, appInsights]);

    // Mark that user agent is outdated on telemetry for filtering
    useEffect(() => {
        if (!preferredBrowsersRegex.test(window.navigator.userAgent))
            setTelemetryCustomProperty("isUserAgentOutdated", true);
        else setTelemetryCustomProperty("isUserAgentOutdated", undefined);
    }, [setTelemetryCustomProperty]);

    const handleIdle = useCallback(async (): Promise<void> => {
        if (await oktaAuth.isAuthenticated()) {
            appInsights?.trackEvent({
                name: EventName.SESSION_DURATION,
                properties: {
                    sessionLength: sessionTimeAggregate.current / 1000,
                },
            });
            logout();
        }
    }, [appInsights, logout, oktaAuth]);

    useIdleTimer({
        timeout: 1000 * 60 * 15,
        onIdle: handleIdle,
        debounce: 500,
    });

    useScrollToTop();

    return (
        <HelmetProvider>
            <QueryClientProvider client={appQueryClient}>
                <AuthorizedFetchProvider>
                    <FeatureFlagProvider>
                        <NetworkErrorBoundary fallbackComponent={Fallback}>
                            <CacheProvider>
                                <ScrollRestoration />
                                <DAPScript
                                    env={config.APP_ENV}
                                    pathname={location.pathname}
                                />
                                <Suspense>
                                    <Layout />
                                </Suspense>
                                <ReactQueryDevtools initialIsOpen={false} />
                            </CacheProvider>
                        </NetworkErrorBoundary>
                    </FeatureFlagProvider>
                </AuthorizedFetchProvider>
            </QueryClientProvider>
        </HelmetProvider>
    );
};

export default App;
