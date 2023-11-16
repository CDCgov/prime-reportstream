import { test, expect } from "@playwright/test";

test.describe("Homepage", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/");
    });
    test("Has correct title", async ({ page }) => {
        await expect(page).toHaveTitle(/CDC Prime ReportStream/);
    });

    test("has About link", async ({ page }) => {
        await page
            .getByTestId("header")
            .getByTestId("navDropDownButton")
            .click();

        await expect(page.getByText("About ReportStream")).toBeTruthy();
        await expect(page.getByText("Our network")).toBeTruthy();
        await expect(page.getByText("News")).toBeTruthy();
        await expect(page.getByText("Case studies")).toBeTruthy();
        await expect(page.getByText("Security")).toBeTruthy();
        await expect(page.getByText("Release notes")).toBeTruthy();
    });

    test("has Getting Started link", async ({ page }) => {
        await page
            .getByTestId("header")
            .getByRole("link", { name: "Getting Started" })
            .click();

        await expect(page).toHaveURL(/getting-started/);
    });

    test("has Developers link", async ({ page }) => {
        await page
            .getByTestId("header")
            .getByRole("link", { name: "Developers" })
            .click();
        await expect(page).toHaveURL(/.*developer-resources/);
    });

    test("has Your Connection link", async ({ page }) => {
        await page
            .getByTestId("header")
            .getByRole("link", { name: "Your Connection" })
            .click();
        await expect(page).toHaveURL(/.*managing-your-connection/);
    });

    test("has Support link", async ({ page }) => {
        await page
            .getByTestId("header")
            .getByRole("link", { name: "Support" })
            .click();
        await expect(page).toHaveURL(/.*support/);
    });
});
