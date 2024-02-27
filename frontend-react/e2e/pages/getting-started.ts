import { expect, Page } from "@playwright/test";

export async function goto(page: Page) {
    await page.goto("/getting-started", {
        waitUntil: "domcontentloaded",
    });
}
export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/getting-started/);
    await expect(page).toHaveTitle(/Getting started/);
}
