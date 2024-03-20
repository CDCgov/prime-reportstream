import { expect, Page } from "@playwright/test";

export async function goto(page: Page) {
    await page.goto("/", {
        waitUntil: "domcontentloaded",
    });
}
export async function onLoad(page: Page) {
    await expect(page).toHaveTitle(
        /ReportStream - CDC's free, interoperable data transfer platform/,
    );
}
