import { Route, Routes } from "react-router-dom";
import { LoginCallback } from "@okta/okta-react";
import React from "react";

import { TermsOfService } from "./pages/TermsOfService";
import { About } from "./pages/About";
import { Login } from "./pages/Login";
import TermsOfServiceForm from "./pages/tos-sign/TermsOfServiceForm";
import { Resources } from "./pages/resources/ResourcesCardGrid";
import { Product } from "./pages/product/ProductIndex";
import { Support } from "./pages/support/SupportCardGrid";
import { AuthRoute } from "./components/AuthRoute";
import Daily from "./pages/daily/Daily";
import { Upload } from "./pages/Upload";
import {
    FeatureFlagName,
    FeatureFlagUIComponent,
} from "./pages/misc/FeatureFlags";
import Validate from "./pages/Validate";
import SubmissionDetails from "./pages/submissions/SubmissionDetails";
import Submissions from "./pages/submissions/Submissions";
import { AdminMain } from "./pages/admin/AdminMain";
import { AdminOrgNew } from "./pages/admin/AdminOrgNew";
import { AdminOrgEdit } from "./pages/admin/AdminOrgEdit";
import { EditReceiverSettings } from "./components/Admin/EditReceiverSettings";
import { EditSenderSettings } from "./components/Admin/EditSenderSettings";
import { NewSetting } from "./components/Admin/NewSetting";
import InternalUserGuides from "./pages/admin/InternalUserGuides";
import { AdminLastMileFailures } from "./pages/admin/AdminLastMileFailures";
import { AdminReceiverDashPage } from "./pages/admin/AdminReceiverDashPage";
import { Details } from "./pages/details/Details";
import ValueSetsDetail from "./pages/admin/value-set-editor/ValueSetsDetail";
import ValueSetsIndex from "./pages/admin/value-set-editor/ValueSetsIndex";
import UploadToPipeline from "./pages/UploadToPipeline";
import { CODES, ErrorPage } from "./pages/error/ErrorPage";
import Home from "./pages/home/Home";
import { MemberType } from "./hooks/UseOktaMemberships";

export const AppRouter = () => {
    return (
        <Routes>
            {/* Public Site */}
            <Route path="/" element={<Home />} />
            <Route path="/terms-of-service" element={<TermsOfService />} />
            <Route path="/about" element={<About />} />
            <Route path="/login" element={<Login />} />
            <Route path="/login/callback" element={<LoginCallback />} />
            <Route path="/sign-tos" element={<TermsOfServiceForm />} />
            <Route path="/resources" element={<Resources />} />
            <Route path="/product" element={<Product />} />
            <Route path="/support" element={<Support />} />
            {/* User pages */}
            <AuthRoute
                path="/daily-data"
                requiredUserType={MemberType.RECEIVER}
                element={<Daily />}
            />
            <AuthRoute
                path="/report-details"
                requiredUserType={MemberType.RECEIVER}
                element={<Details />}
            />
            <AuthRoute
                path="/upload"
                requiredUserType={MemberType.SENDER}
                element={<Upload />}
            />
            <AuthRoute
                path="/submissions/:actionId"
                requiredUserType={MemberType.SENDER}
                element={<SubmissionDetails />}
            />
            <AuthRoute
                path="/submissions"
                requiredUserType={MemberType.SENDER}
                element={<Submissions />}
            />
            {/* Admin pages */}
            <AuthRoute
                path="/admin/settings"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<AdminMain />}
            />
            <AuthRoute
                path="/admin/new/org"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<AdminOrgNew />}
            />
            <AuthRoute
                path="/admin/orgsettings/org/:orgname"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<AdminOrgEdit />}
            />
            <AuthRoute
                path="/admin/orgreceiversettings/org/:orgname/receiver/:receivername/action/:action"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<EditReceiverSettings />}
            />
            <AuthRoute
                path="/admin/orgsendersettings/org/:orgname/sender/:sendername/action/:action"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<EditSenderSettings />}
            />
            <AuthRoute
                path="/admin/orgnewsetting/org/:orgname/settingtype/:settingtype"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<NewSetting />}
            />
            <AuthRoute
                path="/admin/guides"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<InternalUserGuides />}
            />
            <AuthRoute
                path="/admin/lastmile"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<AdminLastMileFailures />}
            />
            <AuthRoute
                path="/admin/send-dash"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<AdminReceiverDashPage />}
            />
            <AuthRoute
                path="/admin/features"
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<FeatureFlagUIComponent />}
            />
            <AuthRoute
                path={"/admin/value-sets/:valueSetName"}
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<ValueSetsDetail />}
            />
            <AuthRoute
                path={"/admin/value-sets"}
                requiredUserType={MemberType.PRIME_ADMIN}
                element={<ValueSetsIndex />}
            />
            {/* Feature-flagged pages */}
            <AuthRoute
                path={"/file-handler/user-upload"}
                requiredUserType={MemberType.PRIME_ADMIN}
                requiredFeatureFlag={FeatureFlagName.USER_UPLOAD}
                element={<UploadToPipeline />}
            />
            <AuthRoute
                path="/file-handler/validate"
                requiredUserType={MemberType.PRIME_ADMIN}
                requiredFeatureFlag={FeatureFlagName.VALIDATION_SERVICE}
                element={<Validate />}
            />
            {/* Handles any undefined route */}
            <Route element={<ErrorPage code={CODES.NOT_FOUND_404} />} />
        </Routes>
    );
};
