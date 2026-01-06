import { expect, test } from "@playwright/test";

test("smoke: static homepage loads", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle(/ReportStream/i);
});
