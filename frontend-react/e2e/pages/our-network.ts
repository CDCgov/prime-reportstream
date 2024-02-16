import { expect, Page } from "@playwright/test";

export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/our-network/);
    await expect(page).toHaveTitle(/Our network/);
}

export async function clickOnLiveMap(page: Page) {
    await page.getByTestId("map").click();
    await expect(page).toHaveURL("/about/our-network");
}
