import { SeverityLevel } from "@microsoft/applicationinsights-web";

import { ai, getAppInsights, withInsights } from "./TelemetryService";

jest.mock("@microsoft/applicationinsights-web", () => {
    return {
        ...jest.requireActual("@microsoft/applicationinsights-web"),
        ApplicationInsights: function () {
            return {
                loadAppInsights() {},
                trackEvent: jest.fn(),
                trackException: jest.fn(),
                addTelemetryInitializer: jest.fn(),
            };
        },
    };
});

const oldEnv = process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING;

describe("TelemetryService", () => {
    beforeEach(() => {
        jest.spyOn(console, "warn").mockImplementation(() => {});
        jest.spyOn(console, "log").mockImplementation(() => {});
        jest.spyOn(console, "error").mockImplementation(() => {});
        jest.spyOn(ai, "initialize");
    });

    afterEach(() => {
        jest.resetAllMocks();
        process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING = oldEnv;
    });

    it("initializes the appInsights service", () => {
        process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
            "fake-connection-string";
        ai.initialize();
        expect(getAppInsights()).not.toBe(null);
    });

    it("calls app insights on console methods", () => {
        process.env.REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING =
            "fake-connection-string";
        const appInsights = getAppInsights();
        withInsights(console);
        const message = "hello there";
        console.log(message);
        expect(appInsights?.trackEvent).toBeCalledWith({
            name: "LOG - hello there",
            properties: {
                severityLevel: SeverityLevel.Information,
                message,
                additionalInformation: undefined,
            },
        });

        const warning = "some warning";
        const data = { oh: "no" };
        console.warn(warning, data);
        expect(appInsights?.trackException).toBeCalledWith({
            id: "some warning",
            severityLevel: SeverityLevel.Warning,
            properties: {
                additionalInformation: JSON.stringify([data]),
            },
        });

        const error = new Error("bad news");
        console.error(error);
        expect(appInsights?.trackException).toBeCalledWith({
            exception: error,
            id: error.message,
            severityLevel: SeverityLevel.Error,
            properties: {
                additionalInformation: undefined,
            },
        });

        const nonErrorError = "something bad happened";
        console.error(nonErrorError);
        expect(appInsights?.trackException).toBeCalledWith({
            exception: undefined,
            id: nonErrorError,
            severityLevel: SeverityLevel.Error,
            properties: {
                additionalInformation: undefined,
            },
        });
    });
});
