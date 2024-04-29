import { expect, test } from "@playwright/test";
import * as organization from "../pages/organization";

test.use({ storageState: "e2e/.auth/admin.json" });

test("Has correct title", async ({ page }) => {
    await organization.goto(page);
    await page.locator(".usa-table tbody").waitFor({ state: "visible" });

    await expect(page).toHaveURL(/settings/);
    await expect(page).toHaveTitle(/Admin-Organizations/);
});
