import { ApplicationInsights } from "@microsoft/applicationinsights-web";

import { createTelemetryService } from "./TelemetryService";

const aiConfig = { connectionString: "instrumentationKey=test" };

describe("TelemetryService", () => {
    describe("when the connection string is falsy", () => {
        test("returns undefined", () => {
            const ai = createTelemetryService({});

            expect(ai).toBeUndefined();
        });
    });

    describe("when the connection string is provided", () => {
        test("returns app insights", () => {
            const ai = createTelemetryService(aiConfig);

            expect(ai).toBeInstanceOf(ApplicationInsights);
        });
    });
});
