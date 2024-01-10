import { test, expect } from "@playwright/test";

import { Utils } from "../helpers/utils";

test.describe("Daily data page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await page.goto("/daily-data");
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("admin test", () => {
        test.use({ storageState: "playwright/.auth/admin.json" });

        test.describe("without org selected", () => {
            test.beforeEach(async ({ page }) => {
                await page.goto("/daily-data");
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
                const utils = new Utils(page);
                await utils.selectTestOrg();

                await page.goto("/daily-data");
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
                await expect(page.getByText("Report ID")).toBeAttached();
                await expect(page.getByText("Available")).toBeAttached();
                await expect(page.getByText("Expires")).toBeAttached();
                await expect(page.getByText("Items")).toBeAttached();
                await expect(page.getByText("File")).toBeAttached();
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

    // TODO: receiver test

    // TODO: sender test
});
