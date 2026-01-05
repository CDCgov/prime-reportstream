import { useAppInsightsContext as _useAppInsightsContext } from "@microsoft/applicationinsights-react-js";
import { ReactPlugin } from "../../utils/TelemetryService/TelemetryService";

export default function useAppInsightsContext() {
    return _useAppInsightsContext() as ReactPlugin;
}
