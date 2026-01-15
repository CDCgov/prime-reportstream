import { lazy } from "react";
import { Outlet, redirect, RouteObject } from "react-router";

import RSErrorBoundary from "./components/RSErrorBoundary/RSErrorBoundary";
import { RequireGate } from "./shared/RequireGate/RequireGate";
import { SenderType } from "./utils/DataDashboardUtils";
import { lazyRouteMarkdown } from "./utils/LazyRouteMarkdown";
import { PERMISSIONS } from "./utils/UsefulTypes";

/* Content Pages */
const Home = lazy(lazyRouteMarkdown(() => import("./content/home/index.mdx")));
const About = lazy(lazyRouteMarkdown(() => import("./content/about/index.mdx")));
const OurNetwork = lazy(lazyRouteMarkdown(() => import("./content/about/our-network.mdx")));
const Roadmap = lazy(lazyRouteMarkdown(() => import("./content/about/roadmap.mdx")));
const News = lazy(lazyRouteMarkdown(() => import("./content/about/news.mdx")));
const Security = lazy(lazyRouteMarkdown(() => import("./content/about/security.mdx")));
const ReleaseNotes = lazy(lazyRouteMarkdown(() => import("./content/about/release-notes.mdx")));
const CaseStudies = lazy(lazyRouteMarkdown(() => import("./content/about/case-studies.mdx")));
const ReferHealthcareOrganizations = lazy(
    lazyRouteMarkdown(() => import("./content/managing-your-connection/refer-healthcare-organizations.mdx")),
);

const GettingStartedSendingData = lazy(lazyRouteMarkdown(() => import("./content/getting-started/sending-data.mdx")));
const GettingStartedReceivingData = lazy(
    lazyRouteMarkdown(() => import("./content/getting-started/receiving-data.mdx")),
);

const ApiOnboardingGuide = lazy(
    lazyRouteMarkdown(
        () => import("./content/developer-resources/reportstream-api/api-onboarding-guide/ApiOnboardingGuide.mdx"),
    ),
);
const DeveloperResourcesIndex = lazy(lazyRouteMarkdown(() => import("./content/developer-resources/index-page.mdx")));

const ReportStreamApiDocumentation = lazy(
    lazyRouteMarkdown(() => import("./content/developer-resources/reportstream-api/documentation/Documentation.mdx")),
);
const ManagingYourConnectionIndex = lazy(
    lazyRouteMarkdown(() => import("./content/managing-your-connection/index.mdx")),
);
const SupportIndex = lazy(lazyRouteMarkdown(() => import("./content/support/index.mdx")));

const SubmissionStatusAndErrors = lazy(
    lazyRouteMarkdown(
        () => import("./content/developer-resources/reportstream-api/documentation/SubmissionStatusAndErrors.mdx"),
    ),
);

/* Public Pages */
const TermsOfService = lazy(() => import("./pages/TermsOfService"));

const LoginCallback = lazy(() => import("./shared/LoginCallback/LoginCallback"));
const LogoutCallback = lazy(() => import("./shared/LogoutCallback/LogoutCallback"));
const Login = lazy(() => import("./pages/Login"));

const ErrorNoPage = lazy(() => import("./pages/error/legacy-content/ErrorNoPage"));

/* Auth Pages */
const AdminMessageTestingPage = lazy(() => import("./pages/admin/AdminMessageTestingPage/AdminMessageTestingPage"));
const FeatureFlagsPage = lazy(() => import("./pages/misc/FeatureFlags"));
const SubmissionDetailsPage = lazy(() => import("./pages/submissions/SubmissionDetails"));
const SubmissionsPage = lazy(() => import("./pages/submissions/Submissions"));
const AdminMainPage = lazy(() => import("./pages/admin/AdminMain"));
const AdminOrgNewPage = lazy(() => import("./pages/admin/AdminOrgNew"));
const AdminOrgEditPage = lazy(() => import("./pages/admin/AdminOrgEdit"));
const EditSenderSettingsPage = lazy(() => import("./components/Admin/EditSenderSettings"));
const AdminLMFPage = lazy(() => import("./pages/admin/AdminLastMileFailures"));
const AdminMessageTrackerPage = lazy(() => import("./pages/admin/AdminMessageTracker"));
const AdminReceiverDashPage = lazy(
    () => import("./pages/admin/receiver-dashboard/AdminReceiverDashboardPage/AdminReceiverDashboardPage"),
);
const DeliveryDetailPage = lazy(() => import("./pages/deliveries/details/DeliveryDetail"));
const ValueSetsDetailPage = lazy(() => import("./pages/admin/value-set-editor/ValueSetsDetail"));
const ValueSetsIndexPage = lazy(() => import("./pages/admin/value-set-editor/ValueSetsIndex"));
const DeliveriesPage = lazy(() => import("./pages/deliveries/Deliveries"));
const EditReceiverSettingsPage = lazy(() => import("./components/Admin/EditReceiverSettings"));
const AdminRevHistoryPage = lazy(() => import("./pages/admin/AdminRevHistory"));
const MessageDetailsPage = lazy(() => import("./components/MessageTracker/MessageDetails"));
const ManagePublicKeyPage = lazy(() => import("./components/ManagePublicKey/ManagePublicKey"));
const DataDashboardPage = lazy(() => import("./pages/data-dashboard/DataDashboard"));
const ReportDetailsPage = lazy(() => import("./components/DataDashboard/ReportDetails/ReportDetails"));
const FacilitiesProvidersPage = lazy(
    () => import("./components/DataDashboard/FacilitiesProviders/FacilitiesProviders"),
);
const FacilityProviderSubmitterDetailsPage = lazy(
    () => import("./components/DataDashboard/FacilityProviderSubmitterDetails/FacilityProviderSubmitterDetails"),
);
const NewSettingPage = lazy(() => import("./components/Admin/NewSetting"));
const CodeMappingPage = lazy(() => import("./pages/onboarding/CodeMappingPage"));

const MainLayout = lazy(() => import("./layouts/Main/MainLayout"));

export const appRoutes: RouteObject[] = [
    /* Public Site */
    {
        path: "/",
        Component: MainLayout,
        ErrorBoundary: RSErrorBoundary,
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
                        path: "roadmap",
                        element: <Roadmap />,
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
                        path: "api-onboarding-guide",
                        index: true,
                        element: <ApiOnboardingGuide />,
                        handle: {
                            isContentPage: true,
                        },
                    },
                    {
                        path: "documentation",
                        children: [
                            {
                                path: "",
                                element: <ReportStreamApiDocumentation />,
                                index: true,
                                handle: {
                                    isContentPage: true,
                                },
                            },
                            {
                                path: "submission-status-and-errors",
                                element: <SubmissionStatusAndErrors />,
                                handle: {
                                    isContentPage: true,
                                },
                            },
                        ],
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
                loader: () => {
                    return redirect("/developer-resources/api-onboarding-guide");
                },
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
                        element: <FacilityProviderSubmitterDetailsPage senderType={SenderType.FACILITY} />,
                    },
                    {
                        path: "provider/:senderId",
                        element: <FacilityProviderSubmitterDetailsPage senderType={SenderType.PROVIDER} />,
                    },
                    {
                        path: "submitter/:senderId",
                        element: <FacilityProviderSubmitterDetailsPage senderType={SenderType.SUBMITTER} />,
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
                        path: "settings",
                        element: <AdminMainPage />,
                    },
                    {
                        path: "new/org",
                        element: <AdminOrgNewPage />,
                    },
                    {
                        path: "orgsettings/org/:orgname",
                        element: <AdminOrgEditPage />,
                    },
                    {
                        path: "orgreceiversettings/org/:orgname/receiver/:receivername/action/:action",
                        element: <EditReceiverSettingsPage />,
                    },
                    {
                        path: "orgreceiversettings/org/:orgname/receiver/:receivername/action/:action/message-testing",
                        element: <AdminMessageTestingPage />,
                    },
                    {
                        path: "orgsendersettings/org/:orgname/sender/:sendername/action/:action",
                        element: <EditSenderSettingsPage />,
                    },
                    {
                        path: "orgnewsetting/org/:orgname/settingtype/:settingtype",
                        element: <NewSettingPage />,
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
                    {
                        path: "revisionhistory/org/:org/settingtype/:settingType",
                        element: <AdminRevHistoryPage />,
                    },
                ],
            },
            {
                path: "/message-details/:id",
                element: (
                    <RequireGate auth>
                        <MessageDetailsPage />
                    </RequireGate>
                ),
            },
            {
                path: "onboarding",
                element: (
                    <RequireGate auth>
                        <Outlet />
                    </RequireGate>
                ),
                children: [
                    {
                        path: "code-mapping",
                        element: <CodeMappingPage />,
                    },
                ],
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
