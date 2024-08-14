import { Page } from "@playwright/test";

export async function publicPageGoto(page: Page, path: string) {
    await page.goto(path, {
        waitUntil: "networkidle",
    });
}
