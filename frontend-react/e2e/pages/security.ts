import { expect, Page } from "@playwright/test";

export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/security/);
    await expect(page).toHaveTitle(/ReportStream security/);
}
