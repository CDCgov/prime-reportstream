import { toRelativeUrl } from "@okta/okta-auth-js";
import { useIdleTimer } from "react-idle-timer";
import { Suspense, useCallback, useEffect, useRef } from "react";
import { CacheProvider, NetworkErrorBoundary } from "rest-hooks";
import { useNavigate } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { HelmetProvider } from "react-helmet-async";

import { OKTA_AUTH } from "./oktaConfig";
import { permissionCheck } from "./utils/PermissionsUtils";
import Spinner from "./components/Spinner";
import "react-toastify/dist/ReactToastify.css";
import { ErrorUnsupportedBrowser } from "./pages/error/legacy-content/ErrorUnsupportedBrowser";
import { ErrorPage } from "./pages/error/ErrorPage";
import { IS_IE } from "./utils/GetIsIE";
import { aiConfig, createTelemetryService } from "./TelemetryService";
import { AuthorizedFetchProvider } from "./contexts/AuthorizedFetchContext";
import { FeatureFlagProvider } from "./contexts/FeatureFlagContext";
import SessionProvider, { useSessionContext } from "./contexts/SessionContext";
import { appQueryClient } from "./network/QueryClients";
import { PERMISSIONS } from "./utils/UsefulTypes";
import AppInsightsContextProvider, {
    EventName,
    useAppInsightsContext,
} from "./contexts/AppInsightsContext";

const appInsights = createTelemetryService(aiConfig);

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
        <AppInsightsContextProvider value={appInsights}>
            <SessionProvider
                oktaAuth={OKTA_AUTH}
                restoreOriginalUri={restoreOriginalUri}
            >
                <AppBase {...props} />
            </SessionProvider>
        </AppInsightsContextProvider>
    );
}

const AppBase = ({ Layout }: AppProps) => {
    const { appInsights, setTelemetryCustomProperty } = useAppInsightsContext();
    const { oktaAuth, authState } = useSessionContext();
    const { email } = authState!!.idToken?.claims ?? {};
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
