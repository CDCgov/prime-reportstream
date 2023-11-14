import {
    IExceptionTelemetry,
    ITraceTelemetry,
    SeverityLevel,
} from "@microsoft/applicationinsights-web";

import { mockConsole } from "../../__mocks__/console";
import { mockAppInsights } from "../../__mocks__/ApplicationInsights";

import { ConsoleLevel, RSConsole } from ".";

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
    describe("when calling info", () => {
        beforeEach(() => {
            mockConsole.info.mockReturnValue(undefined);
        });
        test("calls console.info and trackTrace with the correct message and severity level", () => {
            const rsconsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            rsconsole.info(message, obj);

            expect(mockAppInsights.trackTrace).toBeCalledWith<
                [ITraceTelemetry]
            >({
                message,
                severityLevel: consoleSeverityLevels.info,
                properties: {
                    ...defaultProperties,
                    args: [obj],
                },
            });

            expect(mockConsole.info).toBeCalledWith(message, obj);
        });
    });

    describe("when calling assert", () => {
        describe("when assertion fails", () => {
            beforeEach(() => {
                mockConsole.assert.mockReturnValue(undefined);
            });
            test("calls console.assert and trackTrace with the correct message and severity level", () => {
                const rsconsole = new RSConsole({
                    ai: mockAppInsights as any,
                    consoleSeverityLevels,
                    reportableConsoleLevels,
                });
                const msg = `Assertion failed: ${message}`;

                rsconsole.assert(false, message, obj);

                expect(mockAppInsights.trackException).toBeCalledWith<
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

                expect(mockConsole.assert).toBeCalledWith(false, message, obj);
            });
        });

        describe("when assertion passes", () => {
            beforeEach(() => {
                mockConsole.assert.mockReturnValue(undefined);
            });
            test("only calls console.assert", () => {
                const rsconsole = new RSConsole({
                    ai: mockAppInsights as any,
                    consoleSeverityLevels,
                    reportableConsoleLevels,
                });
                rsconsole.assert(true, message, obj);

                expect(mockAppInsights.trackException).not.toBeCalled();
                expect(mockConsole.assert).toBeCalledWith(true, message, obj);
            });
        });
    });

    describe("when calling debug", () => {
        beforeEach(() => {
            mockConsole.debug.mockReturnValue(undefined);
        });
        test("calls console.debug and trackTrace with the correct message and severity level", () => {
            const rsconsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            // eslint-disable-next-line testing-library/no-debugging-utils
            rsconsole.debug(message, obj);

            expect(mockAppInsights.trackTrace).toBeCalledWith<
                [ITraceTelemetry]
            >({
                message,
                severityLevel: consoleSeverityLevels.debug,
                properties: {
                    ...defaultProperties,
                    args: [obj],
                },
            });
            expect(mockConsole.debug).toBeCalled();
        });
    });

    describe("when calling error", () => {
        beforeEach(() => {
            mockConsole.error.mockReturnValue(undefined);
        });
        test("calls console.error and trackException with the correct message and severity level", () => {
            const rsconsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            const err = new Error(message);
            rsconsole.error(err, obj);

            expect(mockAppInsights.trackException).toBeCalledWith<
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
            expect(mockConsole.error).toBeCalled();
        });
    });

    describe("when calling trace", () => {
        beforeEach(() => {
            mockConsole.trace.mockReturnValue(undefined);
        });
        test("calls console.trace and trackTrace with the correct message and severity level", () => {
            const rsconsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            rsconsole.trace(message, obj);

            expect(mockAppInsights.trackTrace).toBeCalledWith<
                [ITraceTelemetry]
            >({
                message,
                severityLevel: consoleSeverityLevels.trace,
                properties: {
                    ...defaultProperties,
                    args: [obj],
                },
            });
            expect(mockConsole.trace).toBeCalled();
        });
    });

    describe("when calling warn", () => {
        beforeEach(() => {
            mockConsole.warn.mockReturnValue(undefined);
        });
        test("calls console.warn and trackTrace with the correct message and severity level", () => {
            const rsconsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            rsconsole.warn(message, obj);

            expect(mockAppInsights.trackTrace).toBeCalledWith<
                [ITraceTelemetry]
            >({
                message,
                severityLevel: consoleSeverityLevels.warn,
                properties: {
                    ...defaultProperties,
                    args: [obj],
                },
            });
            expect(mockConsole.warn).toBeCalled();
        });
    });

    describe("when calling dev", () => {
        beforeEach(() => {
            mockConsole.log.mockReturnValue(undefined);
        });
        test("does not call console if non-dev env", () => {
            const rsconsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
            });
            rsconsole.dev("test");
            expect(mockConsole.log).not.toBeCalledWith("test");
        });
        test("does call console if dev env", () => {
            const rsconsole = new RSConsole({
                ai: mockAppInsights as any,
                consoleSeverityLevels,
                reportableConsoleLevels,
                env: "development",
            });
            rsconsole.dev("test");
            expect(mockConsole.log).toBeCalledWith("test");
        });
    });
});
