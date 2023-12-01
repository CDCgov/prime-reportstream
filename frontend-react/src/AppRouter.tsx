import React from "react";
import { Outlet, RouteObject, redirect } from "react-router";
import { createBrowserRouter } from "react-router-dom";

import { SenderType } from "./utils/DataDashboardUtils";
import { lazyRouteMarkdown } from "./utils/LazyRouteMarkdown";
import { RequireGate } from "./shared/RequireGate/RequireGate";
import { PERMISSIONS } from "./utils/UsefulTypes";

/* Content Pages */
const Home = React.lazy(
    lazyRouteMarkdown(() => import("./content/home/index.mdx")),
);
const About = React.lazy(
    lazyRouteMarkdown(() => import("./content/about/index.mdx")),
);
const OurNetwork = React.lazy(
    lazyRouteMarkdown(() => import("./content/about/our-network.mdx")),
);
const News = React.lazy(
    lazyRouteMarkdown(() => import("./content/about/news.mdx")),
);
const Security = React.lazy(
    lazyRouteMarkdown(() => import("./content/about/security.mdx")),
);
const ReleaseNotes = React.lazy(
    lazyRouteMarkdown(() => import("./content/about/release-notes.mdx")),
);
const CaseStudies = React.lazy(
    lazyRouteMarkdown(() => import("./content/about/case-studies.mdx")),
);
const ReferHealthcareOrganizations = React.lazy(
    lazyRouteMarkdown(
        () =>
            import(
                "./content/managing-your-connection/refer-healthcare-organizations.mdx"
            ),
    ),
);
const GettingStartedIndex = React.lazy(
    lazyRouteMarkdown(() => import("./content/getting-started/index.mdx")),
);
const GettingStartedSendingData = React.lazy(
    lazyRouteMarkdown(
        () => import("./content/getting-started/sending-data.mdx"),
    ),
);
const GettingStartedReceivingData = React.lazy(
    lazyRouteMarkdown(
        () => import("./content/getting-started/receiving-data.mdx"),
    ),
);
const ReportStreamApiIndex = React.lazy(
    lazyRouteMarkdown(
        () =>
            import(
                "./content/developer-resources/reportstream-api/ReportStreamApi.mdx"
            ),
    ),
);
const DeveloperResourcesIndex = React.lazy(
    lazyRouteMarkdown(
        () => import("./content/developer-resources/index-page.mdx"),
    ),
);
const ReportStreamApiGettingStarted = React.lazy(
    lazyRouteMarkdown(
        () =>
            import(
                "./content/developer-resources/reportstream-api/getting-started/GettingStarted.mdx"
            ),
    ),
);
const ReportStreamApiDocumentation = React.lazy(
    lazyRouteMarkdown(
        () =>
            import(
                "./content/developer-resources/reportstream-api/documentation/Documentation.mdx"
            ),
    ),
);
const ReportStreamApiDocumentationDataModel = React.lazy(
    lazyRouteMarkdown(
        () =>
            import(
                "./content/developer-resources/reportstream-api/documentation/data-model/DataModel.mdx"
            ),
    ),
);
const ReportStreamApiDocumentationResponses = React.lazy(
    lazyRouteMarkdown(
        () =>
            import(
                "./content/developer-resources/reportstream-api/documentation/ResponsesFromReportStream.mdx"
            ),
    ),
);
const ManagingYourConnectionIndex = React.lazy(
    lazyRouteMarkdown(
        () => import("./content/managing-your-connection/index.mdx"),
    ),
);
const SupportIndex = React.lazy(
    lazyRouteMarkdown(() => import("./content/support/index.mdx")),
);
const ReportStreamApiDocumentationPayloads = React.lazy(
    lazyRouteMarkdown(
        () =>
            import(
                "./content/developer-resources/reportstream-api/documentation/SamplePayloadsAndOutput.mdx"
            ),
    ),
);

/* Public Pages */
const TermsOfService = React.lazy(() => import("./pages/TermsOfService"));
const LoginCallback = React.lazy(
    () => import("./shared/LoginCallback/LoginCallback"),
);
const LogoutCallback = React.lazy(
    () => import("./shared/LogoutCallback/LogoutCallback"),
);
const Login = React.lazy(() => import("./pages/Login"));
const FileHandler = React.lazy(
    () => import("./components/FileHandlers/FileHandler"),
);
const ErrorNoPage = React.lazy(
    () => import("./pages/error/legacy-content/ErrorNoPage"),
);

/* Auth Pages */
const FeatureFlagsPage = React.lazy(() => import("./pages/misc/FeatureFlags"));
const SubmissionDetailsPage = React.lazy(
    () => import("./pages/submissions/SubmissionDetails"),
);
const SubmissionsPage = React.lazy(
    () => import("./pages/submissions/Submissions"),
);
const AdminMainPage = React.lazy(() => import("./pages/admin/AdminMain"));
const AdminLMFPage = React.lazy(
    () => import("./pages/admin/AdminLastMileFailures"),
);
const AdminMessageTrackerPage = React.lazy(
    () => import("./pages/admin/AdminMessageTracker"),
);
const AdminReceiverDashPage = React.lazy(
    () => import("./pages/admin/AdminReceiverDashPage"),
);
const DeliveryDetailPage = React.lazy(
    () => import("./pages/deliveries/details/DeliveryDetail"),
);
const ValueSetsDetailPage = React.lazy(
    () => import("./pages/admin/value-set-editor/ValueSetsDetail"),
);
const ValueSetsIndexPage = React.lazy(
    () => import("./pages/admin/value-set-editor/ValueSetsIndex"),
);
const DeliveriesPage = React.lazy(
    () => import("./pages/deliveries/Deliveries"),
);
const AdminSettingFormPage = React.lazy(
    () => import("./pages/admin/AdminSettingForm"),
);
const AdminRevHistoryPage = React.lazy(
    () => import("./pages/admin/AdminRevHistory"),
);
const MessageDetailsPage = React.lazy(
    () => import("./components/MessageTracker/MessageDetails"),
);
const ManagePublicKeyPage = React.lazy(
    () => import("./components/ManagePublicKey/ManagePublicKey"),
);
const DataDashboardPage = React.lazy(
    () => import("./pages/data-dashboard/DataDashboard"),
);
const ReportDetailsPage = React.lazy(
    () => import("./components/DataDashboard/ReportDetails/ReportDetails"),
);
const FacilitiesProvidersPage = React.lazy(
    () =>
        import(
            "./components/DataDashboard/FacilitiesProviders/FacilitiesProviders"
        ),
);
const FacilityProviderSubmitterDetailsPage = React.lazy(
    () =>
        import(
            "./components/DataDashboard/FacilityProviderSubmitterDetails/FacilityProviderSubmitterDetails"
        ),
);

export const appRoutes: RouteObject[] = [
    /* Public Site */
    {
        path: "/",
        children: [
            {
                path: "",
                index: true,
                element: <Home />,
                handle: {
                    isContentPage: true,
                    isFullWidth: true,
                },
            },
            {
                path: "terms-of-service",
                element: <TermsOfService />,
                handle: {
                    isContentPage: true,
                },
            },
            {
                path: "about",
                children: [
                    {
                        index: true,
                        element: <About />,
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
                        },
                    },
                    {
                        path: "our-network",
                        element: <OurNetwork />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "news",
                        element: <News />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "security",
                        element: <Security />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "release-notes",
                        element: <ReleaseNotes />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "case-studies",
                        element: <CaseStudies />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                ],
            },
            {
                path: "login",
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
                path: "logout/callback",
                element: <LogoutCallback />,
            },
            {
                path: "managing-your-connection",
                children: [
                    {
                        path: "refer-healthcare-organizations",
                        handle: {
                            isContentPage: true,
                        },
                        element: <ReferHealthcareOrganizations />,
                    },
                ],
            },
            {
                path: "getting-started",
                children: [
                    {
                        index: true,
                        element: <GettingStartedIndex />,
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
                        },
                    },
                    {
                        path: "sending-data",
                        element: <GettingStartedSendingData />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "receiving-data",
                        handle: {
                            isContentPage: true,
                        },
                        element: <GettingStartedReceivingData />,
                    },
                ],
            },
            {
                path: "/developer-resources",
                children: [
                    {
                        index: true,
                        element: <DeveloperResourcesIndex />,
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
                                element: <ReportStreamApiIndex />,
                                handle: {
                                    isContentPage: true,
                                },
                            },
                            {
                                path: "getting-started",
                                element: <ReportStreamApiGettingStarted />,
                                handle: {
                                    isContentPage: true,
                                },
                            },
                            {
                                path: "documentation",
                                children: [
                                    {
                                        path: "",
                                        element: (
                                            <ReportStreamApiDocumentation />
                                        ),
                                        index: true,
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "data-model",
                                        element: (
                                            <ReportStreamApiDocumentationDataModel />
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "responses-from-reportstream",
                                        element: (
                                            <ReportStreamApiDocumentationResponses />
                                        ),
                                        handle: {
                                            isContentPage: true,
                                        },
                                    },
                                    {
                                        path: "sample-payloads-and-output",
                                        element: (
                                            <ReportStreamApiDocumentationPayloads />
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
                path: "managing-your-connection",
                index: true,
                element: <ManagingYourConnectionIndex />,
                handle: {
                    isContentPage: true,
                    isFullWidth: true,
                },
            },
            {
                path: "support",
                children: [
                    {
                        path: "",
                        element: <SupportIndex />,
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
                path: "daily-data",
                element: (
                    <RequireGate auth>
                        <DeliveriesPage />
                    </RequireGate>
                ),
            },
            {
                path: "manage-public-key",
                element: (
                    <RequireGate auth>
                        <ManagePublicKeyPage />
                    </RequireGate>
                ),
            },
            {
                path: "report-details",
                element: (
                    <RequireGate auth>
                        <Outlet />
                    </RequireGate>
                ),
                children: [
                    {
                        path: ":reportId",
                        element: <DeliveryDetailPage />,
                    },
                ],
            },
            {
                path: "submissions",
                element: (
                    <RequireGate auth>
                        <Outlet />
                    </RequireGate>
                ),
                children: [
                    {
                        path: "",
                        index: true,
                        element: <SubmissionsPage />,
                    },
                    {
                        path: ":actionId",
                        element: <SubmissionDetailsPage />,
                    },
                ],
            },
            /* Data Dashboard pages */
            {
                path: "data-dashboard",
                element: (
                    <RequireGate auth>
                        <Outlet />
                    </RequireGate>
                ),
                children: [
                    {
                        path: "",
                        element: <DataDashboardPage />,
                        index: true,
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
                        },
                    },
                    {
                        path: "report-details/:reportId",
                        element: <ReportDetailsPage />,
                    },
                    {
                        path: "facilities-providers",
                        element: <FacilitiesProvidersPage />,
                        handle: {
                            isContentPage: true,
                            isFullWidth: true,
                        },
                    },
                    {
                        path: "facility/:senderId",
                        element: (
                            <FacilityProviderSubmitterDetailsPage
                                senderType={SenderType.FACILITY}
                            />
                        ),
                    },
                    {
                        path: "provider/:senderId",
                        element: (
                            <FacilityProviderSubmitterDetailsPage
                                senderType={SenderType.PROVIDER}
                            />
                        ),
                    },
                    {
                        path: "submitter/:senderId",
                        element: (
                            <FacilityProviderSubmitterDetailsPage
                                senderType={SenderType.SUBMITTER}
                            />
                        ),
                    },
                ],
            },
            /* Admin pages */
            {
                path: "admin",
                element: (
                    <RequireGate auth={PERMISSIONS.PRIME_ADMIN}>
                        <Outlet />
                    </RequireGate>
                ),
                children: [
                    {
                        path: "organizations",
                        children: [
                            {
                                index: true,
                                element: <AdminMainPage />,
                            },
                            {
                                path: "new",
                                children: [
                                    {
                                        index: true,
                                        element: (
                                            <AdminSettingFormPage
                                                entityType="Organization"
                                                mode="new"
                                            />
                                        ),
                                    },
                                    {
                                        path: ":entityId",
                                        element: (
                                            <AdminSettingFormPage
                                                entityType="Organization"
                                                mode="new"
                                            />
                                        ),
                                    },
                                ],
                            },
                            {
                                path: ":orgId",
                                children: [
                                    {
                                        path: "edit",
                                        element: (
                                            <AdminSettingFormPage
                                                entityType="Organization"
                                                mode="edit"
                                            />
                                        ),
                                    },
                                    {
                                        path: "history",
                                        element: (
                                            <AdminRevHistoryPage settingType="organization" />
                                        ),
                                    },
                                    {
                                        path: "receiver",
                                        children: [
                                            {
                                                path: "new",
                                                children: [
                                                    {
                                                        index: true,
                                                        element: (
                                                            <AdminSettingFormPage
                                                                entityType="Receiver"
                                                                mode="new"
                                                            />
                                                        ),
                                                    },
                                                    {
                                                        path: ":entityId",
                                                        element: (
                                                            <AdminSettingFormPage
                                                                entityType="Receiver"
                                                                mode="new"
                                                            />
                                                        ),
                                                    },
                                                ],
                                            },
                                            {
                                                path: ":entityId",
                                                children: [
                                                    {
                                                        path: "edit",
                                                        element: (
                                                            <AdminSettingFormPage
                                                                entityType="Receiver"
                                                                mode="edit"
                                                            />
                                                        ),
                                                    },
                                                    {
                                                        path: "history",
                                                        element: (
                                                            <AdminRevHistoryPage settingType="receiver" />
                                                        ),
                                                    },
                                                ],
                                            },
                                        ],
                                    },
                                    {
                                        path: "sender",
                                        children: [
                                            {
                                                path: "new",
                                                children: [
                                                    {
                                                        index: true,
                                                        element: (
                                                            <AdminSettingFormPage
                                                                entityType="Sender"
                                                                mode="new"
                                                            />
                                                        ),
                                                    },
                                                    {
                                                        path: ":entityId",
                                                        element: (
                                                            <AdminSettingFormPage
                                                                entityType="Sender"
                                                                mode="new"
                                                            />
                                                        ),
                                                    },
                                                ],
                                            },
                                            {
                                                path: ":entityId",
                                                children: [
                                                    {
                                                        path: "edit",
                                                        element: (
                                                            <AdminSettingFormPage
                                                                entityType="Sender"
                                                                mode="edit"
                                                            />
                                                        ),
                                                    },
                                                    {
                                                        path: "history",
                                                        element: (
                                                            <AdminRevHistoryPage settingType="sender" />
                                                        ),
                                                    },
                                                ],
                                            },
                                        ],
                                    },
                                ],
                            },
                        ],
                    },
                    {
                        path: "lastmile",
                        element: <AdminLMFPage />,
                    },
                    {
                        path: "send-dash",
                        element: <AdminReceiverDashPage />,
                    },
                    {
                        path: "features",
                        element: <FeatureFlagsPage />,
                    },
                    {
                        path: "message-tracker",
                        element: <AdminMessageTrackerPage />,
                    },
                    {
                        path: "value-sets",
                        children: [
                            {
                                path: "",
                                index: true,
                                element: <ValueSetsIndexPage />,
                            },
                            {
                                path: ":valueSetName",
                                element: <ValueSetsDetailPage />,
                            },
                        ],
                    },
                ],
            },
            {
                path: "/message-details/:id",
                element: <MessageDetailsPage />,
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

export function createRouter(
    Component: React.LazyExoticComponent<React.ComponentType>,
) {
    appRoutes[0].element = <Component />;
    const router = createBrowserRouter(appRoutes);
    return router;
}
