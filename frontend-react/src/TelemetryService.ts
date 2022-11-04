import { ApplicationInsights } from "@microsoft/applicationinsights-web";
import { ReactPlugin } from "@microsoft/applicationinsights-react-js";

let reactPlugin: ReactPlugin | null = null;
let appInsights: ApplicationInsights | null = null;

const createTelemetryService = () => {
    // Runs a side effect to load App Insights and the React plugin
    const initialize = () => {
        const connectionString =
            process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING;

        if (!connectionString) {
            if (process.env.NODE_ENV !== "test") {
                console.warn("App Insights connection string not provided");
            }
            return;
        }

        reactPlugin = new ReactPlugin();

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

        appInsights.loadAppInsights();
    };

    return {
        reactPlugin,
        appInsights,
        initialize,
    };
};

export const ai = createTelemetryService();
export const getAppInsights = () => appInsights;
