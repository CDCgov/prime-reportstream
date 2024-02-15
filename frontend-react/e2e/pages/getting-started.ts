import { expect, Page } from "@playwright/test";

export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/getting-started/);
    await expect(page).toHaveTitle(/Getting started/);
}
