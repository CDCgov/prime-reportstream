import {
    ApplicationInsights,
    SeverityLevel,
} from "@microsoft/applicationinsights-web";
import { ReactPlugin } from "@microsoft/applicationinsights-react-js";

import { getSessionMembershipState } from "./utils/SessionStorageTools";

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

        // Add active membership information to all tracking events
        appInsights.addTelemetryInitializer((envelope) => {
            const { activeMembership } = getSessionMembershipState() || {};
            if (activeMembership) {
                (envelope.data as any).activeMembership = activeMembership;
            }
        });
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

export function getAppInsights() {
    return appInsights;
}

export function getAppInsightsHeaders(): { [key: string]: string } {
    return {
        "x-ms-session-id": getAppInsightsSessionId(),
    };
}

function getAppInsightsSessionId(): string {
    return appInsights?.context.getSessionId() || "";
}

const logSeverityMap = {
    log: SeverityLevel.Information,
    warn: SeverityLevel.Warning,
    error: SeverityLevel.Error,
    info: SeverityLevel.Information,
} as const;

export function withInsights(console: Console) {
    const originalConsole = { ...console };

    Object.entries(logSeverityMap).forEach((el) => {
        const [method, severityLevel] = el as [
            keyof typeof logSeverityMap,
            SeverityLevel
        ];

        console[method] = (...data: any[]) => {
            originalConsole[method](...data);

            if (method === "error" || method === "warn") {
                const exception =
                    data[0] instanceof Error ? data[0] : undefined;
                const id = (() => {
                    if (exception) {
                        return exception.message;
                    }
                    if (typeof data[0] === "string") {
                        return data[0];
                    }
                    return JSON.stringify(data[0]);
                })();

                appInsights?.trackException({
                    exception,
                    id,
                    severityLevel,
                    properties: {
                        additionalInformation:
                            data.length === 1
                                ? undefined
                                : JSON.stringify(data.slice(1)),
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
