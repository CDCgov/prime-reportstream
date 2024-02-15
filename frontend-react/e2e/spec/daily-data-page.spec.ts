import { expect, test } from "@playwright/test";

import { selectTestOrg, waitForAPIResponse } from "../helpers/utils";

test.describe("Daily Data page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await page.goto("/daily-data", {
                waitUntil: "domcontentloaded",
            });
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("admin user", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.describe("without org selected", () => {
            test.beforeEach(async ({ page }) => {
                await page.goto("/daily-data", {
                    waitUntil: "domcontentloaded",
                });
            });

            test("will not load page", async ({ page }) => {
                await expect(
                    page.getByText("Cannot fetch Organization data as admin"),
                ).toBeVisible();
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });

        test.describe("with org selected", () => {
            test.beforeEach(async ({ page }) => {
                await selectTestOrg(page);

                await page.goto("/daily-data", {
                    waitUntil: "domcontentloaded",
                });

                await waitForAPIResponse(page, "/api/waters/org/");
            });

            test("has correct title", async ({ page }) => {
                await expect(page).toHaveTitle(/Daily Data - ReportStream/);
            });

            test("has receiver services dropdown", async ({ page }) => {
                await expect(
                    page.getByTestId("services-dropdown"),
                ).toBeAttached();
            });

            test("has filter", async ({ page }) => {
                await expect(
                    page.getByText("From (Start Range):"),
                ).toBeAttached();
                await expect(
                    page.getByText("Until (End Range):"),
                ).toBeAttached();
            });

            test("table has correct headers", async ({ page }) => {
                await expect(page.getByText(/Report ID/)).toBeAttached();
                await expect(page.getByText(/Available/)).toBeAttached();
                await expect(page.getByText(/Expires/)).toBeAttached();
                await expect(page.getByText(/Items/)).toBeAttached();
                await expect(page.getByText(/File/)).toBeAttached();
            });

            test("table has pagination", async ({ page }) => {
                await expect(
                    page.getByTestId("Deliveries pagination"),
                ).toBeAttached();
            });

            test("has footer", async ({ page }) => {
                await expect(page.locator("footer")).toBeAttached();
            });
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await page.goto("/daily-data", {
                waitUntil: "domcontentloaded",
            });

            await waitForAPIResponse(page, "/api/waters/org/");
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Daily Data - ReportStream/);
        });

        test("has filter", async ({ page }) => {
            await expect(page.getByText("From (Start Range):")).toBeAttached();
            await expect(page.getByText("Until (End Range):")).toBeAttached();
        });

        test("table has correct headers", async ({ page }) => {
            await expect(page.getByText(/Report ID/)).toBeAttached();
            await expect(page.getByText(/Available/)).toBeAttached();
            await expect(page.getByText(/Expires/)).toBeAttached();
            await expect(page.getByText(/Items/)).toBeAttached();
            await expect(page.getByText(/File/)).toBeAttached();
        });

        test("table has pagination", async ({ page }) => {
            await expect(
                page.getByTestId("Deliveries pagination"),
            ).toBeAttached();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test.beforeEach(async ({ page }) => {
            await page.goto("/daily-data", {
                waitUntil: "domcontentloaded",
            });

            await waitForAPIResponse(page, "/api/waters/org/");
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Daily Data - ReportStream/);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });
});
