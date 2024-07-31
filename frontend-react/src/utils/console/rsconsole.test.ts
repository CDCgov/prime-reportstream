import {
    IExceptionTelemetry,
    ITraceTelemetry,
    SeverityLevel,
} from "@microsoft/applicationinsights-web";

import { mockAppInsights } from "../../__mocks__/ApplicationInsights";
import { mockConsole } from "../../__mocks__/console";

import { ConsoleLevel, RSConsole } from "./index";

const message = "hello there";
const obj = { hello: "there" };
const defaultProperties = {
    location: "http://localhost:3000/",
};
const reportableConsoleLevels: ConsoleLevel[] = [
    "assert",
    "debug",
    "error",
    "info",
    "trace",
    "warn",
];
const consoleSeverityLevels: Record<ConsoleLevel, SeverityLevel> = {
    info: SeverityLevel.Information,
    warn: SeverityLevel.Warning,
    error: SeverityLevel.Error,
    debug: SeverityLevel.Verbose,
    assert: SeverityLevel.Error,
    trace: SeverityLevel.Warning,
};

describe("RSConsole", () => {
    beforeAll(() => {
        mockConsole.mockImplementationAll();
    });
    afterAll(() => {
        mockConsole.mockRestoreAll();
    });
    afterEach(() => void vi.clearAllMocks());
    describe("when calling info", () => {
        test("calls console.info and trackTrace with the correct message and severity level", () => {
            const rsConsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            rsConsole.info(message, obj);

            expect(mockAppInsights.trackTrace).toHaveBeenCalledWith<
                [ITraceTelemetry]
            >({
                message,
                severityLevel: consoleSeverityLevels.info,
                properties: {
                    ...defaultProperties,
                    args: [obj],
                },
            });

            expect(mockConsole.info).toHaveBeenCalledWith(message, obj);
        });
    });

    describe("when calling assert", () => {
        describe("when assertion fails", () => {
            test("calls console.assert and trackTrace with the correct message and severity level", () => {
                const rsConsole = new RSConsole({
                    ai: mockAppInsights as any,
                    consoleSeverityLevels,
                    reportableConsoleLevels,
                });
                const msg = `Assertion failed: ${message}`;

                rsConsole.assert(false, message, obj);

                expect(mockAppInsights.trackException).toHaveBeenCalledWith<
                    [IExceptionTelemetry]
                >({
                    id: msg,
                    exception: new Error(msg),
                    severityLevel: consoleSeverityLevels.assert,
                    properties: {
                        ...defaultProperties,
                        args: [obj],
                    },
                });

                expect(mockConsole.assert).toHaveBeenCalledWith(
                    false,
                    message,
                    obj,
                );
            });
        });

        describe("when assertion passes", () => {
            test("only calls console.assert", () => {
                const rsConsole = new RSConsole({
                    ai: mockAppInsights as any,
                    consoleSeverityLevels,
                    reportableConsoleLevels,
                });
                rsConsole.assert(true, message, obj);

                expect(mockAppInsights.trackException).not.toHaveBeenCalled();
                expect(mockConsole.assert).toHaveBeenCalledWith(
                    true,
                    message,
                    obj,
                );
            });
        });
    });

    describe("when calling debug", () => {
        test("calls console.debug and trackTrace with the correct message and severity level", () => {
            const rsConsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            // eslint-disable-next-line testing-library/no-debugging-utils
            rsConsole.debug(message, obj);

            expect(mockAppInsights.trackTrace).toHaveBeenCalledWith<
                [ITraceTelemetry]
            >({
                message,
                severityLevel: consoleSeverityLevels.debug,
                properties: {
                    ...defaultProperties,
                    args: [obj],
                },
            });
        });
    });

    describe("when calling error", () => {
        test("calls console.error and trackException with the correct message and severity level", () => {
            const rsConsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            const err = new Error(message);
            rsConsole.error(err, obj);

            expect(mockAppInsights.trackException).toHaveBeenCalledWith<
                [IExceptionTelemetry]
            >({
                exception: err,
                id: err.message,
                severityLevel: consoleSeverityLevels.error,
                properties: {
                    ...defaultProperties,
                    args: [obj],
                },
            });
        });
    });

    describe("when calling trace", () => {
        test("calls console.trace and trackTrace with the correct message and severity level", () => {
            const rsConsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            rsConsole.trace(message, obj);

            expect(mockAppInsights.trackTrace).toHaveBeenCalledWith<
                [ITraceTelemetry]
            >({
                message,
                severityLevel: consoleSeverityLevels.trace,
                properties: {
                    ...defaultProperties,
                    args: [obj],
                },
            });
        });
    });

    describe("when calling warn", () => {
        test("calls console.warn and trackTrace with the correct message and severity level", () => {
            const rsConsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            rsConsole.warn(message, obj);

            expect(mockAppInsights.trackTrace).toHaveBeenCalledWith<
                [ITraceTelemetry]
            >({
                message,
                severityLevel: consoleSeverityLevels.warn,
                properties: {
                    ...defaultProperties,
                    args: [obj],
                },
            });
        });
    });

    describe("when calling dev", () => {
        test("does not call console if non-dev env", () => {
            const rsConsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            rsConsole.dev("test");
            expect(mockConsole.log).not.toHaveBeenCalledWith("test");
        });
        test("does call console if dev env", () => {
            const rsConsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
                env: "development",
            });
            rsConsole.dev("test");
            expect(mockConsole.log).toHaveBeenCalledWith("test");
        });
    });
});
