import { SeverityLevel } from "@microsoft/applicationinsights-web";

import type { ConsoleLevel } from "../utils/console";

const envVars = {
    OKTA_URL: import.meta.env.VITE_OKTA_URL,
    OKTA_CLIENT_ID: import.meta.env.VITE_OKTA_CLIENTID,
    RS_API_URL: import.meta.env.VITE_BACKEND_URL,
    MODE: import.meta.env.MODE,
};

const DEFAULT_FEATURE_FLAGS = import.meta.env.VITE_FEATURE_FLAGS
    ? import.meta.env.VITE_FEATURE_FLAGS.split(",")
    : [];

const config = {
    ...envVars,
    DEFAULT_FEATURE_FLAGS: DEFAULT_FEATURE_FLAGS as string[],
    IS_PREVIEW: envVars.MODE !== "production",
    API_ROOT: `${envVars.RS_API_URL}/api`,
    // Debug ignored by default
    AI_REPORTABLE_CONSOLE_LEVELS: [
        "assert",
        "error",
        "info",
        "trace",
        "warn",
    ] as ConsoleLevel[],
    AI_CONSOLE_SEVERITY_LEVELS: {
        info: SeverityLevel.Information,
        warn: SeverityLevel.Warning,
        error: SeverityLevel.Error,
        debug: SeverityLevel.Verbose,
        assert: SeverityLevel.Error,
        trace: SeverityLevel.Warning,
    } as Record<ConsoleLevel, SeverityLevel>,
} as const;

export type AppConfig = typeof config;

export default config;
