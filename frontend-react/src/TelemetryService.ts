import { ReactPlugin } from "@microsoft/applicationinsights-react-js";
import {
    ApplicationInsights,
    IConfig,
    IConfiguration,
} from "@microsoft/applicationinsights-web";

export let appInsights: ApplicationInsights | undefined;

export const aiConfig = {
    connectionString: import.meta.env
        .VITE_APPLICATIONINSIGHTS_CONNECTION_STRING,
    loggingLevelConsole: import.meta.env.NODE_ENV === "development" ? 2 : 0,
    disableFetchTracking: false,
    enableAutoRouteTracking: true,
    loggingLevelTelemetry: 2,
    maxBatchInterval: 0,
    disableAjaxTracking: false,
    autoTrackPageVisitTime: true,
    enableCorsCorrelation: true,
    enableRequestHeaderTracking: true,
    enableResponseHeaderTracking: true,
} as const satisfies IConfiguration & IConfig;

/**
 * Handles maintaining a singular app insights object. Returns undefined
 * if vital config options are missing. If called multiple times, it will
 * ensure the current app insights object is torn down before being replaced.
 */
export function createTelemetryService(config: IConfiguration & IConfig) {
    if (!config.connectionString) {
        return undefined;
    }

    if (appInsights?.core?.isInitialized?.()) appInsights?.core?.unload();
    // Create insights
    appInsights = new ApplicationInsights({
        config: {
            ...config,
            extensions: [new ReactPlugin()],
        },
    });
    // Initialize for use in ReportStream
    appInsights.loadAppInsights();

    return appInsights;
}
