import { GovBanner } from "@trussworks/react-uswds";
import { toRelativeUrl } from "@okta/okta-auth-js";
import { useIdleTimer } from "react-idle-timer";
import React, { Suspense, useEffect, useRef } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import { ToastContainer } from "react-toastify";
import { useNavigate } from "react-router-dom";

import { ReportStreamFooter } from "./components/ReportStreamFooter";
import { ReportStreamHeader } from "./components/header/ReportStreamHeader";
import { OKTA_AUTH } from "./oktaConfig";
import { permissionCheck, PERMISSIONS } from "./utils/PermissionsUtils";
import Spinner from "./components/Spinner";
import "react-toastify/dist/ReactToastify.css";
import SenderModeBanner from "./components/SenderModeBanner";
import { DAPHeader } from "./components/header/DAPHeader";
import { AppRouter } from "./AppRouter";
import { AppWrapper } from "./components/AppWrapper";
import { ErrorUnsupportedBrowser } from "./pages/error/legacy-content/ErrorUnsupportedBrowser";
import { ErrorPage } from "./pages/error/ErrorPage";
import config from "./config";
import { USLink } from "./components/USLink";
import { useScrollToTop } from "./hooks/UseScrollToTop";
import { EventName, trackAppInsightEvent } from "./utils/Analytics";
import { logout } from "./utils/UserUtils";
import { IS_IE } from "./utils/GetIsIE";

const { APP_ENV } = config;

const App = () => {
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
    const handleIdle = (): void => {
        trackAppInsightEvent(EventName.SESSION_DURATION, {
            sessionLength: sessionTimeAggregate.current / 1000,
        });
        logout();
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
                    window.location.origin
                )
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
                    window.location.origin
                )
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
                    <DAPHeader env={APP_ENV?.toString()} />
                    <USLink className="usa-skipnav" href="#main-content">
                        Skip Nav
                    </USLink>
                    <GovBanner aria-label="Official government website" />
                    <SenderModeBanner />
                    <ReportStreamHeader />
                    {/* Changed from main to div to fix weird padding issue at the top
                            caused by USWDS styling | 01/22 merged styles from .content into main, don't see padding issues anymore? */}
                    <main id="main-content">
                        <AppRouter />
                    </main>
                    <ToastContainer limit={4} />
                    <footer className="usa-identifier footer">
                        <ReportStreamFooter />
                    </footer>
                </NetworkErrorBoundary>
            </Suspense>
        </AppWrapper>
    );
};

export default App;
