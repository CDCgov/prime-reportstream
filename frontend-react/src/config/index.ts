import { SeverityLevel } from "@microsoft/applicationinsights-web";
import type { IIdleTimerProps } from "react-idle-timer";

import type { ConsoleLevel } from "../utils/console";

const envVars = {
    OKTA_URL: process.env.NEXT_PUBLIC_OKTA_URL,
    OKTA_CLIENT_ID: process.env.NEXT_PUBLIC_OKTA_CLIENTID,
    RS_API_URL: process.env.NEXT_PUBLIC_BACKEND_URL,
    MODE: process.env.NODE_ENV,
};

const DEFAULT_FEATURE_FLAGS = process.env.NEXT_PUBLIC_FEATURE_FLAGS
    ? process.env.NEXT_PUBLIC_FEATURE_FLAGS.split(",")
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
    IDLE_TIMERS: {
        timeout: process.env.NEXT_PUBLIC_IDLE_TIMEOUT ?? 1000 * 60 * 15, // 15 minutes
        debounce: 500,
        crossTab: true,
        syncTimers: 200,
        leaderElection: true,
    } as IIdleTimerProps,
    PAGE_META: {
        defaults: {
            title: process.env.NEXT_PUBLIC_TITLE,
            description: process.env.NEXT_PUBLIC_DESCRIPTION,
            openGraph: {
                image: {
                    src: process.env.NEXT_PUBLIC_OPENGRAPH_DEFAULT_IMAGE_SRC,
                    altText: process.env
                        .NEXT_PUBLIC_OPENGRAPH_DEFAULT_IMAGE_ALTTEXT,
                },
            },
        },
    },
} as const;

export type AppConfig = typeof config;

export default config;
