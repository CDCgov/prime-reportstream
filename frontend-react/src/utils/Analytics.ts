import { IEventTelemetry } from "@microsoft/applicationinsights-web";

import { getAppInsights } from "../TelemetryService";

export enum EventName {
    TABLE_FILTER = "Table Filter",
}

export const TrackAppInsightEvent = (
    eventName: string = "",
    eventData: any
) => {
    const appInsights = getAppInsights();
    const telemetryEvent: IEventTelemetry = {
        name: eventName,
    };

    appInsights?.trackEvent(telemetryEvent, eventData);
};
