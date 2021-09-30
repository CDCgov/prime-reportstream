import "./App.css";
import { Home } from "./pages/home/Home";
import { ReportStreamFooter } from "./components/ReportStreamFooter";
import Daily from "./pages/daily/Daily";
import { HowItWorks } from "./pages/how-it-works/HowItWorks";
import { Details } from "./pages/details/Details";
import { Login } from "./pages/Login";
import { TermsOfService } from "./pages/TermsOfService";
import { GovBanner } from "@trussworks/react-uswds";
import { ReportStreamHeader } from "./components/ReportStreamHeader";

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
import { Suspense } from "react";
import Spinner from "./components/Spinner";
import { useIdleTimer } from "react-idle-timer";
import { NotFound } from "./pages/error/NotFound";
import { UnsupportedBrowser } from "./pages/error/UnsupportedBrowser";
import { CODES, ErrorPage } from "./pages/error/ErrorPage";

const OKTA_AUTH = new OktaAuth(oktaAuthConfig);

const App = () => {

    const history = useHistory();
    const customAuthHandler = () => {
        history.push("/login");
    };
    const restoreOriginalUri = async (_oktaAuth, originalUri) => {
        // check if the user would have any data to receive via their organizations from the okta claim
        // direct them to the /upload page if they do not have an organization that receives data
        const authState = OKTA_AUTH.authStateManager._authState;
        if (!reportReceiver(authState) && permissionCheck(PERMISSIONS.SENDER, authState)) {
            history.replace(toRelativeUrl(`${window.location.origin}/upload`, window.location.origin));
            return;
        }
        history.replace(toRelativeUrl(originalUri, window.location.origin));
    };
    const handleIdle = (): void => {
        if (OKTA_AUTH.authStateManager._authState.isAuthenticated) OKTA_AUTH.signOut();
    }

    useIdleTimer({
        timeout: 1000 * 60 * 15,
        onIdle: handleIdle,
        debounce: 500
    })

    if (isIE) return <UnsupportedBrowser />
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
                        <Suspense fallback={<Spinner fullPage />}>
                            <SecureRoute path="/report-details" component={Details} />
                        </Suspense>

                        {/* TODO:
                            This will not work anywhere under the <Suspense /> tags, and MUST be at the bottom
                            Gotta debug this.
                            >>> Kevin Haube, September 30, 2021
                         */}
                        <Route render={() => (<ErrorPage code={CODES.NOT_FOUND_404} />)} />

                    </Switch>
                </div>
                <footer className="usa-identifier footer">
                    <ReportStreamFooter />
                </footer>
            </NetworkErrorBoundary>
        </Security>
    );
};

export default App;
