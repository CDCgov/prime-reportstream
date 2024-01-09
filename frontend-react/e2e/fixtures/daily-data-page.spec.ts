import { test, expect } from "@playwright/test";

import { Utils } from "../helpers/utils";

test.describe("Daily data page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await page.goto("/daily-data");
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("authenticated", () => {
        test.use({ storageState: "playwright/.auth/admin.json" });

        test.beforeEach(async ({ page }) => {
            const utils = new Utils(page);
            await utils.selectTestOrg();

            await page.goto("/daily-data");
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Daily Data - ReportStream/);
        });

        test("has receiver services dropdown", async ({ page }) => {
            await expect(page.getByTestId("services-dropdown")).toBeTruthy();
        });

        test("has filter", async ({ page }) => {
            await expect(page.getByText("From (Start Range):")).toBeTruthy();
            await expect(page.getByText("Until (End Range):")).toBeTruthy();
        });

        test("table has correct headers", async ({ page }) => {
            await expect(page.getByText("Report ID")).toBeTruthy();
            await expect(page.getByText("Available")).toBeTruthy();
            await expect(page.getByText("Expires")).toBeTruthy();
            await expect(page.getByText("Items")).toBeTruthy();
            await expect(page.getByText("File")).toBeTruthy();
        });

        test("table has pagination", async ({ page }) => {
            await expect(
                page.getByTestId("Deliveries pagination"),
            ).toBeTruthy();
            await expect(page.getByTestId("Page 1")).toBeTruthy();
            await expect(page.getByTestId("Page 2")).toBeTruthy();
            await expect(page.getByTestId("Page 3")).toBeTruthy();
            await expect(page.getByTestId("Page 4")).toBeTruthy();
            await expect(page.getByTestId("Page 5")).toBeTruthy();
            await expect(page.getByTestId("Page 6")).toBeTruthy();
            await expect(page.getByTestId("Next page")).toBeTruthy();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeTruthy();
        });
    });
});
