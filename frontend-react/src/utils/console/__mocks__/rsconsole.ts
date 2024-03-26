import { SeverityLevel } from "@microsoft/applicationinsights-web";

import { mockAppInsights } from "../../../__mocks__/ApplicationInsights";
import type { RSConsole } from "../index";

export const mockRsconsole = {
    ai: mockAppInsights as any,
    consoleSeverityLevels: {
        assert: SeverityLevel.Critical,
        debug: SeverityLevel.Critical,
        error: SeverityLevel.Critical,
        info: SeverityLevel.Critical,
        trace: SeverityLevel.Critical,
        warn: SeverityLevel.Critical,
    },
    reportableConsoleLevels: [
        "assert",
        "debug",
        "error",
        "info",
        "trace",
        "warn",
    ],
    assert: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn(),
    error: vi.fn(),
    trace: vi.fn(),
    _assert: vi.fn(),
    _error: vi.fn(),
    _trace: vi.fn(),
} satisfies Partial<RSConsole>;
