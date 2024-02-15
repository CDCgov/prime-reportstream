import { mockAppInsights } from "../../utils/TelemetryService/TelemetryService.fixtures";

module.exports = {
    __esModule: true,
    ...jest.requireActual("@microsoft/applicationinsights-react-js"),
    useAppInsightsContext: jest.fn().mockReturnValue(mockAppInsights),
};
