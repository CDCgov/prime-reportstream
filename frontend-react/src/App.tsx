import { GovBanner } from "@trussworks/react-uswds";
import { OktaAuth, toRelativeUrl } from "@okta/okta-auth-js";
import { useOktaAuth } from "@okta/okta-react";
import { isIE } from "react-device-detect";
import { useIdleTimer } from "react-idle-timer";
import React from "react";
import { ToastContainer } from "react-toastify";
import { useNavigate } from "react-router-dom";

import { ReportStreamFooter } from "./components/ReportStreamFooter";
import { ReportStreamHeader } from "./components/header/ReportStreamHeader";
import { oktaAuthConfig } from "./oktaConfig";
import { permissionCheck, PERMISSIONS } from "./utils/PermissionsUtils";
import { logout } from "./utils/UserUtils";
import "react-toastify/dist/ReactToastify.css";
import SenderModeBanner from "./components/SenderModeBanner";
import { DAPHeader } from "./components/header/DAPHeader";
import { AppRouter } from "./AppRouter";
import { AppWrapper } from "./components/AppWrapper";
import { UnsupportedBrowser } from "./pages/error/content-elements/UnsupportedBrowser";

const OKTA_AUTH = new OktaAuth(oktaAuthConfig);

const App = () => {
    const navigate = useNavigate();
    const handleIdle = (): void => {
        logout(OKTA_AUTH);
    };
    const restoreOriginalUri = async (_oktaAuth: any, originalUri: string) => {
        // check if the user would have any data to receive via their organizations from the okta claim
        // direct them to the /upload page if they do not have an organization that receives data
        const authState = OKTA_AUTH.authStateManager._authState;
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

    if (isIE) return <UnsupportedBrowser />;

    return (
        <AppWrapper
            oktaAuth={OKTA_AUTH}
            restoreOriginalUri={restoreOriginalUri}
            oktaHook={useOktaAuth}
        >
            <DAPHeader env={process.env.REACT_APP_ENV?.toString()} />
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
        </AppWrapper>
    );
};

export default App;
