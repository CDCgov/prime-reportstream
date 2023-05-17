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
import { DataDashboardWithAuth } from "./pages/data-dashboard/DataDashboard";
import ContentLayout from "./layouts/ContentLayout";
import AppLayout from "./layouts/AppLayout";

export enum FeatureName {
    DAILY_DATA = "Daily Data",
    SUBMISSIONS = "Submissions",
    SUPPORT = "Support",
    ADMIN = "Admin",
    UPLOAD = "Upload",
    FACILITIES_PROVIDERS = "All facilities & providers",
    DATA_DASHBOARD = "Data Dashboard",
    REPORT_DETAILS = "Report Details",
}

export const appRoutes = [
    /* Public Site */
    {
        path: "/",
        element: (
            <ContentLayout>
                <Home />
            </ContentLayout>
        ),
    },
    {
        path: "/terms-of-service",
        element: (
            <ContentLayout>
                <TermsOfService />
            </ContentLayout>
        ),
    },
    {
        path: "/about",
        element: (
            <ContentLayout>
                <About />
            </ContentLayout>
        ),
    },
    {
        path: "/login",
        element: (
            <AppLayout>
                <Login />
            </AppLayout>
        ),
    },
    {
        path: "/login/callback",
        element: (
            <AppLayout>
                <LoginCallback />
            </AppLayout>
        ),
    },
    {
        path: "/sign-tos",
        element: (
            <ContentLayout>
                <TermsOfServiceForm />
            </ContentLayout>
        ),
    },
    {
        path: "/resources",
        children: [
            {
                path: "",
                element: (
                    <ContentLayout>
                        <ResourcesPage />{" "}
                    </ContentLayout>
                ),
            },
            {
                path: "*",
                element: (
                    <ContentLayout>
                        <Resources />
                    </ContentLayout>
                ),
            },
        ],
    },
    {
        path: "/product/*",
        element: (
            <ContentLayout>
                <Product />
            </ContentLayout>
        ),
    },
    {
        path: "/support",
        children: [
            {
                path: "faq",
                element: (
                    <ContentLayout>
                        <FaqPage />
                    </ContentLayout>
                ),
            },
            {
                path: "",
                element: (
                    <ContentLayout>
                        <Support />
                    </ContentLayout>
                ),
            },
            {
                path: "*",
                element: (
                    <ContentLayout>
                        <Support />
                    </ContentLayout>
                ),
            },
        ],
    },
    {
        path: "/file-handler/validate",
        element: (
            <AppLayout>
                <FileHandler />
            </AppLayout>
        ),
    },
    {
        path: "/daily-data",
        element: (
            <AppLayout>
                <DeliveriesWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/report-details/:reportId",
        element: (
            <AppLayout>
                <DeliveryDetailWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/upload",
        element: (
            <AppLayout>
                <UploadWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/submissions",
        element: (
            <AppLayout>
                <SubmissionsWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/submissions/:actionId",
        element: (
            <AppLayout>
                <SubmissionDetailsWithAuth />
            </AppLayout>
        ),
    },
    /* Data Dashboard pages */
    {
        path: "/data-dashboard",
        element: (
            <AppLayout>
                <DataDashboardWithAuth />
            </AppLayout>
        ),
    },
    /* Admin pages */
    {
        path: "/admin/settings",
        element: (
            <AppLayout>
                <AdminMainWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/new/org",
        element: (
            <AppLayout>
                <AdminOrgNewWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/orgsettings/org/:orgname",
        element: (
            <AppLayout>
                <AdminOrgEditWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/orgreceiversettings/org/:orgname/receiver/:receivername/action/:action",
        element: (
            <AppLayout>
                <EditReceiverSettingsWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/orgsendersettings/org/:orgname/sender/:sendername/action/:action",
        element: (
            <AppLayout>
                <EditSenderSettingsWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/orgnewsetting/org/:orgname/settingtype/:settingtype",
        element: (
            <AppLayout>
                <NewSettingWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/lastmile",
        element: (
            <AppLayout>
                <AdminLMFWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/send-dash",
        element: (
            <AppLayout>
                <AdminReceiverDashWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/features",
        element: (
            <AppLayout>
                <FeatureFlagUIWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/message-tracker",
        element: (
            <AppLayout>
                <AdminMessageTrackerWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/message-details/:id",
        element: (
            <AppLayout>
                <MessageDetailsWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/value-sets/:valueSetName",
        element: (
            <AppLayout>
                <ValueSetsDetailWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/value-sets",
        element: (
            <AppLayout>
                <ValueSetsIndexWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/admin/revisionhistory/org/:org/settingtype/:settingType",
        element: (
            <AppLayout>
                <AdminRevHistoryWithAuth />
            </AppLayout>
        ),
    },
    {
        path: "/resources/manage-public-key",
        element: (
            <AppLayout>
                <ManagePublicKeyWithAuth />
            </AppLayout>
        ),
    },
    /* Handles any undefined route */
    {
        path: "*",
        element: (
            <ContentLayout>
                <ErrorNoPage />
            </ContentLayout>
        ),
    },
];

export const AppRouter = () => useRoutes(appRoutes);
