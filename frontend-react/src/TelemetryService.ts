import { ReactPlugin } from "@microsoft/applicationinsights-react-js";
import {
    ApplicationInsights,
    SeverityLevel,
} from "@microsoft/applicationinsights-web";

export let appInsights: ApplicationInsights | undefined;
export let origConsole = window.console;
export const proxyConsole = new Proxy(
    {},
    {
        get(
            this: ProxyHandler<Console> & { __cache?: Record<string, any> },
            _target,
            p,
        ) {
            if (
                typeof p !== "string" ||
                !Object.keys(LOG_SEVERITY_MAP).includes(p)
            )
                return Reflect.get(origConsole, p);

            if (!__proxyCache[p]) {
                __proxyCache[p] = (...args: any[]) => {
                    trackConsoleEvent(p, ...args);
                };
            }
            return __proxyCache[p];
        },
    },
) as Console;

export const aiConfig: ApplicationInsights["config"] = {
    connectionString: import.meta.env
        .VITE_APPLICATIONINSIGHTS_CONNECTION_STRING,
    loggingLevelConsole: import.meta.env.NODE_ENV === "development" ? 2 : 0,
    disableFetchTracking: false,
    enableAutoRouteTracking: true,
    loggingLevelTelemetry: 2,
    maxBatchInterval: 0,
    autoTrackPageVisitTime: true,
    enableCorsCorrelation: true,
    enableRequestHeaderTracking: true,
    enableResponseHeaderTracking: true,
};

export const createTelemetryService = (
    config: ApplicationInsights["config"] = {},
    isConsoleAttached = true,
) => {
    if (!config.connectionString) {
        console.warn("App Insights connection string not provided");
        return undefined;
    }

    if (window.console === origConsole && isConsoleAttached) {
        attachAppInsightsToConsole();
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
};

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

export function trackConsoleEvent(p: string, ...args: any[]) {
    const severityLevel = LOG_SEVERITY_MAP[p as ConsoleMethod];

    (origConsole as any)[p](...args);

    if (REPORTABLE_SEVERITY_LEVELS.includes(p as ConsoleMethod)) {
        const exception =
            args[0] instanceof Error ? args[0] : new Error(args[0]);

        appInsights?.trackException({
            exception,
            id: exception.message,
            severityLevel,
            properties: {
                additionalInformation: {
                    error: args[0],
                    location: window.location.href,
                    other: args.slice(1),
                },
            },
        });

        return;
    }

    const message =
        typeof args[0] === "string" ? args[0] : JSON.stringify(args[0]);

    appInsights?.trackEvent({
        name: `${p.toUpperCase()} - ${message}`,
        properties: {
            severityLevel,
            message,
            additionalInformation:
                args.length === 1 ? undefined : JSON.stringify(args.slice(1)),
        },
    });
}

const __proxyCache = {} as any;

/**
 * Swaps out native console with proxy that redirects certain methods
 * to a new one that uses Application Insights. Returns the resetConsole
 * function.
 */
export function attachAppInsightsToConsole() {
    if (window.console !== origConsole)
        throw new Error("Console already replaced");

    window.console = proxyConsole;

    return resetConsole;
}

export function resetConsole() {
    window.console = origConsole;
}
