import { IEventTelemetry } from "@microsoft/applicationinsights-web";

import { getAppInsights } from "../TelemetryService";

export enum EventName {
    TABLE_FILTER = "Table Filter",
}

export const trackAppInsightEvent = (
    eventName: string = "",
    eventData: any
) => {
    const appInsights = getAppInsights();
    const telemetryEvent: IEventTelemetry = {
        name: eventName,
    };

    if (eventName !== "") appInsights?.trackEvent(telemetryEvent, eventData);
};
