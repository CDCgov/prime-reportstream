import { vi } from "vitest";

import type { AppInsightsCtx } from "../AppInsightsContext";

export const defaultCtx: AppInsightsCtx = {
    fetchHeaders: vi.fn(() => ({})),
    setTelemetryCustomProperty: vi.fn(() => void 0),
    telemetryCustomProperties: {},
    appInsights: undefined,
};

export const useAppInsightsContext = vi.fn<any, AppInsightsCtx>(
    () => defaultCtx,
);
export const {
    AppInsightsContext,
    AppInsightsContextProvider,
    EventName,
    default: dft,
} = await vi.importActual<typeof import("../AppInsightsContext")>(
    "../AppInsightsContext",
);
export default dft;
