import { Page } from "@playwright/test";

export const URL_ROADMAP = "/about/roadmap";
export async function goto(page: Page) {
    await page.goto(URL_ROADMAP, {
        waitUntil: "domcontentloaded",
    });
}
