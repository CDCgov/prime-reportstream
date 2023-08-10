import { toRelativeUrl } from "@okta/okta-auth-js";
import { useIdleTimer } from "react-idle-timer";
import { Suspense, useEffect, useRef } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import { useNavigate } from "react-router-dom";

import { OKTA_AUTH } from "./oktaConfig";
import { permissionCheck, PERMISSIONS } from "./utils/PermissionsUtils";
import Spinner from "./components/Spinner";
import "react-toastify/dist/ReactToastify.css";
import { AppWrapper } from "./components/AppWrapper";
import { ErrorUnsupportedBrowser } from "./pages/error/legacy-content/ErrorUnsupportedBrowser";
import { ErrorPage } from "./pages/error/ErrorPage";
import { useScrollToTop } from "./hooks/UseScrollToTop";
import { EventName, trackAppInsightEvent } from "./utils/Analytics";
import { logout } from "./utils/UserUtils";
import { IS_IE } from "./utils/GetIsIE";
import ScrollRestoration from "./components/ScrollRestoration";

export interface AppProps extends React.PropsWithChildren {}

const App = ({ children }: AppProps) => {
    const sessionStartTime = useRef<number>(new Date().getTime());
    const sessionTimeAggregate = useRef<number>(0);
    const calculateAggregateTime = () => {
        return (
            new Date().getTime() -
            sessionStartTime.current +
            sessionTimeAggregate.current
        );
    };

    useEffect(() => {
        const onUnload = () => {
            trackAppInsightEvent(EventName.SESSION_DURATION, {
                sessionLength: calculateAggregateTime() / 1000,
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
    }, []);
    useScrollToTop();

    const navigate = useNavigate();
    const handleIdle = async (): Promise<void> => {
        if (await OKTA_AUTH.isAuthenticated()) {
            trackAppInsightEvent(EventName.SESSION_DURATION, {
                sessionLength: sessionTimeAggregate.current / 1000,
            });
            logout();
        }
    };
    const restoreOriginalUri = async (_oktaAuth: any, originalUri: string) => {
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
    };

    useIdleTimer({
        timeout: 1000 * 60 * 15,
        onIdle: handleIdle,
        debounce: 500,
    });

    if (IS_IE) return <ErrorUnsupportedBrowser />;
    return (
        <AppWrapper
            oktaAuth={OKTA_AUTH}
            restoreOriginalUri={restoreOriginalUri}
        >
            <Suspense fallback={<Spinner size={"fullpage"} />}>
                <NetworkErrorBoundary
                    fallbackComponent={() => <ErrorPage type="page" />}
                >
                    <ScrollRestoration />
                    {children}
                </NetworkErrorBoundary>
            </Suspense>
        </AppWrapper>
    );
};

export default App;
