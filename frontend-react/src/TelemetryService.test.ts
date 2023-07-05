import {
    ApplicationInsights,
    SeverityLevel,
} from "@microsoft/applicationinsights-web";

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
    const oldConnectionString =
        process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING;

    let ai: { initialize: () => void };
    let getAppInsights: () => ApplicationInsights;
    let getAppInsightsHeaders: () => object;
    let withInsights: (console: Console) => void;

    beforeEach(() => {
        // Isolating modules to "reset" previously initialized appInsights, reactPlugin, etc.
        jest.isolateModules(() => {
            ({
                ai,
                getAppInsights,
                getAppInsightsHeaders,
                withInsights,
            } = require("./TelemetryService"));
        });

        delete process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING;
    });

    afterAll(() => {
        process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
            oldConnectionString;
    });

    describe("#initialize", () => {
        beforeEach(() => {
            jest.spyOn(console, "warn").mockImplementation(jest.fn);
        });

        describe("when the connection string is falsy", () => {
            test("warns that the string is not provided and returns undefined", () => {
                ai.initialize();

                expect(console.warn).toHaveBeenCalledWith(
                    "App Insights connection string not provided"
                );
            });
        });

        describe("when the connection string is provided", () => {
            beforeEach(() => {
                process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
                    "test-connection-string";
            });

            afterEach(() => {
                process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
                    oldConnectionString;
            });

            test("does not warn", () => {
                ai.initialize();

                expect(console.warn).not.toHaveBeenCalled();
            });
        });
    });

    describe("#getAppInsights", () => {
        describe("when AppInsights has not been initialized", () => {
            test("returns null", () => {
                expect(getAppInsights()).toBeNull();
            });
        });

        describe("when AppInsights has been initialized", () => {
            beforeEach(() => {
                process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
                    "test";

                ai.initialize();
            });

            afterEach(() => {
                process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
                    oldConnectionString;
            });

            test("returns the AppInsights instance", () => {
                expect(getAppInsights()).not.toBeNull();
            });
        });
    });

    describe("#getAppInsightsHeaders", () => {
        describe("when AppInsights has not been initialized", () => {
            test("returns an object with an empty string for the session header", () => {
                expect(getAppInsightsHeaders()).toEqual({
                    "x-ms-session-id": "",
                });
            });
        });

        describe("when AppInsights has been initialized", () => {
            beforeEach(() => {
                process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
                    "test-connection-string";

                ai.initialize();
            });

            afterEach(() => {
                process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
                    oldConnectionString;
            });

            test("returns an object with the correct value for the session header", () => {
                expect(getAppInsightsHeaders()).toEqual({
                    "x-ms-session-id": "test-session-id",
                });
            });
        });
    });

    describe("#withInsights", () => {
        let appInsights: ApplicationInsights | null;

        beforeAll(() => {
            process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
                "test-connection-string";

            jest.spyOn(console, "log").mockImplementation(jest.fn);
            jest.spyOn(console, "info").mockImplementation(jest.fn);
            jest.spyOn(console, "warn").mockImplementation(jest.fn);
            jest.spyOn(console, "error").mockImplementation(jest.fn);

            ai.initialize();
            appInsights = getAppInsights();
            withInsights(console);
        });

        afterAll(() => {
            jest.resetAllMocks();

            process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
                oldConnectionString;
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
                            additionalInformation: undefined,
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
                            additionalInformation: undefined,
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
                            additionalInformation: JSON.stringify([data]),
                        },
                    });
                });
            });
        });
    });
});
