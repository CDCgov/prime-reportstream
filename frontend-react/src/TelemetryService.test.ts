import {
    ApplicationInsights,
    SeverityLevel,
} from "@microsoft/applicationinsights-web";

import {
    aiConfig,
    createTelemetryService,
    resetConsole,
} from "./TelemetryService";

jest.mock("@microsoft/applicationinsights-web", () => {
    return {
        ...jest.requireActual("@microsoft/applicationinsights-web"),
        ApplicationInsights: function () {
            return {
                loadAppInsights() {},
                trackEvent: jest.fn(),
                trackException: jest.fn(),
                addTelemetryInitializer: jest.fn(),
                context: {
                    getSessionId() {
                        return "test-session-id";
                    },
                },
            };
        },
    };
});

describe("TelemetryService", () => {
    describe("#initialize", () => {
        const consoleWarn = jest.spyOn(console, "warn");
        beforeEach(() => {
            consoleWarn.mockImplementation(jest.fn);
        });
        afterEach(() => {
            resetConsole();
        });

        describe("when the connection string is falsy", () => {
            test("warns that the string is not provided and returns undefined", () => {
                createTelemetryService({
                    connectionString: undefined,
                });

                expect(consoleWarn).toHaveBeenCalledWith(
                    "App Insights connection string not provided",
                );
            });
        });

        describe("when the connection string is provided", () => {
            test("does not warn", () => {
                createTelemetryService({ connectionString: "ABC" });

                expect(consoleWarn).not.toHaveBeenCalled();
            });
        });
    });

    describe("#attachAppInsightsToConsole", () => {
        let appInsights: ApplicationInsights;

        beforeAll(() => {
            jest.spyOn(console, "log").mockImplementation(jest.fn);
            jest.spyOn(console, "info").mockImplementation(jest.fn);
            jest.spyOn(console, "warn").mockImplementation(jest.fn);
            jest.spyOn(console, "error").mockImplementation(jest.fn);

            appInsights = createTelemetryService(aiConfig)!!;
        });

        afterAll(() => {
            jest.resetAllMocks();
            resetConsole();
        });

        describe("when calling console.info", () => {
            describe("when not passing in more arguments", () => {
                test("calls trackEvent with the correct message and severity level", () => {
                    const message = "hello there";

                    console.info(message);

                    expect(appInsights?.trackEvent).toBeCalledWith({
                        name: `INFO - ${message}`,
                        properties: {
                            severityLevel: SeverityLevel.Information,
                            message,
                            additionalInformation: undefined,
                        },
                    });
                });
            });

            describe("when passing in more arguments", () => {
                test("calls trackException with the correct message, severity level, and stringified additional details", () => {
                    const message = "hello there";
                    const data = { a: 1 };

                    console.info(message, data);

                    expect(appInsights?.trackEvent).toBeCalledWith({
                        name: `INFO - ${message}`,
                        properties: {
                            severityLevel: SeverityLevel.Information,
                            message,
                            additionalInformation: JSON.stringify([data]),
                        },
                    });
                });
            });
        });

        describe("when calling console.log", () => {
            describe("when not passing in more arguments", () => {
                test("calls trackEvent with the correct message and severity level", () => {
                    const message = "hello there";

                    // eslint-disable-next-line no-console
                    console.log(message);

                    expect(appInsights?.trackEvent).toBeCalledWith({
                        name: `LOG - ${message}`,
                        properties: {
                            severityLevel: SeverityLevel.Information,
                            message,
                            additionalInformation: undefined,
                        },
                    });
                });
            });

            describe("when passing in more arguments", () => {
                test("calls trackException with the correct message, severity level, and stringified additional details", () => {
                    const message = "hello there";
                    const data = { a: 1 };

                    // eslint-disable-next-line no-console
                    console.log(message, data);

                    expect(appInsights?.trackEvent).toBeCalledWith({
                        name: `LOG - ${message}`,
                        properties: {
                            severityLevel: SeverityLevel.Information,
                            message,
                            additionalInformation: JSON.stringify([data]),
                        },
                    });
                });
            });
        });

        describe("when calling console.warn", () => {
            describe("when not passing in more arguments", () => {
                test("calls trackEvent with the correct message and severity level", () => {
                    const message = "hello there";

                    console.warn(message);

                    expect(appInsights?.trackEvent).toBeCalledWith({
                        name: `WARN - ${message}`,
                        properties: {
                            severityLevel: SeverityLevel.Warning,
                            message,
                            additionalInformation: undefined,
                        },
                    });
                });
            });

            describe("when passing in more arguments", () => {
                test("calls trackException with the correct message, severity level, and stringified additional details", () => {
                    const message = "hello there";
                    const data = { a: 1 };

                    console.warn(message, data);

                    expect(appInsights?.trackEvent).toBeCalledWith({
                        name: `WARN - ${message}`,
                        properties: {
                            severityLevel: SeverityLevel.Warning,
                            message,
                            additionalInformation: JSON.stringify([data]),
                        },
                    });
                });
            });
        });

        describe("when calling console.error", () => {
            describe("when passing in an Error as the first parameter", () => {
                test("calls trackException with the correct Error and severity level", () => {
                    const error = new Error("test error");

                    console.error(error);

                    expect(appInsights?.trackException).toBeCalledWith({
                        exception: error,
                        id: error.message,
                        severityLevel: SeverityLevel.Error,
                        properties: {
                            additionalInformation: {
                                error: error,
                                location: "http://localhost/",
                                other: [],
                            },
                        },
                    });
                });
            });

            describe("when passing in a String as the first parameter", () => {
                test("calls trackException with the correct Error and severity level", () => {
                    const error = "test error";

                    console.error(error);

                    expect(appInsights?.trackException).toBeCalledWith({
                        exception: new Error(error),
                        id: error,
                        severityLevel: SeverityLevel.Error,
                        properties: {
                            additionalInformation: {
                                error: error,
                                location: "http://localhost/",
                                other: [],
                            },
                        },
                    });
                });
            });

            describe("when passing in more arguments", () => {
                test("calls trackException with the correct Error, severity level, and stringified additional details", () => {
                    const error = new Error("test error");
                    const data = { a: 1 };

                    console.error(error, data);

                    expect(appInsights?.trackException).toBeCalledWith({
                        exception: error,
                        id: error.message,
                        severityLevel: SeverityLevel.Error,
                        properties: {
                            additionalInformation: {
                                error: error,
                                location: "http://localhost/",
                                other: [data],
                            },
                        },
                    });
                });
            });
        });
    });
});
