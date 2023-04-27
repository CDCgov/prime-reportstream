import { useRoutes } from "react-router-dom";
import { LoginCallback } from "@okta/okta-react";
import React from "react";

import { TermsOfService } from "./pages/TermsOfService";
import { About } from "./pages/About";
import { Login } from "./pages/Login";
import TermsOfServiceForm from "./pages/tos-sign/TermsOfServiceForm";
import { Resources } from "./pages/resources/Resources";
import { ResourcesPage } from "./pages/resources/ResourcesPage";
import { Product } from "./pages/product/ProductIndex";
import { Support } from "./pages/support/Support";
import { UploadWithAuth } from "./pages/Upload";
import { FeatureFlagUIWithAuth } from "./pages/misc/FeatureFlags";
import { SubmissionDetailsWithAuth } from "./pages/submissions/SubmissionDetails";
import { SubmissionsWithAuth } from "./pages/submissions/Submissions";
import { AdminMainWithAuth } from "./pages/admin/AdminMain";
import { AdminOrgNewWithAuth } from "./pages/admin/AdminOrgNew";
import { AdminOrgEditWithAuth } from "./pages/admin/AdminOrgEdit";
import { EditSenderSettingsWithAuth } from "./components/Admin/EditSenderSettings";
import { NewSettingWithAuth } from "./components/Admin/NewSetting";
import { AdminLMFWithAuth } from "./pages/admin/AdminLastMileFailures";
import { AdminMessageTrackerWithAuth } from "./pages/admin/AdminMessageTracker";
import { AdminReceiverDashWithAuth } from "./pages/admin/AdminReceiverDashPage";
import { DeliveryDetailWithAuth } from "./pages/deliveries/details/DeliveryDetail";
import { ValueSetsDetailWithAuth } from "./pages/admin/value-set-editor/ValueSetsDetail";
import { ValueSetsIndexWithAuth } from "./pages/admin/value-set-editor/ValueSetsIndex";
import Home from "./pages/home/Home";
import { DeliveriesWithAuth } from "./pages/deliveries/Deliveries";
import { EditReceiverSettingsWithAuth } from "./components/Admin/EditReceiverSettings";
import { AdminRevHistoryWithAuth } from "./pages/admin/AdminRevHistory";
import { ErrorNoPage } from "./pages/error/legacy-content/ErrorNoPage";
import { MessageDetailsWithAuth } from "./components/MessageTracker/MessageDetails";
import { ManagePublicKeyWithAuth } from "./components/ManagePublicKey/ManagePublicKey";
import FileHandler from "./components/FileHandlers/FileHandler";
import { FaqPage } from "./pages/support/faq/FaqPage";

export enum FeatureName {
    DAILY_DATA = "Daily Data",
    SUBMISSIONS = "Submissions",
    SUPPORT = "Support",
    ADMIN = "Admin",
    UPLOAD = "Upload",
}

export const appRoutes = [
    /* Public Site */
    { path: "/", element: <Home /> },
    { path: "/terms-of-service", element: <TermsOfService /> },
    { path: "/about", element: <About /> },
    { path: "/login", element: <Login /> },
    { path: "/login/callback", element: <LoginCallback /> },
    { path: "/sign-tos", element: <TermsOfServiceForm /> },
    {
        path: "/resources",
        children: [
            { path: "", element: <ResourcesPage /> },
            { path: "*", element: <Resources /> },
        ],
    },
    { path: "/product/*", element: <Product /> },
    {
        path: "/support",
        children: [
            { path: "faq", element: <FaqPage /> },
            { path: "", element: <Support /> },
            { path: "*", element: <Support /> },
        ],
    },
    { path: "/file-handler/validate", element: <FileHandler /> },
    { path: "/daily-data", element: <DeliveriesWithAuth /> },
    {
        path: "/report-details/:reportId",
        element: <DeliveryDetailWithAuth />,
    },
    { path: "/upload", element: <UploadWithAuth /> },
    { path: "/submissions", element: <SubmissionsWithAuth /> },
    {
        path: "/submissions/:actionId",
        element: <SubmissionDetailsWithAuth />,
    },
    /* Admin pages */
    { path: "/admin/settings", element: <AdminMainWithAuth /> },
    { path: "/admin/new/org", element: <AdminOrgNewWithAuth /> },
    {
        path: "/admin/orgsettings/org/:orgname",
        element: <AdminOrgEditWithAuth />,
    },
    {
        path: "/admin/orgreceiversettings/org/:orgname/receiver/:receivername/action/:action",
        element: <EditReceiverSettingsWithAuth />,
    },
    {
        path: "/admin/orgsendersettings/org/:orgname/sender/:sendername/action/:action",
        element: <EditSenderSettingsWithAuth />,
    },
    {
        path: "/admin/orgnewsetting/org/:orgname/settingtype/:settingtype",
        element: <NewSettingWithAuth />,
    },
    { path: "/admin/lastmile", element: <AdminLMFWithAuth /> },
    {
        path: "/admin/send-dash",
        element: <AdminReceiverDashWithAuth />,
    },
    { path: "/admin/features", element: <FeatureFlagUIWithAuth /> },
    {
        path: "/admin/message-tracker",
        element: <AdminMessageTrackerWithAuth />,
    },
    {
        path: "/message-details/:id",
        element: <MessageDetailsWithAuth />,
    },
    {
        path: "/admin/value-sets/:valueSetName",
        element: <ValueSetsDetailWithAuth />,
    },
    {
        path: "/admin/value-sets",
        element: <ValueSetsIndexWithAuth />,
    },
    {
        path: "/admin/revisionhistory/org/:org/settingtype/:settingType",
        element: <AdminRevHistoryWithAuth />,
    },
    {
        path: "/resources/manage-public-key",
        element: <ManagePublicKeyWithAuth />,
    },
    /* Handles any undefined route */
    { path: "*", element: <ErrorNoPage /> },
];

export const AppRouter = () => useRoutes(appRoutes);
