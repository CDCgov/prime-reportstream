import { test, expect } from "@playwright/test";

test.describe("Our network page", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/about/our-network");
    });

    test("has correct title", async ({ page }) => {
        await expect(page).toHaveTitle(/Our network/);
    });

    test.describe("Side navigation", () => {
        test("has Our network link", async ({ page }) => {
            await page
                .getByTestId("sidenav")
                .getByRole("link", { name: "Our network" })
                .click();

            await expect(page).toHaveURL(/.*about\/our-network/);
        });

        test("has News link", async ({ page }) => {
            await page
                .getByTestId("sidenav")
                .getByRole("link", { name: /News/ })
                .click();

            await expect(page).toHaveURL(/.*about\/news/);
        });

        test("has Case studies link", async ({ page }) => {
            await page
                .getByTestId("sidenav")
                .getByRole("link", { name: /Case studies/ })
                .click();

            await expect(page).toHaveURL(/.*about\/case-studies/);
        });

        test("has Security link", async ({ page }) => {
            await page
                .getByTestId("sidenav")
                .getByRole("link", { name: /Security/ })
                .click();

            await expect(page).toHaveURL(/.*about\/security/);
        });

        test("has Release notes link", async ({ page }) => {
            await page
                .getByTestId("sidenav")
                .getByRole("link", { name: /Release notes/ })
                .click();

            await expect(page).toHaveURL(/.*about\/release-notes/);
        });
    });
});
