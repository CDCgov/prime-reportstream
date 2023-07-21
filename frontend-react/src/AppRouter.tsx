import { createBrowserRouter, redirect, RouteObject } from "react-router-dom";
import { LoginCallback } from "@okta/okta-react";
import React from "react";

import { Login } from "./pages/Login";
import TermsOfServiceForm from "./pages/tos-sign/TermsOfServiceForm";
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
import { lazyRouteMarkdown } from "./layouts/Markdown/MarkdownLayout";

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
                lazy: lazyRouteMarkdown("content/terms-of-service"),
                handle: {
                    isContentPage: true,
                },
            },
            {
                path: "/resources",
                children: [
                    {
                        path: "",
                        index: true,
                        lazy: lazyRouteMarkdown(
                            "content/resources/landing-page"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "api",
                        children: [
                            {
                                path: "",
                                index: true,
                                lazy: lazyRouteMarkdown(
                                    "content/resources/reportstream-api/landing-page"
                                ),
                                handle: {
                                    isContentPage: true,
                                },
                            },
                            {
                                path: "getting-started",
                                lazy: lazyRouteMarkdown(
                                    "content/resources/reportstream-api/getting-started/landing-page"
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
                                            "content/resources/reportstream-api/documentation/landing-page"
                                        ),
                                        index: true,
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "data-model",
                                        lazy: lazyRouteMarkdown(
                                            "content/resources/reportstream-api/documentation/data-model/landing-page"
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "responses-from-reportstream",
                                        lazy: lazyRouteMarkdown(
                                            "content/resources/reportstream-api/documentation/responses-from-reportstream"
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "sample-payloads-and-output",
                                        lazy: lazyRouteMarkdown(
                                            "content/resources/reportstream-api/documentation/sample-payloads-and-output"
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                ],
                            },
                        ],
                    },
                    /**
                     * Deprecated
                     */
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
                        path: "getting-started-submitting-data",
                        lazy: lazyRouteMarkdown(
                            "content/resources/getting-started-submitting-data"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "referral-guide",
                        lazy: lazyRouteMarkdown(
                            "content/resources/referral-guide"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "security-practices",
                        lazy: lazyRouteMarkdown(
                            "content/resources/security-practices"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "system-and-settings",
                        lazy: lazyRouteMarkdown(
                            "content/resources/system-and-settings"
                        ),
                    },
                    {
                        path: "data-download-guide",
                        lazy: lazyRouteMarkdown(
                            "content/resources/data-download-guide"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "account-registration-guide",
                        lazy: lazyRouteMarkdown(
                            "content/resources/account-registration-guide"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "elr-checklist",
                        lazy: lazyRouteMarkdown(
                            "content/resources/elr-checklist"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "getting-started-public-health-departments",
                        lazy: lazyRouteMarkdown(
                            "content/resources/getting-started-phd"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                ],
            },
            /**
             * Deprecated
             */
            {
                path: "/about",
                loader: async () => {
                    return redirect("/product/about");
                },
            },
            {
                path: "/product",
                children: [
                    {
                        path: "",
                        index: true,
                        loader: async () => {
                            return redirect("/product/overview");
                        },
                    },
                    {
                        path: "overview",
                        lazy: lazyRouteMarkdown("content/product/landing-page"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "where-were-live",
                        lazy: lazyRouteMarkdown(
                            "content/product/where-were-live"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "release-notes",
                        lazy: lazyRouteMarkdown(
                            "content/product/release-notes"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "about",
                        lazy: lazyRouteMarkdown("content/product/about"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                ],
            },
            {
                path: "/support",
                children: [
                    {
                        path: "",
                        lazy: lazyRouteMarkdown("content/support/landing-page"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "faq",
                        lazy: lazyRouteMarkdown("content/support/faq"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "service-request",
                        lazy: lazyRouteMarkdown(
                            "content/support/service-request"
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "contact",
                        lazy: lazyRouteMarkdown("content/support/contact"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                ],
            },
            /**
             * AKA Account Registration
             */
            {
                path: "/sign-tos",
                element: <TermsOfServiceForm />,
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
