import { createBrowserRouter, RouteObject } from "react-router-dom";
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
import { DocumentationPage } from "./pages/resources/api-programmers-guide/documentation/Documentation";
import { DataModelPage } from "./pages/resources/api-programmers-guide/documentation/DataModel";
import { ResponsesFromReportStreamPage } from "./pages/resources/api-programmers-guide/documentation/ResponsesFromReportStream";
import { SamplePayloadsAndOutputPage } from "./pages/resources/api-programmers-guide/documentation/SamplePayloadsAndOutput";
import FileHandler from "./components/FileHandlers/FileHandler";
import { FaqPage } from "./pages/support/faq/FaqPage";
import { DataDashboardWithAuth } from "./pages/data-dashboard/DataDashboard";
import MainLayout from "./layouts/MainLayout";

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

export const appRoutes: RsRouteObject[] = [
    /* Public Site */
    {
        path: "/",
        element: <MainLayout />,
        children: [
            {
                path: "",
                index: true,
                element: <Home />,
                handle: {
                    isContentPage: true,
                },
            },
            {
                path: "/terms-of-service",
                element: <TermsOfService />,
                handle: {
                    isContentPage: true,
                },
            },
            {
                path: "/about",
                element: <About />,
                handle: {
                    isContentPage: true,
                },
            },
            {
                path: "/login",
                element: <Login />,
            },
            {
                path: "/login/callback",
                element: <LoginCallback />,
            },
            {
                path: "/sign-tos",
                element: <TermsOfServiceForm />,
                handle: {
                    isContentPage: true,
                },
            },
            {
                path: "/resources",
                children: [
                    {
                        path: "documentation",
                        children: [
                            {
                                path: "",
                                element: <DocumentationPage />,
                                index: true,
                            },
                            { path: "data-model", element: <DataModelPage /> },
                            {
                                path: "responses-from-reportstream",
                                element: <ResponsesFromReportStreamPage />,
                            },
                            {
                                path: "sample-payloads-and-output",
                                element: <SamplePayloadsAndOutputPage />,
                            },
                        ],
                    },
                    {
                        path: "",
                        index: true,
                        element: <ResourcesPage />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "*",
                        element: <Resources />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                ],
            },
            {
                path: "/product/*",
                element: <Product />,
                handle: {
                    isContentPage: true,
                },
            },
            {
                path: "/support",
                children: [
                    {
                        path: "faq",
                        element: <FaqPage />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "",
                        element: <Support />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                ],
            },
            {
                path: "/file-handler/validate",
                element: <FileHandler />,
            },
            {
                path: "/daily-data",
                element: <DeliveriesWithAuth />,
            },
            {
                path: "/report-details/:reportId",
                element: <DeliveryDetailWithAuth />,
            },
            {
                path: "/upload",
                element: <UploadWithAuth />,
            },
            {
                path: "/submissions",
                element: <SubmissionsWithAuth />,
            },
            {
                path: "/submissions/:actionId",
                element: <SubmissionDetailsWithAuth />,
            },
            /* Data Dashboard pages */
            {
                path: "/data-dashboard",
                element: <DataDashboardWithAuth />,
            },
            /* Admin pages */
            {
                path: "/admin/settings",
                element: <AdminMainWithAuth />,
            },
            {
                path: "/admin/new/org",
                element: <AdminOrgNewWithAuth />,
            },
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
            {
                path: "/admin/lastmile",
                element: <AdminLMFWithAuth />,
            },
            {
                path: "/admin/send-dash",
                element: <AdminReceiverDashWithAuth />,
            },
            {
                path: "/admin/features",
                element: <FeatureFlagUIWithAuth />,
            },
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
            {
                path: "*",
                element: <ErrorNoPage />,
                handle: {
                    isContentPage: true,
                },
            },
        ],
    },
];

export const router = createBrowserRouter(appRoutes as RouteObject[]);
