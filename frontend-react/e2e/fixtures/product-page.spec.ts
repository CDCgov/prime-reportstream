import { test, expect } from "@playwright/test";

test.describe("Product page", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/product/overview");
    });

    test("should have correct title", async ({ page }) => {
        await expect(page).toHaveTitle(/Product/);
    });

    test.describe("Side navigation", () => {
        test("should have Overview link", async ({ page }) => {
            await page
                .getByTestId("sidenav")
                .getByRole("link", { name: "Overview" })
                .click();

            await expect(page).toHaveURL(/.*product\/overview/);
        });

        test("should have Live link", async ({ page }) => {
            await page
                .getByTestId("sidenav")
                .getByRole("link", { name: /Where we\'re live/ })
                .click();

            await expect(page).toHaveURL(/.*product\/where-were-live/);
        });

        test("should have Release notes link", async ({ page }) => {
            await page
                .getByTestId("sidenav")
                .getByRole("link", { name: /Release notes/ })
                .click();

            await expect(page).toHaveURL(/.*product\/release-notes/);
        });

        test("should have About link", async ({ page }) => {
            await page
                .getByTestId("sidenav")
                .getByRole("link", { name: /About/ })
                .click();

            await expect(page).toHaveURL(/.*product\/about/);
        });
    });
});
