import {
    ApplicationInsights,
    SeverityLevel,
} from "@microsoft/applicationinsights-web";
import { ReactPlugin } from "@microsoft/applicationinsights-react-js";

let reactPlugin: ReactPlugin | null = null;
let appInsights: ApplicationInsights | null = null;

export const createTelemetryService = (connectionString: string) => {
    // Runs a side effect to initialize App Insights connection and React plugin
    const initialize = () => {
        if (!connectionString) {
            console.warn("App Insights connection string not provided");
            reactPlugin = null;
            appInsights = null;
            return {
                reactPlugin,
                appInsights,
            };
        }
        // Create plugin
        reactPlugin = new ReactPlugin();
        // Create insights
        appInsights = new ApplicationInsights({
            config: {
                connectionString,
                extensions: [reactPlugin],
                loggingLevelConsole:
                    import.meta.env.NODE_ENV === "development" ? 2 : 0,
                disableFetchTracking: false,
                enableAutoRouteTracking: true,
                loggingLevelTelemetry: 2,
                maxBatchInterval: 0,
                autoTrackPageVisitTime: true,
                enableCorsCorrelation: true,
                enableRequestHeaderTracking: true,
                enableResponseHeaderTracking: true,
            },
        });
        // Initialize for use in ReportStream
        appInsights.loadAppInsights();

        return {
            reactPlugin,
            appInsights,
        };
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

export const ai = createTelemetryService(
    import.meta.env.VITE_APPLICATIONINSIGHTS_CONNECTION_STRING,
);

export function getAppInsights() {
    return appInsights;
}

export function getAppInsightsHeaders(): { [key: string]: string } {
    return {
        "x-ms-session-id": appInsights?.context.getSessionId() || "",
    };
}

export enum ConsoleMethod {
    INFO = "info",
    LOG = "log",
    WARN = "warn",
    ERROR = "error",
}

export const LOG_SEVERITY_MAP = {
    [ConsoleMethod.INFO]: SeverityLevel.Information,
    [ConsoleMethod.LOG]: SeverityLevel.Information,
    [ConsoleMethod.WARN]: SeverityLevel.Warning,
    [ConsoleMethod.ERROR]: SeverityLevel.Error,
};

export const REPORTABLE_SEVERITY_LEVELS = [ConsoleMethod.ERROR];

export function withInsights(console: Console) {
    const originalConsole = { ...console };

    Object.entries(LOG_SEVERITY_MAP).forEach((el) => {
        const [method, severityLevel] = el as [
            keyof typeof LOG_SEVERITY_MAP,
            SeverityLevel,
        ];

        console[method] = (...data: any[]) => {
            originalConsole[method](...data);

            if (REPORTABLE_SEVERITY_LEVELS.includes(method)) {
                const exception =
                    data[0] instanceof Error ? data[0] : new Error(data[0]);

                appInsights?.trackException({
                    exception,
                    id: exception.message,
                    severityLevel,
                    properties: {
                        additionalInformation: {
                            error: data[0],
                            location: window.location.href,
                            other: data.slice(1),
                        },
                    },
                });

                return;
            }

            const message =
                typeof data[0] === "string" ? data[0] : JSON.stringify(data[0]);

            appInsights?.trackEvent({
                name: `${method.toUpperCase()} - ${message}`,
                properties: {
                    severityLevel,
                    message,
                    additionalInformation:
                        data.length === 1
                            ? undefined
                            : JSON.stringify(data.slice(1)),
                },
            });
        };
    });
}
