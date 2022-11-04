import {
    useAppInsightsContext,
    useTrackMetric,
} from "@microsoft/applicationinsights-react-js";
import { useEffect } from "react";

export const usePageHitInsight = (componentName: string) => {
    const appInsights = useAppInsightsContext();
    const pageHitTracker = useTrackMetric(appInsights, componentName);
    useEffect(() => {
        // Should run once on page load
        // sends simple page hit to app insights
        pageHitTracker();
    }, []); //eslint-disable-line
};
