import React from "react";
import { RouteObject, redirect } from "react-router";
import { createBrowserRouter } from "react-router-dom";

import { SenderType } from "./utils/DataDashboardUtils";
import { lazyRouteMarkdown } from "./utils/LazyRouteMarkdown";

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
const TermsOfServiceForm = React.lazy(
    () => import("./pages/tos-sign/TermsOfServiceForm"),
);
const LoginCallback = React.lazy(
    () => import("./shared/LoginCallback/LoginCallback"),
);
const LogoutCallback = React.lazy(
    () => import("./shared/LogoutCallback/LogoutCallback"),
);
const DataDownloadGuideIa = React.lazy(
    () => import("./pages/DataDownloadGuide"),
);
const GettingStartedPhd = React.lazy(() => import("./pages/GettingStartedPhd"));
const Login = React.lazy(() => import("./pages/Login"));
const FileHandler = React.lazy(
    () => import("./components/FileHandlers/FileHandler"),
);
const ErrorNoPage = React.lazy(
    () => import("./pages/error/legacy-content/ErrorNoPage"),
);

/* Auth Pages */
const UploadWithAuth = React.lazy(() => import("./pages/Upload"));
const FeatureFlagUIWithAuth = React.lazy(
    () => import("./pages/misc/FeatureFlags"),
);
const SubmissionDetailsWithAuth = React.lazy(
    () => import("./pages/submissions/SubmissionDetails"),
);
const SubmissionsWithAuth = React.lazy(
    () => import("./pages/submissions/Submissions"),
);
const AdminMainWithAuth = React.lazy(() => import("./pages/admin/AdminMain"));
const AdminOrgNewWithAuth = React.lazy(
    () => import("./pages/admin/AdminOrgNew"),
);
const AdminOrgEditWithAuth = React.lazy(
    () => import("./pages/admin/AdminOrgEdit"),
);
const EditSenderSettingsWithAuth = React.lazy(
    () => import("./components/Admin/EditSenderSettings"),
);
const AdminLMFWithAuth = React.lazy(
    () => import("./pages/admin/AdminLastMileFailures"),
);
const AdminMessageTrackerWithAuth = React.lazy(
    () => import("./pages/admin/AdminMessageTracker"),
);
const AdminReceiverDashWithAuth = React.lazy(
    () => import("./pages/admin/AdminReceiverDashPage"),
);
const DeliveryDetailWithAuth = React.lazy(
    () => import("./pages/deliveries/details/DeliveryDetail"),
);
const ValueSetsDetailWithAuth = React.lazy(
    () => import("./pages/admin/value-set-editor/ValueSetsDetail"),
);
const ValueSetsIndexWithAuth = React.lazy(
    () => import("./pages/admin/value-set-editor/ValueSetsIndex"),
);
const DeliveriesWithAuth = React.lazy(
    () => import("./pages/deliveries/Deliveries"),
);
const EditReceiverSettingsWithAuth = React.lazy(
    () => import("./components/Admin/EditReceiverSettings"),
);
const AdminRevHistoryWithAuth = React.lazy(
    () => import("./pages/admin/AdminRevHistory"),
);
const MessageDetailsWithAuth = React.lazy(
    () => import("./components/MessageTracker/MessageDetails"),
);
const ManagePublicKeyWithAuth = React.lazy(
    () => import("./components/ManagePublicKey/ManagePublicKey"),
);
const DataDashboardWithAuth = React.lazy(
    () => import("./pages/data-dashboard/DataDashboard"),
);
const ReportDetailsWithAuth = React.lazy(
    () => import("./components/DataDashboard/ReportDetails/ReportDetails"),
);
const FacilitiesProvidersWithAuth = React.lazy(
    () =>
        import(
            "./components/DataDashboard/FacilitiesProviders/FacilitiesProviders"
        ),
);
const FacilityProviderSubmitterDetailsWithAuth = React.lazy(
    () =>
        import(
            "./components/DataDashboard/FacilityProviderSubmitterDetails/FacilityProviderSubmitterDetails"
        ),
);
const NewSettingWithAuth = React.lazy(
    () => import("./components/Admin/NewSetting"),
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
                    //isDAP: true,
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
                        element: <ReferHealthcareOrganizations />,
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
                path: "/managing-your-connection",
                index: true,
                element: <ManagingYourConnectionIndex />,
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

export function createRouter(
    Component: React.LazyExoticComponent<React.ComponentType>,
) {
    appRoutes[0].element = <Component />;
    const router = createBrowserRouter(appRoutes);
    return router;
}
