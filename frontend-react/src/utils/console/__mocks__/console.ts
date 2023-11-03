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
    assert: jest.fn(),
    warn: jest.fn(),
    info: jest.fn(),
    debug: jest.fn(),
    error: jest.fn(),
    trace: jest.fn(),
    _assert: jest.fn(),
    _error: jest.fn(),
    _trace: jest.fn(),
} satisfies Partial<RSConsole>;
