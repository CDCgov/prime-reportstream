import { Page } from "@playwright/test";

export async function goto(page: Page) {
    await page.goto("/admin/settings", {
        waitUntil: "domcontentloaded",
    });
}
