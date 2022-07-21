import { GovBanner } from "@trussworks/react-uswds";
import { Route, Switch, useHistory } from "react-router-dom";
import { OktaAuth, toRelativeUrl } from "@okta/okta-auth-js";
import {
    LoginCallback,
    SecureRoute,
    Security,
    useOktaAuth,
} from "@okta/okta-react";
import { isIE } from "react-device-detect";
import { useIdleTimer } from "react-idle-timer";
import React, { Suspense } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import { ToastContainer } from "react-toastify";

import { Home } from "./pages/home/Home";
import { ReportStreamFooter } from "./components/ReportStreamFooter";
import Daily from "./pages/daily/Daily";
import { Details } from "./pages/details/Details";
import { Login } from "./pages/Login";
import { TermsOfService } from "./pages/TermsOfService";
import { About } from "./pages/About";
import { ReportStreamHeader } from "./components/header/ReportStreamHeader";
import { oktaAuthConfig } from "./oktaConfig";
import { AuthorizedRoute } from "./components/AuthorizedRoute";
import { permissionCheck, PERMISSIONS } from "./utils/PermissionsUtils";
import { Upload } from "./pages/Upload";
import { CODES, ErrorPage } from "./pages/error/ErrorPage";
import { logout } from "./utils/UserUtils";
import TermsOfServiceForm from "./pages/tos-sign/TermsOfServiceForm";
import Spinner from "./components/Spinner";
import Submissions from "./pages/submissions/Submissions";
import { AdminMain } from "./pages/admin/AdminMain";
import { AdminOrgEdit } from "./pages/admin/AdminOrgEdit";
import { EditReceiverSettings } from "./components/Admin/EditReceiverSettings";
import { EditSenderSettings } from "./components/Admin/EditSenderSettings";
import "react-toastify/dist/ReactToastify.css";
import SubmissionDetails from "./pages/submissions/SubmissionDetails";
import { NewSetting } from "./components/Admin/NewSetting";
import {
    CheckFeatureFlag,
    FeatureFlagName,
    FeatureFlagUIComponent,
} from "./pages/misc/FeatureFlags";
import SenderModeBanner from "./components/SenderModeBanner";
import { AdminOrgNew } from "./pages/admin/AdminOrgNew";
import { DAPHeader } from "./components/header/DAPHeader";
import ValueSetsIndex from "./pages/admin/value-set-editor/ValueSetsIndex";
import ValueSetsDetail from "./pages/admin/value-set-editor/ValueSetsDetail";
import SessionProvider from "./contexts/SessionContext";
import { Resources } from "./pages/resources/ResourcesIndex";
import { Support } from "./pages/support/SupportIndex";
import InternalUserGuides from "./pages/admin/InternalUserGuides";
import { AdminLastMileFailures } from "./pages/admin/AdminLastMileFailures";
import Validate from "./pages/Validate";
import { Product } from "./pages/product/ProductIndex";
import UploadToPipeline from "./pages/UploadToPipeline";

const OKTA_AUTH = new OktaAuth(oktaAuthConfig);

const App = () => {
    // This is for sanity checking and can be removed
    console.log(
        `process.env.REACT_APP_CLIENT_ENV='${
            process.env?.REACT_APP_CLIENT_ENV || "missing"
        }'`
    );
    const history = useHistory();
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
        /* PERMISSIONS REFACTOR: Redirect URL should be determined by active membership type */
        if (
            authState?.accessToken &&
            permissionCheck(PERMISSIONS.PRIME_ADMIN, authState.accessToken)
        ) {
            history.replace(
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
            <SessionProvider oktaHook={useOktaAuth}>
                <Suspense fallback={<Spinner size={"fullpage"} />}>
                    <NetworkErrorBoundary
                        fallbackComponent={() => <ErrorPage type="page" />}
                    >
                        <DAPHeader
                            env={process.env.REACT_APP_ENV?.toString()}
                        />
                        <GovBanner aria-label="Official government website" />
                        <SenderModeBanner />
                        <ReportStreamHeader />
                        {/* Changed from main to div to fix weird padding issue at the top
                            caused by USWDS styling | 01/22 merged styles from .content into main, don't see padding issues anymore? */}
                        <main id="main-content">
                            <Switch>
                                <Route path="/" exact={true} component={Home} />
                                <Route
                                    path="/terms-of-service"
                                    component={TermsOfService}
                                />
                                <Route path="/about" component={About} />
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
                                    path="/resources"
                                    component={Resources}
                                />
                                <Route path="/product" component={Product} />
                                <Route path="/support" component={Support} />
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
                                {CheckFeatureFlag(
                                    FeatureFlagName.VALIDATION_SERVICE
                                ) && (
                                    <AuthorizedRoute
                                        path="/file-handler/validate"
                                        authorize={PERMISSIONS.PRIME_ADMIN}
                                        component={Validate}
                                    />
                                )}
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
                                <AuthorizedRoute
                                    path="/admin/guides"
                                    authorize={PERMISSIONS.PRIME_ADMIN}
                                    component={InternalUserGuides}
                                />
                                <AuthorizedRoute
                                    path="/admin/lastmile"
                                    authorize={PERMISSIONS.PRIME_ADMIN}
                                    component={AdminLastMileFailures}
                                />
                                <SecureRoute
                                    path="/report-details"
                                    component={Details}
                                />
                                <SecureRoute
                                    path="/admin/features"
                                    component={FeatureFlagUIComponent}
                                />
                                <AuthorizedRoute
                                    path={"/admin/value-sets/:valueSetName"}
                                    authorize={PERMISSIONS.PRIME_ADMIN}
                                    component={ValueSetsDetail}
                                />
                                <AuthorizedRoute
                                    path={"/admin/value-sets"}
                                    authorize={PERMISSIONS.PRIME_ADMIN}
                                    component={ValueSetsIndex}
                                />
                                {CheckFeatureFlag(
                                    FeatureFlagName.USER_UPLOAD
                                ) && (
                                    <AuthorizedRoute
                                        path={"/file-handler/user-upload"}
                                        authorize={PERMISSIONS.PRIME_ADMIN}
                                        component={UploadToPipeline}
                                    />
                                )}
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
            </SessionProvider>
        </Security>
    );
};

export default App;
