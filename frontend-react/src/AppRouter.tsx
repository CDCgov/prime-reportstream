import { createBrowserRouter, redirect, RouteObject } from "react-router-dom";
import React from "react";

import { TermsOfService } from "./pages/TermsOfService";
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
import { DeliveriesWithAuth } from "./pages/deliveries/Deliveries";
import { EditReceiverSettingsWithAuth } from "./components/Admin/EditReceiverSettings";
import { AdminRevHistoryWithAuth } from "./pages/admin/AdminRevHistory";
import { ErrorNoPage } from "./pages/error/legacy-content/ErrorNoPage";
import { MessageDetailsWithAuth } from "./components/MessageTracker/MessageDetails";
import { ManagePublicKeyWithAuth } from "./components/ManagePublicKey/ManagePublicKey";
import FileHandler from "./components/FileHandlers/FileHandler";
import { DataDashboardWithAuth } from "./pages/data-dashboard/DataDashboard";
import { ReportDetailsWithAuth } from "./components/DataDashboard/ReportDetails/ReportDetails";
import { FacilitiesProvidersWithAuth } from "./components/DataDashboard/FacilitiesProviders/FacilitiesProviders";
import { FacilityProviderSubmitterDetailsWithAuth } from "./components/DataDashboard/FacilityProviderSubmitterDetails/FacilityProviderSubmitterDetails";
import { SenderType } from "./utils/DataDashboardUtils";
import { lazyRouteMarkdown } from "./utils/LazyRouteMarkdown";
import LoginCallback from "./shared/LoginCallback/LoginCallback";
import LogoutCallback from "./shared/LogoutCallback/LogoutCallback";
import { DataDownloadGuideIa } from "./pages/DataDownloadGuide";
import { GettingStartedPhd } from "./pages/GettingStartedPhd";

export const appRoutes: RouteObject[] = [
    /* Public Site */
    {
        path: "/",
        children: [
            {
                path: "",
                index: true,
                lazy: lazyRouteMarkdown("content/home/index"),
                handle: {
                    isContentPage: true,
                    isFullWidth: true,
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
                children: [
                    {
                        index: true,
                        lazy: lazyRouteMarkdown("content/about/index"),
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
                        },
                    },
                    {
                        path: "our-network",
                        lazy: lazyRouteMarkdown("content/about/our-network"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "news",
                        lazy: lazyRouteMarkdown("content/about/news"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "security",
                        lazy: lazyRouteMarkdown("content/about/security"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "release-notes",
                        lazy: lazyRouteMarkdown("content/about/release-notes"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "case-studies",
                        lazy: lazyRouteMarkdown("content/about/case-studies"),
                        handle: {
                            isContentPage: true,
                        },
                    },
                ],
            },
            {
                path: "/login",
                children: [
                    {
                        element: <Login />,
                        index: true,
                        handle: {
                            isLoginPage: true,
                        },
                    },
                    {
                        path: "callback",
                        element: <LoginCallback />,
                    },
                ],
            },
            {
                path: "/logout/callback",
                element: <LogoutCallback />,
            },
            {
                path: "/sign-tos",
                element: <TermsOfServiceForm />,
                handle: {
                    isContentPage: true,
                },
            },
            {
                path: "managing-your-connection",
                children: [
                    {
                        path: "refer-healthcare-organizations",
                        handle: {
                            isContentPage: true,
                        },
                        lazy: lazyRouteMarkdown(
                            "content/managing-your-connection/refer-healthcare-organizations",
                        ),
                    },
                    {
                        path: "data-download-guide",
                        handle: {
                            isContentPage: true,
                        },
                        element: <DataDownloadGuideIa />,
                    },
                ],
            },
            {
                path: "/getting-started",
                children: [
                    {
                        index: true,
                        lazy: lazyRouteMarkdown(
                            "content/getting-started/index",
                        ),
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
                        },
                    },
                    {
                        path: "sending-data",
                        lazy: lazyRouteMarkdown(
                            "content/getting-started/sending-data",
                        ),
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "getting-started-public-health-departments",
                        handle: {
                            isContentPage: true,
                        },
                        element: <GettingStartedPhd />,
                    },
                ],
            },
            {
                path: "/developer-resources",
                children: [
                    {
                        index: true,
                        lazy: lazyRouteMarkdown(
                            "content/developer-resources/index-page",
                        ),
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
                        },
                    },
                    {
                        path: "api",
                        children: [
                            {
                                path: "",
                                index: true,
                                lazy: lazyRouteMarkdown(
                                    "content/developer-resources/reportstream-api/ReportStreamApi",
                                ),
                                handle: {
                                    isContentPage: true,
                                },
                            },
                            {
                                path: "getting-started",
                                lazy: lazyRouteMarkdown(
                                    "content/developer-resources/reportstream-api/getting-started/GettingStarted",
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
                                            "content/developer-resources/reportstream-api/documentation/Documentation",
                                        ),
                                        index: true,
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "data-model",
                                        lazy: lazyRouteMarkdown(
                                            "content/developer-resources/reportstream-api/documentation/data-model/DataModel",
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "responses-from-reportstream",
                                        lazy: lazyRouteMarkdown(
                                            "content/developer-resources/reportstream-api/documentation/ResponsesFromReportStream",
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "sample-payloads-and-output",
                                        lazy: lazyRouteMarkdown(
                                            "content/developer-resources/reportstream-api/documentation/SamplePayloadsAndOutput",
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
                            return redirect("/developer-resources/api");
                        },
                    },
                ],
            },
            {
                path: "/managing-your-connection",
                index: true,
                lazy: lazyRouteMarkdown(
                    "content/managing-your-connection/index",
                ),
                handle: {
                    isContentPage: true,
                    isFullWidth: true,
                },
            },
            {
                path: "/support",
                children: [
                    {
                        path: "",
                        lazy: lazyRouteMarkdown("content/support/index"),
                        index: true,
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
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
                path: "manage-public-key",
                element: <ManagePublicKeyWithAuth />,
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
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
                        },
                    },
                    {
                        path: "report-details/:reportId",
                        element: <ReportDetailsWithAuth />,
                    },
                    {
                        path: "facilities-providers",
                        element: <FacilitiesProvidersWithAuth />,
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
                        },
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

export function createRouter(root: React.ReactElement) {
    appRoutes[0].element = root;
    const router = createBrowserRouter(appRoutes);
    return router;
}
