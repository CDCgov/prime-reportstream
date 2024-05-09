import { expect, test } from "@playwright/test";

import { tableRows } from "../helpers/utils";
import * as lastMileFailures from "../pages/last-mile-failures";

test.describe("Last Mile Failure page", () => {
    test.describe("not authenticated", () => {
        test("redirects to login", async ({ page }) => {
            await lastMileFailures.goto(page);
            await expect(page).toHaveURL("/login");
        });
    });

    test.describe("admin user - happy path", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.beforeEach(async ({ page }) => {
            // Mock the api call before navigating
            await lastMileFailures.mockGetSendFailuresResponse(page);
            await lastMileFailures.mockGetResendResponse(page);
            await lastMileFailures.goto(page);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Last mile failures/);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });

        test("table has correct headers", async ({ page }) => {
            await expect(page.locator(".column-header-text").nth(0)).toHaveText(
                /Failed At/,
            );
            await expect(page.locator(".column-header-text").nth(1)).toHaveText(
                /ReportId/,
            );
            await expect(page.locator(".column-header-text").nth(2)).toHaveText(
                /Receiver/,
            );
        });

        test("table column 'Failed At' has expected data", async ({ page }) => {
            await expect(
                tableRows(page).nth(0).locator("td").nth(0),
            ).toHaveText("Tue, 2/20/2024, 9:35 PM");
        });

        test("table column 'ReportId' will open a modal with report details", async ({
            page,
        }) => {
            const reportId = tableRows(page).nth(0).locator("td").nth(1);
            await expect(reportId).toContainText(
                /e5ce49c0-b230-4364-8230-964273249fa1/,
            );
            await reportId.click();

            const modal = page.getByTestId("modalWindow").nth(0);
            await expect(modal).toContainText(
                /Report ID:e5ce49c0-b230-4364-8230-964273249fa1/,
            );
        });

        test("table column 'Receiver' will open receiver edit page", async ({
            page,
        }) => {
            const receiver = tableRows(page).nth(0).locator("td").nth(2);
            await expect(receiver).toContainText(
                /flexion.etor-service-receiver-results/,
            );
            await receiver.click();

            await expect(page).toHaveURL(
                "/admin/orgreceiversettings/org/flexion/receiver/etor-service-receiver-results/action/edit",
            );
        });
    });

    test.describe("admin user - server error", () => {
        test.use({ storageState: "e2e/.auth/admin.json" });

        test.beforeEach(async ({ page }) => {
            await lastMileFailures.mockGetSendFailuresResponse(page, 500);
            await lastMileFailures.goto(page);
        });

        test("has correct title", async ({ page }) => {
            await expect(page).toHaveTitle(/Last mile failures/);
        });

        test("has alert", async ({ page }) => {
            await expect(page.getByTestId("alert")).toBeAttached();
            await expect(
                page.getByText(
                    /Our apologies, there was an error loading this content./,
                ),
            ).toBeAttached();
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("receiver user", () => {
        test.use({ storageState: "e2e/.auth/receiver.json" });

        test.beforeEach(async ({ page }) => {
            await lastMileFailures.goto(page);
        });

        test("returns Page Not Found", async ({ page }) => {
            await expect(page).toHaveTitle(/Page Not Found/);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });

    test.describe("sender user", () => {
        test.use({ storageState: "e2e/.auth/sender.json" });

        test.beforeEach(async ({ page }) => {
            await lastMileFailures.goto(page);
        });

        test("returns Page Not Found", async ({ page }) => {
            await expect(page).toHaveTitle(/Page Not Found/);
        });

        test("has footer", async ({ page }) => {
            await expect(page.locator("footer")).toBeAttached();
        });
    });
});
