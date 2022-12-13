import { ApplicationInsights } from "@microsoft/applicationinsights-web";
import { ReactPlugin } from "@microsoft/applicationinsights-react-js";

let reactPlugin: ReactPlugin | null = null;
let appInsights: ApplicationInsights | null = null;

const createTelemetryService = () => {
    // Runs a side effect to initialize App Insights connection and React plugin
    const initialize = () => {
        const connectionString =
            process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING;
        if (!connectionString) {
            console.warn("App Insights connection string not provided");
            return;
        }
        // Create plugin
        reactPlugin = new ReactPlugin();
        // Create insights
        appInsights = new ApplicationInsights({
            config: {
                connectionString,
                extensions: [reactPlugin],
                loggingLevelConsole:
                    process.env.NODE_ENV === "development" ? 2 : 0,
                disableFetchTracking: false,
                enableAutoRouteTracking: true,
                loggingLevelTelemetry: 2,
                maxBatchInterval: 0,
            },
        });
        // Initialize for use in ReportStream
        appInsights.loadAppInsights();
    };

    return {
        // Use for React integration
        reactPlugin,
        // Use for insight tracking outside of React Hooks & Components
        appInsights,
        // Use to initialize App Insights instance
        initialize,
    };
};

export const ai = createTelemetryService();

export function getAppInsightsHeaders(): { [key: string]: string } {
    return {
        "x-ms-session-id": getAppInsightsSessionId(),
    };
}

function getAppInsightsSessionId(): string {
    return appInsights?.context.getSessionId() || "";
}
