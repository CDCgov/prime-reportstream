import "./App.css";
import { Home } from "./pages/Home";
import { ReportStreamFooter } from "./components/ReportStreamFooter";
import { Daily } from "./pages/Daily";
import { Documentation } from "./pages/Documentation";
import { Details } from "./pages/Details";
import { Login } from "./pages/Login";
import { TermsOfService } from "./pages/TermsOfService";
import { GovBanner } from "@trussworks/react-uswds";
import { ReportStreamHeader } from "./components/ReportStreamHeader";
import { Suspense } from "react";
import { oktaSignInConfig, oktaAuthConfig } from "./oktaConfig";
import { Route, useHistory, Switch } from "react-router-dom";
import { OktaAuth, toRelativeUrl } from "@okta/okta-auth-js";
import { Security, SecureRoute, LoginCallback } from "@okta/okta-react";
import { NetworkErrorBoundary } from "rest-hooks";
import { SpinnerCircular } from "spinners-react";

const oktaAuth = new OktaAuth(oktaAuthConfig);

const App = () => {
    const history = useHistory();

    const customAuthHandler = () => {
        history.push("/login");
    };

    const restoreOriginalUri = async (_oktaAuth, originalUri) => {
        history.replace(toRelativeUrl(originalUri, window.location.origin));
    };

    return (
        <Security
            oktaAuth={oktaAuth}
            onAuthRequired={customAuthHandler}
            restoreOriginalUri={restoreOriginalUri}
        >
            <Suspense
                fallback={
                    <div id="div">
                        <div id="spinner">
                            <SpinnerCircular
                                color="rgba(57, 88, 172, 1)"
                                size="30%"
                            />
                        </div>
                    </div>
                }
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
                            <SecureRoute path="/daily" component={Daily} />
                            <Route
                                path="/documentation"
                                component={Documentation}
                            />
                            <SecureRoute
                                path="/report-details"
                                component={Details}
                            />
                            <Route
                                path="/terms-of-service"
                                component={TermsOfService}
                            />
                            <Route
                                path="/login"
                                render={() => (
                                    <Login config={oktaSignInConfig} />
                                )}
                            />
                            <Route
                                path="/login/callback"
                                component={LoginCallback}
                            />
                        </Switch>
                        <div className="footer">
                            <ReportStreamFooter />
                        </div>
                    </div>
                </NetworkErrorBoundary>
            </Suspense>
        </Security>
    );
};

export default App;
