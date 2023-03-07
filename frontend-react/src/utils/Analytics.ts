import { getAppInsights } from "../TelemetryService";

export enum EventName {
    TABLE_FILTER = "Table Filter",
    SESSION_DURATION = "Session Duration",
    TABLE_PAGINATION = "Table Pagination",
    FILE_VALIDATOR = "File Validator",
}

export const trackAppInsightEvent = (eventName: string, eventData: any) => {
    const appInsights = getAppInsights();

    const telemetryEvent = {
        name: eventName,
        properties: eventData,
    };

    if (eventName !== "") appInsights?.trackEvent(telemetryEvent);
};

export const setAuthenticatedUserContext = (email: string | undefined) => {
    // Add user email as user_AuthenticatedId to all tracking events
    if (email) {
        const appInsights = getAppInsights();
        appInsights?.setAuthenticatedUserContext(email);
    }
};
