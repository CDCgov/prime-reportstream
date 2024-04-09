import { defineConfig } from "@playwright/test";
import process from "process";

import type { TestOptions } from "./e2e/helpers/rs-test.ts";

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
export default defineConfig<TestOptions>({
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
            landingPage: "/",
        },
    },

    projects: [
        /* Test setup (ex: authenticated sessions) */
        { name: "setup", testMatch: /\w+\.setup\.ts$/ },
        {
            name: "chromium",
            use: { browserName: "chromium" },
            dependencies: ["setup"],
        },

        {
            name: "firefox",
            use: { browserName: "firefox" },
            dependencies: ["setup"],
        },

        {
            name: "webkit",
            use: { browserName: "webkit" },
            dependencies: ["setup"],
        },

        /* Test against mobile viewports. */
        // {
        //   name: 'Mobile Chrome',
        //   use: { ...devices['Pixel 5'] },
        // },
        // {
        //   name: 'Mobile Safari',
        //   use: { ...devices['iPhone 12'] },
        // },
    ],
});
