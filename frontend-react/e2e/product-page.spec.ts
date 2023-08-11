import { test, expect } from "@playwright/test";

beforeEach(async ({ page }) => {
    await page.goto("/product/overview");
});

test("Has correct title", async ({ page }) => {
    await expect(page).toHaveTitle(/Product/);
});

test("has Product link", async ({ page }) => {
    await page.getByRole("link", { name: "Overview" }).click();

    await expect(page).toHaveURL(/.*product\/overview/);
});

test("has Live link", async ({ page }) => {
    await page.getByRole("link", { name: /Where we\'re live/ }).click();

    await expect(page).toHaveURL(/.*product\/where-were-live/);
});

test("has Release notes link", async ({ page }) => {
    await page.getByRole("link", { name: /Release notes/ }).click();

    await expect(page).toHaveURL(/.*product\/release-notes/);
});

test("has About link", async ({ page }) => {
    await page.getByRole("link", { name: /Release notes/ }).click();

    await expect(page).toHaveURL(/.*product\/release-notes/);
});
