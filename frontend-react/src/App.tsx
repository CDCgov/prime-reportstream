import { GovBanner } from "@trussworks/react-uswds";
import { Route, Switch, useHistory } from "react-router-dom";
import { OktaAuth, toRelativeUrl } from "@okta/okta-auth-js";
import { LoginCallback, SecureRoute, Security } from "@okta/okta-react";
import { isIE } from "react-device-detect";
import { useIdleTimer } from "react-idle-timer";
import React, { Suspense, useContext } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import { ToastContainer } from "react-toastify";

import { Home } from "./pages/home/Home";
import { ReportStreamFooter } from "./components/ReportStreamFooter";
import Daily from "./pages/daily/Daily";
import { HowItWorks } from "./pages/how-it-works/HowItWorks";
import { Details } from "./pages/details/Details";
import { Login } from "./pages/Login";
import { TermsOfService } from "./pages/TermsOfService";
import { ReportStreamHeader } from "./components/header/ReportStreamHeader";
import { oktaAuthConfig } from "./oktaConfig";
import { AuthorizedRoute } from "./components/AuthorizedRoute";
import { PERMISSIONS, permissionCheck } from "./utils/PermissionsUtils";
import { Upload } from "./pages/Upload";
import { CODES, ErrorPage } from "./pages/error/ErrorPage";
import { logout } from "./utils/UserUtils";
import TermsOfServiceForm from "./pages/tos-sign/TermsOfServiceForm";
import Spinner from "./components/Spinner";
import Submissions from "./pages/submissions/Submissions";
import { GettingStartedPublicHealthDepartments } from "./pages/getting-started/public-health-departments";
import { GettingStartedTestingFacilities } from "./pages/getting-started/testing-facilities";
import { AdminMain } from "./pages/admin/AdminMain";
import { AdminOrgEdit } from "./pages/admin/AdminOrgEdit";
import { EditReceiverSettings } from "./components/Admin/EditReceiverSettings";
import { EditSenderSettings } from "./components/Admin/EditSenderSettings";
import "react-toastify/dist/ReactToastify.css";
import SubmissionDetails from "./pages/submissions/SubmissionDetails";
import { NewSetting } from "./components/Admin/NewSetting";
import { FeatureFlagUIComponent } from "./pages/misc/FeatureFlags";
import SenderModeBanner from "./components/SenderModeBanner";
import { SessionStorageContext } from "./contexts/SessionStorageContext";
import { AdminOrgNew } from "./pages/admin/AdminOrgNew";
import { DAPHeader } from "./components/header/DAPHeader";

const OKTA_AUTH = new OktaAuth(oktaAuthConfig);

const App = () => {
    // This is for sanity checking and can be removed
    console.log(
        `process.env.REACT_APP_CLIENT_ENV='${
            process.env?.REACT_APP_CLIENT_ENV || "missing"
        }'`
    );
    const history = useHistory();
    const context = useContext(SessionStorageContext);
    const customAuthHandler = (): void => {
        history.push("/login");
    };
    const handleIdle = (): void => {
        logout(OKTA_AUTH);
    };
    const restoreOriginalUri = async (_oktaAuth: any, originalUri: string) => {
        // check if the user would have any data to receive via their organizations from the okta claim
        // direct them to the /upload page if they do not have an organization that receives data
        const authState = OKTA_AUTH.authStateManager._authState;
        if (
            authState?.accessToken &&
            permissionCheck(PERMISSIONS.SENDER, authState.accessToken)
        ) {
            history.replace(
                toRelativeUrl(
                    `${window.location.origin}/upload`,
                    window.location.origin
                )
            );
            return;
        }
        history.replace(toRelativeUrl(originalUri, window.location.origin));
    };

    useIdleTimer({
        timeout: 1000 * 60 * 15,
        onIdle: handleIdle,
        debounce: 500,
    });

    if (isIE) return <ErrorPage code={CODES.UNSUPPORTED_BROWSER} />;
    return (
        <Security
            oktaAuth={OKTA_AUTH}
            onAuthRequired={customAuthHandler}
            restoreOriginalUri={restoreOriginalUri}
        >
            <Suspense fallback={<Spinner size={"fullpage"} />}>
                <NetworkErrorBoundary
                    fallbackComponent={() => <ErrorPage type="page" />}
                >
                    <DAPHeader env={process.env.REACT_APP_ENV?.toString()} />
                    <GovBanner aria-label="Official government website" />
                    {context.values.org && context.values.senderName ? (
                        <SenderModeBanner />
                    ) : null}
                    <ReportStreamHeader />
                    {/* Changed from main to div to fix weird padding issue at the top
                            caused by USWDS styling | 01/22 merged styles from .content into main, don't see padding issues anymore? */}
                    <main id="main-content">
                        <Switch>
                            <Route path="/" exact={true} component={Home} />
                            <Route
                                path="/how-it-works"
                                component={HowItWorks}
                            />
                            <Route
                                path="/terms-of-service"
                                component={TermsOfService}
                            />
                            <Route path="/login" render={() => <Login />} />
                            <Route
                                path="/login/callback"
                                component={LoginCallback}
                            />
                            <Route
                                path="/sign-tos"
                                component={TermsOfServiceForm}
                            />
                            <Route
                                path="/getting-started/public-health-departments"
                                component={
                                    GettingStartedPublicHealthDepartments
                                }
                            />
                            <Route
                                path="/getting-started/testing-facilities"
                                component={GettingStartedTestingFacilities}
                            />
                            <AuthorizedRoute
                                path="/daily-data"
                                authorize={PERMISSIONS.RECEIVER}
                                component={Daily}
                            />
                            <AuthorizedRoute
                                path="/upload"
                                authorize={PERMISSIONS.SENDER}
                                component={Upload}
                            />
                            {/* TODO: AuthorizedRoute needs to take many potential auth groups.
                             *  We should fix this when we refactor our permissions layer.
                             */}
                            <AuthorizedRoute
                                path="/submissions/:actionId"
                                authorize={PERMISSIONS.SENDER}
                                component={SubmissionDetails}
                            />
                            <AuthorizedRoute
                                path="/submissions"
                                authorize={PERMISSIONS.SENDER}
                                component={Submissions}
                            />
                            <AuthorizedRoute
                                path="/admin/settings"
                                authorize={PERMISSIONS.PRIME_ADMIN}
                                component={AdminMain}
                            />
                            <AuthorizedRoute
                                path="/admin/new/org"
                                authorize={PERMISSIONS.PRIME_ADMIN}
                                component={AdminOrgNew}
                            />
                            <AuthorizedRoute
                                path="/admin/orgsettings/org/:orgname"
                                authorize={PERMISSIONS.PRIME_ADMIN}
                                component={AdminOrgEdit}
                            />
                            <AuthorizedRoute
                                path="/admin/orgreceiversettings/org/:orgname/receiver/:receivername/action/:action"
                                authorize={PERMISSIONS.PRIME_ADMIN}
                                component={EditReceiverSettings}
                            />
                            <AuthorizedRoute
                                path="/admin/orgsendersettings/org/:orgname/sender/:sendername/action/:action"
                                authorize={PERMISSIONS.PRIME_ADMIN}
                                component={EditSenderSettings}
                            />
                            <AuthorizedRoute
                                path="/admin/orgnewsetting/org/:orgname/settingtype/:settingtype"
                                authorize={PERMISSIONS.PRIME_ADMIN}
                                component={NewSetting}
                            />
                            <SecureRoute
                                path="/report-details"
                                component={Details}
                            />
                            <SecureRoute
                                path="/features"
                                component={FeatureFlagUIComponent}
                            />
                            {/* Handles any undefined route */}
                            <Route
                                render={() => (
                                    <ErrorPage code={CODES.NOT_FOUND_404} />
                                )}
                            />
                        </Switch>
                    </main>
                    <ToastContainer limit={4} />
                    <footer className="usa-identifier footer">
                        <ReportStreamFooter />
                    </footer>
                </NetworkErrorBoundary>
            </Suspense>
        </Security>
    );
};

export default App;
