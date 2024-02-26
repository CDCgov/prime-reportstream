import { expect, Page } from "@playwright/test";

export async function goto(page: Page) {
    await page.goto("/support", {
        waitUntil: "domcontentloaded",
    });
}
export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/support/);
    await expect(page).toHaveTitle(/ReportStream support/);
}
