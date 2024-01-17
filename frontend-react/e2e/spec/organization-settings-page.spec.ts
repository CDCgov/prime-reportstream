import { expect, test } from "@playwright/test";

test.use({ storageState: "playwright/.auth/admin.json" });

// eslint-disable-next-line playwright/no-skipped-test
test.skip("Has correct title", async ({ page }) => {
    await page.goto("/admin/settings");

    await expect(page).toHaveTitle(/Admin-Organizations/);
});
