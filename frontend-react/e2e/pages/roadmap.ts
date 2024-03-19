import { expect, Page } from "@playwright/test";

const URL_ROADMAP = "/about/roadmap";
export async function goto(page: Page) {
    await page.goto(URL_ROADMAP, {
        waitUntil: "domcontentloaded",
    });
}
export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/.*about\/roadmap/);
    await expect(page).toHaveTitle(/Product roadmap/);
}
