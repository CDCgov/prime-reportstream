import { toRelativeUrl } from "@okta/okta-auth-js";
import { useIdleTimer } from "react-idle-timer";
import { Suspense, useCallback, useEffect, useRef } from "react";
import { CacheProvider, NetworkErrorBoundary } from "rest-hooks";
import { useNavigate } from "react-router-dom";
import { Security, useOktaAuth } from "@okta/okta-react";
import {
    AppInsightsContext,
    ReactPlugin,
    useAppInsightsContext,
} from "@microsoft/applicationinsights-react-js";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { HelmetProvider } from "react-helmet-async";

import { OKTA_AUTH } from "./oktaConfig";
import { permissionCheck } from "./utils/PermissionsUtils";
import Spinner from "./components/Spinner";
import "react-toastify/dist/ReactToastify.css";
import { ErrorUnsupportedBrowser } from "./pages/error/legacy-content/ErrorUnsupportedBrowser";
import { ErrorPage } from "./pages/error/ErrorPage";
import { EventName, trackAppInsightEvent } from "./utils/Analytics";
import { IS_IE } from "./utils/GetIsIE";
import { ai, withInsights } from "./TelemetryService";
import { AuthorizedFetchProvider } from "./contexts/AuthorizedFetchContext";
import { FeatureFlagProvider } from "./contexts/FeatureFlagContext";
import SessionProvider, { useSessionContext } from "./contexts/SessionContext";
import { appQueryClient } from "./network/QueryClients";
import { PERMISSIONS } from "./utils/UsefulTypes";

// Initialize the App Insights connection and React app plugin from Microsoft
// The plugin is provided in the AppInsightsProvider in AppWrapper.tsx
const { reactPlugin, appInsights } = ai.initialize();
withInsights(console);

/**
 * Proxy of AppInsight ReactPlugin that provides access to root sdk object
 * via `sdk` property.
 */
const prox = new Proxy(reactPlugin ?? {}, {
    get(target: ReactPlugin, p, receiver) {
        if (p === "sdk") {
            return appInsights;
        }
        return Reflect.get(target, p, receiver);
    },
}) as ReactPlugin;

export interface AppProps {
    Layout: React.ComponentType;
}

/**
 * App entrypoint that bootstraps all needed systems. Expects a `Layout` component
 * prop that handles rendering content.
 */
function App(props: AppProps) {
    const navigate = useNavigate();
    const restoreOriginalUri = useCallback(
        async (_oktaAuth: any, originalUri: string) => {
            // check if the user would have any data to receive via their organizations from the okta claim
            // direct them to the /upload page if they do not have an organization that receives data
            const authState = OKTA_AUTH.authStateManager.getAuthState();
            /* PERMISSIONS REFACTOR: Redirect URL should be determined by active membership type */
            if (
                authState?.accessToken &&
                permissionCheck(PERMISSIONS.PRIME_ADMIN, authState.accessToken)
            ) {
                navigate(
                    toRelativeUrl(
                        `${window.location.origin}/admin/settings`,
                        window.location.origin,
                    ),
                );
                return;
            }
            if (
                authState?.accessToken &&
                permissionCheck(PERMISSIONS.SENDER, authState.accessToken)
            ) {
                navigate(
                    toRelativeUrl(
                        `${window.location.origin}/upload`,
                        window.location.origin,
                    ),
                );
                return;
            }
            navigate(toRelativeUrl(originalUri, window.location.origin));
        },
        [navigate],
    );
    return (
        <AppInsightsContext.Provider value={prox}>
            <Security
                oktaAuth={OKTA_AUTH}
                restoreOriginalUri={restoreOriginalUri}
            >
                <AuthenticationBarrier>
                    <SessionProvider>
                        <AppBase {...props} />
                    </SessionProvider>
                </AuthenticationBarrier>
            </Security>
        </AppInsightsContext.Provider>
    );
}

/**
 * Block rendering children until authentication data is loaded.
 */
function AuthenticationBarrier({ children }: React.PropsWithChildren) {
    const { authState } = useOktaAuth();

    // If we have a null object, okta is still loading
    if (authState?.isAuthenticated == null) return null;

    return <>{children}</>;
}

const AppBase = ({ Layout }: AppProps) => {
    const appInsights = useAppInsightsContext();
    const { oktaAuth, authState } = useOktaAuth();
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

    useEffect(() => {
        const onUnload = () => {
            appInsights.trackEvent({
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

        // Add user email as user_AuthenticatedId to all tracking events
        const { email } = authState!!.idToken?.claims ?? {};
        if (email) {
            if (!appInsights.sdk.context.user.authenticatedId) {
                appInsights.sdk.setAuthenticatedUserContext(
                    email,
                    undefined,
                    true,
                );
            }
        } else if (appInsights.sdk.context.user.authenticatedId) {
            appInsights.sdk.clearAuthenticatedUserContext();
        }

        return () => {
            window.removeEventListener("beforeunload", onUnload);
            window.removeEventListener("visibilitychange", onVisibilityChange);
        };
    }, [appInsights, authState, oktaAuth.authStateManager]);

    useEffect(() => {
        // Add active membership information to all tracking events
        appInsights.sdk.addTelemetryInitializer((envelope) => {
            if (activeMembership) {
                (envelope.data as any).activeMembership = activeMembership;
            }
        });
    }, [activeMembership, appInsights.sdk]);

    const handleIdle = useCallback(async (): Promise<void> => {
        if (await oktaAuth.isAuthenticated()) {
            trackAppInsightEvent(EventName.SESSION_DURATION, {
                sessionLength: sessionTimeAggregate.current / 1000,
            });
            logout();
        }
    }, [logout, oktaAuth]);

    useIdleTimer({
        timeout: 1000 * 60 * 15,
        onIdle: handleIdle,
        debounce: 500,
    });

    if (IS_IE) return <ErrorUnsupportedBrowser />;
    return (
        <HelmetProvider>
            <QueryClientProvider client={appQueryClient}>
                <AuthorizedFetchProvider>
                    <FeatureFlagProvider>
                        <CacheProvider>
                            <Suspense fallback={<Spinner size={"fullpage"} />}>
                                <NetworkErrorBoundary
                                    fallbackComponent={Fallback}
                                >
                                    <Layout />
                                    <ReactQueryDevtools initialIsOpen={false} />
                                </NetworkErrorBoundary>
                            </Suspense>
                        </CacheProvider>
                    </FeatureFlagProvider>
                </AuthorizedFetchProvider>
            </QueryClientProvider>
        </HelmetProvider>
    );
};

export default App;
