import { createBrowserRouter, redirect, RouteObject } from "react-router-dom";
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
import { DataDashboardWithAuth } from "./pages/data-dashboard/DataDashboard";
import MainLayout from "./layouts/Main/MainLayout";
import { ReportDetailsWithAuth } from "./components/DataDashboard/ReportDetails/ReportDetails";
import { FacilitiesProvidersWithAuth } from "./components/DataDashboard/FacilitiesProviders/FacilitiesProviders";
import { FacilityProviderSubmitterDetailsWithAuth } from "./components/DataDashboard/FacilityProviderSubmitterDetails/FacilityProviderSubmitterDetails";
import { SenderType } from "./utils/DataDashboardUtils";
import { lazyRouteMarkdown } from "./utils/LazyRouteMarkdown";

export enum FeatureName {
    DAILY_DATA = "Daily Data",
    SUBMISSIONS = "Submissions",
    SUPPORT = "Support",
    ADMIN = "Admin",
    UPLOAD = "Upload",
    FACILITIES_PROVIDERS = "All facilities & providers",
    DATA_DASHBOARD = "Data Dashboard",
    REPORT_DETAILS = "Report Details",
    PUBLIC_KEY = "Public Key",
}

export const appRoutes: RouteObject[] = [
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
                        path: "api",
                        children: [
                            {
                                path: "",
                                index: true,
                                lazy: lazyRouteMarkdown(
                                    "content/resources/reportstream-api/ReportStreamApi",
                                ),
                                handle: {
                                    isContentPage: true,
                                },
                            },
                            {
                                path: "getting-started",
                                lazy: lazyRouteMarkdown(
                                    "content/resources/reportstream-api/getting-started/GettingStarted",
                                ),
                                handle: {
                                    isContentPage: true,
                                },
                            },
                            {
                                path: "documentation",
                                children: [
                                    {
                                        path: "",
                                        lazy: lazyRouteMarkdown(
                                            "content/resources/reportstream-api/documentation/Documentation",
                                        ),
                                        index: true,
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "data-model",
                                        lazy: lazyRouteMarkdown(
                                            "content/resources/reportstream-api/documentation/data-model/DataModel",
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "responses-from-reportstream",
                                        lazy: lazyRouteMarkdown(
                                            "content/resources/reportstream-api/documentation/ResponsesFromReportStream",
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "sample-payloads-and-output",
                                        lazy: lazyRouteMarkdown(
                                            "content/resources/reportstream-api/documentation/SamplePayloadsAndOutput",
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                ],
                            },
                        ],
                    },
                    {
                        path: "programmers-guide",
                        loader: async () => {
                            return redirect("/resources/api");
                        },
                    },
                    {
                        path: "manage-public-key",
                        element: <ManagePublicKeyWithAuth />,
                    },
                    {
                        path: "",
                        index: true,
                        lazy: lazyRouteMarkdown(
                            "content/resources/ResourcesIndex",
                        ),
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
                        lazy: lazyRouteMarkdown("content/support/faq/FaqIndex"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "",
                        element: <Support />,
                        index: true,
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "*",
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
                children: [
                    {
                        path: "",
                        index: true,
                        element: <SubmissionsWithAuth />,
                    },
                    {
                        path: "/submissions/:actionId",
                        element: <SubmissionDetailsWithAuth />,
                    },
                ],
            },
            /* Data Dashboard pages */
            {
                path: "/data-dashboard",
                children: [
                    {
                        path: "",
                        element: <DataDashboardWithAuth />,
                        index: true,
                    },
                    {
                        path: "report-details/:reportId",
                        element: <ReportDetailsWithAuth />,
                    },
                    {
                        path: "facilities-providers",
                        element: <FacilitiesProvidersWithAuth />,
                    },
                    {
                        path: "facility/:senderId",
                        element: (
                            <FacilityProviderSubmitterDetailsWithAuth
                                senderType={SenderType.FACILITY}
                            />
                        ),
                    },
                    {
                        path: "provider/:senderId",
                        element: (
                            <FacilityProviderSubmitterDetailsWithAuth
                                senderType={SenderType.PROVIDER}
                            />
                        ),
                    },
                    {
                        path: "submitter/:senderId",
                        element: (
                            <FacilityProviderSubmitterDetailsWithAuth
                                senderType={SenderType.SUBMITTER}
                            />
                        ),
                    },
                ],
            },
            /* Admin pages */
            {
                path: "admin",
                children: [
                    {
                        path: "settings",
                        element: <AdminMainWithAuth />,
                    },
                    {
                        path: "new/org",
                        element: <AdminOrgNewWithAuth />,
                    },
                    {
                        path: "orgsettings/org/:orgname",
                        element: <AdminOrgEditWithAuth />,
                    },
                    {
                        path: "orgreceiversettings/org/:orgname/receiver/:receivername/action/:action",
                        element: <EditReceiverSettingsWithAuth />,
                    },
                    {
                        path: "orgsendersettings/org/:orgname/sender/:sendername/action/:action",
                        element: <EditSenderSettingsWithAuth />,
                    },
                    {
                        path: "orgnewsetting/org/:orgname/settingtype/:settingtype",
                        element: <NewSettingWithAuth />,
                    },
                    {
                        path: "lastmile",
                        element: <AdminLMFWithAuth />,
                    },
                    {
                        path: "send-dash",
                        element: <AdminReceiverDashWithAuth />,
                    },
                    {
                        path: "features",
                        element: <FeatureFlagUIWithAuth />,
                    },
                    {
                        path: "message-tracker",
                        element: <AdminMessageTrackerWithAuth />,
                    },
                    {
                        path: "value-sets",
                        children: [
                            {
                                path: "",
                                index: true,
                                element: <ValueSetsIndexWithAuth />,
                            },
                            {
                                path: ":valueSetName",
                                element: <ValueSetsDetailWithAuth />,
                            },
                        ],
                    },
                    {
                        path: "revisionhistory/org/:org/settingtype/:settingType",
                        element: <AdminRevHistoryWithAuth />,
                    },
                ],
            },
            {
                path: "/message-details/:id",
                element: <MessageDetailsWithAuth />,
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
] satisfies RsRouteObject[];

export const router = createBrowserRouter(appRoutes);
