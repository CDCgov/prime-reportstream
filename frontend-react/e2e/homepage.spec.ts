import { test, expect } from "@playwright/test";

test("Has correct title", async ({ page }) => {
    await page.goto("/");

    await expect(page).toHaveTitle(/CDC Prime ReportStream/);
});

test("has Product link", async ({ page }) => {
    await page.goto("/");

    await page
        .getByTestId("header")
        .getByRole("link", { name: "Product" })
        .click();

    await expect(page).toHaveURL(/.*product\/overview/);
});

test("has Resources link", async ({ page }) => {
    await page.goto("/");

    await page
        .getByTestId("header")
        .getByRole("link", { name: "Resources" })
        .click();

    await expect(page).toHaveURL(/.*resources/);
});

test("has Support link", async ({ page }) => {
    await page.goto("/");

    await page
        .getByTestId("header")
        .getByRole("link", { name: "Support" })
        .click();
    await expect(page).toHaveURL(/.*support/);
});

test('has Resources link', async ({ page }) => {
  await page.goto('/');

  await page.getByRole('link', { name: 'Resources' }).click();

  await expect(page).toHaveURL(/.*resources/);
});

test('has Support link', async ({ page }) => {
  await page.goto('/');

  await page.getByRole('link', { name: 'Support' }).click();

  await expect(page).toHaveURL(/.*support/);
});

