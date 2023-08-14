import { createRoot } from "react-dom/client";
import { CacheProvider } from "rest-hooks";
import { RouterProvider } from "react-router";

import { ai, withInsights } from "./TelemetryService";
import "./global.scss";
import { router } from "./AppRouter";

// Initialize the App Insights connection and React app plugin from Microsoft
// The plugin is provided in the AppInsightsProvider in AppWrapper.tsx
ai.initialize();
withInsights(console);

const root = createRoot(document.getElementById("root")!);
root.render(
    <CacheProvider>
        <RouterProvider router={router} />
    </CacheProvider>,
);
