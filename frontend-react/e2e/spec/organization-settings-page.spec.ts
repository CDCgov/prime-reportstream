import { expect, test } from "@playwright/test";

test.use({ storageState: "e2e/.auth/admin.json" });

test("Has correct title", async ({ page }) => {
    await page.goto("/admin/settings", {
        waitUntil: "domcontentloaded",
    });

    await expect(page).toHaveTitle(/Admin-Organizations/);
});
