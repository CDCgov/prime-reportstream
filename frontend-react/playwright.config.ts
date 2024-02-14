import { defineConfig } from "@playwright/test";
import dotenvflow from "dotenv-flow";

import type { TestOptions } from "./e2e/helpers/rs-test.ts";

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
                    path: `playwright/.auth/${type}.json`,
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
    /* Run tests in files in parallel */
    fullyParallel: true,
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: isCi,
    /* Retry on CI only */
    retries: isCi ? 2 : 0,
    /* Opt out of parallel tests on CI. */
    workers: isCi ? 1 : undefined,
    /* Reporter to use. See https://playwright.dev/docs/test-reporters */
    reporter: "html",
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
        /* Base URL to use in actions like `await page.goto('/')`. */
        baseURL: "http://localhost:4173",

        /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
        trace: "on-first-retry",

        /* Screenshot on failure. See https://playwright.dev/docs/screenshots */
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

    /* Configure projects for major browsers */
    projects: [
        { name: "setup", testMatch: /.*\.setup\.ts/ },
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

        /* Test against branded browsers. */
        // {
        //   name: 'Microsoft Edge',
        //   use: { ...devices['Desktop Edge'], channel: 'msedge' },
        // },
        // {
        //   name: 'Google Chrome',
        //   use: { ...devices['Desktop Chrome'], channel: 'chrome' },
        // },
    ],

    /* Run the local dev server and start the tests */
    webServer: {
        command: "yarn run preview:build:test",
        url: "http://localhost:4173",
        timeout: 1000 * 180,
        stdout: "pipe",
        // reuseExistingServer: !process.env.CI,
    },
});
