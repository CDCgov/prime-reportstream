import { vi } from "vitest";
import type { PartialDeep } from "type-fest";

import type { AppInsightsCtx } from "..";
import { mockAppInsights } from "../../../__mocks__/ApplicationInsights";

export const defaultCtx = {
    fetchHeaders: vi.fn(() => ({})),
    setTelemetryCustomProperty: vi.fn(() => void 0),
    telemetryCustomProperties: {},
    appInsights: mockAppInsights,
} as const satisfies PartialDeep<AppInsightsCtx>;

module.exports = {
    ...(await vi.importActual<typeof import("..")>("../")),
    useAppInsightsContext: vi.fn(() => defaultCtx),
};
