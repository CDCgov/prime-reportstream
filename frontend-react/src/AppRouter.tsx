import { Route, Routes } from "react-router-dom";
import { LoginCallback } from "@okta/okta-react";
import React from "react";

import { TermsOfService } from "./pages/TermsOfService";
import { About } from "./pages/About";
import { Login } from "./pages/Login";
import TermsOfServiceForm from "./pages/tos-sign/TermsOfServiceForm";
import { Resources } from "./pages/resources/Resources";
import { Product } from "./pages/product/ProductIndex";
import { Support } from "./pages/support/Support";
import { UploadWithAuth } from "./pages/Upload";
import { FeatureFlagUIWithAuth } from "./pages/misc/FeatureFlags";
import { ValidateWithAuth } from "./pages/Validate";
import { SubmissionDetailsWithAuth } from "./pages/submissions/SubmissionDetails";
import { SubmissionsWithAuth } from "./pages/submissions/Submissions";
import { AdminMainWithAuth } from "./pages/admin/AdminMain";
import { AdminOrgNewWithAuth } from "./pages/admin/AdminOrgNew";
import { AdminOrgEditWithAuth } from "./pages/admin/AdminOrgEdit";
import { EditSenderSettingsWithAuth } from "./components/Admin/EditSenderSettings";
import { NewSettingWithAuth } from "./components/Admin/NewSetting";
import { AdminLMFWithAuth } from "./pages/admin/AdminLastMileFailures";
import { AdminReceiverDashWithAuth } from "./pages/admin/AdminReceiverDashPage";
import { DetailsWithAuth } from "./pages/details/Details";
import { ValueSetsDetailWithAuth } from "./pages/admin/value-set-editor/ValueSetsDetail";
import { ValueSetsIndexWithAuth } from "./pages/admin/value-set-editor/ValueSetsIndex";
import { UploadToPipelineWithAuth } from "./pages/UploadToPipeline";
import Home from "./pages/home/Home";
import { DailyWithAuth } from "./pages/daily/Daily";
import { EditReceiverSettingsWithAuth } from "./components/Admin/EditReceiverSettings";
import { ErrorNoPage } from "./pages/error/legacy-content/ErrorNoPage";

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
            <Route path="/resources/*" element={<Resources />} />
            <Route path="/product/*" element={<Product />} />
            <Route path="/support/*" element={<Support />} />
            {/* User pages */}
            <Route path="/daily-data" element={<DailyWithAuth />} />
            <Route path="/report-details" element={<DetailsWithAuth />} />
            <Route path="/upload" element={<UploadWithAuth />} />
            <Route path="/submissions" element={<SubmissionsWithAuth />} />
            <Route
                path="/submissions/:actionId"
                element={<SubmissionDetailsWithAuth />}
            />
            {/* Admin pages */}
            <Route path="/admin/settings" element={<AdminMainWithAuth />} />
            <Route path="/admin/new/org" element={<AdminOrgNewWithAuth />} />
            <Route
                path="/admin/orgsettings/org/:orgname"
                element={<AdminOrgEditWithAuth />}
            />
            <Route
                path="/admin/orgreceiversettings/org/:orgname/receiver/:receivername/action/:action"
                element={<EditReceiverSettingsWithAuth />}
            />
            <Route
                path="/admin/orgsendersettings/org/:orgname/sender/:sendername/action/:action"
                element={<EditSenderSettingsWithAuth />}
            />
            <Route
                path="/admin/orgnewsetting/org/:orgname/settingtype/:settingtype"
                element={<NewSettingWithAuth />}
            />
            <Route path="/admin/lastmile" element={<AdminLMFWithAuth />} />
            <Route
                path="/admin/send-dash"
                element={<AdminReceiverDashWithAuth />}
            />
            <Route path="/admin/features" element={<FeatureFlagUIWithAuth />} />
            <Route
                path={"/admin/value-sets/:valueSetName"}
                element={<ValueSetsDetailWithAuth />}
            />
            <Route
                path={"/admin/value-sets"}
                element={<ValueSetsIndexWithAuth />}
            />
            {/* Feature-flagged pages */}
            <Route
                path={"/file-handler/user-upload"}
                element={<UploadToPipelineWithAuth />}
            />
            <Route
                path="/file-handler/validate"
                element={<ValidateWithAuth />}
            />
            {/* Handles any undefined route */}
            <Route path={"*"} element={<ErrorNoPage />} />
        </Routes>
    );
};
