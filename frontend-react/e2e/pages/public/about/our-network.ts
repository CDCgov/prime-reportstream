import { expect, Page } from "@playwright/test";

const URL_OUR_NETWORK = "/about/our-network";
export async function goto(page: Page) {
    await page.goto(URL_OUR_NETWORK, {
        waitUntil: "domcontentloaded",
    });
}
export async function onLoad(page: Page) {
    await expect(page).toHaveURL(/our-network/);
    await expect(page).toHaveTitle(/Our network/);
}

export async function clickOnLiveMap(page: Page) {
    await page.getByTestId("map").click();
    await expect(page).toHaveURL(URL_OUR_NETWORK);
}
