import { defineConfig } from "@playwright/test";
import dotenvflow from "dotenv-flow";
import process from "node:process";

dotenvflow.config({
    purge_dotenv: true,
    silent: true,
    default_node_env: "test",
    // in case IDE testing extensions call this with a different CWD
    path: import.meta.dirname,
});

const isCi = Boolean(process.env.CI);

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
    testDir: "e2e",
    fullyParallel: true,
    forbidOnly: isCi,
    retries: isCi ? 2 : 0,
    workers: isCi ? "100%" : undefined,
    reporter: [["html", { outputFolder: "e2e-data/report" }]],
    outputDir: "e2e-data/results",
    use: {
        // keep playwright and browser timezones aligned. set preferably UTC by env var
        timezoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
        baseURL: "http://localhost:4173",
        trace: "on-first-retry",
        screenshot: "only-on-failure",
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
            testMatch: "spec/all/**/*.spec.ts",
        },
        {
            name: "firefox",
            use: { browserName: "firefox" },
            dependencies: ["setup"],
            testMatch: "spec/all/**/*.spec.ts",
        },
        {
            name: "webkit",
            use: { browserName: "webkit" },
            dependencies: ["setup"],
            testMatch: "spec/all/**/*.spec.ts",
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
