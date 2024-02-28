import { expect, Page } from "@playwright/test";

export async function goto(page: Page) {
    await page.goto("/admin/settings", {
        waitUntil: "domcontentloaded",
    });
}
export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/settings/);
    await expect(page).toHaveTitle(/Admin-Organizations/);
}
