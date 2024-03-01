import { expect, Page } from "@playwright/test";

export async function goto(page: Page) {
    await page.goto("/developer-resources", {
        waitUntil: "domcontentloaded",
    });
}
export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/developer-resources/);
    await expect(page).toHaveTitle(/Developer resources/);
}
