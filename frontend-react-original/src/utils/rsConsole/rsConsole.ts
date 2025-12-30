/* eslint-disable no-console */
import type { SeverityLevel } from "@microsoft/applicationinsights-web";
import type { ReactPlugin } from "../TelemetryService/TelemetryService";

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
    env: string;
    protected _ai: ReactPlugin;
    reportableLevels: ConsoleLevel[];
    severityLevels: Record<ConsoleLevel, SeverityLevel>;

    constructor({
        ai,
        severityLevels,
        reportableLevels = [],
        env = "production",
    }: {
        ai: ReactPlugin;
        severityLevels: Record<ConsoleLevel, SeverityLevel>;
        reportableLevels: ConsoleLevel[];
        env?: string;
    }) {
        this.log = console.log;
        this._ai = ai;
        this.reportableLevels = reportableLevels;
        this.severityLevels = severityLevels;
        this.env = env;
    }

    get ai() {
        return this._ai;
    }

    /**
     * Original rsConsole.log, does not emit telemetry.
     */
    log: {
        (...data: any[]): void;
        (message?: any, ...optionalParams: any[]): void;
    };

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
        const msg = `Assertion failed: ${message}`;
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
        const properties = { location: window.location.href, args };

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
        return this.reportableLevels.includes(consoleLevel);
    }

    getSeverityLevel(consoleLevel: ConsoleLevel) {
        return this.severityLevels[consoleLevel];
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
    dev(...data: any[]): void;
    dev(message?: any, ...optionalParams: any[]): void;
    dev(...args: any[]) {
        if (this.env === "development") console.log(...args);
    }

    updateAi(ai: ReactPlugin) {
        this._ai = ai;
    }
}
