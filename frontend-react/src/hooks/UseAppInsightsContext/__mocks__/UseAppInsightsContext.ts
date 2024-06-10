import { appInsightsFixture } from "../../../utils/TelemetryService/TelemetryService.fixtures";

const useAppInsightsContext = vi.fn(() => appInsightsFixture);

export default useAppInsightsContext;
