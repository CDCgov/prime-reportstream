import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router";
import React from "react";

import { createRouter } from "./AppRouter";
import config from "./config";
import { aiConfig, createTelemetryService } from "./TelemetryService";
import AppInsightsContextProvider from "./contexts/AppInsights";
import { OKTA_AUTH } from "./oktaConfig";

import "./global.scss";

const appInsights = createTelemetryService(aiConfig);
const router = createRouter(
    React.lazy(async () => {
        const MainLayout = React.lazy(
            () => import("./layouts/Main/MainLayout"),
        );
        const App = (await import("./App")).default;
        return {
            default: () => (
                <App Layout={MainLayout} config={config} oktaAuth={OKTA_AUTH} />
            ),
        };
    }),
);
const root = createRoot(document.getElementById("root")!);

/**
 * Initialize appinsights as soon as possible. Do user agent check
 * before trying to render router (prevent as much code as possible
 * from running that could be unsupported). Anything below vite's
 * minimums fail.
 */
root.render(
    <AppInsightsContextProvider value={appInsights}>
        <RouterProvider router={router} />
    </AppInsightsContextProvider>,
);
