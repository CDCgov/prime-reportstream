import { getAppInsights } from "../TelemetryService";

import { TrackAppInsightEvent } from "./Analytics";

let mockAppInsights = jest.fn(() => {
    console.log("Here");
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
});

jest.mock("../TelemetryService", () => ({
    ...jest.requireActual("../TelemetryService"),
    getAppInsights: () => mockAppInsights(),
}));

describe("Analytics", () => {
    describe("TrackAppInsightEvent", () => {
        let appInsights = getAppInsights();

        test("calls trackEvent with the correct event and event data", () => {
            const eventName = "Event Name";
            const eventData = {
                tableFilter: {
                    startRange: "1/1/2021",
                    endRange: "1/20/2021",
                },
            };

            TrackAppInsightEvent(eventName, eventData);

            expect(appInsights?.trackEvent).toBeCalledWith({
                name: eventName,
                eventData,
            });
        });
    });
});
