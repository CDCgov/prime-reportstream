import { useCallback, useRef } from "react";
import {
    useAppInsightsContext,
    useTrackEvent,
} from "@microsoft/applicationinsights-react-js";

import { useSessionContext } from "../../contexts/SessionContext";

export default function useTrackEventWithSession(eventName: string) {
    const { activeMembership, memberships } = useSessionContext();
    const appInsights = useAppInsightsContext();
    const trackEvent = useTrackEvent(appInsights, eventName, {}, true);
    const didLogWarningRef = useRef(false);

    const trackFn = useCallback(
        (eventData = {}) => {
            trackEvent({
                activeMembership,
                memberships,
                ...eventData,
            });
        },
        [activeMembership, memberships, trackEvent]
    );

    if (!appInsights) {
        if (!didLogWarningRef.current) {
            didLogWarningRef.current = true;

            console.warn(
                `useTrackEventWithSession for ${eventName} called without appInsights`
            );

            return () => {};
        }
    }

    return trackFn;
}
