import { defineConfig } from "@playwright/test";
import dotenvflow from "dotenv-flow";
import process from "process";

import type { CustomFixtures } from "./e2e/test.ts";

dotenvflow.config({
    purge_dotenv: true,
    silent: true,
    default_node_env: "test",
});

const isCi = Boolean(process.env.CI);

function createLogins<const T extends Array<string>>(
    loginTypes: T,
): {
    [K in T extends ReadonlyArray<infer U> ? U : never]: {
        username: string;
        password: string;
        totpCode: string;
        path: string;
    };
} {
    const logins = Object.fromEntries(
        loginTypes.map((type) => {
            const username = process.env[`TEST_${type.toUpperCase()}_USERNAME`];
            const password = process.env[`TEST_${type.toUpperCase()}_PASSWORD`];
            const totpCode =
                process.env[`TEST_${type.toUpperCase()}_TOTP_CODE`];

            if (!username)
                throw new TypeError(`Missing username for login type: ${type}`);
            if (!password)
                throw new TypeError(`Missing password for login type: ${type}`);

            return [
                type,
                {
                    username,
                    password,
                    totpCode,
                    path: `e2e/.auth/${type}.json`,
                },
            ];
        }),
    );
    return logins as any;
}

const logins = createLogins(["admin", "receiver", "sender"]);

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig<CustomFixtures>({
    testDir: "e2e",
    fullyParallel: true,
    forbidOnly: isCi,
    retries: isCi ? 2 : 0,
    workers: isCi ? "100%" : undefined,
    reporter: [["html", { outputFolder: "e2e-data/report" }]],
    outputDir: "e2e-data/results",
    use: {
        timezoneId: "UTC",
        baseURL: "http://localhost:4173",
        trace: "on-first-retry",
        screenshot: "only-on-failure",
        adminLogin: {
            ...logins.admin,
            landingPage: "/admin/settings",
        },
        senderLogin: {
            ...logins.sender,
            landingPage: "/submissions",
        },
        receiverLogin: {
            ...logins.receiver,
            landingPage: "/daily-data",
        },
    },

    projects: [
        { name: "setup", testMatch: /\w+\.setup\.ts$/ },
        // We have a suite of tests that are ONLY checking links so to
        // save bandwidth, we only need to utilize a single browser
        {
            name: "chromium-only",
            use: { browserName: "chromium" },
            dependencies: ["setup"],
            testMatch: "spec/chromium-only/*.spec.ts",
        },
        {
            name: "chromium",
            use: { browserName: "chromium" },
            dependencies: ["setup"],
            testMatch: "spec/*.spec.ts",
        },
        {
            name: "firefox",
            use: { browserName: "firefox" },
            dependencies: ["setup"],
            testMatch: "spec/*.spec.ts",
        },
        {
            name: "webkit",
            use: { browserName: "webkit" },
            dependencies: ["setup"],
            testMatch: "spec/*.spec.ts",
        },
    ],
    webServer: {
        command: `yarn cross-env yarn run preview:build:${isCi ? "ci" : "test"}`,
        url: "http://localhost:4173",
        timeout: 1000 * 180,
        stdout: "pipe",
        // reuseExistingServer: !process.env.CI,
    },
});
