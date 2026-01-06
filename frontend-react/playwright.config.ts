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

// Skip all E2E tests, test only static page - functionality has been removed for static sunset website
const E2E_TESTS_STATIC = true;

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
    testDir: "e2e",
    fullyParallel: true,
    forbidOnly: isCi,
    retries: isCi ? 2 : 0,
    // Do not consume 100% cpu, as this will cause instability
    workers: isCi ? "75%" : undefined,
    // Tests sharded in CI runner and reported as blobs that are later turned into html report
    reporter: isCi ? [["blob", { outputDir: "e2e-data/report" }]] : [["html", { outputFolder: "e2e-data/report" }]],
    outputDir: "e2e-data/results",
    timeout: 1000 * 180,
    use: {
        // keep playwright and browser timezones aligned. set preferably UTC by env var
        timezoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
        baseURL: "http://localhost:4173",
        trace: "on-first-retry",
        screenshot: "only-on-failure",
    },

    projects: E2E_TESTS_STATIC
        ? [
              {
                  name: "chromium",
                  use: { browserName: "chromium" },
                  testMatch: ["spec/static-page/*.spec.ts"],
              },
          ]
        : [
              { name: "setup", testMatch: /\w+\.setup\.ts$/ },
              // We have a suite of tests that are ONLY checking links so to
              // save bandwidth, we only need to utilize a single browser
              {
                  name: "chromium",
                  use: { browserName: "chromium" },
                  dependencies: ["setup"],
                  testMatch: [
                      "spec/all/*.spec.ts",
                      "spec/all/**/*.spec.ts",
                      "spec/chromium-only/*.spec.ts",
                      "spec/chromium-only/**/*.spec.ts",
                  ],
              },
              {
                  name: "firefox",
                  use: { browserName: "firefox" },
                  dependencies: ["setup"],
                  testMatch: ["spec/all/*.spec.ts", "spec/all/**/*.spec.ts"],
              },
              {
                  name: "webkit",
                  use: { browserName: "webkit" },
                  dependencies: ["setup"],
                  testMatch: ["spec/all/*.spec.ts", "spec/all/**/*.spec.ts"],
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
