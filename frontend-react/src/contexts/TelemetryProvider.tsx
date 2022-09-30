import React, {
    PropsWithChildren,
    useEffect,
    createContext,
    useRef,
} from "react";
import {
    ApplicationInsights,
    SeverityLevel,
} from "@microsoft/applicationinsights-web";
import { ReactPlugin } from "@microsoft/applicationinsights-react-js";

import config from "../config";

interface ITelemetryContext {
    appInsights: ApplicationInsights | undefined;
}
const { APP_ENV, APPLICATIONINSIGHTS_CONNECTION_STRING } = config;

export const TelemetryContext = createContext<ITelemetryContext>({
    appInsights: undefined,
});

const logSeverityMap = {
    log: SeverityLevel.Information,
    warn: SeverityLevel.Warning,
    error: SeverityLevel.Error,
    info: SeverityLevel.Information,
} as const;

export function withInsights(
    console: Console,
    appInsights: ApplicationInsights
) {
    const originalConsole = { ...console };

    Object.entries(logSeverityMap).forEach((el) => {
        const [method, severityLevel] = el as [
            keyof typeof logSeverityMap,
            SeverityLevel
        ];

        console[method] = (...data: any[]) => {
            originalConsole[method](data);

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

// initializes Azure AppInsights, and provides an appInsights object to the application
// though it is unclear whether the application will actually need it
const TelemetryProvider = ({ children }: PropsWithChildren<{}>) => {
    const appInsights = useRef<ApplicationInsights | undefined>();
    useEffect(() => {
        appInsights.current = initializeTelemetry();
        if (appInsights.current) {
            withInsights(console, appInsights.current);
        }
    }, []);

    return (
        <TelemetryContext.Provider value={{ appInsights: appInsights.current }}>
            {children}
        </TelemetryContext.Provider>
    );
};

const initializeTelemetry = () => {
    if (!APPLICATIONINSIGHTS_CONNECTION_STRING) {
        console.warn("Instrumentation key not provided");
        return;
    }

    const reactPlugin = new ReactPlugin();

    const appInsights = new ApplicationInsights({
        config: {
            instrumentationKey: APPLICATIONINSIGHTS_CONNECTION_STRING,
            extensions: [reactPlugin],
            loggingLevelConsole: APP_ENV === "development" ? 2 : 0,
            disableFetchTracking: false,
            enableAutoRouteTracking: true,
            loggingLevelTelemetry: 2,
            maxBatchInterval: 0,
        },
    });

    appInsights.loadAppInsights();
    return appInsights;
};

export default TelemetryProvider;
