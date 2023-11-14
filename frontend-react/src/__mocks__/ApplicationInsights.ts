import { ApplicationInsights } from "@microsoft/applicationinsights-web";
import { PartialDeep } from "type-fest";

export const mockAppInsights = {
    trackEvent: vi.fn(),
    trackException: vi.fn(),
    trackTrace: vi.fn(),
    trackMetric: vi.fn(),
} as const satisfies PartialDeep<ApplicationInsights>;
