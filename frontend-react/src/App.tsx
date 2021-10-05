// @ts-nocheck // TODO: fix types in this file
import "./App.css";
import { Home } from "./pages/home/Home";
import { ReportStreamFooter } from "./components/ReportStreamFooter";
import Daily from "./pages/daily/Daily";
import { HowItWorks } from "./pages/how-it-works/HowItWorks";
import { Details } from "./pages/details/Details";
import { Login } from "./pages/Login";
import { TermsOfService } from "./pages/TermsOfService";
import { GovBanner } from "@trussworks/react-uswds";
import { ReportStreamHeader } from "./components/header/ReportStreamHeader";

import { oktaSignInConfig, oktaAuthConfig } from "./oktaConfig";
import { Route, useHistory, Switch } from "react-router-dom";
import { OktaAuth, toRelativeUrl } from "@okta/okta-auth-js";
import { Security, SecureRoute, LoginCallback } from "@okta/okta-react";
import { NetworkErrorBoundary } from "rest-hooks";
import { isIE } from "react-device-detect";

import { About } from "./pages/About";
import { AuthorizedRoute } from "./components/AuthorizedRoute";
import { PERMISSIONS } from "./resources/PermissionsResource";
import { permissionCheck, reportReceiver } from "./webreceiver-utils";
import { Upload } from "./pages/Upload";
import { useIdleTimer } from "react-idle-timer";
import { CODES, ErrorPage } from "./pages/error/ErrorPage";
import GlobalContextProvider from "./components/GlobalContextProvider";
import { logout } from "./utils/UserUtils";

const OKTA_AUTH = new OktaAuth(oktaAuthConfig);

const App = () => {
    const history = useHistory();
    const customAuthHandler = (): void => {
        history.push("/login");
    };
    const handleIdle = (): void => {
        logout(OKTA_AUTH);
    }
    const restoreOriginalUri = async (_oktaAuth: any, originalUri: string) => {
        // check if the user would have any data to receive via their organizations from the okta claim
        // direct them to the /upload page if they do not have an organization that receives data
        const authState = OKTA_AUTH.authStateManager._authState;
        if (!reportReceiver(authState) && permissionCheck(PERMISSIONS.SENDER, authState)) {
            history.replace(toRelativeUrl(`${window.location.origin}/upload`, window.location.origin));
            return;
        }
        history.replace(toRelativeUrl(originalUri, window.location.origin));
    };

    useIdleTimer({
        timeout: 1000 * 60 * 15,
        onIdle: handleIdle,
        debounce: 500
    })

    if (isIE) return <ErrorPage code={CODES.UNSUPPORTED_BROWSER} />
    return (
        <Security
            oktaAuth={OKTA_AUTH}
            onAuthRequired={customAuthHandler}
            restoreOriginalUri={restoreOriginalUri}
        >
            <NetworkErrorBoundary
                fallbackComponent={() => {
                    return <div></div>;
                }}
            >
                <GlobalContextProvider>
                    <div className="content">
                        <GovBanner aria-label="Official government website" />
                        <ReportStreamHeader />
                        <Switch>
                            <Route path="/" exact={true} component={Home} />
                            <Route path="/about" component={About} />
                            <Route path="/how-it-works" component={HowItWorks} />
                            <Route path="/terms-of-service" component={TermsOfService} />
                            <Route path="/login" render={() => (<Login config={oktaSignInConfig} />)} />
                            <Route path="/login/callback" component={LoginCallback} />
                            <AuthorizedRoute path='/daily-data' authorize={PERMISSIONS.RECEIVER} component={Daily} />
                            <AuthorizedRoute path='/upload' authorize={PERMISSIONS.SENDER} component={Upload} />
                            <SecureRoute path="/report-details" component={Details} />
                            {/* Handles any undefined route */}
                            <Route render={() => (<ErrorPage code={CODES.NOT_FOUND_404} />)} />
                        </Switch>
                    </div>
                </GlobalContextProvider>
                <footer className="usa-identifier footer">
                    <ReportStreamFooter />
                </footer>
            </NetworkErrorBoundary>
        </Security>
    );
};

export default App;
