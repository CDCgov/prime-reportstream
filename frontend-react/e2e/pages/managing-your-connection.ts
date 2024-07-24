import { expect, Page } from "@playwright/test";

export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/managing-your-connection/);
    await expect(page).toHaveTitle(/Managing your connection/);
}
