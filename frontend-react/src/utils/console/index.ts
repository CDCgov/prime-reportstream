/* eslint-disable no-console */
import {
    ApplicationInsights,
    SeverityLevel,
} from "@microsoft/applicationinsights-web";

export type ConsoleLevel =
    | "info"
    | "warn"
    | "error"
    | "debug"
    | "assert"
    | "trace";

export interface ConsoleTelemetryProperties {
    location: string;
    args: any[];
}

/**
 * Mimic of global console that emits telemetry. Log DOES NOT
 * emit telemetry. Use the most appropriate function for your
 * purposes other than log for emission. Unlike the console
 * methods, MESSAGES ARE REQUIRED.
 */
export class RSConsole {
    constructor({
        ai,
        consoleSeverityLevels,
        reportableConsoleLevels = [],
    }: {
        ai?: ApplicationInsights;
        consoleSeverityLevels: Record<ConsoleLevel, SeverityLevel>;
        reportableConsoleLevels: ConsoleLevel[];
    }) {
        this.log = console.log;
        this.ai = ai;
        this.reportableConsoleLevels = reportableConsoleLevels;
        this.consoleSeverityLevels = consoleSeverityLevels;
    }

    /**
     * Original rsconsole.log, does not emit telemetry.
     */
    log: {
        (...data: any[]): void;
        (message?: any, ...optionalParams: any[]): void;
    };
    ai?: ApplicationInsights;
    reportableConsoleLevels: ConsoleLevel[];
    consoleSeverityLevels: Record<ConsoleLevel, SeverityLevel>;

    _trace(
        {
            args: [message, ...otherArgs],
            ...otherProperties
        }: ConsoleTelemetryProperties,
        severityLevel: SeverityLevel,
    ) {
        this.ai?.trackTrace({
            message,
            severityLevel,
            properties: {
                ...otherProperties,
                args: otherArgs,
            },
        });
    }

    _error(
        {
            args: [error, ...otherArgs],
            ...otherProperties
        }: ConsoleTelemetryProperties,
        severityLevel: SeverityLevel,
    ) {
        this.ai?.trackException({
            exception: error,
            id: error.message,
            severityLevel,
            properties: {
                ...otherProperties,
                args: otherArgs,
            },
        });
    }

    _assert(
        {
            args: [value, message, ...otherArgs],
            ...otherProperties
        }: ConsoleTelemetryProperties,
        severityLevel: SeverityLevel,
    ) {
        let msg = `Assertion failed: ${message}`;
        if (!value)
            this._error(
                { ...otherProperties, args: [new Error(msg), ...otherArgs] },
                severityLevel,
            );
    }

    trackConsoleEvent(consoleLevel: ConsoleLevel, ...args: any[]) {
        console[consoleLevel](...args);
        const severityLevel = this.getSeverityLevel(consoleLevel);
        if (!this.isReportable(consoleLevel)) return;

        // original args along with any other extra properties desired
        let properties = { location: window.location.href, args };

        switch (consoleLevel) {
            case "error": {
                this._error(properties, severityLevel);
                break;
            }
            case "assert": {
                this._assert(properties, severityLevel);
                break;
            }
            default: {
                this._trace(properties, severityLevel);
                break;
            }
        }
    }

    isReportable(consoleLevel: ConsoleLevel) {
        return this.reportableConsoleLevels.includes(consoleLevel);
    }

    getSeverityLevel(consoleLevel: ConsoleLevel) {
        return this.consoleSeverityLevels[consoleLevel];
    }

    warn(...args: [message: string, ...optionalParams: any[]]) {
        return this.trackConsoleEvent("warn", ...args);
    }
    error(...args: [error: Error, ...optionalParams: any[]]) {
        return this.trackConsoleEvent("error", ...args);
    }
    debug(...args: [message: string, ...optionalParams: any[]]) {
        return this.trackConsoleEvent("debug", ...args);
    }
    assert(...args: [value: any, message: string, ...optionalParams: any[]]) {
        return this.trackConsoleEvent("assert", ...args);
    }
    info(...args: [message: string, ...optionalParams: any[]]) {
        return this.trackConsoleEvent("info", ...args);
    }
    trace(...args: [message: string, ...optionalParams: any[]]) {
        return this.trackConsoleEvent("trace", ...args);
    }
}
