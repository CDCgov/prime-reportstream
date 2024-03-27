import { appInsightsFixture } from "../../utils/TelemetryService/TelemetryService.fixtures";

module.exports = {
    ...(await vi.importActual("@microsoft/applicationinsights-react-js")),
    useAppInsightsContext: vi.fn().mockReturnValue(appInsightsFixture),
};
