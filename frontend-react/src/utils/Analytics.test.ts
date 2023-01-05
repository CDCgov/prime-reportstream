import { trackAppInsightEvent } from "./Analytics";

const mockAppInsights = {
    trackEvent: jest.fn(),
};

jest.mock("../TelemetryService", () => ({
    ...jest.requireActual("../TelemetryService"),
    getAppInsights: () => mockAppInsights,
}));

describe("Analytics", () => {
    describe("TrackAppInsightEvent", () => {
        test("calls trackEvent with the correct event and event data", () => {
            const eventName = "Event Name";
            const eventData = {
                tableFilter: {
                    startRange: "1/1/2021",
                    endRange: "1/20/2021",
                },
            };

            trackAppInsightEvent(eventName, eventData);

            expect(mockAppInsights.trackEvent).toBeCalledWith(
                {
                    name: eventName,
                },
                {
                    tableFilter: {
                        endRange: "1/20/2021",
                        startRange: "1/1/2021",
                    },
                }
            );
        });

        test("does not call trackEvent when eventName is empty", () => {
            const eventName = "";
            const eventData = {
                tableFilter: {
                    startRange: "1/1/2021",
                    endRange: "1/20/2021",
                },
            };

            trackAppInsightEvent(eventName, eventData);

            expect(mockAppInsights.trackEvent).not.toBeCalled();
        });
    });
});
