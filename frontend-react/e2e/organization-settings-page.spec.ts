import { test, expect } from "@playwright/test";

test.use({ storageState: "playwright/.auth/admin.json" });

test("Has correct title", async ({ page }) => {
    await page.goto("/admin/settings");

    await expect(page).toHaveTitle(/Admin-Organizations/);
});
