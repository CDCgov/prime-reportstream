import { Page } from "@playwright/test";

const URL_DAILY_DATA = "/daily-data";
export async function goto(page: Page) {
    await page.goto(URL_DAILY_DATA, {
        waitUntil: "domcontentloaded",
    });
}
