import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router";

import "./global.scss";

import { createRouter } from "./AppRouter";
import App from "./App";
import MainLayout from "./layouts/Main/MainLayout";
import UserAgentGate from "./shared/UserAgentGate/UserAgentGate";
import { minimumBrowsersRegex } from "./utils/SupportedBrowsers";
import { UserAgentNotSupported } from "./pages/error/UserAgentNotSupported";
import config from "./config";
import AppInsightsContextProvider from "./contexts/AppInsightsContext";
import { aiConfig, createTelemetryService } from "./TelemetryService";

const appInsights = createTelemetryService(aiConfig);
const router = createRouter(<App Layout={MainLayout} config={config} />);
const root = createRoot(document.getElementById("root")!);

/**
 * Initialize appinsights as soon as possible. Do user agent check
 * before trying to render router (prevent as much code as possible
 * from running that could be unsupported). Anything below vite's
 * minimums fail.
 */
root.render(
    <AppInsightsContextProvider value={appInsights}>
        <UserAgentGate
            regex={minimumBrowsersRegex}
            userAgent={window.navigator.userAgent}
            failElement={<UserAgentNotSupported />}
        >
            <RouterProvider router={router} />
        </UserAgentGate>
    </AppInsightsContextProvider>,
);
